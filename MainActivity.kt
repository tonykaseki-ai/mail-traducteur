package com.tony.mailtraducteur

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.status)
        val names = findViewById<EditText>(R.id.names)
        val voice = findViewById<Switch>(R.id.voice)
        val silent = findViewById<Switch>(R.id.silent)

        names.setText(Prefs.namesRaw(this))
        voice.isChecked = Prefs.voiceEnabled(this)
        silent.isChecked = Prefs.respectSilent(this)

        voice.setOnCheckedChangeListener { _, checked -> Prefs.setVoice(this, checked) }
        silent.setOnCheckedChangeListener { _, checked -> Prefs.setRespectSilent(this, checked) }

        findViewById<Button>(R.id.save).setOnClickListener {
            Prefs.setNames(this, names.text.toString())
            toast("Noms enregistrés : " + Prefs.names(this).joinToString(", "))
        }

        findViewById<Button>(R.id.access).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.download).setOnClickListener {
            toast("Téléchargement des modèles… (Wi-Fi conseillé)")
            FrTranslator.downloadModels { msg -> runOnUiThread { toast(msg) } }
        }

        findViewById<Button>(R.id.test).setOnClickListener {
            val name = Prefs.names(this).firstOrNull() ?: "Tony"
            MailProcessor.process(
                this,
                MailInfo(
                    Prefs.OUTLOOK,
                    "John Smith",
                    "Meeting tomorrow",
                    "Hello $name, could you please send me the report before Friday? Thank you very much."
                )
            )
            toast("Mail de test envoyé — écoute !")
        }

        findViewById<Button>(R.id.replay).setOnClickListener {
            val last = Prefs.lastMessage(this)
            if (last == null) toast("Aucun mail lu pour l'instant") else Speaker.speak(this, last)
        }

        findViewById<Button>(R.id.stop).setOnClickListener { Speaker.stop() }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        status.text = if (listenerEnabled()) {
            "✅ Actif : je surveille les mails Outlook qui te citent."
        } else {
            "❌ Inactif : appuie sur « 1. Autoriser l'accès aux notifications »."
        }
    }

    private fun listenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        val me = ComponentName(this, MailListenerService::class.java)
        return flat.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
