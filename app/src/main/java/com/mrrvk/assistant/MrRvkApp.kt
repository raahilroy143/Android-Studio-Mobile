package com.mrrvk.assistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class MrRvkApp : Application() {

    companion object {
        const val CHANNEL_ID = "mr_rvk_service_channel"
        const val CHANNEL_NAME = "Mr. RVK Assistant"
        const val PREFS_NAME = "mr_rvk_prefs"
        const val KEY_USER_NAME = "user_name"
        const val KEY_SETUP_COMPLETE = "setup_complete"
        const val KEY_LANGUAGE = "language"

        fun getUserName(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_USER_NAME, "Sir") ?: "Sir"
        }

        fun isSetupComplete(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_SETUP_COMPLETE, false)
        }

        fun setSetupComplete(context: Context, userName: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(KEY_SETUP_COMPLETE, true)
                .putString(KEY_USER_NAME, userName)
                .apply()
        }

        fun getLanguage(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        }

        fun setLanguage(context: Context, lang: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LANGUAGE, lang).apply()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Mr. RVK Assistant background service"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
