package com.tony.mailtraducteur

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.concurrent.atomic.AtomicBoolean

data class TranslationResult(
    val text: String,
    val sourceLang: String,
    val translated: Boolean,
    val error: String? = null
)

/** Traduction vers le français, entièrement sur le téléphone (ML Kit). */
object FrTranslator {

    /** Au-delà, on lit le texte original plutôt que de rester muet. */
    private const val TIMEOUT_MS = 15_000L

    private val main = Handler(Looper.getMainLooper())

    private val identifier by lazy {
        LanguageIdentification.getClient(
            LanguageIdentificationOptions.Builder().setConfidenceThreshold(0.35f).build()
        )
    }

    fun toFrench(context: Context, text: String, callback: (TranslationResult) -> Unit) {
        val c = context.applicationContext
        val done = AtomicBoolean(false)
        fun finish(r: TranslationResult) {
            if (done.compareAndSet(false, true)) main.post { callback(r) }
        }

        main.postDelayed({
            if (!done.get()) {
                Journal.log(c, "⏱ Traduction trop longue (modèle en téléchargement ?) → lecture du texte original")
                finish(
                    TranslationResult(
                        text, "en", false,
                        "Traduction pas encore prête : le modèle se télécharge. Réessaie dans quelques minutes."
                    )
                )
            }
        }, TIMEOUT_MS)

        try {
            identifier.identifyLanguage(text)
                .addOnSuccessListener { tag ->
                    Journal.log(c, "Langue détectée : $tag")
                    translateFrom(c, tag, text, ::finish)
                }
                .addOnFailureListener { e ->
                    Journal.log(c, "⚠ Détection de langue impossible (${e.message}) → on suppose l'anglais")
                    translateFrom(c, LanguageIdentifier.UNDETERMINED_LANGUAGE_TAG, text, ::finish)
                }
        } catch (e: Exception) {
            Journal.log(c, "❌ Erreur détection : ${e.message}")
            finish(TranslationResult(text, "en", false, e.message))
        }
    }

    private fun translateFrom(c: Context, tag: String, text: String, finish: (TranslationResult) -> Unit) {
        if (tag == "fr") {
            finish(TranslationResult(text, "fr", false))
            return
        }
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
        Journal.log(c, "Préparation du modèle $source → fr…")
        translator.downloadModelIfNeeded(DownloadConditions.Builder().build())
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { out ->
                        translator.close()
                        Journal.log(c, "✅ Traduction réussie")
                        finish(TranslationResult(out, source, true))
                    }
                    .addOnFailureListener { err ->
                        translator.close()
                        Journal.log(c, "❌ Échec de traduction : ${err.message}")
                        finish(TranslationResult(text, source, false, err.message))
                    }
            }
            .addOnFailureListener { err ->
                translator.close()
                Journal.log(c, "❌ Modèle $source → fr indisponible : ${err.message}")
                finish(TranslationResult(text, source, false, "Modèle de traduction indisponible (connexion internet ?)"))
            }
    }

    /** Pré-télécharge les modèles les plus utiles pour pouvoir traduire sans internet. */
    fun downloadModels(context: Context, onDone: (String) -> Unit) {
        val c = context.applicationContext
        val langs = listOf(TranslateLanguage.ENGLISH, TranslateLanguage.SPANISH, TranslateLanguage.PORTUGUESE)
        var remaining = langs.size
        var failures = 0
        Journal.log(c, "Téléchargement des modèles en, es, pt → fr…")
        langs.forEach { lang ->
            val tr = Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(lang)
                    .setTargetLanguage(TranslateLanguage.FRENCH)
                    .build()
            )
            tr.downloadModelIfNeeded(DownloadConditions.Builder().build())
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Journal.log(c, "✅ Modèle $lang prêt")
                    } else {
                        failures++
                        Journal.log(c, "❌ Modèle $lang : ${task.exception?.message}")
                    }
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

    /** Liste des langues déjà téléchargées sur le téléphone. */
    fun downloadedModels(onResult: (List<String>) -> Unit) {
        RemoteModelManager.getInstance()
            .getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener { models -> onResult(models.map { it.language }.sorted()) }
            .addOnFailureListener { onResult(emptyList()) }
    }
}
