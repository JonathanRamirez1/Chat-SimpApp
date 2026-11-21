-- Tokens de notificación (FCM) por usuario. Reemplaza users/{uid}/private/fcm de Firestore.
CREATE TABLE IF NOT EXISTS fcm_tokens (
  token TEXT PRIMARY KEY,
  uid TEXT NOT NULL,
  updated_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_fcm_tokens_uid ON fcm_tokens(uid);

-- Última vez que cada persona abrió la app ("últ. vez hace…"). Reemplaza Realtime Database.
CREATE TABLE IF NOT EXISTS presence (
  uid TEXT PRIMARY KEY,
  last_seen INTEGER NOT NULL
);

-- Evita enviar dos veces la misma notificación (reintentos de la app).
CREATE TABLE IF NOT EXISTS notified (
  key TEXT PRIMARY KEY,
  at INTEGER NOT NULL
);

-- Caché pequeño (token de acceso de Google para FCM y Firestore).
CREATE TABLE IF NOT EXISTS kv (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL,
  expires_at INTEGER NOT NULL
);
