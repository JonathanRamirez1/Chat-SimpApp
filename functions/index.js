/**
 * Backend de Chat SimpApp (Cloud Functions, Node 22).
 *
 * - Notificaciones push: mensajes de chats privados/grupos, avisos oficiales, emergencias,
 *   votaciones nuevas y cambios de estado de reportes. El chat general del edificio NO envía
 *   push a propósito: es justamente el ruido que los vecinos quieren evitar.
 * - Pagos: valida suscripciones de Google Play y activa el plan Pro de la comunidad.
 *
 * Se usa FCM HTTP v1 a través del Admin SDK (la API heredada con "server key" fue apagada en 2024).
 */
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onMessagePublished } = require("firebase-functions/v2/pubsub");
const { logger } = require("firebase-functions");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue, Timestamp } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { google } = require("googleapis");

initializeApp();
const db = getFirestore();

const PACKAGE_NAME = "com.jonathan.chatsimpapp";
const PRO_PRODUCT_ID = "comunidad_pro";
const RTDN_TOPIC = "play-billing";

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
// Pagos: suscripción "Comunidad Pro"
// ---------------------------------------------------------------------------

const publisher = google.androidpublisher({
  version: "v3",
  auth: new google.auth.GoogleAuth({ scopes: ["https://www.googleapis.com/auth/androidpublisher"] }),
});

const ACTIVE_STATES = new Set(["SUBSCRIPTION_STATE_ACTIVE", "SUBSCRIPTION_STATE_IN_GRACE_PERIOD"]);

/** Consulta la suscripción en Google Play y actualiza el plan de la comunidad que la pagó. */
async function syncSubscription(purchaseToken, expectedCommunityId, expectedUid) {
  const { data: sub } = await publisher.purchases.subscriptionsv2.get({ packageName: PACKAGE_NAME, token: purchaseToken });

  const item = (sub.lineItems || []).find((li) => li.productId === PRO_PRODUCT_ID);
  if (!item) throw new HttpsError("invalid-argument", "La compra no es del plan Pro.");

  const ids = sub.externalAccountIdentifiers || {};
  const communityId = ids.obfuscatedExternalProfileId;
  if (!communityId) throw new HttpsError("failed-precondition", "La compra no está asociada a una comunidad.");
  if (expectedCommunityId && communityId !== expectedCommunityId) {
    throw new HttpsError("permission-denied", "La compra es de otra comunidad.");
  }
  if (expectedUid && ids.obfuscatedExternalAccountId !== expectedUid) {
    throw new HttpsError("permission-denied", "La compra la hizo otra persona.");
  }

  const active = ACTIVE_STATES.has(sub.subscriptionState);
  const expiry = item.expiryTime ? Timestamp.fromDate(new Date(item.expiryTime)) : null;
  await db.doc(`communities/${communityId}`).update({
    plan: active ? "pro" : "free",
    planExpiresAt: expiry,
    planPurchaseToken: purchaseToken,
    planUpdatedAt: FieldValue.serverTimestamp(),
  });

  // Google cancela y reembolsa las compras que no se confirman en 3 días.
  if (active && sub.acknowledgementState === "ACKNOWLEDGEMENT_STATE_PENDING") {
    await publisher.purchases.subscriptions.acknowledge({
      packageName: PACKAGE_NAME,
      subscriptionId: PRO_PRODUCT_ID,
      token: purchaseToken,
      requestBody: {},
    });
  }
  logger.info(`comunidad ${communityId}: ${sub.subscriptionState}, vence ${item.expiryTime}`);
  return { active, communityId, expiresAt: item.expiryTime || null };
}

/** La app llama a esto después de una compra (o al abrirse, para reflejar renovaciones). */
exports.verifyProSubscription = onCall(async (request) => {
  const uid = request.auth && request.auth.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Inicia sesión.");
  const { communityId, purchaseToken } = request.data || {};
  if (typeof communityId !== "string" || typeof purchaseToken !== "string") {
    throw new HttpsError("invalid-argument", "Faltan datos.");
  }
  const community = (await db.doc(`communities/${communityId}`).get()).data();
  if (!community) throw new HttpsError("not-found", "La comunidad no existe.");
  if (!(community.admins || []).includes(uid)) {
    throw new HttpsError("permission-denied", "Solo la administración puede activar Pro.");
  }
  return syncSubscription(purchaseToken, communityId, uid);
});

/**
 * Notificaciones en tiempo real de Google Play (renovación, cancelación, reembolso...).
 * Configura en Play Console → Monetización → Configuración el tema Pub/Sub "play-billing".
 */
exports.playBillingNotifications = onMessagePublished(RTDN_TOPIC, async (event) => {
  const payload = event.data.message.json || {};
  const token = payload.subscriptionNotification && payload.subscriptionNotification.purchaseToken;
  if (!token) return;
  try {
    await syncSubscription(token);
  } catch (e) {
    logger.error("No se pudo sincronizar la suscripción", e);
  }
});
