import { distanceBetween, geohashQueryBounds } from "geofire-common";
import { MAX_DIRECT_SENDS, MAX_TOPIC_SENDS, PUSH_ALERT_TYPES, SECTOR_RADIUS_M } from "./config";
import type { Env } from "./env";
import { sendToTokens, sendToTopics, topicForCommunity, topicForUnit } from "./fcm";
import { getDoc, query } from "./firestore";
import { HttpError, requireString } from "./http";

/**
 * Notificaciones. Firestore no puede llamar al Worker por su cuenta (eso era de Cloud Functions),
 * así que la app avisa justo después de publicar. Por eso el Worker SIEMPRE comprueba en Firestore
 * que lo publicado existe y es de quien llama: nadie puede mandar notificaciones falsas.
 *
 * - Chats privados y grupos: a los tokens de cada miembro (guardados en D1).
 * - Todo lo de la comunidad (avisos, emergencias, votaciones, reportes, alertas): a temas FCM
 *   (`c_<comunidad>` y `c_<comunidad>_u_<unidad>`), un solo envío sin importar cuántos vecinos haya.
 * - El chat general del edificio NO notifica (es el ruido que los vecinos quieren evitar).
 * - Cada mensaje lleva `authorId` para que el autor no reciba su propia notificación.
 */

type Body = Record<string, unknown>;

const EMERGENCY_LABELS: Record<string, string> = {
  water: "Fuga o corte de agua", gas: "Olor a gas", fire: "Incendio o humo",
  security: "Seguridad", power: "Corte de luz", other: "Emergencia",
};
const STATUS_LABELS: Record<string, string> = { open: "Abierto", in_progress: "En proceso", resolved: "Resuelto" };
const ALERT_LABELS: Record<string, string> = {
  theft: "Robo o peligro", streetlight: "Luz apagada", road: "Daño en la calle",
  water: "Fuga de agua", trash: "Basura", noise: "Ruido",
};
const RECENT_MS = 10 * 60 * 1000;

/** Marca la notificación como enviada; false si ya se había enviado (reintento). */
async function firstTime(env: Env, key: string): Promise<boolean> {
  const res = await env.DB.prepare("INSERT OR IGNORE INTO notified (key, at) VALUES (?, ?)").bind(key, Date.now()).run();
  return (res.meta.changes ?? 0) > 0;
}

function assertRecent(createdAt: unknown) {
  if (typeof createdAt === "number" && Date.now() - createdAt > RECENT_MS) throw new HttpError(409, "Demasiado viejo para notificar");
}

export async function notify(env: Env, uid: string, body: Body): Promise<{ sent: number }> {
  switch (body.kind) {
    case "message": return { sent: await notifyMessage(env, uid, body) };
    case "announcement": return { sent: await notifyAnnouncement(env, uid, body) };
    case "poll": return { sent: await notifyPoll(env, uid, body) };
    case "report": return { sent: await notifyReport(env, uid, body) };
    case "alert": return { sent: await notifyAlert(env, uid, body) };
    default: throw new HttpError(400, "Tipo de notificación desconocido");
  }
}

async function notifyMessage(env: Env, uid: string, body: Body): Promise<number> {
  const chatId = requireString(body.chatId, "chatId");
  const messageId = requireString(body.messageId, "messageId");
  const chat = await getDoc(env, `chats/${chatId}`);
  if (!chat) throw new HttpError(404, "Chat no encontrado");
  const members = (chat.members as string[] | undefined) ?? [];
  if (!members.includes(uid)) throw new HttpError(403, "No eres miembro");
  if (chat.type === "community") return 0;

  const message = await getDoc(env, `chats/${chatId}/messages/${messageId}`);
  if (!message || message.senderId !== uid) throw new HttpError(403, "El mensaje no es tuyo");
  assertRecent(message.sentAt);
  if (!(await firstTime(env, `m:${messageId}`))) return 0;

  const recipients = members.filter((m) => m !== uid);
  if (recipients.length === 0) return 0;
  const placeholders = recipients.map(() => "?").join(",");
  const rows = await env.DB.prepare(`SELECT token FROM fcm_tokens WHERE uid IN (${placeholders}) ORDER BY updated_at DESC LIMIT ?`)
    .bind(...recipients, MAX_DIRECT_SENDS)
    .all<{ token: string }>();

  const sender = (await getDoc(env, `users/${uid}`)) ?? {};
  const isGroup = chat.type === "group";
  const senderName = String(sender.displayName ?? "");
  return sendToTokens(env, rows.results.map((r) => r.token), {
    kind: "message",
    chatId,
    authorId: uid,
    senderName,
    senderPhoto: String(sender.photoUrl ?? ""),
    title: isGroup ? String(chat.name ?? "") : senderName,
    body: String(message.text ?? "").slice(0, 300),
    isGroup: isGroup ? "1" : "0",
  });
}

async function notifyAnnouncement(env: Env, uid: string, body: Body): Promise<number> {
  const cid = requireString(body.communityId, "communityId");
  const aid = requireString(body.announcementId, "announcementId");
  const a = await getDoc(env, `communities/${cid}/announcements/${aid}`);
  if (!a || a.authorId !== uid) throw new HttpError(403, "El aviso no es tuyo");
  assertRecent(a.createdAt);
  if (!(await firstTime(env, `a:${aid}`))) return 0;

  const community = (await getDoc(env, `communities/${cid}`)) ?? {};
  const emergency = a.type === "emergency";
  const title = emergency
    ? `🚨 ${EMERGENCY_LABELS[String(a.emergencyKind)] ?? "Emergencia"} · ${community.name ?? ""}`
    : `📢 ${a.title}`;
  const text = emergency
    ? [a.body, `Reportado por ${a.authorUnit}`].filter(Boolean).join(" · ")
    : String(a.body ?? "").slice(0, 300) || String(community.name ?? "");
  return sendToTopics(env, [topicForCommunity(cid)], {
    kind: emergency ? "emergency" : "notice", key: `a-${aid}`, communityId: cid, authorId: uid, title, body: text,
  });
}

async function notifyPoll(env: Env, uid: string, body: Body): Promise<number> {
  const cid = requireString(body.communityId, "communityId");
  const pid = requireString(body.pollId, "pollId");
  const poll = await getDoc(env, `communities/${cid}/polls/${pid}`);
  if (!poll || poll.createdBy !== uid) throw new HttpError(403, "La votación no es tuya");
  assertRecent(poll.createdAt);
  if (!(await firstTime(env, `p:${pid}`))) return 0;
  return sendToTopics(env, [topicForCommunity(cid)], {
    kind: "poll", key: `p-${pid}`, communityId: cid, authorId: uid, title: "🗳️ Nueva votación", body: String(poll.question ?? ""),
  }, false);
}

async function notifyReport(env: Env, uid: string, body: Body): Promise<number> {
  const cid = requireString(body.communityId, "communityId");
  const rid = requireString(body.reportId, "reportId");
  const community = await getDoc(env, `communities/${cid}`);
  if (!community || !((community.admins as string[] | undefined) ?? []).includes(uid)) {
    throw new HttpError(403, "Solo la administración");
  }
  const report = await getDoc(env, `communities/${cid}/reports/${rid}`);
  if (!report) throw new HttpError(404, "Reporte no encontrado");
  const history = (report.history as { note?: string }[] | undefined) ?? [];
  if (!(await firstTime(env, `r:${rid}:${report.status}:${history.length}`))) return 0;

  // Solo a las unidades afectadas por el reporte.
  const units = ((report.affectedUnits as string[] | undefined) ?? []).slice(0, MAX_TOPIC_SENDS);
  const note = history[history.length - 1]?.note;
  return sendToTopics(env, units.map((u) => topicForUnit(cid, u)), {
    kind: "report", key: `r-${rid}`, communityId: cid, authorId: uid,
    title: `🛠️ ${report.title}: ${STATUS_LABELS[String(report.status)] ?? report.status}`,
    body: note || "La administración actualizó el reporte.",
  }, false);
}

async function notifyAlert(env: Env, uid: string, body: Body): Promise<number> {
  const alertId = requireString(body.alertId, "alertId");
  const alert = await getDoc(env, `alerts/${alertId}`);
  if (!alert || alert.createdBy !== uid) throw new HttpError(403, "La alerta no es tuya");
  if (!PUSH_ALERT_TYPES.has(String(alert.type))) return 0;
  assertRecent(alert.createdAt);
  if (!(await firstTime(env, `al:${alertId}`))) return 0;

  const nearby = await communitiesNear(env, Number(alert.lat), Number(alert.lng));
  const label = ALERT_LABELS[String(alert.type)] ?? "Alerta";
  let sent = 0;
  for (const c of nearby.slice(0, MAX_TOPIC_SENDS)) {
    const blocks = Math.max(1, Math.round(c.meters / 100));
    sent += await sendToTopics(env, [topicForCommunity(c.id)], {
      kind: "alert", key: `al-${alertId}`, communityId: c.id, authorId: uid,
      title: `🛡️ ${label} cerca de ${c.name}`,
      body: [`A ${blocks} ${blocks === 1 ? "cuadra" : "cuadras"}`, alert.description].filter(Boolean).join(" · "),
    });
  }
  return sent;
}

/** Comunidades cuyo edificio está dentro del radio del barrio (búsqueda por geohash). */
export async function communitiesNear(env: Env, lat: number, lng: number, radius = SECTOR_RADIUS_M) {
  const bounds = geohashQueryBounds([lat, lng], radius);
  const results = await Promise.all(
    bounds.map(([start, end]) =>
      query(env, "", "communities", [
        { field: "geohash", op: "GREATER_THAN_OR_EQUAL", value: start },
        { field: "geohash", op: "LESS_THAN_OR_EQUAL", value: end },
      ], 100),
    ),
  );
  const found = new Map<string, { id: string; name: string; meters: number }>();
  for (const doc of results.flat()) {
    if (typeof doc.lat !== "number" || typeof doc.lng !== "number" || !doc._id) continue;
    const meters = distanceBetween([lat, lng], [doc.lat, doc.lng]) * 1000;
    if (meters <= radius) found.set(doc._id, { id: doc._id, name: String(doc.name ?? ""), meters });
  }
  return [...found.values()].sort((a, b) => a.meters - b.meters);
}
