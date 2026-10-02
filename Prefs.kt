package com.tony.mailtraducteur

import android.content.Context

object Prefs {
    const val OUTLOOK = "com.microsoft.office.outlook"
    val MAIL_PACKAGES = setOf(OUTLOOK)

    private const val FILE = "reglages"
    private const val K_NAMES = "noms"
    private const val K_VOICE = "voix"
    private const val K_SILENT = "respecter_silencieux"
    private const val K_LAST = "dernier_message"

    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun namesRaw(c: Context): String = sp(c).getString(K_NAMES, "Tony, Kaseki") ?: ""
    fun names(c: Context): List<String> =
        namesRaw(c).split(',', ';', '\n').map { it.trim() }.filter { it.length >= 2 }
    fun setNames(c: Context, v: String) = sp(c).edit().putString(K_NAMES, v).apply()

    fun voiceEnabled(c: Context) = sp(c).getBoolean(K_VOICE, true)
    fun setVoice(c: Context, v: Boolean) = sp(c).edit().putBoolean(K_VOICE, v).apply()

    fun respectSilent(c: Context) = sp(c).getBoolean(K_SILENT, true)
    fun setRespectSilent(c: Context, v: Boolean) = sp(c).edit().putBoolean(K_SILENT, v).apply()

    fun lastMessage(c: Context): String? = sp(c).getString(K_LAST, null)
    fun setLastMessage(c: Context, v: String) = sp(c).edit().putString(K_LAST, v).apply()
}
