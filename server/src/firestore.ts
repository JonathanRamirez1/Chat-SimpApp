import type { Env } from "./env";
import { googleAccessToken } from "./google";

/** Cliente mínimo de Firestore REST (la cuenta de servicio no pasa por las reglas de seguridad). */

type FsValue = Record<string, unknown>;
export type Doc = Record<string, unknown> & { _name?: string; _id?: string };

const base = (env: Env) =>
  `https://firestore.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents`;

export function decode(v: FsValue | undefined): unknown {
  if (!v) return undefined;
  if ("stringValue" in v) return v.stringValue;
  if ("integerValue" in v) return Number(v.integerValue);
  if ("doubleValue" in v) return Number(v.doubleValue);
  if ("booleanValue" in v) return v.booleanValue;
  if ("timestampValue" in v) return Date.parse(String(v.timestampValue));
  if ("nullValue" in v) return null;
  if ("arrayValue" in v) return ((v.arrayValue as { values?: FsValue[] }).values ?? []).map(decode);
  if ("mapValue" in v) return decodeFields((v.mapValue as { fields?: Record<string, FsValue> }).fields);
  if ("referenceValue" in v) return v.referenceValue;
  if ("geoPointValue" in v) return v.geoPointValue;
  return undefined;
}

export function decodeFields(fields: Record<string, FsValue> | undefined): Doc {
  const out: Doc = {};
  for (const [k, v] of Object.entries(fields ?? {})) out[k] = decode(v);
  return out;
}

export function encode(value: unknown): FsValue {
  if (value === null || value === undefined) return { nullValue: null };
  if (value instanceof Date) return { timestampValue: value.toISOString() };
  if (typeof value === "string") return { stringValue: value };
  if (typeof value === "boolean") return { booleanValue: value };
  if (typeof value === "number") return Number.isInteger(value) ? { integerValue: String(value) } : { doubleValue: value };
  if (Array.isArray(value)) return { arrayValue: { values: value.map(encode) } };
  if (typeof value === "object") {
    const fields: Record<string, FsValue> = {};
    for (const [k, v] of Object.entries(value as Record<string, unknown>)) fields[k] = encode(v);
    return { mapValue: { fields } };
  }
  throw new Error(`Tipo no soportado: ${typeof value}`);
}

async function call(env: Env, url: string, init: RequestInit = {}): Promise<Response> {
  const token = await googleAccessToken(env);
  return fetch(url, { ...init, headers: { ...(init.headers ?? {}), authorization: `Bearer ${token}`, "content-type": "application/json" } });
}

/** Lee un documento; null si no existe. `path` sin barra inicial, p. ej. "chats/abc". */
export async function getDoc(env: Env, path: string): Promise<Doc | null> {
  const res = await call(env, `${base(env)}/${path}`);
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`Firestore GET ${path}: ${res.status}`);
  const body = (await res.json()) as { name: string; fields?: Record<string, FsValue> };
  return { ...decodeFields(body.fields), _name: body.name, _id: body.name.split("/").pop() };
}

export interface FieldFilter {
  field: string;
  op: "EQUAL" | "LESS_THAN" | "LESS_THAN_OR_EQUAL" | "GREATER_THAN" | "GREATER_THAN_OR_EQUAL" | "ARRAY_CONTAINS";
  value: unknown;
}

/** Consulta una colección (o subcolección si `parent` no está vacío). */
export async function query(
  env: Env,
  parent: string,
  collectionId: string,
  filters: FieldFilter[] = [],
  limit = 200,
): Promise<Doc[]> {
  const where =
    filters.length === 0
      ? undefined
      : filters.length === 1
        ? { fieldFilter: toFilter(filters[0]) }
        : { compositeFilter: { op: "AND", filters: filters.map((f) => ({ fieldFilter: toFilter(f) })) } };
  const url = parent ? `${base(env)}/${parent}:runQuery` : `${base(env)}:runQuery`;
  const res = await call(env, url, {
    method: "POST",
    body: JSON.stringify({ structuredQuery: { from: [{ collectionId }], where, limit } }),
  });
  if (!res.ok) throw new Error(`Firestore query ${collectionId}: ${res.status}`);
  const rows = (await res.json()) as { document?: { name: string; fields?: Record<string, FsValue> } }[];
  return rows
    .filter((r) => r.document)
    .map((r) => ({ ...decodeFields(r.document!.fields), _name: r.document!.name, _id: r.document!.name.split("/").pop() }));
}

function toFilter(f: FieldFilter) {
  return { field: { fieldPath: f.field }, op: f.op, value: encode(f.value) };
}

/** Actualiza solo los campos indicados de un documento existente. */
export async function patchDoc(env: Env, path: string, data: Record<string, unknown>): Promise<void> {
  const mask = Object.keys(data).map((k) => `updateMask.fieldPaths=${encodeURIComponent(k)}`).join("&");
  const fields: Record<string, FsValue> = {};
  for (const [k, v] of Object.entries(data)) fields[k] = encode(v);
  const res = await call(env, `${base(env)}/${path}?${mask}&currentDocument.exists=true`, {
    method: "PATCH",
    body: JSON.stringify({ fields }),
  });
  if (!res.ok) throw new Error(`Firestore PATCH ${path}: ${res.status}`);
}

/** Borra varios documentos (nombres completos) en una sola escritura. */
export async function deleteDocs(env: Env, names: string[]): Promise<void> {
  if (names.length === 0) return;
  const res = await call(env, `${base(env)}:commit`, {
    method: "POST",
    body: JSON.stringify({ writes: names.map((name) => ({ delete: name })) }),
  });
  if (!res.ok) throw new Error(`Firestore commit: ${res.status}`);
}
