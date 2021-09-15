package com.jonathan.loginfuturo.providers


import com.jonathan.loginfuturo.model.FCMBody
import com.jonathan.loginfuturo.model.FCMResponse
import com.jonathan.loginfuturo.retrofit2.IFCMApi
import com.jonathan.loginfuturo.retrofit2.RetrofitClient
import retrofit2.Call

class NotificationProvider {

    private val url: String = "https://fcm.googleapis.com"

    fun sendNotification(body: FCMBody?): Call<FCMResponse?>? {
        return RetrofitClient.getClient(url).create(IFCMApi::class.java).send(body)
    }
}