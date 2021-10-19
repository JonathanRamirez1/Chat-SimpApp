package com.jonathan.loginfuturo.data.model.providers

import com.jonathan.loginfuturo.data.model.FCMBody
import com.jonathan.loginfuturo.data.model.FCMResponse
import com.jonathan.loginfuturo.data.network.IFCMApi
import com.jonathan.loginfuturo.data.network.RetrofitClient
import retrofit2.Call

class NotificationProvider {

    private val url: String = "https://fcm.googleapis.com"

    fun sendNotification(body: FCMBody?): Call<FCMResponse?>? {
        return RetrofitClient.getClient(url).create(IFCMApi::class.java).send(body)
    }
}