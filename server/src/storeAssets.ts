// TEMPORAL: imágenes de la ficha de Play Console. Quitar este archivo, src/store-assets/,
// la regla [[rules]] de wrangler.toml y la ruta en index.ts cuando Jonathan avise.
import icon from "./store-assets/icon.png";
import feature from "./store-assets/feature.png";
import shot1 from "./store-assets/shot1.png";
import shot2 from "./store-assets/shot2.png";

const FILES: Record<string, ArrayBuffer> = {
  "/store-assets/icon.png": icon,
  "/store-assets/feature.png": feature,
  "/store-assets/shot1.png": shot1,
  "/store-assets/shot2.png": shot2,
};

export function storeAsset(path: string): Response | null {
  const body = FILES[path];
  if (!body) return null;
  return new Response(body, {
    headers: {
      "Content-Type": "image/png",
      "Access-Control-Allow-Origin": "*",
      "Cache-Control": "public, max-age=300",
    },
  });
}
