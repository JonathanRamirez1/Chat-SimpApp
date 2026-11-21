import { SignJWT, importPKCS8 } from "jose";
import type { Env } from "./env";

interface ServiceAccount {
  client_email: string;
  private_key: string;
}

const SCOPES = [
  "https://www.googleapis.com/auth/firebase.messaging",
  "https://www.googleapis.com/auth/datastore",
].join(" ");

let memory: { token: string; expiresAt: number } | null = null;

/**
 * Token de acceso de Google con la cuenta de servicio (para FCM HTTP v1 y Firestore REST).
 * Se guarda en memoria y en D1 para no firmar uno nuevo en cada petición (ahorra CPU).
 */
export async function googleAccessToken(env: Env): Promise<string> {
  const now = Date.now();
  if (memory && memory.expiresAt - 300_000 > now) return memory.token;

  const cached = await env.DB.prepare("SELECT value, expires_at FROM kv WHERE key = 'google_token'").first<{ value: string; expires_at: number }>();
  if (cached && cached.expires_at - 300_000 > now) {
    memory = { token: cached.value, expiresAt: cached.expires_at };
    return cached.value;
  }

  const sa = JSON.parse(env.FIREBASE_SERVICE_ACCOUNT) as ServiceAccount;
  const key = await importPKCS8(sa.private_key, "RS256");
  const iat = Math.floor(now / 1000);
  const assertion = await new SignJWT({ scope: SCOPES })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuer(sa.client_email)
    .setAudience("https://oauth2.googleapis.com/token")
    .setIssuedAt(iat)
    .setExpirationTime(iat + 3600)
    .sign(key);

  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }),
  });
  if (!res.ok) throw new Error(`OAuth de Google falló: ${res.status}`);
  const data = (await res.json()) as { access_token: string; expires_in: number };
  const expiresAt = now + data.expires_in * 1000;
  memory = { token: data.access_token, expiresAt };
  await env.DB.prepare("INSERT OR REPLACE INTO kv (key, value, expires_at) VALUES ('google_token', ?, ?)")
    .bind(data.access_token, expiresAt)
    .run();
  return data.access_token;
}
