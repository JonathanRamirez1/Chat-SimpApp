# Chat SimpApp — instrucciones del proyecto

App Android nativa (Kotlin, Jetpack Compose, Hilt, Firebase Spark + Cloudflare) para comunidades de vecinos.
Ver README.md para arquitectura y monetización, y PENDIENTES.md para lo que falta configurar.

## Regla: actualización obligatoria

- La app SIEMPRE debe mantener la actualización forzada **IMMEDIATE** de Google Play In-App Updates
  (`com.google.android.play:app-update` + `app-update-ktx`), implementada en
  `app/src/main/java/com/jonathan/chatsimpapp/update/ForcedUpdateManager.kt` y conectada en `MainActivity`
  (`onCreate` → revisa y lanza el flujo; `onResume` → relanza si quedó `DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS`).
- Si el usuario cancela o la actualización falla, la app queda bloqueada en `UpdateRequiredScreen`
  ("Debes actualizar para continuar") con un botón que relanza el flujo o abre la ficha de Play.
  No se puede usar la app con una versión vieja.
- Respaldo: Remote Config `min_version_code`. Si `BuildConfig.VERSION_CODE` es menor, la app se bloquea igual.
- **No se puede quitar, desactivar ni saltar este mecanismo, y no se puede publicar ninguna versión sin él.**
  Cualquier cambio en `MainActivity` o en el arranque de la app debe conservarlo.
- Pruebas: `ForcedUpdateTest` (con `FakeAppUpdateManager`). En dispositivo, In-App Updates solo funciona con la
  app instalada desde Google Play (pista de prueba interna o Internal App Sharing).

## Regla: compras de prueba del dueño

Jonathan (ramirezjj201@gmail.com) SIEMPRE debe poder hacer compras de prueba reales de Google Play (sin cobro)
en prueba interna, cerrada y producción. Nunca bloquear, ignorar ni revertir compras sandbox: RevenueCat debe
otorgar el entitlement `pro` y el Worker (`/v1/pro/sync` y `/webhooks/revenuecat`) debe activarlo igual que una
compra real: `ACCEPT_SANDBOX_EVENTS = true` en `server/src/config.ts` (lo revisa `scripts/check-release.sh`). En Play Console la lista de testers con licencia ("Prueba Paxora Plus") debe seguir marcada.

## Regla: backend (esquema nuevo de RJ2)

- Firebase en **plan Spark, sin cuenta de facturación**. Prohibido: Cloud Functions, Cloud Storage, Realtime
  Database (100 conexiones), login por teléfono, Identity Platform, Google Maps (pide facturación).
- Lo que necesite servidor va en **Cloudflare** (cuenta rj2techsolutions@gmail.com): Worker `chat-simpapp-api`
  (carpeta `server/`) y D1 `chat-simpapp`. Notificaciones con FCM HTTP v1 desde el Worker.
- **Sin Cron Triggers propios**: las tareas programadas son handlers `POST /cron/<tarea>` que llama el Worker
  compartido `rj2-cron`.
- Login **solo con Google** (no hay uso sin cuenta; para comprar Pro hace falta Google).
- Sin subida de archivos/fotos (no hay Storage). Mapa con MapLibre + OpenFreeMap (atribución OSM visible).
- Secretos en Workers Secrets (`wrangler secret put`), nunca en el repo ni en el chat. Despliegue con
  `npx wrangler deploy` usando `CLOUDFLARE_API_TOKEN` y `CLOUDFLARE_ACCOUNT_ID` del entorno.
- Firestore Spark ≈ 50.000 lecturas/día: evitar escuchas innecesarias y consultas sin límite.

## Cuentas (RJ2)

- Firebase (Spark), RevenueCat y Cloudflare: rj2techsolutions@gmail.com. Play Console y AdMob: cuenta personal.
- Paquete (applicationId): `com.rj2techsolutions.chatsimpapp`, proyecto Firebase `chat-simpapp-efb77`.
  El namespace del código sigue siendo `com.jonathan.chatsimpapp` (no afecta a Play ni a Firebase).
- Correo público de la app: chatsimpapp@rj2techsolutions.com. Nunca el correo personal en nada público.

## Notas de compilación

- Pruebas: `./gradlew testDebugUnitTest` · reglas de Firestore: `cd rules-tests && npm test` · Worker: `cd server && npm test`.
- Antes de publicar: `./scripts/check-release.sh`.
- Claves locales en `~/.gradle/gradle.properties` (nunca en el repo): `revenueCatApiKey`, ids de AdMob.
- Subir `versionCode` en `app/build.gradle.kts` en cada publicación.
