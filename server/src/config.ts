/**
 * Configuración del Worker de Chat SimpApp.
 *
 * REGLA DE RJ2 (compras de prueba del dueño): los eventos de RevenueCat del entorno SANDBOX
 * (compras de prueba de testers con licencia) se procesan igual que los reales. No cambiar.
 * `check-release` busca esta línea.
 */
export const ACCEPT_SANDBOX_EVENTS = true;

/** Entitlement de RevenueCat que activa el plan Pro de la comunidad. */
export const PRO_ENTITLEMENT = "pro";

/** Rangos de unidades por plan base de Play (`comunidad_pro:u80-mensual` cubre hasta 80). */
export const TIERS: Record<string, number> = { u30: 30, u80: 80, u200: 200, u500: 500, u5000: 5000 };

/** Radio del barrio para las alertas de seguridad (metros). */
export const SECTOR_RADIUS_M = 2000;

/** Solo estas alertas del barrio envían notificación; las demás se ven en el mapa sin sonar. */
export const PUSH_ALERT_TYPES = new Set(["theft"]);

/** Tope de envíos por petición (el plan gratis permite 50 subpeticiones por invocación). */
export const MAX_DIRECT_SENDS = 35;
export const MAX_TOPIC_SENDS = 25;
