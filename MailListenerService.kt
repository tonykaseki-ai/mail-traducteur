package com.tony.mailtraducteur

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Écoute les notifications d'Outlook. Quand un mail cite ton nom,
 * il est traduit en français, affiché et lu à voix haute.
 */
class MailListenerService : NotificationListenerService() {

    // Évite de lire deux fois le même mail (Outlook met parfois à jour sa notification)
    private val recent = LinkedHashMap<Int, Long>()

    override fun onListenerConnected() {
        super.onListenerConnected()
        Journal.log(this, "🟢 Écoute des notifications connectée")
        Speaker.init(this)
    }

    override fun onListenerDisconnected() {
        Journal.log(this, "🔴 Écoute des notifications coupée par Android → reconnexion demandée")
        NotificationListenerService.requestRebind(ComponentName(this, MailListenerService::class.java))
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (pkg !in Prefs.MAIL_PACKAGES) {
            // Aide au diagnostic : signale les autres applis de mail
            val p = pkg.lowercase()
            if ("mail" in p || "outlook" in p) Journal.log(this, "Notification ignorée d'une autre appli : $pkg")
            return
        }

        val n = sbn.notification ?: return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            Journal.log(this, "Outlook : résumé de groupe ignoré")
            return
        }
        if (n.flags and Notification.FLAG_ONGOING_EVENT != 0) return

        val mail = MailExtractor.extract(pkg, n)
        if (mail == null) {
            Journal.log(this, "Outlook : notification sans texte (contenu masqué ?)")
            return
        }
        Journal.log(this, "📨 Outlook : de « ${mail.sender} » — « ${mail.searchableText.replace('\n', ' ').take(70)} »")

        val names = Prefs.names(this)
        val found = NameMatcher.matches(mail.searchableText, names)
        if (!found && !Prefs.allMails(this)) {
            Journal.log(this, "Nom (${names.joinToString(", ")}) absent de l'aperçu → ignoré")
            return
        }
        Journal.log(this, if (found) "✅ Ton nom est cité" else "Mode « tous les mails » activé")

        val signature = (mail.sender + "|" + mail.searchableText).hashCode()
        val now = System.currentTimeMillis()
        recent.entries.removeAll { now - it.value > 15 * 60 * 1000 }
        if (recent.containsKey(signature)) {
            Journal.log(this, "Déjà lu il y a peu → ignoré")
            return
        }
        recent[signature] = now

        MailProcessor.process(this, mail)
    }
}
