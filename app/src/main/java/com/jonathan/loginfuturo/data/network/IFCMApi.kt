package com.jonathan.loginfuturo.data.network

import com.jonathan.loginfuturo.data.model.FCMBody
import com.jonathan.loginfuturo.data.model.FCMResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface IFCMApi {

    @Headers(
        "Content-Type:application/json",
        "Authorization:key=AAAA_RTE-YU:APA91bHrgz8dYzOA0mdwstR8EGIeo0ighdOIERTYpUcP-DTiOpmABp-mARQBlf2prxohMHDXID_RS5TvYvqGpDpi6CEZaK7fF6OCtyQio5VwosDyeQVwx0eqPyDz0z9lQhkU5cY4O32G"
    )
    @POST("fcm/send")
    fun send(@Body body: FCMBody?): Call<FCMResponse?>?
}