package com.jonathan.loginfuturo.core

import android.app.Application
import com.google.android.gms.ads.MobileAds

class LoginFuturoAddApp: Application() {

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
    }
}