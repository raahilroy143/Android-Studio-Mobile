package com.mrrvk.assistant.command

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import com.mrrvk.assistant.util.CommandAction
import com.mrrvk.assistant.util.CommandParser
import com.mrrvk.assistant.util.ParsedCommand
import com.mrrvk.assistant.util.ResponseGenerator

class CommandExecutor(private val context: Context) {

    interface CommandCallback {
        fun onSpeak(text: String)
        fun onCommandExecuted(success: Boolean, message: String)
    }

    private var callback: CommandCallback? = null
    private var flashlightOn = false

    fun setCallback(cb: CommandCallback) {
        callback = cb
    }

    fun execute(command: ParsedCommand) {
        val response = ResponseGenerator.getCommandResponse(context, command)

        when (command.action) {
            CommandAction.OPEN_APP -> openApp(command.target, response)
            CommandAction.CALL_CONTACT -> makeCall(command.target, response)
            CommandAction.SEND_MESSAGE -> sendMessage(command.target, command.extras, response)
            CommandAction.SEND_WHATSAPP -> openWhatsApp(command.target, response)
            CommandAction.TAKE_PHOTO, CommandAction.OPEN_CAMERA -> openCamera(response)
            CommandAction.RECORD_VIDEO -> recordVideo(response)
            CommandAction.OPEN_GALLERY -> openGallery(response)
            CommandAction.PLAY_MUSIC -> playMusic(command.target, response)
            CommandAction.SET_ALARM -> setAlarm(command.target, response)
            CommandAction.SET_TIMER -> setTimer(command.target, response)
            CommandAction.SET_REMINDER -> setReminder(command.target, response)
            CommandAction.OPEN_SETTINGS -> openSettings(response)
            CommandAction.TOGGLE_WIFI -> toggleWifi(response)
            CommandAction.TOGGLE_BLUETOOTH -> toggleBluetooth(response)
            CommandAction.TOGGLE_FLASHLIGHT -> toggleFlashlight(response)
            CommandAction.INCREASE_VOLUME -> changeVolume(true, response)
            CommandAction.DECREASE_VOLUME -> changeVolume(false, response)
            CommandAction.INCREASE_BRIGHTNESS -> changeBrightness(true, response)
            CommandAction.DECREASE_BRIGHTNESS -> changeBrightness(false, response)
            CommandAction.OPEN_BROWSER -> openBrowser(response)
            CommandAction.SEARCH_WEB -> searchWeb(command.target, response)
            CommandAction.OPEN_YOUTUBE -> openYouTube(response)
            CommandAction.PLAY_VIDEO -> searchYouTube(command.target, response)
            CommandAction.BATTERY_STATUS -> getBatteryStatus()
            CommandAction.TIME_NOW -> getTime()
            CommandAction.DATE_NOW -> getDate()
            CommandAction.NAVIGATE_TO -> navigateTo(command.target, response)
            CommandAction.TAKE_SCREENSHOT -> takeScreenshot(response)
            CommandAction.OPEN_FILE_MANAGER -> openFileManager(response)
            CommandAction.OPEN_CALCULATOR -> openCalculator(response)
            CommandAction.OPEN_CALENDAR -> openCalendar(response)
            CommandAction.OPEN_NOTES -> openNotes(response)
            CommandAction.SILENT_MODE -> setSoundMode(AudioManager.RINGER_MODE_SILENT, response)
            CommandAction.NORMAL_MODE -> setSoundMode(AudioManager.RINGER_MODE_NORMAL, response)
            CommandAction.VIBRATE_MODE -> setSoundMode(AudioManager.RINGER_MODE_VIBRATE, response)
            CommandAction.WHO_ARE_YOU, CommandAction.GREETING,
            CommandAction.THANK_YOU, CommandAction.HOW_ARE_YOU -> {
                speak(response)
            }
            CommandAction.WEATHER -> openWeather(response)
            CommandAction.LOCK_SCREEN -> lockScreen(response)
            CommandAction.READ_NOTIFICATION -> speak(response)
            CommandAction.UNKNOWN -> speak(response)
        }
    }

    fun executeFromText(text: String) {
        // Auto-detect language from text
        val detectedLang = ResponseGenerator.detectLanguageFromText(text)
        com.mrrvk.assistant.MrRvkApp.setLanguage(context, detectedLang)

        val command = CommandParser.parse(text)
        execute(command)
    }

    private fun speak(text: String) {
        callback?.onSpeak(text)
    }

    private fun openApp(appName: String, response: String) {
        if (appName.isEmpty()) {
            speak(response)
            return
        }

        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(getPackageName(appName))

        if (intent != null) {
            speak(response)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            callback?.onCommandExecuted(true, "App opened")
        } else {
            // Try fuzzy match by app name
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val matched = packages.firstOrNull { app ->
                val label = pm.getApplicationLabel(app).toString().lowercase()
                label.contains(appName.lowercase()) || appName.lowercase().contains(label)
            }

            if (matched != null) {
                val launchIntent = pm.getLaunchIntentForPackage(matched.packageName)
                if (launchIntent != null) {
                    speak(response)
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    callback?.onCommandExecuted(true, "App opened")
                    return
                }
            }

            val lang = com.mrrvk.assistant.MrRvkApp.getLanguage(context)
            val userName = com.mrrvk.assistant.MrRvkApp.getUserName(context)
            if (lang == "hi") {
                speak("$userName, $appName app nahi mila. Kya aap Play Store se install karna chahenge?")
            } else {
                speak("$userName, I couldn't find $appName. Would you like me to search for it on Play Store?")
            }
            callback?.onCommandExecuted(false, "App not found")
        }
    }

    private fun getPackageName(appName: String): String {
        val appMap = mapOf(
            "whatsapp" to "com.whatsapp",
            "instagram" to "com.instagram.android",
            "facebook" to "com.facebook.katana",
            "twitter" to "com.twitter.android",
            "x" to "com.twitter.android",
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "gmail" to "com.google.android.gm",
            "maps" to "com.google.android.apps.maps",
            "google maps" to "com.google.android.apps.maps",
            "spotify" to "com.spotify.music",
            "telegram" to "org.telegram.messenger",
            "snapchat" to "com.snapchat.android",
            "netflix" to "com.netflix.mediaclient",
            "amazon" to "in.amazon.mShop.android.shopping",
            "flipkart" to "com.flipkart.android",
            "paytm" to "net.one97.paytm",
            "phonepe" to "com.phonepe.app",
            "gpay" to "com.google.android.apps.nbu.paisa.user",
            "google pay" to "com.google.android.apps.nbu.paisa.user",
            "camera" to "com.android.camera",
            "calculator" to "com.android.calculator2",
            "calendar" to "com.google.android.calendar",
            "clock" to "com.android.deskclock",
            "contacts" to "com.android.contacts",
            "files" to "com.google.android.apps.nbu.files",
            "file manager" to "com.google.android.apps.nbu.files",
            "gallery" to "com.google.android.apps.photos",
            "photos" to "com.google.android.apps.photos",
            "music" to "com.google.android.music",
            "play store" to "com.android.vending",
            "settings" to "com.android.settings",
            "messages" to "com.google.android.apps.messaging",
            "phone" to "com.android.dialer",
            "dialer" to "com.android.dialer",
            "notes" to "com.google.android.keep",
            "keep" to "com.google.android.keep",
            "drive" to "com.google.android.apps.docs",
            "meet" to "com.google.android.apps.tachyon",
            "zoom" to "us.zoom.videomeetings",
            "tiktok" to "com.zhiliaoapp.musically",
            "reddit" to "com.reddit.frontpage",
            "linkedin" to "com.linkedin.android",
            "pinterest" to "com.pinterest",
            "uber" to "com.ubercab",
            "ola" to "com.olacabs.customer",
            "zomato" to "com.application.zomato",
            "swiggy" to "in.swiggy.android"
        )

        return appMap[appName.lowercase()] ?: "com.${appName.lowercase().replace(" ", "")}"
    }

    private fun makeCall(contact: String, response: String) {
        speak(response)
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:${contact.replace(" ", "")}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            callback?.onCommandExecuted(true, "Call initiated")
        } catch (e: SecurityException) {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${contact.replace(" ", "")}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            callback?.onCommandExecuted(true, "Dialer opened")
        }
    }

    private fun sendMessage(target: String, message: String, response: String) {
        speak(response)
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$target")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        callback?.onCommandExecuted(true, "Message app opened")
    }

    private fun openWhatsApp(target: String, response: String) {
        speak(response)
        try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/$target")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
            callback?.onCommandExecuted(true, "WhatsApp opened")
        } catch (e: Exception) {
            callback?.onCommandExecuted(false, "Failed to open WhatsApp")
        }
    }

    private fun openCamera(response: String) {
        speak(response)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            callback?.onCommandExecuted(true, "Camera opened")
        } catch (e: Exception) {
            val fallback = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    private fun recordVideo(response: String) {
        speak(response)
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        callback?.onCommandExecuted(true, "Video recording started")
    }

    private fun openGallery(response: String) {
        speak(response)
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                type = "image/*"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.photos")
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent != null) context.startActivity(intent)
        }
        callback?.onCommandExecuted(true, "Gallery opened")
    }

    private fun playMusic(query: String, response: String) {
        speak(response)
        try {
            val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE)
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val spotifyIntent = context.packageManager.getLaunchIntentForPackage("com.spotify.music")
            if (spotifyIntent != null) {
                spotifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(spotifyIntent)
            }
        }
        callback?.onCommandExecuted(true, "Music playing")
    }

    private fun setAlarm(target: String, response: String) {
        speak(response)
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Try to parse time from target
        val timeRegex = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|AM|PM)?")
        val match = timeRegex.find(target)
        if (match != null) {
            var hour = match.groupValues[1].toIntOrNull() ?: 0
            val minute = match.groupValues[2].toIntOrNull() ?: 0
            val amPm = match.groupValues[3].lowercase()
            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0
            intent.putExtra(AlarmClock.EXTRA_HOUR, hour)
            intent.putExtra(AlarmClock.EXTRA_MINUTES, minute)
        }

        context.startActivity(intent)
        callback?.onCommandExecuted(true, "Alarm set")
    }

    private fun setTimer(target: String, response: String) {
        speak(response)
        val seconds = extractSeconds(target)
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        callback?.onCommandExecuted(true, "Timer set")
    }

    private fun extractSeconds(text: String): Int {
        var seconds = 0
        val minRegex = Regex("(\\d+)\\s*min")
        val secRegex = Regex("(\\d+)\\s*sec")
        val hourRegex = Regex("(\\d+)\\s*hour")

        hourRegex.find(text)?.let { seconds += (it.groupValues[1].toIntOrNull() ?: 0) * 3600 }
        minRegex.find(text)?.let { seconds += (it.groupValues[1].toIntOrNull() ?: 0) * 60 }
        secRegex.find(text)?.let { seconds += (it.groupValues[1].toIntOrNull() ?: 0) }

        if (seconds == 0) {
            val numRegex = Regex("(\\d+)")
            numRegex.find(text)?.let { seconds = (it.groupValues[1].toIntOrNull() ?: 60) * 60 }
        }

        return if (seconds > 0) seconds else 300
    }

    private fun setReminder(target: String, response: String) {
        speak(response)
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = android.provider.CalendarContract.Events.CONTENT_URI
            putExtra(android.provider.CalendarContract.Events.TITLE, target)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val calIntent = Intent(Intent.ACTION_VIEW).apply {
                data = android.provider.CalendarContract.CONTENT_URI
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(calIntent)
        }
        callback?.onCommandExecuted(true, "Reminder set")
    }

    private fun openSettings(response: String) {
        speak(response)
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        callback?.onCommandExecuted(true, "Settings opened")
    }

    private fun toggleWifi(response: String) {
        speak(response)
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        callback?.onCommandExecuted(true, "WiFi settings opened")
    }

    private fun toggleBluetooth(response: String) {
        speak(response)
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        callback?.onCommandExecuted(true, "Bluetooth settings opened")
    }

    private fun toggleFlashlight(response: String) {
        speak(response)
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            flashlightOn = !flashlightOn
            cameraManager.setTorchMode(cameraId, flashlightOn)
            callback?.onCommandExecuted(true, "Flashlight toggled")
        } catch (e: CameraAccessException) {
            callback?.onCommandExecuted(false, "Failed to toggle flashlight")
        }
    }

    private fun changeVolume(increase: Boolean, response: String) {
        speak(response)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            direction,
            AudioManager.FLAG_SHOW_UI
        )
        // Repeat a few times for noticeable change
        repeat(2) {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                direction,
                0
            )
        }
        callback?.onCommandExecuted(true, "Volume changed")
    }

    private fun changeBrightness(increase: Boolean, response: String) {
        speak(response)
        try {
            val currentBrightness = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                128
            )
            val newBrightness = if (increase) {
                (currentBrightness + 50).coerceAtMost(255)
            } else {
                (currentBrightness - 50).coerceAtLeast(10)
            }
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                newBrightness
            )
            callback?.onCommandExecuted(true, "Brightness changed")
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private fun openBrowser(response: String) {
        speak(response)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://www.google.com")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        callback?.onCommandExecuted(true, "Browser opened")
    }

    private fun searchWeb(query: String, response: String) {
        speak(response)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        callback?.onCommandExecuted(true, "Search opened")
    }

    private fun openYouTube(response: String) {
        speak(response)
        try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://www.youtube.com")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
            callback?.onCommandExecuted(true, "YouTube opened")
        } catch (e: Exception) {
            callback?.onCommandExecuted(false, "Failed to open YouTube")
        }
    }

    private fun searchYouTube(query: String, response: String) {
        speak(response)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        callback?.onCommandExecuted(true, "YouTube search opened")
    }

    private fun getBatteryStatus() {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = batteryManager.isCharging
        val response = ResponseGenerator.getBatteryResponse(context, level, isCharging)
        speak(response)
        callback?.onCommandExecuted(true, "Battery status read")
    }

    private fun getTime() {
        val response = ResponseGenerator.getTimeResponse(context)
        speak(response)
        callback?.onCommandExecuted(true, "Time read")
    }

    private fun getDate() {
        val response = ResponseGenerator.getDateResponse(context)
        speak(response)
        callback?.onCommandExecuted(true, "Date read")
    }

    private fun navigateTo(destination: String, response: String) {
        speak(response)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("google.navigation:q=${Uri.encode(destination)}")
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://www.google.com/maps/search/${Uri.encode(destination)}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
        callback?.onCommandExecuted(true, "Navigation started")
    }

    private fun takeScreenshot(response: String) {
        speak(response)
        // Screenshots require system-level access or accessibility service
        callback?.onCommandExecuted(true, "Screenshot requested")
    }

    private fun openFileManager(response: String) {
        speak(response)
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                type = "*/*"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open File Manager").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.nbu.files")
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent != null) context.startActivity(intent)
        }
        callback?.onCommandExecuted(true, "File manager opened")
    }

    private fun openCalculator(response: String) {
        openApp("calculator", response)
    }

    private fun openCalendar(response: String) {
        speak(response)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = android.provider.CalendarContract.CONTENT_URI
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            openApp("calendar", response)
        }
        callback?.onCommandExecuted(true, "Calendar opened")
    }

    private fun openNotes(response: String) {
        openApp("notes", response)
    }

    private fun setSoundMode(mode: Int, response: String) {
        speak(response)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try {
            audioManager.ringerMode = mode
            callback?.onCommandExecuted(true, "Sound mode changed")
        } catch (e: SecurityException) {
            val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private fun openWeather(response: String) {
        speak(response)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://www.google.com/search?q=weather")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        callback?.onCommandExecuted(true, "Weather opened")
    }

    private fun lockScreen(response: String) {
        speak(response)
        callback?.onCommandExecuted(true, "Lock screen requested")
    }
}
