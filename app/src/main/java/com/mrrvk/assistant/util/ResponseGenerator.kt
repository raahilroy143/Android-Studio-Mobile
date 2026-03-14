package com.mrrvk.assistant.util

import android.content.Context
import com.mrrvk.assistant.MrRvkApp
import java.util.Calendar

object ResponseGenerator {

    fun getGreeting(context: Context): String {
        val userName = MrRvkApp.getUserName(context)
        val lang = MrRvkApp.getLanguage(context)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        return when {
            lang == "hi" -> {
                when {
                    hour < 12 -> "Suprabhat, $userName! Main Mr. RVK hoon. Aapki kya seva kar sakta hoon?"
                    hour < 17 -> "Namaskar, $userName! Main Mr. RVK, aapki seva mein hazir hoon."
                    else -> "Shubh sandhya, $userName! Mr. RVK aapki seva mein."
                }
            }
            else -> {
                when {
                    hour < 12 -> "Good morning, $userName! I'm Mr. RVK. How can I assist you?"
                    hour < 17 -> "Good afternoon, $userName! Mr. RVK at your service."
                    else -> "Good evening, $userName! Mr. RVK here. What can I do for you?"
                }
            }
        }
    }

    fun getWakeResponse(context: Context): String {
        val userName = MrRvkApp.getUserName(context)
        val lang = detectLanguage(context)

        return if (lang == "hi") {
            "Boliye, $userName"
        } else {
            "Yes, $userName"
        }
    }

    fun getCommandResponse(context: Context, command: ParsedCommand): String {
        val userName = MrRvkApp.getUserName(context)
        val lang = detectLanguage(context)

        return when (command.action) {
            CommandAction.OPEN_APP -> {
                val appName = command.target.replaceFirstChar { it.uppercase() }
                if (lang == "hi") "$appName khol raha hoon, $userName"
                else "Opening $appName for you, $userName"
            }
            CommandAction.CALL_CONTACT -> {
                if (lang == "hi") "${command.target} ko call kar raha hoon, $userName"
                else "Calling ${command.target}, $userName"
            }
            CommandAction.SEND_MESSAGE -> {
                if (lang == "hi") "Message bhej raha hoon, $userName"
                else "Sending message, $userName"
            }
            CommandAction.SEND_WHATSAPP -> {
                if (lang == "hi") "WhatsApp message bhej raha hoon, $userName"
                else "Sending WhatsApp message, $userName"
            }
            CommandAction.TAKE_PHOTO -> {
                if (lang == "hi") "Photo capture kar raha hoon, $userName"
                else "Capturing photo, $userName"
            }
            CommandAction.RECORD_VIDEO -> {
                if (lang == "hi") "Video record kar raha hoon, $userName"
                else "Recording video, $userName"
            }
            CommandAction.OPEN_CAMERA -> {
                if (lang == "hi") "Camera khol raha hoon, $userName"
                else "Opening camera, $userName"
            }
            CommandAction.OPEN_GALLERY -> {
                if (lang == "hi") "Gallery khol raha hoon, $userName"
                else "Opening gallery, $userName"
            }
            CommandAction.PLAY_MUSIC -> {
                if (lang == "hi") "Music chala raha hoon, $userName"
                else "Playing music, $userName"
            }
            CommandAction.SET_ALARM -> {
                if (lang == "hi") "Alarm set kar raha hoon, $userName"
                else "Setting alarm, $userName"
            }
            CommandAction.SET_TIMER -> {
                if (lang == "hi") "Timer set kar raha hoon, $userName"
                else "Setting timer, $userName"
            }
            CommandAction.SET_REMINDER -> {
                if (lang == "hi") "Reminder set kar raha hoon, $userName"
                else "Setting reminder, $userName"
            }
            CommandAction.OPEN_SETTINGS -> {
                if (lang == "hi") "Settings khol raha hoon, $userName"
                else "Opening settings, $userName"
            }
            CommandAction.TOGGLE_WIFI -> {
                if (lang == "hi") "WiFi toggle kar raha hoon, $userName"
                else "Toggling WiFi, $userName"
            }
            CommandAction.TOGGLE_BLUETOOTH -> {
                if (lang == "hi") "Bluetooth toggle kar raha hoon, $userName"
                else "Toggling Bluetooth, $userName"
            }
            CommandAction.TOGGLE_FLASHLIGHT -> {
                if (lang == "hi") "Flashlight toggle kar raha hoon, $userName"
                else "Toggling flashlight, $userName"
            }
            CommandAction.INCREASE_VOLUME -> {
                if (lang == "hi") "Volume badha raha hoon, $userName"
                else "Increasing volume, $userName"
            }
            CommandAction.DECREASE_VOLUME -> {
                if (lang == "hi") "Volume kam kar raha hoon, $userName"
                else "Decreasing volume, $userName"
            }
            CommandAction.INCREASE_BRIGHTNESS -> {
                if (lang == "hi") "Brightness badha raha hoon, $userName"
                else "Increasing brightness, $userName"
            }
            CommandAction.DECREASE_BRIGHTNESS -> {
                if (lang == "hi") "Brightness kam kar raha hoon, $userName"
                else "Decreasing brightness, $userName"
            }
            CommandAction.OPEN_BROWSER -> {
                if (lang == "hi") "Browser khol raha hoon, $userName"
                else "Opening browser, $userName"
            }
            CommandAction.SEARCH_WEB -> {
                if (lang == "hi") "\"${command.target}\" search kar raha hoon, $userName"
                else "Searching for \"${command.target}\", $userName"
            }
            CommandAction.OPEN_YOUTUBE -> {
                if (lang == "hi") "YouTube khol raha hoon, $userName"
                else "Opening YouTube, $userName"
            }
            CommandAction.PLAY_VIDEO -> {
                if (lang == "hi") "Video chala raha hoon, $userName"
                else "Playing video, $userName"
            }
            CommandAction.BATTERY_STATUS -> ""  // handled dynamically
            CommandAction.TIME_NOW -> ""  // handled dynamically
            CommandAction.DATE_NOW -> ""  // handled dynamically
            CommandAction.NAVIGATE_TO -> {
                if (lang == "hi") "${command.target} ka navigation start kar raha hoon, $userName"
                else "Navigating to ${command.target}, $userName"
            }
            CommandAction.TAKE_SCREENSHOT -> {
                if (lang == "hi") "Screenshot le raha hoon, $userName"
                else "Taking screenshot, $userName"
            }
            CommandAction.OPEN_FILE_MANAGER -> {
                if (lang == "hi") "File manager khol raha hoon, $userName"
                else "Opening file manager, $userName"
            }
            CommandAction.OPEN_CALCULATOR -> {
                if (lang == "hi") "Calculator khol raha hoon, $userName"
                else "Opening calculator, $userName"
            }
            CommandAction.OPEN_CALENDAR -> {
                if (lang == "hi") "Calendar khol raha hoon, $userName"
                else "Opening calendar, $userName"
            }
            CommandAction.OPEN_NOTES -> {
                if (lang == "hi") "Notes khol raha hoon, $userName"
                else "Opening notes, $userName"
            }
            CommandAction.LOCK_SCREEN -> {
                if (lang == "hi") "Screen lock kar raha hoon, $userName"
                else "Locking screen, $userName"
            }
            CommandAction.SILENT_MODE -> {
                if (lang == "hi") "Silent mode on kar raha hoon, $userName"
                else "Enabling silent mode, $userName"
            }
            CommandAction.NORMAL_MODE -> {
                if (lang == "hi") "Normal mode on kar raha hoon, $userName"
                else "Switching to normal mode, $userName"
            }
            CommandAction.VIBRATE_MODE -> {
                if (lang == "hi") "Vibrate mode on kar raha hoon, $userName"
                else "Enabling vibrate mode, $userName"
            }
            CommandAction.WHO_ARE_YOU -> {
                if (lang == "hi") "Main Mr. RVK hoon, $userName. Aapka personal AI assistant. Aap mujhe koi bhi command de sakte hain aur main use turant execute karunga."
                else "I am Mr. RVK, $userName. Your personal AI assistant. You can give me any command and I will execute it instantly."
            }
            CommandAction.GREETING -> getGreeting(context)
            CommandAction.THANK_YOU -> {
                if (lang == "hi") "Aapka swagat hai, $userName! Main hamesha aapki seva mein hoon."
                else "You're welcome, $userName! I'm always at your service."
            }
            CommandAction.HOW_ARE_YOU -> {
                if (lang == "hi") "Main bilkul theek hoon, $userName! Aapki seva ke liye hamesha taiyar. Aap bataiye, kya kar sakta hoon aapke liye?"
                else "I'm doing great, $userName! Always ready to serve you. How can I help you today?"
            }
            CommandAction.WEATHER -> {
                if (lang == "hi") "Mausam ki jaankaari le raha hoon, $userName"
                else "Getting weather information, $userName"
            }
            CommandAction.READ_NOTIFICATION -> {
                if (lang == "hi") "Notifications padh raha hoon, $userName"
                else "Reading notifications, $userName"
            }
            CommandAction.UNKNOWN -> {
                if (lang == "hi") "Maaf kijiye $userName, yeh command mujhe samajh nahi aaya. Kya aap dubara bata sakte hain?"
                else "I'm sorry $userName, I didn't understand that command. Could you please repeat?"
            }
        }
    }

    fun getBatteryResponse(context: Context, level: Int, isCharging: Boolean): String {
        val userName = MrRvkApp.getUserName(context)
        val lang = detectLanguage(context)
        val chargingText = if (isCharging) {
            if (lang == "hi") " aur charge ho rahi hai" else " and charging"
        } else ""

        return if (lang == "hi") {
            "$userName, aapki battery $level percent hai$chargingText."
        } else {
            "$userName, your battery is at $level percent$chargingText."
        }
    }

    fun getTimeResponse(context: Context): String {
        val userName = MrRvkApp.getUserName(context)
        val lang = detectLanguage(context)
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        val minute = cal.get(Calendar.MINUTE)
        val amPm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"

        return if (lang == "hi") {
            "$userName, abhi samay hai $hour baje $minute minute $amPm."
        } else {
            "$userName, the current time is $hour:${String.format("%02d", minute)} $amPm."
        }
    }

    fun getDateResponse(context: Context): String {
        val userName = MrRvkApp.getUserName(context)
        val lang = detectLanguage(context)
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)

        val dayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val dayNameHi = arrayOf("Ravivar", "Somvar", "Mangalvar", "Budhvar", "Guruvar", "Shukravar", "Shanivar")
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1

        return if (lang == "hi") {
            "$userName, aaj ki date hai $day/$month/$year, ${dayNameHi[dayOfWeek]}."
        } else {
            "$userName, today's date is $day/$month/$year, ${dayNames[dayOfWeek]}."
        }
    }

    private fun detectLanguage(context: Context): String {
        return MrRvkApp.getLanguage(context)
    }

    fun detectLanguageFromText(text: String): String {
        val hindiPatterns = listOf(
            "khol", "kar", "bata", "bhej", "dikha", "chala", "band",
            "badha", "kam", "laga", "baja", "hoon", "hai", "kya",
            "mera", "meri", "mere", "kaise", "kitna", "kitne",
            "kaun", "kahan", "kab", "aaj", "kal", "abhi",
            "raha", "rahi", "rahe", "karo", "kijiye", "dijiye",
            "boliye", "suniye", "bhai", "yaar", "ji", "sahab",
            "namaskar", "namaste", "shukriya", "dhanyavaad"
        )

        val lowerText = text.lowercase()
        val hindiWordCount = hindiPatterns.count { lowerText.contains(it) }

        return if (hindiWordCount >= 1) "hi" else "en"
    }
}
