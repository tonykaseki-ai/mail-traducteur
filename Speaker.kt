package com.tony.mailtraducteur

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Lecture à voix haute en français. */
object Speaker : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ready = false
    private val pending = mutableListOf<String>()

    private fun ensure(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.FRENCH
            tts?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            ready = true
            pending.forEach { say(it) }
        } else {
            tts = null
        }
        pending.clear()
    }

    fun speak(context: Context, text: String) {
        ensure(context)
        if (ready) say(text) else pending.add(text)
    }

    fun stop() {
        tts?.stop()
    }

    private fun say(text: String) {
        val max = TextToSpeech.getMaxSpeechInputLength() - 50
        text.chunked(max).forEach { part ->
            tts?.speak(part, TextToSpeech.QUEUE_ADD, null, "mail-" + System.nanoTime())
        }
    }
}
