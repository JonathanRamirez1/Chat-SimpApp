package com.jonathan.chatsimpapp.data.model.providers

import com.jonathan.chatsimpapp.data.model.FCMBody
import com.jonathan.chatsimpapp.data.model.FCMResponse
import com.jonathan.chatsimpapp.data.network.IFCMApi
import com.jonathan.chatsimpapp.data.network.RetrofitClient
import retrofit2.Call

class NotificationProvider {

    private val url: String = "https://fcm.googleapis.com"

    fun sendNotification(body: FCMBody?): Call<FCMResponse?>? {
        return RetrofitClient.getClient(url).create(IFCMApi::class.java).send(body)
    }
}