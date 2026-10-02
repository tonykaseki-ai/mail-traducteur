# Mail Traducteur

Application Android : quand un mail **Outlook** cite ton nom (Tony / Kaseki),
elle le **traduit en français**, l'**affiche sur l'écran de verrouillage** et le **lit à voix haute**.
La traduction se fait sur le téléphone (Google ML Kit), sans compte ni abonnement.

## Obtenir l'APK (gratuit, sans rien installer sur ton PC)

1. Crée un compte sur https://github.com (si tu n'en as pas).
2. Clique sur **New repository**, donne-lui un nom (ex. `mail-traducteur`), puis **Create repository**.
3. Clique sur **uploading an existing file**, puis glisse **tout le contenu** du dossier `MailTraducteur`
   (y compris le dossier `.github`). Clique sur **Commit changes**.
   > Si le dossier `.github` ne s'envoie pas (il est caché), crée le fichier à la main :
   > **Add file → Create new file**, nom `.github/workflows/build.yml`, et colle son contenu.
4. Ouvre l'onglet **Actions** : la compilation démarre (≈ 5 min). Quand elle est verte ✅,
   clique dessus et télécharge **MailTraducteur-apk** en bas de la page (un .zip contenant `app-debug.apk`).

(Autre option : ouvrir le dossier dans **Android Studio** → *Build → Build APK(s)*.)

## Installer sur le téléphone

1. Copie `app-debug.apk` sur le téléphone et ouvre-le. Autorise « Installer des applis inconnues ».
2. **Android 13 et plus** : avant l'étape suivante, va dans
   *Paramètres → Applications → Mail Traducteur → ⋮ (en haut à droite) → Autoriser les paramètres restreints*.
   Sans ça, Android bloque l'accès aux notifications pour une appli installée hors Play Store.
3. Ouvre l'appli :
   - **1. Autoriser l'accès aux notifications** → active *Mail Traducteur*.
   - **2. Télécharger les traductions** (en Wi-Fi, ~30 Mo par langue : anglais, espagnol, portugais).
     Les autres langues se téléchargent automatiquement la première fois qu'elles apparaissent.
   - Vérifie tes noms, puis appuie sur **Tester avec un faux mail** : tu dois entendre la traduction.
4. Dans Outlook, laisse les notifications activées.
5. Conseillé : *Paramètres → Batterie → Mail Traducteur → Non restreinte*
   (surtout sur Samsung, Tecno, Infinix, Xiaomi), sinon Android peut endormir l'appli.

## Ce que fait l'appli

- Surveille uniquement les notifications d'Outlook.
- Cherche tes noms dans l'objet et l'aperçu du mail (sans tenir compte des majuscules ni des accents).
- Détecte la langue ; si le mail est déjà en français, il est lu tel quel.
- Notification visible écran verrouillé, avec les boutons **🔊 Relire** et **⏹ Stop**.
- Option : ne pas parler quand le téléphone est en silencieux / vibreur.

## Limite à connaître

L'appli lit ce qu'Outlook met dans sa notification : l'expéditeur, l'objet et le **début du mail**
(souvent quelques lignes). Pour un très long mail, tu auras le résumé du début, pas tout le texte.
Lire le mail entier demanderait une connexion directe à ton compte Microsoft (version plus complexe, possible plus tard).
