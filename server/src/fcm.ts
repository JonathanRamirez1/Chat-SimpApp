import type { Env } from "./env";
import { googleAccessToken } from "./google";

/** Nombres de temas FCM (la app se suscribe a los mismos; ver TopicSubscriptions.kt). */
export const topicForCommunity = (cid: string) => `c_${cid}`;
export const topicForUnit = (cid: string, unit: string) => `c_${cid}_u_${sanitizeTopicPart(unit)}`;

/** FCM acepta [a-zA-Z0-9-_.~%] en los temas. */
export function sanitizeTopicPart(value: string): string {
  return value.trim().toUpperCase().replace(/[^A-Z0-9\-_.~]/g, "_").slice(0, 40);
}

export type Data = Record<string, string>;

type Target = { token: string } | { topic: string };

/** Envía un mensaje de datos (la app arma la notificación). Devuelve false si el token ya no sirve. */
async function send(env: Env, target: Target, data: Data, highPriority: boolean): Promise<boolean> {
  const token = await googleAccessToken(env);
  const res = await fetch(`https://fcm.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/messages:send`, {
    method: "POST",
    headers: { authorization: `Bearer ${token}`, "content-type": "application/json" },
    body: JSON.stringify({ message: { ...target, data, android: { priority: highPriority ? "HIGH" : "NORMAL" } } }),
  });
  if (res.ok) return true;
  if (res.status === 404 || res.status === 400) return false; // UNREGISTERED / token inválido
  throw new Error(`FCM ${res.status}`);
}

export async function sendToTopics(env: Env, topics: string[], data: Data, highPriority = true): Promise<number> {
  const results = await Promise.allSettled(topics.map((topic) => send(env, { topic }, data, highPriority)));
  return results.filter((r) => r.status === "fulfilled" && r.value).length;
}

/** Envía a tokens concretos y borra de D1 los que FCM rechaza. */
export async function sendToTokens(env: Env, tokens: string[], data: Data): Promise<number> {
  const results = await Promise.allSettled(tokens.map((token) => send(env, { token }, data, true)));
  const invalid = tokens.filter((_, i) => {
    const r = results[i];
    return r.status === "fulfilled" && r.value === false;
  });
  if (invalid.length > 0) {
    await env.DB.batch(invalid.map((t) => env.DB.prepare("DELETE FROM fcm_tokens WHERE token = ?").bind(t)));
  }
  return results.filter((r) => r.status === "fulfilled" && r.value).length;
}
