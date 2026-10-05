package com.tony.mailtraducteur

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var journal: TextView
    private val main = Handler(Looper.getMainLooper())
    private var models: List<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Speaker.init(this)

        status = findViewById(R.id.status)
        journal = findViewById(R.id.journal)
        val names = findViewById<EditText>(R.id.names)
        val voice = findViewById<Switch>(R.id.voice)
        val silent = findViewById<Switch>(R.id.silent)
        val all = findViewById<Switch>(R.id.allMails)

        names.setText(Prefs.namesRaw(this))
        voice.isChecked = Prefs.voiceEnabled(this)
        silent.isChecked = Prefs.respectSilent(this)
        all.isChecked = Prefs.allMails(this)

        voice.setOnCheckedChangeListener { _, checked -> Prefs.setVoice(this, checked); refresh() }
        silent.setOnCheckedChangeListener { _, checked -> Prefs.setRespectSilent(this, checked); refresh() }
        all.setOnCheckedChangeListener { _, checked -> Prefs.setAllMails(this, checked); refresh() }

        findViewById<Button>(R.id.save).setOnClickListener {
            Prefs.setNames(this, names.text.toString())
            toast("Noms enregistrés : " + Prefs.names(this).joinToString(", "))
        }

        findViewById<Button>(R.id.access).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.notifPerm).setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            } else {
                startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                )
            }
        }

        findViewById<Button>(R.id.battery).setOnClickListener {
            try {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                )
            } catch (e: Exception) {
                toast("Ouvre Paramètres → Applications → Mail Traducteur → Batterie")
            }
        }

        findViewById<Button>(R.id.download).setOnClickListener {
            toast("Téléchargement des modèles… (Wi-Fi conseillé, quelques minutes)")
            FrTranslator.downloadModels(this) { msg -> runOnUiThread { toast(msg); models = null; refresh() } }
        }

        findViewById<Button>(R.id.voiceTest).setOnClickListener {
            Journal.log(this, "Test de la voix seule")
            Speaker.speak(this, "Bonjour Tony, la voix fonctionne.")
            main.postDelayed({ refresh() }, 1500)
        }

        findViewById<Button>(R.id.installVoice).setOnClickListener {
            try {
                startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
            } catch (e: Exception) {
                toast("Ouvre Paramètres → Synthèse vocale et installe le français")
            }
        }

        findViewById<Button>(R.id.test).setOnClickListener {
            val name = Prefs.names(this).firstOrNull() ?: "Tony"
            Journal.log(this, "— Test avec un faux mail —")
            MailProcessor.process(
                this,
                MailInfo(
                    Prefs.OUTLOOK,
                    "John Smith",
                    "Meeting tomorrow",
                    "Hello $name, could you please send me the report before Friday? Thank you very much."
                )
            )
            toast("Mail de test lancé — regarde le journal en bas")
            main.postDelayed({ refresh() }, 2000)
            main.postDelayed({ refresh() }, 17000)
        }

        findViewById<Button>(R.id.replay).setOnClickListener {
            val last = Prefs.lastMessage(this)
            if (last == null) toast("Aucun mail lu pour l'instant") else Speaker.speak(this, last)
        }

        findViewById<Button>(R.id.stop).setOnClickListener { Speaker.stop() }
        findViewById<Button>(R.id.refresh).setOnClickListener { models = null; refresh() }
        findViewById<Button>(R.id.clear).setOnClickListener { Journal.clear(this); refresh() }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        models = null
        refresh()
    }

    private fun refresh() {
        if (models == null) {
            FrTranslator.downloadedModels { list ->
                models = list
                runOnUiThread { render() }
            }
        }
        render()
    }

    private fun render() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val ringer = when (am.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> "✅ Son activé"
            AudioManager.RINGER_MODE_VIBRATE -> if (Prefs.respectSilent(this)) "🔇 Vibreur : l'appli ne parlera pas" else "✅ Vibreur (l'appli parle quand même)"
            else -> if (Prefs.respectSilent(this)) "🔇 Silencieux : l'appli ne parlera pas" else "✅ Silencieux (l'appli parle quand même)"
        }
        val m = models
        val modelsLine = when {
            m == null -> "… vérification des modèles"
            m.contains("en") -> "✅ Modèles installés : " + m.joinToString(", ")
            else -> "❌ Modèle anglais → français pas téléchargé (bouton 2)"
        }
        status.text = listOf(
            if (listenerEnabled()) "✅ Accès aux notifications autorisé" else "❌ Accès aux notifications NON autorisé (bouton 1)",
            if (nm.areNotificationsEnabled()) "✅ Notifications de l'appli autorisées" else "❌ Notifications de l'appli bloquées (bouton 3)",
            modelsLine,
            Speaker.status,
            ringer,
            "Volume média : ${am.getStreamVolume(AudioManager.STREAM_MUSIC)}/${am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)}"
        ).joinToString("\n")
        journal.text = Journal.read(this)
    }

    private fun listenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        val me = ComponentName(this, MailListenerService::class.java)
        return flat.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
