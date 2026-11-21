export interface Env {
  DB: D1Database;
  /** ID del proyecto de Firebase (p. ej. "chat-simpapp-rj2"). Variable en wrangler.toml. */
  FIREBASE_PROJECT_ID: string;
  /** Número del proyecto de Firebase, para validar App Check. */
  FIREBASE_PROJECT_NUMBER: string;
  /** "true" para exigir el token de App Check en cada petición de la app. */
  APP_CHECK_ENFORCE: string;
  /** JSON de la cuenta de servicio de Firebase (FCM + Firestore). Secreto. */
  FIREBASE_SERVICE_ACCOUNT: string;
  /** Clave secreta v1 de RevenueCat (sk_...). Secreto. */
  REVENUECAT_SECRET_KEY: string;
  /** Valor del header Authorization que manda el webhook de RevenueCat. Secreto. */
  REVENUECAT_WEBHOOK_AUTH: string;
  /** Clave que usa el Worker compartido rj2-cron para llamar a las tareas programadas. Secreto. */
  CRON_SECRET: string;
}
