# Chat SimpApp · Tu edificio, organizado

App Android para **comunidades de vecinos** (edificios y conjuntos residenciales). No compite con WhatsApp: resuelve
lo que los grupos de WhatsApp del edificio hacen mal.

<p>
  <img src="screenshots/01_home.png" width="190">
  <img src="screenshots/04_poll.png" width="190">
  <img src="screenshots/05_reports.png" width="190">
  <img src="screenshots/07_board.png" width="190">
</p>

## Qué problema resuelve

| En el grupo de WhatsApp del edificio… | En Chat SimpApp |
|---|---|
| Los avisos importantes se pierden entre memes y discusiones | **Avisos oficiales** separados del chat, fijables, con "visto por N" |
| Votar es contar "👍" a mano; votan dos del mismo apto | **Votaciones con un voto por unidad**, quórum automático y **acta** (texto o PDF) |
| "¿Ya arreglaron el ascensor?" × 40 mensajes | **Reportes con foto y estado** (abierto → en proceso → resuelto) y "a mí también me afecta" |
| Quien administra el grupo saca a quien quiera | Unirse con **código**; el canal oficial no depende de un administrador de grupo |
| Tu número queda expuesto a 200 personas | Te identificas por **tu unidad**; chats privados sin compartir teléfono |
| Nadie lee 300 mensajes | **Resumen semanal con IA**: acuerdos, compromisos y fechas |
| Una fuga de gas se avisa igual que un meme | **Alerta de emergencia** con sonido de alarma para todo el edificio |
| Préstamos y recomendaciones se pierden | **Tablón**: presto, busco, recomiendo, perdido/encontrado, vendo |

Además conserva el chat general del edificio, chats privados y grupos (texto e imágenes, en línea, escribiendo,
leído). El chat general **no envía notificaciones push** a propósito: es el ruido que los vecinos quieren evitar.
Avisos, emergencias, votaciones y cambios en tus reportes sí notifican.

## Cómo gana dinero

**Plan gratuito (con anuncios)**: todo lo esencial. Banner de AdMob abajo en Inicio, Votaciones, Reportes y Tablón
(nunca dentro de una conversación). Máximo 2 votaciones abiertas a la vez.

**Comunidad Pro (suscripción por edificio, la paga la administración)**:
sin anuncios para *ningún* vecino, resumen semanal con IA, actas en PDF, votaciones ilimitadas y confirmación de
lectura de avisos. Un solo pago cubre a todo el edificio: el cliente es la administración, que ya maneja presupuesto
y gana mucho con actas y quórum ordenados.

### Números aproximados (verifícalos con tus datos reales)

- **Costo de Firebase por edificio de ~50 vecinos activos**: ~300 lecturas por persona al día → ~450 mil lecturas
  al mes. A ~US$0,03 por 100 mil lecturas (región us-central1) son **centavos al mes**. El nivel gratuito
  (50 mil lecturas/día) cubre unos 3 edificios sin pagar nada.
- **Anuncios**: los banners en Latinoamérica pagan poco (estudios públicos hablan de ~US$0,12–0,50 por cada mil
  vistas). 50 vecinos × 10 vistas al día ≈ 15 mil vistas al mes ≈ **US$2–7 por edificio al mes**. Alcanza de sobra
  para pagar Firebase, no para vivir de eso.
- **Pro**: sugerencia de **US$9,99/mes o US$99/año por edificio** (ajústalo por país en Play Console). Google
  se queda con el 15 % de las suscripciones. Con 20 edificios Pro: ~US$170 netos al mes.
- **IA**: un resumen por semana y por edificio Pro con `gemini-3.5-flash-lite` cuesta fracciones de centavo.

**La conclusión: los anuncios pagan la infraestructura y Pro es la ganancia.** El código ya limita costos:
listas con tope, paginación de mensajes, imágenes comprimidas, el resumen IA se genera una vez por semana y lo leen
todos, y App Check impide que alguien use tu Firebase desde fuera de la app.

Otras ideas para después: precio para administradoras que manejan varios edificios, reserva de zonas comunes
(salón, BBQ) como función Pro, y un panel web para la administración.

## Puesta en marcha

### 1. Firebase (proyecto `chat-simpapp`)
1. **Pasa el proyecto al plan Blaze** y crea una **alerta de presupuesto** (p. ej. US$5) en Google Cloud → Facturación.
   Es obligatorio: desde febrero de 2026 Cloud Storage (fotos) solo funciona en Blaze, y las Cloud Functions también
   lo requieren. Blaze conserva la misma cuota gratuita; con una app pequeña normalmente pagas US$0.
2. **Authentication**: habilita *Correo/contraseña* y *Google*. Agrega la **SHA-1** de tu llave
   (`./gradlew signingReport`) en Configuración del proyecto y vuelve a descargar `google-services.json` a `app/`.
3. Crea **Firestore**, **Realtime Database** y **Storage** si no existen.
4. **App Check**: registra la app con *Play Integrity*. En depuración la app imprime un token de prueba en Logcat:
   agrégalo en App Check → Administrar tokens de depuración. Activa la aplicación obligatoria cuando todo funcione.
5. **AI Logic**: en la consola, *AI Logic → Comenzar* con el proveedor *Gemini Developer API*.
   El modelo se cambia sin publicar versión desde **Remote Config** (`ai_model`).
6. Publica reglas, índices y funciones:
   ```bash
   firebase login && firebase use chat-simpapp
   firebase deploy --only firestore,database,storage
   cd functions && npm install && cd .. && firebase deploy --only functions
   ```

### 2. Google Play (para cobrar)
1. Crea la app en Play Console y una **suscripción** con id `comunidad_pro` (planes mensual y anual).
2. En *Usuarios y permisos*, invita a la cuenta de servicio de las Cloud Functions
   (`chat-simpapp@appspot.gserviceaccount.com` o la que aparezca en Cloud Functions) con permiso para ver datos
   financieros y gestionar pedidos y suscripciones.
3. En *Monetización → Configuración*, activa las **notificaciones en tiempo real** con el tema Pub/Sub
   `projects/chat-simpapp/topics/play-billing` (la función `playBillingNotifications` lo escucha y procesa
   renovaciones, cancelaciones y reembolsos).

### 3. AdMob
Crea la app y un bloque de anuncios *banner*. Pon los ids en `~/.gradle/gradle.properties` (no en el repositorio):
```properties
admobAppId=ca-app-pub-XXXXXXXX~YYYYYYYY
admobBannerId=ca-app-pub-XXXXXXXX/ZZZZZZZZ
```
Sin esto se usan los ids de prueba de Google. El consentimiento de anuncios (UMP) ya está integrado; configura
el mensaje en AdMob → Privacidad y mensajes.

## Seguridad

Las reglas de [`firestore.rules`](firestore.rules) impiden, entre otras cosas, que alguien: lea una comunidad de la
que no es miembro, se una sin el código vigente, vote dos veces o por otra unidad, vote después del cierre, publique
avisos oficiales sin ser administración, cambie el estado de un reporte, o **se active el plan Pro sin pagar** (solo
la Cloud Function, tras validar la compra con Google Play, puede escribir el plan).

Están probadas contra el emulador: `cd rules-tests && npm install && npm test` (15 pruebas).

> ⚠️ La antigua *server key* de FCM de la versión 1 quedó en el historial de git. Ya no sirve con la API nueva,
> pero revócala en Configuración del proyecto → Cloud Messaging.

## Arquitectura

```
app/src/main/java/com/jonathan/chatsimpapp/
├── data/model          Comunidad, miembro, aviso, votación, reporte, publicación, chat, mensaje
├── data/repository     Firebase como Flow/suspend; CommunitySession = comunidad seleccionada
├── ai                  Resumen semanal con Gemini (Firebase AI Logic)
├── billing             Google Play Billing (la verificación la hace el servidor)
├── ads                 AdMob + consentimiento UMP
├── config              Remote Config (modelo de IA, límites del plan gratuito)
├── notifications       FCM: chats, avisos, emergencias (canal con alarma)
├── ui/…                Pantallas Compose + ViewModels
└── util                Quórum, códigos de invitación, fechas, PDF del acta
functions/              Cloud Functions: notificaciones y verificación de pagos
rules-tests/            Pruebas de las reglas de seguridad
```

Kotlin 2.2, Jetpack Compose + Material 3, Hilt, corrutinas/Flow, Navigation con rutas tipadas.

### Datos

| Ruta | Contenido |
|---|---|
| `communities/{id}` | nombre, unidades, código, administración, plan |
| `communities/{id}/members/{uid}` | unidad y rol |
| `communities/{id}/announcements` | avisos y emergencias (+ `reads` para "visto por") |
| `communities/{id}/polls/{pid}/votes/{unidad}` | un documento por unidad = un voto por unidad |
| `communities/{id}/reports`, `posts`, `summaries` | reportes, tablón, resúmenes IA |
| `invites/{código}` | a qué comunidad lleva cada código |
| `chats/{id}` | chat general (mismo id que la comunidad), privados y grupos |

## Pruebas

```bash
./gradlew testDebugUnitTest          # lógica (quórum, códigos, fechas…) + capturas de pantalla
./gradlew recordRoborazziDebug       # regenera las capturas de /screenshots
cd rules-tests && npm test           # reglas de seguridad en el emulador
```

## Migración desde la versión 1
Las cuentas siguen sirviendo (nombre y foto se copian de `UsersRegister`). Los chats y mensajes viejos no se migran.
Se quitaron Facebook Login y la sección "Rates". La app ahora está en español.
