package com.tony.mailtraducteur

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.AudioManager

/** Traduit le mail, affiche la notification sur l'écran de verrouillage et le lit à voix haute. */
object MailProcessor {

    private const val CHANNEL_ID = "traductions"

    fun process(context: Context, mail: MailInfo) {
        val app = context.applicationContext
        Speaker.init(app)
        Journal.log(app, "Traitement du mail de « ${mail.sender} »…")
        FrTranslator.toFrench(app, mail.textToTranslate) { result ->
            val sender = mail.sender.ifEmpty { "Expéditeur inconnu" }
            val langName = java.util.Locale(result.sourceLang).getDisplayLanguage(java.util.Locale.FRENCH)
            val header = if (result.translated) "Traduit de l'$langName" else "Mail en français"

            val spoken = buildString {
                append("Nouveau mail de ").append(sender).append(". ")
                append(result.text)
            }
            Prefs.setLastMessage(app, spoken)

            showNotification(app, mail, sender, header, result)

            when {
                !Prefs.voiceEnabled(app) -> Journal.log(app, "Voix désactivée dans les réglages")
                !canSpeak(app) -> Journal.log(app, "🔇 Téléphone en silencieux/vibreur : pas de lecture")
                else -> {
                    Journal.log(app, "🔊 Lecture à voix haute")
                    Speaker.speak(app, spoken)
                }
            }
        }
    }

    private fun canSpeak(c: Context): Boolean {
        if (!Prefs.respectSilent(c)) return true
        val am = c.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return am.ringerMode == AudioManager.RINGER_MODE_NORMAL
    }

    private fun showNotification(c: Context, mail: MailInfo, sender: String, header: String, r: TranslationResult) {
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val ch = NotificationChannel(CHANNEL_ID, "Mails traduits", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Mails qui te mentionnent, traduits en français"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(ch)
        }

        val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        // Bouton "Relire" utilisable sans déverrouiller
        val replay = Intent(c, SpeakReceiver::class.java).apply {
            action = SpeakReceiver.ACTION_SPEAK
            putExtra(SpeakReceiver.EXTRA_TEXT, "Mail de $sender. ${r.text}")
        }
        val replayPi = PendingIntent.getBroadcast(c, id, replay, flags)

        val stop = Intent(c, SpeakReceiver::class.java).apply { action = SpeakReceiver.ACTION_STOP }
        val stopPi = PendingIntent.getBroadcast(c, id + 1, stop, flags)

        val details = buildString {
            append(r.text)
            if (r.error != null) append("\n\n⚠ ").append(r.error)
            append("\n\n— ").append(header)
        }

        val builder = Notification.Builder(c, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("✉ $sender")
            .setContentText(r.text)
            .setStyle(Notification.BigTextStyle().bigText(details))
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_EMAIL)
            .setAutoCancel(true)
            .addAction(Notification.Action.Builder(Icon.createWithResource(c, R.drawable.ic_notif), "🔊 Relire", replayPi).build())
            .addAction(Notification.Action.Builder(Icon.createWithResource(c, R.drawable.ic_notif), "⏹ Stop", stopPi).build())

        c.packageManager.getLaunchIntentForPackage(mail.pkg)?.let { launch ->
            builder.setContentIntent(PendingIntent.getActivity(c, id + 2, launch, flags))
        }

        if (!nm.areNotificationsEnabled()) {
            Journal.log(c, "⚠ Notifications de Mail Traducteur bloquées dans les réglages Android")
        }
        try {
            nm.notify(id, builder.build())
            Journal.log(c, "📩 Notification affichée")
        } catch (e: SecurityException) {
            Journal.log(c, "❌ Notification refusée : ${e.message}")
        }
    }
}
