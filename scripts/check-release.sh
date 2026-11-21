#!/usr/bin/env bash
# Revisión obligatoria antes de publicar una versión de Chat SimpApp (reglas de RJ2).
# Uso: ./scripts/check-release.sh   → sale con error si algo no cumple.
set -u
cd "$(dirname "$0")/.."
fail=0
ok() { echo "  ✓ $1"; }
bad() { echo "  ✗ $1"; fail=1; }

echo "Reglas de RJ2:"

# 1. Compras de prueba: el Worker debe aceptar eventos SANDBOX de RevenueCat.
grep -Eq '^export const ACCEPT_SANDBOX_EVENTS = true;' server/src/config.ts \
  && ok "ACCEPT_SANDBOX_EVENTS = true en server/src/config.ts" \
  || bad "ACCEPT_SANDBOX_EVENTS debe ser true en server/src/config.ts"

# 2. Actualización obligatoria IMMEDIATE conectada en MainActivity.
grep -q 'AppUpdateType.IMMEDIATE' app/src/main/java/com/jonathan/chatsimpapp/update/ForcedUpdateManager.kt \
  && grep -q 'forcedUpdate.onCreate()' app/src/main/java/com/jonathan/chatsimpapp/MainActivity.kt \
  && grep -q 'forcedUpdate.onResume()' app/src/main/java/com/jonathan/chatsimpapp/MainActivity.kt \
  && ok "Actualización obligatoria (IMMEDIATE) en MainActivity" \
  || bad "Falta la actualización obligatoria IMMEDIATE en MainActivity"

# 3. Esquema nuevo: nada que exija el plan Blaze de Firebase.
if grep -Eq 'firebase-(storage|database|functions)' gradle/libs.versions.toml app/build.gradle.kts; then
  bad "La app usa Storage, Realtime Database o Cloud Functions (exigen Blaze o no caben en Spark)"
else
  ok "Sin Storage, Realtime Database ni Cloud Functions"
fi
[ -d functions ] && bad "Existe la carpeta functions/ (Cloud Functions exige Blaze)" || ok "Sin carpeta functions/"
grep -q 'PhoneAuthProvider' -r app/src/main/java && bad "Login por teléfono (no existe en Spark)" || ok "Sin login por teléfono"

# 4. Sin Cron Triggers propios (se usan los del Worker compartido rj2-cron).
grep -Eq '^\[triggers\]|^crons' server/wrangler.toml && bad "server/wrangler.toml tiene Cron Triggers propios" || ok "Sin Cron Triggers propios"

# 5. Correo personal solo en la regla de compras de prueba del CLAUDE.md.
leaks=$(grep -rl 'ramirezjj201@gmail.com' --exclude-dir=node_modules --exclude-dir=build --exclude-dir=.git . | grep -v '^./CLAUDE.md$' | grep -v '^./scripts/check-release.sh$' || true)
[ -z "$leaks" ] && ok "Correo personal fuera de lo público" || bad "Correo personal en: $leaks"

# 6. Secretos fuera del repo.
grep -Eq 'sk_[A-Za-z0-9]{10,}|"private_key"' -r --exclude-dir=node_modules --exclude-dir=build --exclude-dir=.git --exclude=check-release.sh . \
  && bad "Parece haber una clave secreta en el repositorio" || ok "Sin claves secretas en el repositorio"

# 7. Paquete final de RJ2.
grep -q 'applicationId = "com.rj2techsolutions.chatsimpapp"' app/build.gradle.kts \
  && ok "Paquete com.rj2techsolutions.chatsimpapp" \
  || bad "El paquete aún no es com.rj2techsolutions.chatsimpapp (falta el google-services.json del proyecto nuevo)"

echo
[ $fail -eq 0 ] && echo "Listo para publicar." || echo "Hay puntos pendientes antes de publicar."
exit $fail
