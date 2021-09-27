package com.jonathan.loginfuturo.data.model

class OtherUserId {

    private var idUser: String = ""

    fun other(idUser: String) {
        this.idUser = idUser
    }

    fun getOtherId(): String {
        return idUser
    }

    fun setOtherId(idUser: String) {
        this.idUser = idUser
    }
}