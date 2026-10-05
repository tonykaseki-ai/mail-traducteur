package com.tony.mailtraducteur

import android.content.Context

object Prefs {
    const val OUTLOOK = "com.microsoft.office.outlook"
    val MAIL_PACKAGES = setOf(OUTLOOK, "com.microsoft.outlooklite")

    private const val FILE = "reglages"
    private const val K_NAMES = "noms"
    private const val K_VOICE = "voix"
    private const val K_SILENT = "respecter_silencieux_v2"
    private const val K_ALL = "tous_les_mails"
    private const val K_LAST = "dernier_message"

    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun namesRaw(c: Context): String = sp(c).getString(K_NAMES, "Tony, Kaseki") ?: ""
    fun names(c: Context): List<String> =
        namesRaw(c).split(',', ';', '\n').map { it.trim() }.filter { it.length >= 2 }
    fun setNames(c: Context, v: String) = sp(c).edit().putString(K_NAMES, v).apply()

    fun voiceEnabled(c: Context) = sp(c).getBoolean(K_VOICE, true)
    fun setVoice(c: Context, v: Boolean) = sp(c).edit().putBoolean(K_VOICE, v).apply()

    /** Désactivé par défaut : l'appli parle même en vibreur. */
    fun respectSilent(c: Context) = sp(c).getBoolean(K_SILENT, false)
    fun setRespectSilent(c: Context, v: Boolean) = sp(c).edit().putBoolean(K_SILENT, v).apply()

    /** Mode essai : réagir à tous les mails, même sans ton nom. */
    fun allMails(c: Context) = sp(c).getBoolean(K_ALL, false)
    fun setAllMails(c: Context, v: Boolean) = sp(c).edit().putBoolean(K_ALL, v).apply()

    fun lastMessage(c: Context): String? = sp(c).getString(K_LAST, null)
    fun setLastMessage(c: Context, v: String) = sp(c).edit().putString(K_LAST, v).apply()
}
