package com.jonathan.chatsimpapp.billing

/**
 * Precio de Pro según el tamaño del edificio (número de unidades).
 *
 * En Play Console la suscripción `comunidad_pro` tiene un "plan base" por rango y periodo, con id
 * `<rango>-mensual` o `<rango>-anual`, p. ej. `u80-mensual`. En RevenueCat cada rango es un
 * "offering" con el mismo id (`u80`). Un edificio de 50 y uno de 60 unidades caen en el mismo
 * rango (hasta 80) y pagan lo mismo: es más simple de entender que cobrar por unidad.
 *
 * Precios sugeridos (US$/mes): hasta 30 → 5,99 · hasta 80 → 12,99 · hasta 200 → 34,99 ·
 * hasta 500 → 79,99 · más → 149,99. El anual cuesta 10 meses.
 * Regla: Pro debe dejar (después del 15 % de Google) al menos lo que dejarían los anuncios de un
 * edificio lleno de ese rango (~US$0,13 por vecino al mes); si no, cada edificio que pasa a Pro te hace perder.
 */
object ProTiers {
    data class Tier(val id: String, val maxUnits: Int)

    val all = listOf(
        Tier("u30", 30),
        Tier("u80", 80),
        Tier("u200", 200),
        Tier("u500", 500),
        Tier("u5000", 5000),
    )

    fun forUnits(units: Int): Tier = all.firstOrNull { units <= it.maxUnits } ?: all.last()

    fun ofBasePlan(basePlanId: String): Tier? = all.firstOrNull { basePlanId.startsWith(it.id + "-") }
}
