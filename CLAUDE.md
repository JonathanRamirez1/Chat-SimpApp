# Chat SimpApp — instrucciones del proyecto

App Android nativa (Kotlin, Jetpack Compose, Hilt, Firebase) para comunidades de vecinos.
Ver README.md para arquitectura, monetización y puesta en marcha.

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

## Notas de compilación

- Pruebas: `./gradlew testDebugUnitTest` · reglas de Firestore: `cd rules-tests && npm test`.
- Subir `versionCode` en `app/build.gradle.kts` en cada publicación.
