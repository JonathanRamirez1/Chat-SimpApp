package com.jonathan.loginfuturo.data.model

import java.util.*
import kotlin.collections.ArrayList

class ChatModel {

    private var id: String = ""
    private var idEmisor: String = ""
    private var idReceptor: String = ""
    private var idNotification: Int = 0
    private var isWritting: Boolean = false
    private var timeStamp: Date = Date()
    private var photo: String = ""
    private var email: String = ""
    private var ids = ArrayList<String>()

    fun Chat(
        id: String,
        idEmisor: String,
        idReceptor: String,
        idNotification: Int,
        isWritting: Boolean,
        timeStamp: Date,
        photo: String,
        email: String,
        ids: ArrayList<String>
    ) {
        this.id = id
        this.idEmisor = idEmisor
        this.idReceptor = idReceptor
        this.idNotification = idNotification
        this.isWritting = isWritting
        this.timeStamp = timeStamp
        this.photo = photo
        this.email = email
        this.ids = ids
    }

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

    fun isWritting(): Boolean {
        return isWritting
    }

    fun setWritting(writting: Boolean) {
        isWritting = writting
    }

    fun getTimeStamp(): Date {
        return timeStamp
    }

    fun setTimeStamp(timeStamp: Date) {
        this.timeStamp = timeStamp
    }

    fun getPhoto(): String {
        return photo
    }

    fun setPhoto(photo: String) {
        this.photo = photo
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