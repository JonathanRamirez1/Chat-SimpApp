# Pendientes para publicar Chat SimpApp

Esquema nuevo de RJ2: **Firebase en plan Spark (sin facturación) + Cloudflare**. No se crea cuenta de
facturación "RJ2 Apps N" ni alerta de 20.000 COP para esta app.

## Ya hecho (código)

- [x] App Android sin Storage, Realtime Database, Cloud Functions ni Google Maps; login solo con Google.
- [x] Worker `chat-simpapp-api` desplegado: https://chat-simpapp-api.rj2techsolutions.workers.dev
- [x] Base D1 `chat-simpapp` con su esquema (tokens FCM, últ. vez, envíos, caché).
- [x] Reglas de Firestore probadas (19 pruebas), pruebas de la app (41) y del Worker (12).
- [x] `ACCEPT_SANDBOX_EVENTS = true` en `server/src/config.ts`; `./scripts/check-release.sh`.

## 1. Correo y Firebase (cuenta rj2techsolutions@gmail.com)

- [ ] Crear `chatsimpapp@rj2techsolutions.com` en Spaceship (reenvío a rj2techsolutions@gmail.com).
- [ ] Crear el proyecto Firebase **en plan Spark** (no vincular facturación).
- [ ] Agregar la app Android con paquete **`com.rj2techsolutions.chatsimpapp`** y las huellas SHA-1 (depuración
      y la de firma de Play). **Pasarle a Claude el `google-services.json`** para cambiar el paquete en el código.
- [ ] Authentication → habilitar **Google** (solo Google).
- [ ] Crear **Firestore** (modo producción).
- [ ] **App Check** → registrar la app con **Play Integrity**; en depuración, agregar el token de prueba de Logcat.
- [ ] **AI Logic** → Comenzar → proveedor **Gemini Developer API** (gratis en Spark).
- [ ] **Remote Config** → crear `min_version_code` (0) y, si se quiere, `ai_model`.
- [ ] Desplegar: `firebase deploy --only firestore,hosting` (reglas, índices y páginas legales).
- [ ] Cuenta de servicio: Configuración del proyecto → Cuentas de servicio → **Generar nueva clave privada**
      (JSON). Se usa solo como secreto del Worker; **nunca** en el repo ni en el chat.

## 2. Cloudflare (Worker `chat-simpapp-api`)

- [ ] En `server/wrangler.toml` poner `FIREBASE_PROJECT_ID` y `FIREBASE_PROJECT_NUMBER` del proyecto nuevo
      (o decirle a Claude los valores, no son secretos).
- [ ] Cargar secretos desde el panel de Cloudflare (Workers → chat-simpapp-api → Settings → Variables) o con
      `npx wrangler secret put`: `FIREBASE_SERVICE_ACCOUNT`, `REVENUECAT_SECRET_KEY`, `REVENUECAT_WEBHOOK_AUTH`,
      `CRON_SECRET`.
- [ ] Volver a desplegar (`cd server && npx wrangler deploy`).
- [ ] Cuando App Check funcione: `APP_CHECK_ENFORCE = "true"` y desplegar.
- [ ] **rj2-cron**: registrar `POST /cron/cleanup-alerts` (cada hora) y `POST /cron/cleanup-db` (diario) con el
      header `X-Cron-Secret`.

## 3. RevenueCat (proyecto en rj2techsolutions@gmail.com)

- [ ] Crear el proyecto y conectar la app de Play (credenciales de cuenta de servicio de Play).
- [ ] Entitlement **`pro`**.
- [ ] Offerings **`u30`, `u80`, `u200`, `u500`, `u5000`**, cada uno con paquete **Monthly** y **Annual**
      (productos `comunidad_pro:<rango>-mensual` / `-anual`), todos dando `pro`.
- [ ] Webhook → `https://chat-simpapp-api.rj2techsolutions.workers.dev/webhooks/revenuecat` con el mismo valor de
      `REVENUECAT_WEBHOOK_AUTH` en *Authorization header*.
- [ ] Clave pública `goog_…` en `~/.gradle/gradle.properties` como `revenueCatApiKey`.

## 4. Play Console (cuenta personal)

- [ ] Crear la app con el paquete `com.rj2techsolutions.chatsimpapp`.
- [ ] Ficha: correo de contacto `chatsimpapp@rj2techsolutions.com`, sitio web https://rj2techsolutions.com.
- [ ] Suscripción **`comunidad_pro`** con planes base `u30-mensual`, `u30-anual`, `u80-mensual`, `u80-anual`,
      `u200-…`, `u500-…`, `u5000-…` (precios con coma decimal; sugeridos en el README).
- [ ] Licencia para testing: lista "Prueba Paxora Plus" marcada (`RESPOND_NORMALLY`).
- [ ] Subir a **prueba interna**; probar la actualización obligatoria y una compra de prueba real.
- [ ] Formulario de seguridad de datos: ubicación aproximada (alertas del barrio), nombre/correo de Google,
      mensajes. Sin fotos.

## 5. AdMob (cuenta personal)

- [ ] App + 4 bloques: banner, apertura de app, intersticial y recompensado. Ids en `~/.gradle/gradle.properties`.
- [ ] Mensaje de consentimiento (Privacidad y mensajes).

## 6. Páginas legales (Firebase Hosting)

- [ ] `hosting/privacy-policy.html`, `terms.html` y `delete-data.html` con el diseño de Paxora y el correo
      `chatsimpapp@rj2techsolutions.com`. (Claude las puede escribir cuando exista el proyecto.)
- [ ] Agregar la app y su política al sitio rj2techsolutions.com.

## 7. A vigilar

- **Firestore Spark ≈ 50.000 lecturas al día** (≈ 150 vecinos activos al día). Revisar el uso semanalmente.
- **Cloudflare gratis: 100.000 peticiones al día compartidas entre todas las apps.** Cuando la suma se acerque,
  pasar la cuenta a Workers Paid (5 USD/mes).
