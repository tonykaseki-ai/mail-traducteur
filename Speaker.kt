package com.tony.mailtraducteur

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Lecture à voix haute en français. */
object Speaker {

    private var tts: TextToSpeech? = null
    private var ready = false
    private val pending = mutableListOf<String>()
    private var appContext: Context? = null

    /** Texte d'état lisible, affiché dans l'appli. */
    var status: String = "Voix pas encore initialisée"
        private set
    var frenchMissing = false
        private set

    fun init(context: Context) {
        if (tts != null) return
        val c = context.applicationContext
        appContext = c
        status = "Voix en cours d'initialisation…"
        tts = TextToSpeech(c) { code -> onInit(code) }
    }

    private fun onInit(code: Int) {
        val c = appContext
        if (code != TextToSpeech.SUCCESS) {
            status = "❌ Moteur de synthèse vocale indisponible"
            c?.let { Journal.log(it, status) }
            tts = null
            pending.clear()
            return
        }
        val res = tts?.setLanguage(Locale.FRENCH) ?: TextToSpeech.LANG_NOT_SUPPORTED
        frenchMissing = res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED
        tts?.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        status = if (frenchMissing) "⚠ Voix française non installée" else "✅ Voix française prête"
        c?.let { Journal.log(it, status) }
        ready = true
        pending.forEach { say(it) }
        pending.clear()
    }

    fun speak(context: Context, text: String) {
        init(context)
        if (ready) say(text) else pending.add(text)
    }

    fun stop() {
        tts?.stop()
    }

    private fun say(text: String) {
        val max = TextToSpeech.getMaxSpeechInputLength() - 50
        text.chunked(max).forEach { part ->
            val r = tts?.speak(part, TextToSpeech.QUEUE_ADD, null, "mail-" + System.nanoTime())
            if (r != TextToSpeech.SUCCESS) appContext?.let { Journal.log(it, "❌ La voix a refusé de lire (code $r)") }
        }
    }
}
