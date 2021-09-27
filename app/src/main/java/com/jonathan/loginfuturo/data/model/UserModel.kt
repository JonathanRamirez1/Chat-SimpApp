package com.jonathan.loginfuturo.data.model

import java.util.*

class UserModel {

    //TODO PARA USAR ESTA CLASE VER CARPETA 3 VIDEO 1; 12:15
    private var id: String = ""
    private var email: String = ""
    private var password: String = ""
    private var username: String = ""
    private var photo: String = ""
    private var cover: String = ""
    private var phone: String = ""
    private var gender: String = ""
    private var timeStamp: Date = Date()
    private var online: Boolean = false
    private var lastConnect: Long = 123

    fun User() {

    }

    fun User(id: String, email: String, username: String, photo: String, cover: String, phone: String, gender: String, timeStamp: Date, online: Boolean, lastConnect: Long) {
        this.id = id
        this.email = email
        this.username = username
        this.photo = photo
        this.cover = cover
        this.phone = phone
        this.gender = gender
        this.timeStamp = timeStamp
        this.online = online
        this.lastConnect = lastConnect
    }

    fun getId(): String {
        return id
    }

    fun setId(id: String) {
        this.id = id
    }

    fun getEmail(): String {
        return email
    }

    fun setEmail(email: String) {
        this.email = email
    }

    fun getUsername(): String {
        return username
    }

    fun setUsername(username: String) {
        this.username = username
    }

    fun getPhoto(): String {
        return photo
    }

    fun setPhoto(photo: String) {
        this.photo = photo
    }

    fun getCover(): String {
        return cover
    }

    fun setCover(cover: String) {
        this.cover = cover
    }

    fun getPhone(): String {
        return phone
    }

    fun setPhone(phone: String) {
        this.phone = phone
    }

    fun getGender(): String {
        return gender
    }

    fun setGender(gender: String) {
        this.gender = gender
    }

    fun getTimeStamp(): Date {
        return timeStamp
    }

    fun setTimeStamp(timeStamp: Date) {
        this.timeStamp = timeStamp
    }

    fun getOnline(): Boolean {
        return online
    }

    fun setOnline(online: Boolean) {
        this.online = online
    }

    fun getLastConnect(): Long {
        return lastConnect
    }

    fun setLastConnect(lastConnect: Long) {
        this.lastConnect = lastConnect
    }
}