import type { Env } from "./env";
import { encode, getDoc, commit } from "./firestore";
import { googleAccessToken } from "./google";

/**
 * Eliminar la cuenta (exigido por Google Play). Borra:
 *  - el perfil `users/{uid}` y la membresía en cada comunidad (y te saca de los chats generales y de la administración);
 *  - los tokens de notificación y la "últ. vez" en D1;
 *  - el usuario de Firebase Authentication.
 * Los mensajes ya enviados quedan en sus chats sin tu nombre ("Usuario desconocido").
 * La suscripción Pro es de la comunidad, no de la persona: no se cancela aquí.
 */
export async function deleteAccount(env: Env, uid: string): Promise<{ communities: number }> {
  const user = await getDoc(env, `users/${uid}`);
  const communities = ((user?.communities as string[] | undefined) ?? []).filter((c) => typeof c === "string" && !c.includes("/"));
  const base = `projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents`;
  const remove = (doc: string, field: string) => ({
    transform: {
      document: `${base}/${doc}`,
      fieldTransforms: [{ fieldPath: field, removeAllFromArray: { values: [encode(uid)] } }],
    },
  });

  const writes: unknown[] = [];
  for (const cid of communities) {
    writes.push({ delete: `${base}/communities/${cid}/members/${uid}` });
    writes.push(remove(`communities/${cid}`, "admins"));
    writes.push(remove(`chats/${cid}`, "members"));
  }
  writes.push({ delete: `${base}/users/${uid}` });
  // Las transformaciones fallan si el documento no existe; se aplican por separado para no bloquear el resto.
  await commit(env, writes.filter((w) => "delete" in (w as object)));
  for (const w of writes.filter((w) => "transform" in (w as object))) {
    await commit(env, [w]).catch(() => undefined);
  }

  await env.DB.batch([
    env.DB.prepare("DELETE FROM fcm_tokens WHERE uid = ?").bind(uid),
    env.DB.prepare("DELETE FROM presence WHERE uid = ?").bind(uid),
  ]);

  const token = await googleAccessToken(env);
  const res = await fetch(`https://identitytoolkit.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/accounts:delete`, {
    method: "POST",
    headers: { authorization: `Bearer ${token}`, "content-type": "application/json" },
    body: JSON.stringify({ localId: uid }),
  });
  if (!res.ok && res.status !== 400) throw new Error(`Auth delete ${res.status}`);
  return { communities: communities.length };
}
