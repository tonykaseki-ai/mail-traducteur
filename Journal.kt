package com.tony.mailtraducteur

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Petit journal visible dans l'appli, pour comprendre ce qui se passe. */
object Journal {
    private const val FILE = "journal"
    private const val KEY = "lignes"
    private const val MAX = 60

    @Synchronized
    fun log(c: Context, msg: String) {
        Log.d("MailTraducteur", msg)
        val sp = c.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val time = SimpleDateFormat("dd/MM HH:mm:ss", Locale.FRENCH).format(Date())
        val lines = (sp.getString(KEY, "") ?: "").split('\n').filter { it.isNotBlank() }.toMutableList()
        lines.add(0, "$time  $msg")
        sp.edit().putString(KEY, lines.take(MAX).joinToString("\n")).apply()
    }

    fun read(c: Context): String =
        c.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY, "")?.ifBlank { "(journal vide)" } ?: "(journal vide)"

    fun clear(c: Context) {
        c.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
