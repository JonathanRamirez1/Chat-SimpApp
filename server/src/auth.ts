import { createRemoteJWKSet, jwtVerify } from "jose";
import type { Env } from "./env";
import { HttpError } from "./http";

// Las claves públicas se cachean en memoria del isolate (jose respeta los headers de caché).
const firebaseJwks = createRemoteJWKSet(
  new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"),
);
const appCheckJwks = createRemoteJWKSet(new URL("https://firebaseappcheck.googleapis.com/v1/jwks"));

/**
 * Valida el idToken de Firebase Auth (y App Check si está activado) y devuelve el uid.
 * La app manda: `Authorization: Bearer <idToken>` y `X-Firebase-AppCheck: <token>`.
 */
export async function authenticate(req: Request, env: Env): Promise<string> {
  const header = req.headers.get("authorization") ?? "";
  const idToken = header.startsWith("Bearer ") ? header.slice(7) : "";
  if (!idToken) throw new HttpError(401, "Falta el token de sesión");

  let uid: string;
  try {
    const { payload } = await jwtVerify(idToken, firebaseJwks, {
      issuer: `https://securetoken.google.com/${env.FIREBASE_PROJECT_ID}`,
      audience: env.FIREBASE_PROJECT_ID,
    });
    if (!payload.sub) throw new Error("sin sub");
    uid = payload.sub;
  } catch {
    throw new HttpError(401, "Sesión inválida o vencida");
  }

  if (env.APP_CHECK_ENFORCE === "true") {
    const token = req.headers.get("x-firebase-appcheck") ?? "";
    try {
      await jwtVerify(token, appCheckJwks, {
        issuer: `https://firebaseappcheck.googleapis.com/${env.FIREBASE_PROJECT_NUMBER}`,
        audience: `projects/${env.FIREBASE_PROJECT_NUMBER}`,
      });
    } catch {
      throw new HttpError(401, "App Check inválido");
    }
  }
  return uid;
}

/** Compara secretos sin filtrar información por el tiempo de respuesta. */
export function safeEqual(a: string, b: string): boolean {
  if (!a || !b || a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}
