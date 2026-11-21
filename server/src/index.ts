import { authenticate, safeEqual } from "./auth";
import { CRON_TASKS, type CronTask } from "./cron";
import type { Env } from "./env";
import { HttpError, json, readJson, requireString } from "./http";
import { notify } from "./notify";
import { handleWebhook, syncFromApp } from "./revenuecat";

/**
 * Worker `chat-simpapp-api`: el servidor de Chat SimpApp (reemplaza las Cloud Functions).
 *
 *   GET  /                       → salud
 *   POST /v1/tokens              → guarda el token FCM del teléfono        (app)
 *   POST /v1/tokens/delete       → borra el token al cerrar sesión         (app)
 *   POST /v1/presence            → "estoy en la app" (últ. vez)            (app)
 *   POST /v1/presence/query      → últ. vez de varias personas             (app)
 *   POST /v1/notify              → envía la notificación de algo publicado (app)
 *   POST /v1/pro/sync            → sincroniza Pro desde RevenueCat         (app, administración)
 *   POST /webhooks/revenuecat    → webhook de RevenueCat
 *   POST /cron/<tarea>           → tareas programadas (Worker compartido rj2-cron)
 */
export default {
  async fetch(req: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    const url = new URL(req.url);
    const path = url.pathname.replace(/\/+$/, "") || "/";
    try {
      if (req.method === "GET" && path === "/") return json({ ok: true, app: "chat-simpapp" });
      if (req.method !== "POST") throw new HttpError(405, "Método no permitido");

      if (path === "/webhooks/revenuecat") {
        if (!safeEqual(req.headers.get("authorization") ?? "", env.REVENUECAT_WEBHOOK_AUTH ?? "")) {
          throw new HttpError(401, "No autorizado");
        }
        const synced = await handleWebhook(env, await readJson(req));
        return json({ ok: true, synced });
      }

      if (path.startsWith("/cron/")) {
        if (!safeEqual(req.headers.get("x-cron-secret") ?? "", env.CRON_SECRET ?? "")) throw new HttpError(401, "No autorizado");
        const task = path.slice("/cron/".length) as CronTask;
        const run = CRON_TASKS[task];
        if (!run) throw new HttpError(404, "Tarea desconocida");
        return json({ ok: true, task, result: await run(env) });
      }

      const uid = await authenticate(req, env);
      const body = await readJson<Record<string, unknown>>(req);

      switch (path) {
        case "/v1/tokens": {
          const token = requireString(body.token, "token", 4096);
          await env.DB.prepare("INSERT OR REPLACE INTO fcm_tokens (token, uid, updated_at) VALUES (?, ?, ?)")
            .bind(token, uid, Date.now()).run();
          return json({ ok: true });
        }
        case "/v1/tokens/delete": {
          const token = requireString(body.token, "token", 4096);
          await env.DB.prepare("DELETE FROM fcm_tokens WHERE token = ? AND uid = ?").bind(token, uid).run();
          return json({ ok: true });
        }
        case "/v1/presence": {
          await env.DB.prepare("INSERT OR REPLACE INTO presence (uid, last_seen) VALUES (?, ?)").bind(uid, Date.now()).run();
          return json({ ok: true });
        }
        case "/v1/presence/query": {
          const uids = (Array.isArray(body.uids) ? body.uids : []).filter((u): u is string => typeof u === "string").slice(0, 50);
          if (uids.length === 0) return json({ lastSeen: {} });
          const rows = await env.DB.prepare(`SELECT uid, last_seen FROM presence WHERE uid IN (${uids.map(() => "?").join(",")})`)
            .bind(...uids).all<{ uid: string; last_seen: number }>();
          return json({ lastSeen: Object.fromEntries(rows.results.map((r) => [r.uid, r.last_seen])) });
        }
        case "/v1/notify":
          return json({ ok: true, ...(await notify(env, uid, body)) });
        case "/v1/pro/sync":
          return json({ ok: true, ...(await syncFromApp(env, uid, body)) });
        default:
          throw new HttpError(404, "No encontrado");
      }
    } catch (e) {
      if (e instanceof HttpError) return json({ error: e.message }, e.status);
      console.error(e);
      return json({ error: "Error interno" }, 500);
    }
  },
} satisfies ExportedHandler<Env>;
