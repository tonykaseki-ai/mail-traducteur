package com.tony.mailtraducteur

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Écoute les notifications d'Outlook. Quand un mail cite ton nom,
 * il est traduit en français, affiché et lu à voix haute.
 */
class MailListenerService : NotificationListenerService() {

    // Évite de lire deux fois le même mail (Outlook met parfois à jour sa notification)
    private val recent = LinkedHashMap<Int, Long>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in Prefs.MAIL_PACKAGES) return

        val n = sbn.notification ?: return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (n.flags and Notification.FLAG_ONGOING_EVENT != 0) return

        val mail = MailExtractor.extract(sbn.packageName, n) ?: return
        if (!NameMatcher.matches(mail.searchableText, Prefs.names(this))) return

        val signature = (mail.sender + "|" + mail.searchableText).hashCode()
        val now = System.currentTimeMillis()
        recent.entries.removeAll { now - it.value > 15 * 60 * 1000 }
        if (recent.containsKey(signature)) return
        recent[signature] = now

        MailProcessor.process(this, mail)
    }
}
