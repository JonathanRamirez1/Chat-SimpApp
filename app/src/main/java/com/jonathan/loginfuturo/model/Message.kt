package com.jonathan.loginfuturo.model

class Message {
    private var id: String = ""
    private var idEmisor: String = ""
    private var idReceptor: String = ""
    private var idChat: String = ""
    private var message: String = ""
    private var timeStamp: Long = 123
    private var viewed: Boolean = false
    private var profileImageUrl: String = ""

    fun Message() {

    }

    fun Message(id: String, idEmisor: String, idReceptor: String, idChat: String, message: String, timeStamp: Long, viewed: Boolean, profileImageUrl: String) {
        this.id = id
        this.idEmisor = idEmisor
        this.idReceptor = idReceptor
        this.idChat = idChat
        this.message = message
        this.timeStamp = timeStamp
        this.viewed = viewed
        this.profileImageUrl = profileImageUrl
    }

    fun getId(id: String): String {
        return id
    }

    fun setId(id: String) {
        this.id = id
    }

    fun getIdEmisor(idEmisor: String): String {
        return idEmisor
    }

    fun setIdEmisor(idEmisor: String) {
        this.idEmisor = idEmisor
    }

    fun getIdReceptor(idReceptor: String): String {
        return idReceptor
    }

    fun setIdReceptor(idReceptor: String) {
        this.idReceptor = idReceptor
    }

    fun getIdChat(idChat: String): String {
        return idChat
    }

    fun setIdChat(idChat: String) {
        this.idChat = idChat
    }

    fun getMessage(): String {
        return message
    }

    fun setMessage(message: String) {
        this.message = message
    }

    fun getTimeStamp(timeStamp: Long): Long {
        return timeStamp
    }

    fun setTimeStamp(timeStamp: Long) {
        this.timeStamp = timeStamp
    }

    fun getViewed(viewed: Boolean): Boolean {
        return viewed
    }

    fun setViewed(viewed: Boolean) {
        this.viewed = viewed
    }

    fun getProfileImageUrl(): String {
        return profileImageUrl
    }

    fun setProfileImageUrl(profileImageUrl: String) {
        this.profileImageUrl = profileImageUrl
    }
}
