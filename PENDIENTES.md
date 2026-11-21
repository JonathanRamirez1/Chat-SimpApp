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

Proyecto: **`chat-simpapp-efb77`** (número 929638718992), plan Spark. App Android
`com.rj2techsolutions.chatsimpapp` (appId `1:929638718992:android:1b4b3f79f777b2aeb5f495`).

- [ ] Crear `chatsimpapp@rj2techsolutions.com` en Spaceship (reenvío a rj2techsolutions@gmail.com).
- [x] Proyecto Firebase en plan Spark.
- [x] App Android `com.rj2techsolutions.chatsimpapp`.
- [x] Agregar las huellas en Configuración del proyecto → la app Android:

      | Clave | SHA-1 | SHA-256 |
      |---|---|---|
      | Debug (`app/debug.keystore`, en el repo) | `52:F3:D8:E3:A8:79:7C:69:36:A9:CD:91:39:83:A5:28:FB:D2:64:71` | `A7:6B:A2:F4:2A:2C:DA:2B:AD:6F:73:A5:2C:D6:54:EA:68:7C:A4:45:81:B8:A8:D1:FE:8E:55:05:14:EF:4C:69` |
      | Subida (upload, fuera del repo) | `FB:78:51:42:97:CE:13:E6:60:43:5A:16:6A:AA:EE:E5:19:27:7B:19` | `74:71:7A:F9:CF:DA:A2:DA:95:C9:08:04:5C:EC:62:90:DD:AA:EA:FA:92:C7:7B:1D:85:83:0A:38:A0:E4:D3:D5` |
      | Firma de la app de Play (App Signing) | se ve en Play Console → Integridad de la app, tras la primera subida | ídem |

- [ ] **Pasarle a Claude el `google-services.json`** para cambiar el paquete en el código.
- [x] Authentication → **Google** (nombre público Chat SimpApp).
- [x] **Firestore** Standard en `nam5`, modo producción.
- [x] **App Check** → registrar la app con **Play Integrity**; en depuración, agregar el token de prueba de Logcat.
- [x] **AI Logic** → Comenzar → proveedor **Gemini Developer API** (gratis en Spark).
- [ ] **Remote Config** → crear `min_version_code` (0) y, si se quiere, `ai_model`.
- [ ] Desplegar: `firebase deploy --only firestore,hosting` (reglas, índices y páginas legales).
- [ ] Cuenta de servicio: Configuración del proyecto → Cuentas de servicio → **Generar nueva clave privada**
      (JSON). Se usa solo como secreto del Worker; **nunca** en el repo ni en el chat.

## 2. Cloudflare (Worker `chat-simpapp-api`)

- [x] `FIREBASE_PROJECT_ID` y `FIREBASE_PROJECT_NUMBER` en `server/wrangler.toml` (desplegado).
- [x] Secretos `REVENUECAT_WEBHOOK_AUTH` y `CRON_SECRET` (generados y cargados por Claude).
- [ ] Secretos que faltan (panel de Cloudflare → Workers → chat-simpapp-api → Settings → Variables and Secrets,
      tipo *Secret*): `FIREBASE_SERVICE_ACCOUNT` (JSON completo de la cuenta de servicio) y `REVENUECAT_SECRET_KEY`.
- [ ] Cuando App Check funcione: `APP_CHECK_ENFORCE = "true"` y desplegar.
- [x] **rj2-cron**: tareas `cleanup-alerts` y `cleanup-db` registradas en el cron diario (ver docs/rj2-cron.md).

## 3. RevenueCat (proyecto en rj2techsolutions@gmail.com)

IDs exactos (el código depende de ellos):

| Qué | ID |
|---|---|
| Entitlement | `pro` |
| Productos (se importan de Play) | `comunidad_pro:u30-mensual`, `comunidad_pro:u30-anual`, `comunidad_pro:u80-mensual`, `comunidad_pro:u80-anual`, `comunidad_pro:u200-mensual`, `comunidad_pro:u200-anual`, `comunidad_pro:u500-mensual`, `comunidad_pro:u500-anual`, `comunidad_pro:u5000-mensual`, `comunidad_pro:u5000-anual` — los 10 adjuntos a `pro` |
| Offerings | `u30`, `u80`, `u200`, `u500`, `u5000` |
| Paquetes de cada offering | **Monthly** (`$rc_monthly`) → `…-mensual` del mismo rango; **Annual** (`$rc_annual`) → `…-anual` del mismo rango |

- [x] Proyecto Chat SimpApp con la app de Play.
- [x] Entitlement, productos, offerings y paquetes como en la tabla (los paquetes deben ser de tipo
      Monthly y Annual estándar: la app los busca por tipo). Marcar cualquiera como *current* (la app no lo usa).
- [ ] Webhook → **`https://chat-simpapp-api.rj2techsolutions.workers.dev/webhooks/revenuecat`**, *Authorization header*
      = el contenido del archivo `revenuecat-webhook-authorization.txt` (enviado por Claude), entorno *Both*.
- [ ] Clave pública `goog_…` (no es secreta): pasársela a Claude para compilar el AAB, o ponerla en
      `~/.gradle/gradle.properties` como `revenueCatApiKey`. Sin ella la pantalla Pro dice "no disponible".
- [ ] Clave secreta v1 (`sk_…`) como secreto `REVENUECAT_SECRET_KEY` del Worker (panel de Cloudflare).

## 4. Play Console (cuenta personal)

- [x] App creada: `com.rj2techsolutions.chatsimpapp`, es-419, gratis.
- [x] Ficha: correo `chatsimpapp@rj2techsolutions.com`, sitio web https://rj2techsolutions.com.
- [ ] Subir el primer AAB a prueba interna (Claude lo compila cuando tenga el `google-services.json` nuevo). La
      suscripción solo se puede crear después de subir un AAB.
- [ ] Suscripción con ID de producto **`comunidad_pro`** y 10 **planes base** de renovación automática:

      | Plan base | Periodo | Precio sugerido (USD) |
      |---|---|---|
      | `u30-mensual` / `u30-anual` | 1 mes / 1 año | 5,99 / 59,90 |
      | `u80-mensual` / `u80-anual` | 1 mes / 1 año | 12,99 / 129,90 |
      | `u200-mensual` / `u200-anual` | 1 mes / 1 año | 34,99 / 349,90 |
      | `u500-mensual` / `u500-anual` | 1 mes / 1 año | 79,99 / 799,90 |
      | `u5000-mensual` / `u5000-anual` | 1 mes / 1 año | 149,99 / 1499,90 |

      Precios con **coma decimal** en la consola en español. Sin pruebas gratis por ahora.
- [ ] Licencia para testing: lista "Prueba Paxora Plus" marcada (`RESPOND_NORMALLY`).
- [ ] Activar **Play App Signing** y subir a **prueba interna** firmando con la clave de subida; luego copiar
      las huellas de la clave de firma de Play a Firebase (Play Integrity / App Check las necesita).
- [ ] Probar la actualización obligatoria y una compra de prueba real.
- [ ] Formulario de seguridad de datos: ubicación aproximada (alertas del barrio), nombre/correo de Google,
      mensajes. Sin fotos.

## 5. AdMob (cuenta personal)

- [x] App y 4 bloques creados. Los IDs (no son secretos) ya están en `app/build.gradle.kts` para **release**;
      debug usa siempre los de prueba de Google. Copia también en `gradle.properties.example`.

      | Formato | Propiedad | ID |
      |---|---|---|
      | App | `admobAppId` | `ca-app-pub-2631068461383213~6764034990` |
      | Banner | `admobBannerId` | `ca-app-pub-2631068461383213/6847795470` |
      | Apertura de app | `admobAppOpenId` | `ca-app-pub-2631068461383213/5770729550` |
      | Intersticial | `admobInterstitialId` | `ca-app-pub-2631068461383213/4457647885` |
      | Bonificado | `admobRewardedId` | `ca-app-pub-2631068461383213/6884500675` |

- [ ] Mensaje de consentimiento (Privacidad y mensajes → GDPR y regulaciones de EE. UU.).
- [ ] Vincular AdMob con la app de Play cuando esté publicada.

## 6. Páginas legales (Firebase Hosting)

- [x] `hosting/privacy-policy.html`, `terms.html` y `delete-data.html` con el diseño de Paxora (ES/EN) y el correo
      `chatsimpapp@rj2techsolutions.com`.
- [x] Eliminación de cuenta dentro de la app (Perfil → Eliminar mi cuenta), como exige Google Play.
- [ ] Publicar: `firebase deploy --only hosting` (proyecto `chat-simpapp-efb77`, ya en `.firebaserc`). Quedan en
      `https://chat-simpapp-efb77.web.app/privacy-policy.html`, `/terms.html` y `/delete-data.html`.
- [ ] En Play Console: política de privacidad y URL de eliminación de cuenta con esas direcciones.
- [ ] Agregar la app y su política al sitio rj2techsolutions.com.

## 7. A vigilar

- **Firestore Spark ≈ 50.000 lecturas al día** (≈ 150 vecinos activos al día). Revisar el uso semanalmente.
- **Cloudflare gratis: 100.000 peticiones al día compartidas entre todas las apps.** Cuando la suma se acerque,
  pasar la cuenta a Workers Paid (5 USD/mes).
