package com.jonathan.chatsimpapp.core.objects

import android.app.ActivityManager
import android.content.Context
import com.jonathan.chatsimpapp.data.model.providers.AuthProvider

object UpdateStateHelper {

    fun updateState(online: String, offline: String, context: Context) {
        val authProvider = AuthProvider()
        if (authProvider.getUid() != null) {
            if (isApplicationSendToBackground(context)) {
                OfflinePersistence.updateUserStatus(authProvider.getUid(), online, offline)
            } else  {
                OfflinePersistence.updateUserStatus(authProvider.getUid(), online, offline)
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