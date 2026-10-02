package com.tony.mailtraducteur

import android.app.Notification
import java.text.Normalizer

data class MailInfo(
    val pkg: String,
    val sender: String,
    val subject: String,
    val body: String
) {
    /** Texte dans lequel on cherche le nom (objet + contenu). */
    val searchableText: String get() = "$subject\n$body"

    /** Texte à traduire, sans doublon objet/contenu. */
    val textToTranslate: String
        get() = if (subject.isNotEmpty() && subject != body) "$subject.\n$body" else body
}

object MailExtractor {

    fun extract(pkg: String, n: Notification): MailInfo? {
        val e = n.extras ?: return null
        val title = e.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().trim()
        val text = e.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty().trim()
        val big = e.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty().trim()
        val lines = e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.joinToString("\n") { it.toString() }.orEmpty().trim()

        var body = when {
            big.isNotEmpty() -> big
            lines.isNotEmpty() -> lines
            else -> text
        }
        if (body.isEmpty()) return null

        // Outlook met souvent l'objet en 1re ligne du texte, puis l'aperçu du mail.
        val subject = text.lineSequence().firstOrNull()?.trim().orEmpty()
        if (subject.isNotEmpty() && body.startsWith(subject) && body.length > subject.length) {
            body = body.removePrefix(subject).trim()
        }
        return MailInfo(pkg, title, subject, body)
    }
}

object NameMatcher {

    private fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()

    fun matches(text: String, names: List<String>): Boolean {
        if (names.isEmpty()) return false
        val t = normalize(text)
        return names.any { name ->
            val n = normalize(name)
            Regex("(?<![\\p{L}\\p{N}])" + Regex.escape(n) + "(?![\\p{L}\\p{N}])").containsMatchIn(t)
        }
    }
}
