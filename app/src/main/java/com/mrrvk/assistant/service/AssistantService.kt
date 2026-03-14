package com.mrrvk.assistant.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mrrvk.assistant.MrRvkApp
import com.mrrvk.assistant.R
import com.mrrvk.assistant.command.CommandExecutor
import com.mrrvk.assistant.ui.MainActivity
import com.mrrvk.assistant.util.ResponseGenerator

class AssistantService : Service() {

    companion object {
        private const val TAG = "AssistantService"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.mrrvk.assistant.START"
        const val ACTION_STOP = "com.mrrvk.assistant.STOP"

        fun start(context: Context) {
            val intent = Intent(context, AssistantService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, AssistantService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    interface AssistantListener {
        fun onWakeWordDetected()
        fun onListeningStarted()
        fun onListeningStopped()
        fun onCommandRecognized(text: String)
        fun onSpeaking(text: String)
        fun onError(message: String)
        fun onPartialResult(text: String)
    }

    private val binder = AssistantBinder()
    private var listener: AssistantListener? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: SciFiTTS? = null
    private var commandExecutor: CommandExecutor? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var isListeningForWakeWord = false
    private var isListeningForCommand = false
    private var isProcessingCommand = false

    inner class AssistantBinder : Binder() {
        fun getService(): AssistantService = this@AssistantService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun setListener(l: AssistantListener?) {
        listener = l
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        tts = SciFiTTS(this)
        tts?.init(object : SciFiTTS.TTSCallback {
            override fun onSpeakStart() {}
            override fun onSpeakDone() {
                if (!isProcessingCommand) {
                    startWakeWordListening()
                }
            }
        })

        commandExecutor = CommandExecutor(this)
        commandExecutor?.setCallback(object : CommandExecutor.CommandCallback {
            override fun onSpeak(text: String) {
                listener?.onSpeaking(text)
                tts?.speak(text)
            }

            override fun onCommandExecuted(success: Boolean, message: String) {
                isProcessingCommand = false
                // Resume wake word listening after command execution
                android.os.Handler(mainLooper).postDelayed({
                    if (!tts!!.isSpeaking()) {
                        startWakeWordListening()
                    }
                }, 2000)
            }
        })

        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
                startWakeWordListening()
            }
            ACTION_STOP -> {
                stopListening()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, MrRvkApp.CHANNEL_ID)
            .setContentTitle("Mr. RVK Active")
            .setContentText("Listening for \"Mr. RVK\" wake word...")
            .setSmallIcon(R.drawable.ic_assistant)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun startWakeWordListening() {
        if (isListeningForCommand || isProcessingCommand) return

        isListeningForWakeWord = true
        isListeningForCommand = false

        destroySpeechRecognizer()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(WakeWordListener())

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "hi-IN")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "en-US", "hi-IN"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            }

            speechRecognizer?.startListening(intent)
            Log.d(TAG, "Wake word listening started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting wake word listening", e)
            // Retry after a delay
            android.os.Handler(mainLooper).postDelayed({
                startWakeWordListening()
            }, 3000)
        }
    }

    private fun startCommandListening() {
        isListeningForWakeWord = false
        isListeningForCommand = true

        destroySpeechRecognizer()
        listener?.onListeningStarted()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(CommandListener())

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "en-US", "hi-IN"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            }

            speechRecognizer?.startListening(intent)
            Log.d(TAG, "Command listening started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting command listening", e)
            listener?.onError("Speech recognition error")
            startWakeWordListening()
        }
    }

    private fun processWakeWord(text: String): Boolean {
        val lower = text.lowercase().trim()
        val wakePatterns = listOf(
            "mr rvk", "mr. rvk", "mister rvk",
            "mr r v k", "mr. r v k", "mister r v k",
            "mr rvk", "mrrvk", "mr arvk",
            "hey rvk", "ok rvk", "hi rvk",
            "mr r b k", "mr. r b k",
            "mr rv k", "mr arv k",
            "mr rbk", "mr. rbk",
            "mister rbk", "mister arvk"
        )

        return wakePatterns.any { lower.contains(it) }
    }

    private fun onWakeWordTriggered() {
        Log.d(TAG, "Wake word detected!")
        isListeningForWakeWord = false

        listener?.onWakeWordDetected()
        tts?.playActivationBeep()

        val wakeResponse = ResponseGenerator.getWakeResponse(this)
        listener?.onSpeaking(wakeResponse)
        tts?.speak(wakeResponse)

        // Start command listening after brief delay for TTS
        android.os.Handler(mainLooper).postDelayed({
            startCommandListening()
        }, 1500)
    }

    private fun processCommand(text: String) {
        if (text.isBlank()) {
            startWakeWordListening()
            return
        }

        isProcessingCommand = true
        isListeningForCommand = false
        listener?.onListeningStopped()
        listener?.onCommandRecognized(text)

        // Detect language and update
        val detectedLang = ResponseGenerator.detectLanguageFromText(text)
        MrRvkApp.setLanguage(this, detectedLang)

        // Check if the command text also contains wake word (sometimes user says "Mr RVK open WhatsApp")
        var commandText = text
        val wakePatterns = listOf("mr rvk ", "mr. rvk ", "mister rvk ", "hey rvk ", "ok rvk ")
        for (pattern in wakePatterns) {
            if (commandText.lowercase().startsWith(pattern)) {
                commandText = commandText.substring(pattern.length).trim()
                break
            }
        }

        tts?.playDeactivationBeep()

        android.os.Handler(mainLooper).postDelayed({
            commandExecutor?.executeFromText(commandText)
        }, 200)
    }

    fun executeDirectCommand(text: String) {
        stopListening()
        processCommand(text)
    }

    private fun stopListening() {
        isListeningForWakeWord = false
        isListeningForCommand = false
        destroySpeechRecognizer()
    }

    private fun destroySpeechRecognizer() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying speech recognizer", e)
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MrRVK::AssistantWakeLock"
        )
        wakeLock?.acquire(Long.MAX_VALUE)
    }

    override fun onDestroy() {
        stopListening()
        tts?.shutdown()
        wakeLock?.release()
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }

    // Wake Word Listener
    private inner class WakeWordListener : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Wake: Ready for speech")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Wake: Speech beginning")
        }

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(TAG, "Wake: End of speech")
        }

        override fun onError(error: Int) {
            Log.d(TAG, "Wake: Error $error")
            if (isListeningForWakeWord && !isProcessingCommand) {
                android.os.Handler(mainLooper).postDelayed({
                    startWakeWordListening()
                }, 500)
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            Log.d(TAG, "Wake results: $matches")

            if (matches != null) {
                for (match in matches) {
                    if (processWakeWord(match)) {
                        onWakeWordTriggered()
                        return
                    }
                }

                // Check if the result itself is a command (user might say "Mr RVK open WhatsApp")
                val fullText = matches.firstOrNull() ?: ""
                if (processWakeWord(fullText)) {
                    // Extract command part after wake word
                    onWakeWordTriggered()
                    return
                }
            }

            // No wake word, restart listening
            if (isListeningForWakeWord) {
                android.os.Handler(mainLooper).postDelayed({
                    startWakeWordListening()
                }, 300)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (matches != null) {
                for (match in matches) {
                    if (processWakeWord(match)) {
                        onWakeWordTriggered()
                        return
                    }
                }
                listener?.onPartialResult(matches.firstOrNull() ?: "")
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    // Command Listener
    private inner class CommandListener : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Command: Ready for speech")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Command: Speech beginning")
        }

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(TAG, "Command: End of speech")
            listener?.onListeningStopped()
        }

        override fun onError(error: Int) {
            Log.d(TAG, "Command: Error $error")
            isListeningForCommand = false
            listener?.onListeningStopped()

            if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                val userName = MrRvkApp.getUserName(this@AssistantService)
                val lang = MrRvkApp.getLanguage(this@AssistantService)
                val msg = if (lang == "hi") {
                    "$userName, koi command nahi suni. Kya aap dubara bolenge?"
                } else {
                    "$userName, I didn't catch that. Could you please repeat?"
                }
                listener?.onSpeaking(msg)
                tts?.speak(msg)
            }

            android.os.Handler(mainLooper).postDelayed({
                startWakeWordListening()
            }, 3000)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            Log.d(TAG, "Command results: $text")

            if (text.isNotBlank()) {
                processCommand(text)
            } else {
                startWakeWordListening()
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            if (text.isNotBlank()) {
                listener?.onPartialResult(text)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
