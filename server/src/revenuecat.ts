import { ACCEPT_SANDBOX_EVENTS, PRO_ENTITLEMENT, TIERS } from "./config";
import type { Env } from "./env";
import { getDoc, patchDoc } from "./firestore";
import { HttpError, requireString } from "./http";

/**
 * Plan Pro con RevenueCat. El cliente de RevenueCat es la COMUNIDAD (App User ID
 * `community_<id>`). El Worker lee el estado real en RevenueCat y lo copia a Firestore,
 * que es lo que usan las reglas de seguridad y la app.
 */

const PREFIX = "community_";

/** "comunidad_pro:u80-mensual" → 80 unidades (0 si no se reconoce). */
export function maxUnitsFor(productId: string | undefined): number {
  const id = productId ?? "";
  const basePlan = id.includes(":") ? id.split(":")[1] : id;
  return TIERS[basePlan.split("-")[0]] ?? 0;
}

export interface EntitlementState {
  active: boolean;
  expiresAt: number | null;
  maxUnits: number;
}

export function entitlementState(subscriber: unknown, now = Date.now()): EntitlementState {
  const ent = (subscriber as { entitlements?: Record<string, { expires_date?: string | null; product_identifier?: string }> })
    ?.entitlements?.[PRO_ENTITLEMENT];
  if (!ent) return { active: false, expiresAt: null, maxUnits: 0 };
  const expiresAt = ent.expires_date ? Date.parse(ent.expires_date) : null;
  return { active: expiresAt !== null && expiresAt > now, expiresAt, maxUnits: maxUnitsFor(ent.product_identifier) };
}

export async function syncCommunity(env: Env, communityId: string): Promise<EntitlementState> {
  const res = await fetch(`https://api.revenuecat.com/v1/subscribers/${encodeURIComponent(PREFIX + communityId)}`, {
    headers: { authorization: `Bearer ${env.REVENUECAT_SECRET_KEY}` },
  });
  if (!res.ok) throw new Error(`RevenueCat ${res.status}`);
  const { subscriber } = (await res.json()) as { subscriber: unknown };
  const state = entitlementState(subscriber);
  await patchDoc(env, `communities/${communityId}`, {
    plan: state.active ? "pro" : "free",
    planExpiresAt: state.expiresAt ? new Date(state.expiresAt) : null,
    planMaxUnits: state.maxUnits,
    planUpdatedAt: new Date(),
  });
  return state;
}

/** La app lo llama después de comprar o al abrir la pantalla de Pro. Solo administración. */
export async function syncFromApp(env: Env, uid: string, body: Record<string, unknown>) {
  const communityId = requireString(body.communityId, "communityId");
  const community = await getDoc(env, `communities/${communityId}`);
  if (!community) throw new HttpError(404, "La comunidad no existe");
  if (!((community.admins as string[] | undefined) ?? []).includes(uid)) throw new HttpError(403, "Solo la administración");
  return syncCommunity(env, communityId);
}

/** Comunidades a las que se refiere un evento del webhook. */
export function communitiesInEvent(event: Record<string, unknown>): string[] {
  const ids = [event.app_user_id, event.original_app_user_id, ...((event.aliases as unknown[]) ?? []), ...((event.transferred_to as unknown[]) ?? [])];
  return [...new Set(ids.filter((id): id is string => typeof id === "string" && id.startsWith(PREFIX)).map((id) => id.slice(PREFIX.length)))];
}

/** Webhook de RevenueCat (renovaciones, cancelaciones, reembolsos y compras de prueba). */
export async function handleWebhook(env: Env, payload: { event?: Record<string, unknown> }): Promise<number> {
  const event = payload.event ?? {};
  if (event.environment === "SANDBOX" && !ACCEPT_SANDBOX_EVENTS) return 0;
  const ids = communitiesInEvent(event);
  await Promise.all(ids.map((id) => syncCommunity(env, id)));
  return ids.length;
}
