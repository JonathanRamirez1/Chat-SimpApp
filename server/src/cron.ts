import type { Env } from "./env";
import { deleteDocs, query } from "./firestore";

/**
 * Tareas programadas. Esta app NO crea Cron Triggers propios (la cuenta solo tiene 5):
 * las dispara el Worker compartido `rj2-cron` con `POST /cron/<tarea>` y el header
 * `X-Cron-Secret`. Frecuencia sugerida en el README.
 */
export const CRON_TASKS = {
  /** Cada hora: borra de Firestore las alertas del barrio vencidas (reemplaza el TTL). */
  "cleanup-alerts": cleanupExpiredAlerts,
  /** Una vez al día: limpia registros viejos de D1. */
  "cleanup-db": cleanupDb,
} as const;

export type CronTask = keyof typeof CRON_TASKS;

async function cleanupExpiredAlerts(env: Env): Promise<{ deleted: number }> {
  const expired = await query(env, "", "alerts", [{ field: "expiresAt", op: "LESS_THAN", value: new Date() }], 200);
  await deleteDocs(env, expired.map((d) => d._name!).filter(Boolean));
  return { deleted: expired.length };
}

async function cleanupDb(env: Env): Promise<{ deleted: number }> {
  const twoDays = Date.now() - 2 * 86_400_000;
  const sixMonths = Date.now() - 180 * 86_400_000;
  const results = await env.DB.batch([
    env.DB.prepare("DELETE FROM notified WHERE at < ?").bind(twoDays),
    env.DB.prepare("DELETE FROM fcm_tokens WHERE updated_at < ?").bind(sixMonths),
    env.DB.prepare("DELETE FROM kv WHERE expires_at < ?").bind(Date.now()),
  ]);
  return { deleted: results.reduce((n, r) => n + (r.meta.changes ?? 0), 0) };
}
