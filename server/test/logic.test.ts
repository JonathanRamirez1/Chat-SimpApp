import { describe, expect, it } from "vitest";
import { safeEqual } from "../src/auth";
import { ACCEPT_SANDBOX_EVENTS } from "../src/config";
import { sanitizeTopicPart, topicForCommunity, topicForUnit } from "../src/fcm";
import { decodeFields, encode } from "../src/firestore";
import worker from "../src/index";
import { communitiesInEvent, entitlementState, handleWebhook, maxUnitsFor } from "../src/revenuecat";
import type { Env } from "../src/env";

describe("reglas de RJ2", () => {
  it("acepta compras de prueba (sandbox)", () => {
    expect(ACCEPT_SANDBOX_EVENTS).toBe(true);
  });
});

describe("RevenueCat", () => {
  it("lee el rango de unidades del producto de Play", () => {
    expect(maxUnitsFor("comunidad_pro:u80-mensual")).toBe(80);
    expect(maxUnitsFor("comunidad_pro:u5000-anual")).toBe(5000);
    expect(maxUnitsFor("otro")).toBe(0);
  });

  it("calcula si Pro está activo", () => {
    const now = Date.parse("2026-11-21T00:00:00Z");
    const sub = { entitlements: { pro: { expires_date: "2026-12-21T00:00:00Z", product_identifier: "comunidad_pro:u30-mensual" } } };
    expect(entitlementState(sub, now)).toEqual({ active: true, expiresAt: Date.parse("2026-12-21T00:00:00Z"), maxUnits: 30 });
    expect(entitlementState(sub, Date.parse("2027-01-01T00:00:00Z")).active).toBe(false);
    expect(entitlementState({ entitlements: {} }, now).active).toBe(false);
  });

  it("encuentra las comunidades de un evento (incluidas transferencias)", () => {
    expect(communitiesInEvent({ app_user_id: "community_A", aliases: ["community_A", "$RCAnonymousID:x"], transferred_to: ["community_B"] }))
      .toEqual(["A", "B"]);
  });

  it("procesa eventos SANDBOX (no los ignora)", async () => {
    const calls: string[] = [];
    const env = { REVENUECAT_SECRET_KEY: "sk" } as Env;
    const realFetch = globalThis.fetch;
    globalThis.fetch = (async (url: string) => {
      calls.push(String(url));
      throw new Error("sin red en pruebas");
    }) as typeof fetch;
    await expect(handleWebhook(env, { event: { environment: "SANDBOX", app_user_id: "community_X" } })).rejects.toThrow();
    globalThis.fetch = realFetch;
    expect(calls[0]).toContain("community_X");
  });
});

describe("FCM", () => {
  it("arma temas válidos", () => {
    expect(topicForCommunity("abc123")).toBe("c_abc123");
    expect(topicForUnit("abc", "Casa 12/B")).toBe("c_abc_u_CASA_12_B");
    expect(sanitizeTopicPart("t2-301")).toBe("T2-301");
  });
});

describe("Firestore REST", () => {
  it("codifica y decodifica valores", () => {
    const fields = encode({ a: "x", n: 3, d: 1.5, b: true, arr: ["u1"], when: new Date("2026-01-01T00:00:00Z"), none: null }) as {
      mapValue: { fields: Record<string, Record<string, unknown>> };
    };
    expect(decodeFields(fields.mapValue.fields)).toEqual({ a: "x", n: 3, d: 1.5, b: true, arr: ["u1"], when: Date.parse("2026-01-01T00:00:00Z"), none: null });
  });
});

describe("router", () => {
  const env = { REVENUECAT_WEBHOOK_AUTH: "secreto", CRON_SECRET: "cron" } as Env;
  const ctx = {} as ExecutionContext;

  it("responde salud", async () => {
    const res = await worker.fetch(new Request("https://x/"), env, ctx);
    expect(await res.json()).toEqual({ ok: true, app: "chat-simpapp" });
  });

  it("rechaza el webhook sin el header correcto", async () => {
    const res = await worker.fetch(new Request("https://x/webhooks/revenuecat", { method: "POST", body: "{}" }), env, ctx);
    expect(res.status).toBe(401);
  });

  it("rechaza tareas programadas sin la clave", async () => {
    const res = await worker.fetch(new Request("https://x/cron/cleanup-db", { method: "POST" }), env, ctx);
    expect(res.status).toBe(401);
  });

  it("exige sesión para las rutas de la app", async () => {
    const res = await worker.fetch(new Request("https://x/v1/notify", { method: "POST", body: "{}" }), env, ctx);
    expect(res.status).toBe(401);
  });

  it("compara secretos de forma segura", () => {
    expect(safeEqual("abc", "abc")).toBe(true);
    expect(safeEqual("abc", "abd")).toBe(false);
    expect(safeEqual("", "")).toBe(false);
  });
});
