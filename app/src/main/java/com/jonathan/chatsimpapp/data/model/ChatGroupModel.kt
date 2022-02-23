package com.jonathan.chatsimpapp.data.model

import java.util.*
import kotlin.collections.ArrayList

class ChatGroupModel {

    private var idGroup: String = ""
    private var idAdmin: String = ""
    private var idNotification: Int = 0
    private var timeStamp: Date = Date()
    private var email: String = ""
    private var idUsers = ArrayList<String>()

    fun getIdGroup(): String {
        return idGroup
    }

    fun setIdGroup(idGroup: String) {
        this.idGroup = idGroup
    }

    fun getIdAmin(): String {
        return  idAdmin
    }

    fun setIdAdmin(idAdmin: String) {
        this.idAdmin = idAdmin
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

    fun getIdUsers(): ArrayList<String> {
        return idUsers
    }

    fun setIdUsers(ids: ArrayList<String>) {
        this.idUsers = ids
    }
}