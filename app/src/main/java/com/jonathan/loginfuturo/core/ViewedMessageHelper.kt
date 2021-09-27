package com.jonathan.loginfuturo.core

import android.app.ActivityManager
import android.content.Context
import com.jonathan.loginfuturo.data.model.providers.AuthProvider
import com.jonathan.loginfuturo.data.model.providers.UserProvider


object ViewedMessageHelper {

    fun updateState(state: Boolean, context: Context) {
        val userProvider = UserProvider()
        val authProvider = AuthProvider()
        if (authProvider.getUid() != null) {
            if (isApplicationSendToBackground(context)) {
                userProvider.updateState(authProvider.getUid(), state)
            } else if (state) {
                userProvider.updateState(authProvider.getUid(), state)
            }
        }
    }

    private fun isApplicationSendToBackground(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val tasks = activityManager.getRunningTasks(1)
        if (tasks.isNotEmpty()) {
            val topActivity = tasks[0].topActivity
            if (topActivity!!.packageName != context.packageName) {
                return true
            }
        }
        return false
    }
}