package com.jonathan.loginfuturo.data.model

class MessageModel {
    private var id: String = ""
    private var idEmisor: String = ""
    private var idReceptor: String = ""
    private var idChat: String = ""
    private var message: String = ""
    private var timeStamp: Long = 123
    private var viewed: Boolean = false
    private var profileImageUrl: String = ""

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

    fun getIdChat(): String {
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

    fun getTimeStamp(): Long {
        return timeStamp
    }

    fun setTimeStamp(timeStamp: Long) {
        this.timeStamp = timeStamp
    }

    fun getViewed(): Boolean {
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
