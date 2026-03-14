package com.mrrvk.assistant.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mrrvk.assistant.MrRvkApp
import com.mrrvk.assistant.service.AssistantService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Log.d("BootReceiver", "Boot completed, starting Mr. RVK service")
                if (MrRvkApp.isSetupComplete(context)) {
                    AssistantService.start(context)
                }
            }
        }
    }
}
