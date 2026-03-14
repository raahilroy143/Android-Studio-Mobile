package com.mrrvk.assistant.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class SciFiTTS(private val context: Context) {

    interface TTSCallback {
        fun onSpeakStart()
        fun onSpeakDone()
    }

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var callback: TTSCallback? = null
    private val pendingQueue = mutableListOf<String>()

    fun init(cb: TTSCallback? = null) {
        callback = cb
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
                tts?.let { engine ->
                    engine.language = Locale.US
                    engine.setSpeechRate(1.0f)
                    engine.setPitch(0.85f)

                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            callback?.onSpeakStart()
                        }

                        override fun onDone(utteranceId: String?) {
                            callback?.onSpeakDone()
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            callback?.onSpeakDone()
                        }
                    })
                }

                // Process pending queue
                synchronized(pendingQueue) {
                    pendingQueue.forEach { speak(it) }
                    pendingQueue.clear()
                }
            } else {
                Log.e("SciFiTTS", "TTS initialization failed with status: $status")
            }
        }
    }

    fun speak(text: String) {
        if (!isReady) {
            synchronized(pendingQueue) {
                pendingQueue.add(text)
            }
            return
        }

        // Detect language and set accordingly
        val isHindi = containsHindi(text)
        tts?.let { engine ->
            if (isHindi) {
                val hindiLocale = Locale("hi", "IN")
                if (engine.isLanguageAvailable(hindiLocale) >= TextToSpeech.LANG_AVAILABLE) {
                    engine.language = hindiLocale
                }
            } else {
                engine.language = Locale.US
            }

            // Sci-fi voice parameters
            engine.setPitch(0.85f)
            engine.setSpeechRate(0.95f)

            val params = android.os.Bundle()
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)

            engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, "mrrvk_${System.currentTimeMillis()}")
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }

    fun isSpeaking(): Boolean {
        return tts?.isSpeaking == true
    }

    private fun containsHindi(text: String): Boolean {
        val hindiWords = listOf(
            "hoon", "hai", "hain", "kya", "mein", "aap", "aapka",
            "kar", "raha", "rahi", "rahe", "nahi", "abhi", "aaj",
            "kal", "khol", "bhej", "dikha", "chala", "band",
            "boliye", "shukriya", "namaskar", "namaste", "ji"
        )
        val lower = text.lowercase()
        return hindiWords.any { lower.contains(it) }
    }

    fun playActivationBeep() {
        Thread {
            try {
                val sampleRate = 44100
                val duration = 0.15
                val numSamples = (duration * sampleRate).toInt()
                val samples = ShortArray(numSamples)

                // Generate a sci-fi activation beep (ascending tone)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val freq = 800.0 + (1200.0 * t / duration)
                    val envelope = if (t < duration * 0.1) t / (duration * 0.1)
                    else if (t > duration * 0.7) (duration - t) / (duration * 0.3)
                    else 1.0
                    samples[i] = (Short.MAX_VALUE * 0.6 * envelope *
                            Math.sin(2.0 * Math.PI * freq * t)).toInt().toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .build()
                    )
                    .setBufferSizeInBytes(numSamples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, numSamples)
                audioTrack.play()

                Thread.sleep((duration * 1000).toLong() + 100)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                Log.e("SciFiTTS", "Error playing beep", e)
            }
        }.start()
    }

    fun playDeactivationBeep() {
        Thread {
            try {
                val sampleRate = 44100
                val duration = 0.12
                val numSamples = (duration * sampleRate).toInt()
                val samples = ShortArray(numSamples)

                // Generate descending tone
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val freq = 1600.0 - (800.0 * t / duration)
                    val envelope = if (t < duration * 0.1) t / (duration * 0.1)
                    else if (t > duration * 0.7) (duration - t) / (duration * 0.3)
                    else 1.0
                    samples[i] = (Short.MAX_VALUE * 0.5 * envelope *
                            Math.sin(2.0 * Math.PI * freq * t)).toInt().toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .build()
                    )
                    .setBufferSizeInBytes(numSamples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, numSamples)
                audioTrack.play()

                Thread.sleep((duration * 1000).toLong() + 100)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                Log.e("SciFiTTS", "Error playing beep", e)
            }
        }.start()
    }
}
