package com.jonathan.chatsimpapp.data.network

import com.jonathan.chatsimpapp.data.model.FCMBody
import com.jonathan.chatsimpapp.data.model.FCMResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface IFCMApi {

    @Headers(
        "Content-Type:application/json",
        "Authorization:key=AAAATpk0svE:APA91bGP4YajlARbPAWdhqW_7mlI0RyB4bduvv8eXBW_6e9yiSUHcK6RhfTAiES4b7B6Axss9BTf7Yp2GrZ4BcXW6hCkMCX5FCLfsNb1llXfD1z9YfGq9ZTbOjQqihFwIg8GrpJrQ9GL"
    )
    @POST("fcm/send")
    fun send(@Body body: FCMBody?): Call<FCMResponse?>?
}