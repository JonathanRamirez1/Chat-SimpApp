export class HttpError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

export const json = (data: unknown, status = 200) =>
  new Response(JSON.stringify(data), { status, headers: { "content-type": "application/json; charset=utf-8" } });

export async function readJson<T>(req: Request): Promise<T> {
  try {
    return (await req.json()) as T;
  } catch {
    throw new HttpError(400, "JSON inválido");
  }
}

export function requireString(value: unknown, name: string, max = 200): string {
  if (typeof value !== "string" || value.length === 0 || value.length > max || value.includes("/")) {
    throw new HttpError(400, `Falta o es inválido: ${name}`);
  }
  return value;
}
