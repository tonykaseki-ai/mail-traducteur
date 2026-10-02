package com.tony.mailtraducteur

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

data class TranslationResult(
    val text: String,
    val sourceLang: String,
    val translated: Boolean,
    val error: String? = null
)

/** Traduction vers le français, entièrement sur le téléphone (ML Kit). */
object FrTranslator {

    private val identifier by lazy {
        LanguageIdentification.getClient(
            LanguageIdentificationOptions.Builder().setConfidenceThreshold(0.35f).build()
        )
    }

    fun toFrench(text: String, callback: (TranslationResult) -> Unit) {
        identifier.identifyLanguage(text)
            .addOnSuccessListener { tag -> translateFrom(tag, text, callback) }
            .addOnFailureListener { translateFrom(LanguageIdentifier.UNDETERMINED_LANGUAGE_TAG, text, callback) }
    }

    private fun translateFrom(tag: String, text: String, callback: (TranslationResult) -> Unit) {
        if (tag == "fr") {
            callback(TranslationResult(text, "fr", false))
            return
        }
        // Langue inconnue : on suppose de l'anglais (cas le plus fréquent).
        val source = if (tag == LanguageIdentifier.UNDETERMINED_LANGUAGE_TAG) {
            TranslateLanguage.ENGLISH
        } else {
            TranslateLanguage.fromLanguageTag(tag) ?: TranslateLanguage.ENGLISH
        }

        val translator = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(source)
                .setTargetLanguage(TranslateLanguage.FRENCH)
                .build()
        )
        translator.downloadModelIfNeeded(DownloadConditions.Builder().build())
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { out ->
                        translator.close()
                        callback(TranslationResult(out, source, true))
                    }
                    .addOnFailureListener { err ->
                        translator.close()
                        callback(TranslationResult(text, source, false, err.message))
                    }
            }
            .addOnFailureListener { err ->
                translator.close()
                callback(TranslationResult(text, source, false, "Modèle de traduction indisponible : ${err.message}"))
            }
    }

    /** Pré-télécharge les modèles les plus utiles pour pouvoir traduire sans internet. */
    fun downloadModels(onDone: (String) -> Unit) {
        val langs = listOf(TranslateLanguage.ENGLISH, TranslateLanguage.SPANISH, TranslateLanguage.PORTUGUESE)
        var remaining = langs.size
        var failures = 0
        langs.forEach { lang ->
            val tr = Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(lang)
                    .setTargetLanguage(TranslateLanguage.FRENCH)
                    .build()
            )
            tr.downloadModelIfNeeded(DownloadConditions.Builder().build())
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) failures++
                    tr.close()
                    remaining--
                    if (remaining == 0) {
                        onDone(
                            if (failures == 0) "Modèles anglais, espagnol et portugais prêts ✔"
                            else "$failures modèle(s) non téléchargé(s). Vérifie ta connexion."
                        )
                    }
                }
        }
    }
}
