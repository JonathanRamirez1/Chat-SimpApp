package com.jonathan.loginfuturo.model


class FCMBody(private var to: String,
              private var priority: String,
              private var ttl: String,
              private var data: Map<String, String>) {

    fun getTo(): String {
        return to
    }

    fun setTo(to: String) {
        this.to = to
    }

    fun getPriority(): String {
        return priority
    }

    fun setPriority(priority: String) {
        this.priority = priority
    }

    fun getTtl(): String {
        return ttl
    }

    fun setTtl(ttl: String) {
        this.ttl = ttl
    }

    fun getData(): Map<String, String> {
        return data
    }

    fun setData(data: Map<String, String>) {
        this.data = data
    }
}