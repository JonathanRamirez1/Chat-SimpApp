package com.jonathan.loginfuturo.data.model

class Token(private var token: String?) {

    fun token(token: String) {
        this.token = token
    }

    fun getToken(): String? {
        return token
    }

    fun setToken(token: String) {
        this.token = token
    }
}