package com.jonathan.chatsimpapp.core

import android.app.Application
import com.google.android.gms.ads.MobileAds

class ChatSimpAppAddApp: Application() {

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
    }
}