package com.mrrvk.assistant.util

data class ParsedCommand(
    val action: CommandAction,
    val target: String = "",
    val extras: String = "",
    val rawText: String = ""
)

enum class CommandAction {
    OPEN_APP,
    CALL_CONTACT,
    SEND_MESSAGE,
    SEND_WHATSAPP,
    TAKE_PHOTO,
    RECORD_VIDEO,
    OPEN_CAMERA,
    PLAY_MUSIC,
    SET_ALARM,
    SET_TIMER,
    SET_REMINDER,
    OPEN_SETTINGS,
    TOGGLE_WIFI,
    TOGGLE_BLUETOOTH,
    TOGGLE_FLASHLIGHT,
    INCREASE_VOLUME,
    DECREASE_VOLUME,
    INCREASE_BRIGHTNESS,
    DECREASE_BRIGHTNESS,
    OPEN_BROWSER,
    SEARCH_WEB,
    OPEN_GALLERY,
    READ_NOTIFICATION,
    BATTERY_STATUS,
    TIME_NOW,
    DATE_NOW,
    WEATHER,
    NAVIGATE_TO,
    OPEN_YOUTUBE,
    PLAY_VIDEO,
    TAKE_SCREENSHOT,
    OPEN_FILE_MANAGER,
    OPEN_CALCULATOR,
    OPEN_CALENDAR,
    OPEN_NOTES,
    LOCK_SCREEN,
    SILENT_MODE,
    NORMAL_MODE,
    VIBRATE_MODE,
    WHO_ARE_YOU,
    GREETING,
    THANK_YOU,
    HOW_ARE_YOU,
    UNKNOWN
}

object CommandParser {

    private data class CommandPattern(
        val keywords: List<String>,
        val action: CommandAction,
        val extractTarget: Boolean = false
    )

    private val patterns = listOf(
        // App launching
        CommandPattern(listOf("open ", "launch ", "start ", "chalu kar", "khol", "kholna", "khole"), CommandAction.OPEN_APP, true),

        // Calls
        CommandPattern(listOf("call ", "phone ", "dial ", "ring ", "call kar", "phone kar", "ko call", "ko phone"), CommandAction.CALL_CONTACT, true),

        // Messages
        CommandPattern(listOf("send message", "text ", "sms ", "message bhej", "msg bhej", "sms bhej", "message kar"), CommandAction.SEND_MESSAGE, true),
        CommandPattern(listOf("whatsapp message", "whatsapp bhej", "whatsapp par", "whatsapp pe", "whatsapp send"), CommandAction.SEND_WHATSAPP, true),

        // Camera
        CommandPattern(listOf("take photo", "take picture", "click photo", "photo le", "photo khich", "selfie", "tasveer", "photo capture"), CommandAction.TAKE_PHOTO),
        CommandPattern(listOf("record video", "video record", "video bana", "video le"), CommandAction.RECORD_VIDEO),
        CommandPattern(listOf("open camera", "camera khol", "camera on", "camera chalu"), CommandAction.OPEN_CAMERA),

        // Gallery
        CommandPattern(listOf("open gallery", "gallery khol", "photos dikha", "gallery dikha", "gallery open"), CommandAction.OPEN_GALLERY),

        // Music
        CommandPattern(listOf("play music", "play song", "gaana baja", "gaana chala", "music baja", "music chala", "song play"), CommandAction.PLAY_MUSIC, true),

        // Alarms & Timers
        CommandPattern(listOf("set alarm", "alarm set", "alarm laga", "alarm baja"), CommandAction.SET_ALARM, true),
        CommandPattern(listOf("set timer", "timer set", "timer laga", "timer chalu"), CommandAction.SET_TIMER, true),
        CommandPattern(listOf("remind", "yaad dila", "reminder set", "reminder laga"), CommandAction.SET_REMINDER, true),

        // Settings
        CommandPattern(listOf("open settings", "settings khol", "setting open", "setting khol"), CommandAction.OPEN_SETTINGS),

        // Toggles
        CommandPattern(listOf("wifi on", "wifi chalu", "wifi off", "wifi band", "toggle wifi", "wifi enable", "wifi disable"), CommandAction.TOGGLE_WIFI),
        CommandPattern(listOf("bluetooth on", "bluetooth chalu", "bluetooth off", "bluetooth band", "toggle bluetooth"), CommandAction.TOGGLE_BLUETOOTH),
        CommandPattern(listOf("flashlight", "torch", "flash on", "flash off", "torch on", "torch off", "torch chalu", "torch band"), CommandAction.TOGGLE_FLASHLIGHT),

        // Volume
        CommandPattern(listOf("volume up", "volume badha", "awaz badha", "increase volume", "volume increase"), CommandAction.INCREASE_VOLUME),
        CommandPattern(listOf("volume down", "volume kam", "awaz kam", "decrease volume", "volume decrease"), CommandAction.DECREASE_VOLUME),

        // Brightness
        CommandPattern(listOf("brightness up", "brightness badha", "brightness increase", "roshni badha"), CommandAction.INCREASE_BRIGHTNESS),
        CommandPattern(listOf("brightness down", "brightness kam", "brightness decrease", "roshni kam"), CommandAction.DECREASE_BRIGHTNESS),

        // Browser & Search
        CommandPattern(listOf("open browser", "browser khol", "chrome khol", "chrome open"), CommandAction.OPEN_BROWSER),
        CommandPattern(listOf("search ", "google ", "search kar", "google kar", "dhoondh", "khoj"), CommandAction.SEARCH_WEB, true),

        // YouTube
        CommandPattern(listOf("open youtube", "youtube khol", "youtube chala"), CommandAction.OPEN_YOUTUBE),
        CommandPattern(listOf("play video", "video chala", "youtube par", "youtube pe"), CommandAction.PLAY_VIDEO, true),

        // Battery & Status
        CommandPattern(listOf("battery", "charge", "kitna charge", "battery kitna", "battery status"), CommandAction.BATTERY_STATUS),
        CommandPattern(listOf("time", "samay", "kitne baje", "kya time", "what time", "time kya"), CommandAction.TIME_NOW),
        CommandPattern(listOf("date", "tarikh", "aaj ki date", "what date", "date kya", "aaj kya"), CommandAction.DATE_NOW),
        CommandPattern(listOf("weather", "mausam", "temperature"), CommandAction.WEATHER),

        // Navigation
        CommandPattern(listOf("navigate to", "direction", "rasta dikha", "kaise jaaye", "map"), CommandAction.NAVIGATE_TO, true),

        // Screenshot
        CommandPattern(listOf("screenshot", "screen capture", "screen shot"), CommandAction.TAKE_SCREENSHOT),

        // File Manager
        CommandPattern(listOf("file manager", "files", "file khol", "files open"), CommandAction.OPEN_FILE_MANAGER),

        // Calculator
        CommandPattern(listOf("calculator", "calc", "calculate", "hisab"), CommandAction.OPEN_CALCULATOR),

        // Calendar
        CommandPattern(listOf("calendar", "calender", "takvim"), CommandAction.OPEN_CALENDAR),

        // Notes
        CommandPattern(listOf("note", "notes", "likhna", "likh"), CommandAction.OPEN_NOTES),

        // Sound modes
        CommandPattern(listOf("silent mode", "silent kar", "chup kar", "silent", "mute"), CommandAction.SILENT_MODE),
        CommandPattern(listOf("normal mode", "sound on", "awaz chalu", "ringer on"), CommandAction.NORMAL_MODE),
        CommandPattern(listOf("vibrate mode", "vibrate", "vibration mode"), CommandAction.VIBRATE_MODE),

        // Identity
        CommandPattern(listOf("who are you", "kaun ho", "kaun hai", "tum kaun", "aap kaun", "tumhara naam", "your name"), CommandAction.WHO_ARE_YOU),

        // Greetings
        CommandPattern(listOf("hello", "hi ", "hey ", "namaste", "namaskar", "good morning", "good afternoon", "good evening", "good night"), CommandAction.GREETING),

        // Thanks
        CommandPattern(listOf("thank", "thanks", "shukriya", "dhanyavaad", "dhanyawad"), CommandAction.THANK_YOU),

        // How are you
        CommandPattern(listOf("how are you", "kaise ho", "kaisa hai", "kya haal", "kaisi ho"), CommandAction.HOW_ARE_YOU)
    )

    fun parse(input: String): ParsedCommand {
        val text = input.lowercase().trim()

        for (pattern in patterns) {
            for (keyword in pattern.keywords) {
                if (text.contains(keyword.lowercase())) {
                    val target = if (pattern.extractTarget) {
                        extractTarget(text, keyword.lowercase())
                    } else ""
                    return ParsedCommand(
                        action = pattern.action,
                        target = target,
                        rawText = input
                    )
                }
            }
        }

        return ParsedCommand(
            action = CommandAction.UNKNOWN,
            rawText = input
        )
    }

    private fun extractTarget(text: String, keyword: String): String {
        val idx = text.indexOf(keyword)
        if (idx == -1) return text

        val afterKeyword = text.substring(idx + keyword.length).trim()

        // Remove common filler words
        val cleaned = afterKeyword
            .replace(Regex("^(the |a |an |mera |meri |mere |ko |ka |ki |par |pe |se |do |karo |kar |please |bhai |yaar )"), "")
            .trim()

        return if (cleaned.isNotEmpty()) cleaned else afterKeyword
    }
}
