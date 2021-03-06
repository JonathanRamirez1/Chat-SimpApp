package com.jonathan.chatsimpapp.data.model

import java.util.*
import kotlin.collections.ArrayList

class ChatModel {

    private var id: String = ""
    private var idEmisor: String = ""
    private var idReceptor: String = ""
    private var idNotification: Int = 0
    private var timeStamp: Date = Date()
    private var email: String = ""
    private var ids = ArrayList<String>()

    fun getId(): String {
        return id
    }

    fun setId(id: String) {
        this.id = id
    }

    fun getIdEmisor(): String {
        return idEmisor
    }

    fun setIdEmisor(idEmisor: String) {
        this.idEmisor = idEmisor
    }

    fun getIdReceptor(): String {
        return idReceptor
    }

    fun setIdReceptor(idReceptor: String) {
        this.idReceptor = idReceptor
    }

    fun getIdNotification(): Int {
        return idNotification
    }

    fun setIdNotification(idNotification: Int) {
        this.idNotification = idNotification
    }

    fun getTimeStamp(): Date {
        return timeStamp
    }

    fun setTimeStamp(timeStamp: Date) {
        this.timeStamp = timeStamp
    }

    fun getEmail(): String {
        return email
    }

    fun setEmail(email: String) {
        this.email = email
    }

    fun getIds(): ArrayList<String> {
        return ids
    }

    fun setIds(ids: ArrayList<String>) {
        this.ids = ids
    }
}