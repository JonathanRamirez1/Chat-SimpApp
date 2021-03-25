package com.jonathan.loginfuturo.Utils

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

object RxBus {

    private val publisher = PublishSubject.create<Any>()

    /** Publica el evento a lo que se este escuchando **/
    fun publish(event : Any) {
        publisher.onNext(event)
    }

    /**Se encarga de escuhar los events**/
    fun <T> listern(eventType : Class<T>) : Observable<T> = publisher.ofType(eventType)
}