/**
 * Backend de Chat SimpApp (Cloud Functions, Node 22).
 *
 * - Notificaciones push: mensajes de chats privados/grupos, avisos oficiales, emergencias,
 *   votaciones nuevas y cambios de estado de reportes. El chat general del edificio NO envía
 *   push a propósito: es justamente el ruido que los vecinos quieren evitar.
 * - Alertas del barrio: avisa a los edificios cercanos (solo alertas de seguridad).
 * - Pagos: sincroniza el plan Pro de cada comunidad desde RevenueCat.
 *
 * Se usa FCM HTTP v1 a través del Admin SDK (la API heredada con "server key" fue apagada en 2024).
 */
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { onCall, onRequest, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { logger } = require("firebase-functions");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue, Timestamp } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { geohashQueryBounds, distanceBetween } = require("geofire-common");

initializeApp();
const db = getFirestore();


const INVALID_TOKEN_ERRORS = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
  "messaging/invalid-argument",
]);

const EMERGENCY_LABELS = {
  water: "Fuga o corte de agua",
  gas: "Olor a gas",
  fire: "Incendio o humo",
  security: "Seguridad",
  power: "Corte de luz",
  other: "Emergencia",
};

const STATUS_LABELS = { open: "Abierto", in_progress: "En proceso", resolved: "Resuelto" };

// ---------------------------------------------------------------------------
// Utilidades de envío
// ---------------------------------------------------------------------------

/** Tokens FCM de una lista de usuarios, recordando de quién es cada uno para limpiarlos luego. */
async function tokensFor(uids) {
  if (uids.length === 0) return { tokens: [], owners: {} };
  const docs = await db.getAll(...uids.map((uid) => db.doc(`users/${uid}/private/fcm`)));
  const tokens = [];
  const owners = {};
  for (const doc of docs) {
    for (const token of doc.get("tokens") || []) {
      tokens.push(token);
      owners[token] = doc.ref;
    }
  }
  return { tokens, owners };
}

/** Envía un mensaje "data" a muchos usuarios (en lotes de 500) y borra tokens inválidos. */
async function sendToUsers(uids, data, highPriority = true) {
  const { tokens, owners } = await tokensFor([...new Set(uids)]);
  let sent = 0;
  const cleanup = [];
  for (let i = 0; i < tokens.length; i += 500) {
    const batch = tokens.slice(i, i + 500);
    const res = await getMessaging().sendEachForMulticast({
      tokens: batch,
      data,
      android: { priority: highPriority ? "high" : "normal" },
    });
    sent += res.successCount;
    res.responses.forEach((r, j) => {
      if (!r.success && r.error && INVALID_TOKEN_ERRORS.has(r.error.code)) {
        cleanup.push(owners[batch[j]].update({ tokens: FieldValue.arrayRemove(batch[j]) }));
      }
    });
  }
  await Promise.all(cleanup);
  return sent;
}

async function memberUids(cid, filter = () => true) {
  const snap = await db.collection(`communities/${cid}/members`).get();
  return snap.docs.filter((d) => filter(d.data())).map((d) => d.id);
}

// ---------------------------------------------------------------------------
// Chats
// ---------------------------------------------------------------------------

exports.onMessageCreated = onDocumentCreated("chats/{chatId}/messages/{messageId}", async (event) => {
  const message = event.data && event.data.data();
  if (!message) return;
  const { chatId } = event.params;

  const chat = (await db.doc(`chats/${chatId}`).get()).data();
  if (!chat || chat.type === "community") return; // el chat general del edificio no notifica

  const recipients = (chat.members || []).filter((uid) => uid !== message.senderId);
  const sender = (await db.doc(`users/${message.senderId}`).get()).data() || {};
  const isGroup = chat.type === "group";
  const senderName = sender.displayName || "";

  const sent = await sendToUsers(recipients, {
    kind: "message",
    chatId,
    senderId: message.senderId,
    senderName,
    senderPhoto: sender.photoUrl || "",
    title: isGroup ? chat.name || "" : senderName,
    body: (message.text || "").slice(0, 300),
    hasImage: message.imageUrl ? "1" : "0",
    isGroup: isGroup ? "1" : "0",
  });
  logger.info(`chat ${chatId}: ${sent} notificaciones`);
});

// ---------------------------------------------------------------------------
// Comunidad: avisos, emergencias, votaciones y reportes
// ---------------------------------------------------------------------------

exports.onAnnouncementCreated = onDocumentCreated("communities/{cid}/announcements/{aid}", async (event) => {
  const a = event.data && event.data.data();
  if (!a) return;
  const { cid, aid } = event.params;
  const community = (await db.doc(`communities/${cid}`).get()).data() || {};
  const emergency = a.type === "emergency";

  const recipients = await memberUids(cid);
  const title = emergency
    ? `🚨 ${EMERGENCY_LABELS[a.emergencyKind] || "Emergencia"} · ${community.name || ""}`
    : `📢 ${a.title}`;
  const body = emergency
    ? [a.body, `Reportado por ${a.authorUnit}`].filter(Boolean).join(" · ")
    : (a.body || "").slice(0, 300) || community.name || "";

  const sent = await sendToUsers(
    recipients.filter((uid) => uid !== a.authorId),
    { kind: emergency ? "emergency" : "notice", key: `a-${aid}`, communityId: cid, title, body },
  );
  logger.info(`aviso ${aid} (${a.type}) en ${cid}: ${sent} notificaciones`);
});

exports.onPollCreated = onDocumentCreated("communities/{cid}/polls/{pid}", async (event) => {
  const poll = event.data && event.data.data();
  if (!poll) return;
  const { cid, pid } = event.params;
  const recipients = (await memberUids(cid)).filter((uid) => uid !== poll.createdBy);
  await sendToUsers(
    recipients,
    { kind: "poll", key: `p-${pid}`, communityId: cid, title: "🗳️ Nueva votación", body: poll.question || "" },
    false,
  );
});

exports.onReportUpdated = onDocumentUpdated("communities/{cid}/reports/{rid}", async (event) => {
  const before = event.data.before.data();
  const after = event.data.after.data();
  if (!before || !after || before.status === after.status) return;
  const { cid, rid } = event.params;

  // Avisa solo a las unidades afectadas por el reporte.
  const affected = new Set(after.affectedUnits || []);
  const recipients = await memberUids(cid, (m) => affected.has(m.unit));
  const last = (after.history || []).slice(-1)[0] || {};
  await sendToUsers(
    recipients,
    {
      kind: "report",
      key: `r-${rid}`,
      communityId: cid,
      title: `🛠️ ${after.title}: ${STATUS_LABELS[after.status] || after.status}`,
      body: last.note || "La administración actualizó el reporte.",
    },
    false,
  );
});

// ---------------------------------------------------------------------------
// Alertas del barrio
// ---------------------------------------------------------------------------

const SECTOR_RADIUS_M = 2000;
const ALERT_LABELS = {
  theft: "Robo o peligro",
  streetlight: "Luz apagada",
  road: "Daño en la calle",
  water: "Fuga de agua",
  trash: "Basura",
  noise: "Ruido",
};
// Solo las alertas de seguridad suenan; las demás se ven en el mapa sin molestar.
const PUSH_ALERT_TYPES = new Set(["theft"]);

/** Comunidades cuyo edificio está a menos de `radius` metros del punto. */
async function communitiesNear(lat, lng, radius) {
  const bounds = geohashQueryBounds([lat, lng], radius);
  const snaps = await Promise.all(
    bounds.map(([start, end]) => db.collection("communities").orderBy("geohash").startAt(start).endAt(end).get()),
  );
  const found = new Map();
  for (const snap of snaps) {
    for (const doc of snap.docs) {
      const c = doc.data();
      if (typeof c.lat !== "number" || typeof c.lng !== "number") continue;
      const meters = distanceBetween([lat, lng], [c.lat, c.lng]) * 1000;
      if (meters <= radius) found.set(doc.id, { id: doc.id, name: c.name || "", meters });
    }
  }
  return [...found.values()];
}

exports.onAlertCreated = onDocumentCreated("alerts/{alertId}", async (event) => {
  const alert = event.data && event.data.data();
  if (!alert || !PUSH_ALERT_TYPES.has(alert.type)) return;
  const { alertId } = event.params;

  const nearby = await communitiesNear(alert.lat, alert.lng, SECTOR_RADIUS_M);
  let sent = 0;
  for (const c of nearby) {
    const recipients = (await memberUids(c.id)).filter((uid) => uid !== alert.createdBy);
    const blocks = Math.max(1, Math.round(c.meters / 100));
    sent += await sendToUsers(recipients, {
      kind: "alert",
      key: `al-${alertId}`,
      communityId: c.id,
      title: `🛡️ ${ALERT_LABELS[alert.type] || "Alerta"} cerca de ${c.name}`,
      body: [`A ${blocks} ${blocks === 1 ? "cuadra" : "cuadras"}`, alert.description].filter(Boolean).join(" · "),
    });
  }
  logger.info(`alerta ${alertId}: ${nearby.length} comunidades, ${sent} notificaciones`);
});

// ---------------------------------------------------------------------------
// Pagos: suscripción "Comunidad Pro" con RevenueCat
// ---------------------------------------------------------------------------
//
// El cliente de RevenueCat es la comunidad: App User ID = "community_<id>".
// Entitlement "pro". Productos de Play "comunidad_pro:<rango>-mensual|anual" (u30, u80, u200, u500, u5000).
// Las compras de prueba (sandbox) se tratan igual que las reales: nunca se bloquean.

const REVENUECAT_SECRET_KEY = defineSecret("REVENUECAT_SECRET_KEY");
const REVENUECAT_WEBHOOK_AUTH = defineSecret("REVENUECAT_WEBHOOK_AUTH");
const ENTITLEMENT = "pro";
const TIERS = { u30: 30, u80: 80, u200: 200, u500: 500, u5000: 5000 };

/** "comunidad_pro:u80-mensual" -> 80 unidades. */
function maxUnitsFor(productId) {
  const basePlan = (productId || "").split(":")[1] || productId || "";
  return TIERS[basePlan.split("-")[0]] || 0;
}

/** Lee el estado real en RevenueCat y lo copia a Firestore (fuente de verdad para las reglas). */
async function syncCommunity(communityId) {
  const appUserId = `community_${communityId}`;
  const res = await fetch(`https://api.revenuecat.com/v1/subscribers/${encodeURIComponent(appUserId)}`, {
    headers: { Authorization: `Bearer ${REVENUECAT_SECRET_KEY.value()}` },
  });
  if (!res.ok) throw new Error(`RevenueCat ${res.status}`);
  const { subscriber } = await res.json();
  const ent = subscriber && subscriber.entitlements && subscriber.entitlements[ENTITLEMENT];
  const expires = ent && ent.expires_date ? new Date(ent.expires_date) : null;
  const active = !!expires && expires.getTime() > Date.now();

  await db.doc(`communities/${communityId}`).update({
    plan: active ? "pro" : "free",
    planExpiresAt: expires ? Timestamp.fromDate(expires) : null,
    planMaxUnits: ent ? maxUnitsFor(ent.product_identifier) : 0,
    planUpdatedAt: FieldValue.serverTimestamp(),
  });
  logger.info(`comunidad ${communityId}: pro=${active} hasta ${expires} (${ent && ent.product_identifier})`);
  return { active, expiresAt: expires ? expires.toISOString() : null };
}

/** La app lo llama después de comprar o al abrir la pantalla de Pro. */
exports.syncProPlan = onCall({ secrets: [REVENUECAT_SECRET_KEY] }, async (request) => {
  const uid = request.auth && request.auth.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Inicia sesión.");
  const { communityId } = request.data || {};
  if (typeof communityId !== "string") throw new HttpsError("invalid-argument", "Falta la comunidad.");
  const community = (await db.doc(`communities/${communityId}`).get()).data();
  if (!community) throw new HttpsError("not-found", "La comunidad no existe.");
  if (!(community.admins || []).includes(uid)) throw new HttpsError("permission-denied", "Solo la administración.");
  return syncCommunity(communityId);
});

/**
 * Webhook de RevenueCat (renovaciones, cancelaciones, reembolsos, compras de prueba).
 * En RevenueCat → Integrations → Webhooks: URL de esta función y el mismo valor en "Authorization header".
 */
exports.revenueCatWebhook = onRequest({ secrets: [REVENUECAT_SECRET_KEY, REVENUECAT_WEBHOOK_AUTH] }, async (req, res) => {
  if (req.get("Authorization") !== REVENUECAT_WEBHOOK_AUTH.value()) {
    res.status(401).send("unauthorized");
    return;
  }
  const event = (req.body && req.body.event) || {};
  const ids = [event.app_user_id, event.original_app_user_id, ...(event.aliases || []), ...(event.transferred_to || [])]
    .filter((id) => typeof id === "string" && id.startsWith("community_"));
  try {
    await Promise.all([...new Set(ids)].map((id) => syncCommunity(id.slice("community_".length))));
    res.status(200).send("ok");
  } catch (e) {
    logger.error("Webhook de RevenueCat falló", e);
    res.status(500).send("error"); // RevenueCat reintenta
  }
});
