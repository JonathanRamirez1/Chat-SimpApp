package com.jonathan.chatsimpapp.data.model

import java.util.*
import kotlin.collections.ArrayList

class ChatGroupModel {

    private var idGroup: String = ""
    private var idAdmin: String = ""
    private var nameGroup: String = ""
    private var description: String = ""
    private var photoGroup: String = ""
    private var email: String = ""
    private var idNotification: Int = 0
    private var timeStamp: Date = Date()
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

    fun getNameGroup(): String {
        return  nameGroup
    }

    fun setNameGroup(nameGroup: String) {
        this.nameGroup = nameGroup
    }

    fun getDescription(): String {
        return  description
    }

    fun setDescription(description: String) {
        this.description = description
    }

    fun getPhotoGroup(): String {
        return  photoGroup
    }

    fun setPhotoGroup(photoGroup: String) {
        this.photoGroup = photoGroup
    }

    fun getEmail(): String {
        return email
    }

    fun setEmail(email: String) {
        this.email = email
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

    fun getIdUsers(): ArrayList<String> {
        return idUsers
    }

    fun setIdUsers(ids: ArrayList<String>) {
        this.idUsers = ids
    }
}