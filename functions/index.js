/**
 * Envía una notificación push a los demás miembros del chat cada vez que se crea un mensaje.
 *
 * Reemplaza el envío desde la app con la "server key" (API heredada de FCM, apagada por Google
 * en 2024). Aquí se usa la API HTTP v1 a través del Admin SDK, con credenciales del servidor.
 */
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { logger } = require("firebase-functions");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const INVALID_TOKEN_ERRORS = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
  "messaging/invalid-argument",
]);

exports.onMessageCreated = onDocumentCreated("chats/{chatId}/messages/{messageId}", async (event) => {
  const message = event.data && event.data.data();
  if (!message) return;

  const { chatId } = event.params;
  const db = getFirestore();

  const chat = (await db.doc(`chats/${chatId}`).get()).data();
  if (!chat) return;

  const recipients = (chat.members || []).filter((uid) => uid !== message.senderId);
  if (recipients.length === 0) return;

  const sender = (await db.doc(`users/${message.senderId}`).get()).data() || {};
  const tokenDocs = await db.getAll(...recipients.map((uid) => db.doc(`users/${uid}/private/fcm`)));

  const tokens = [];
  const owners = {};
  for (const doc of tokenDocs) {
    for (const token of doc.get("tokens") || []) {
      tokens.push(token);
      owners[token] = doc.ref;
    }
  }
  if (tokens.length === 0) return;

  const isGroup = chat.type === "group";
  const senderName = sender.displayName || "";
  const response = await getMessaging().sendEachForMulticast({
    tokens,
    // Mensaje solo de datos: la app arma la notificación (avatar, agrupado por chat, etc.).
    data: {
      chatId,
      senderId: message.senderId,
      senderName,
      senderPhoto: sender.photoUrl || "",
      title: isGroup ? chat.name || "" : senderName,
      body: (message.text || "").slice(0, 300),
      hasImage: message.imageUrl ? "1" : "0",
      isGroup: isGroup ? "1" : "0",
    },
    android: { priority: "high" },
  });

  // Limpia los tokens de teléfonos donde la app ya no está instalada.
  const cleanup = [];
  response.responses.forEach((result, i) => {
    if (!result.success && result.error && INVALID_TOKEN_ERRORS.has(result.error.code)) {
      cleanup.push(owners[tokens[i]].update({ tokens: FieldValue.arrayRemove(tokens[i]) }));
    }
  });
  await Promise.all(cleanup);
  logger.info(`chat ${chatId}: ${response.successCount} enviados, ${cleanup.length} tokens eliminados`);
});
