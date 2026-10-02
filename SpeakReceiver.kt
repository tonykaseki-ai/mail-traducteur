package com.tony.mailtraducteur

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SpeakReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SPEAK = "com.tony.mailtraducteur.SPEAK"
        const val ACTION_STOP = "com.tony.mailtraducteur.STOP"
        const val EXTRA_TEXT = "texte"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_STOP -> Speaker.stop()
            ACTION_SPEAK -> {
                val text = intent.getStringExtra(EXTRA_TEXT) ?: return
                Speaker.stop()
                Speaker.speak(context, text)
            }
        }
    }
}
