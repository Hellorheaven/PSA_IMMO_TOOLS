# PSA Immo Tool

> 🔧 Application Android de diagnostic automobile dédiée aux véhicules PSA (Peugeot, Citroën, DS).
> Supporte plusieurs modules de communication (CAN, K-Line, OBD2) via USB, UART et Bluetooth.
> Conçue pour l’usage mobile et Android Auto Automotive OS.

---

## 📁 Arborescence du Projet

```
psa_immo_usb_tool_project/
├── automotive/              # Module Android Auto
├── core/                    # (optionnel) Contenait des utilitaires partagés
├── mobile/                  # Module principal (application mobile)
│   ├── res/                 # Ressources UI (layout, drawables, strings...)
│   ├── java/com/helly/psaimmotool/
│   │   ├── MainActivity.kt  # Activité principale
│   │   ├── modules/         # Tous les modules de communication
│   │   ├── utils/           # Outils génériques (log, permissions, langue, UI, update)
│   │   └── can/             # Interfaces et trames CAN
│   └── AndroidManifest.xml
├── settings.gradle
├── build.gradle
└── README.md                # Ce fichier
```

---

## 🧩 Fonctionnalités par module

### 1. CanBusModule (USB)

* 📡 Communication CAN via port USB
* 🔑 `sendPinRequest()` → Lecture du code PIN
* 🚗 `sendVinRequest()` → Lecture du VIN
* 🧪 `listenAll()` → Réception en continu
* 🧾 `sendCustomFrame(frame)` → Envoi de trame personnalisée

### 2. CanBusUartModule (UART)

* Utilise `usb-serial-for-android`
* 🔌 `connectUsb(context)`
* Toutes les fonctions ci-dessus + :

  * 📈 `sendTripDataCar(...)` : distance, consommation, vitesse
  * 🔧 `sendCarInfo(...)` : vitesse, RPM, niveau carburant
  * 🎛️ `sendButtonCode(code)` : commandes bouton volant
  * 🌡️ `sendTemperature(temp)`

### 3. KLineUsbModule (USB)

* Pour anciens calculateurs
* Supporte :

  * `connectUsb(context)`
  * `sendVinRequest()`
  * `sendCommand(frame)`

### 4. Obd2UsbModule (USB)

* OBD2 par port USB
* `connectUsb(context)`
* `sendVinRequest()`

### 5. Obd2BluetoothModule

* Communication OBD2 via Bluetooth classique
* 🔎 Scanne et associe un périphérique
* 🔄 Connecte via `createRfcommSocketToServiceRecord()`
* `sendVinRequest()` ou `sendCommand(frame)`

---

## 🎨 Interface utilisateur (activity\_main.xml)

* Sélecteur de module (USB / UART / Bluetooth)
* Liste des périphériques Bluetooth
* Boutons :

  * Connexion
  * Lecture VIN
  * Lecture PIN
  * Écoute CAN
  * Envoi de trame personnalisée
  * Export / suppression des logs
  * **Génération de rapports** (Diagnostic complet)
  * **Mise à jour automatique** (UpdateManager)
* Thème clair/sombre sélectionnable
* Langue : 🇫🇷 / 🇬🇧
* Zone de logs avec défilement (ScrollView)

---

## 🌐 Traductions (strings.xml)

* Français / Anglais supportés
* Via `LocaleUtils.setLocaleAndRestart(...)`

---

## 🖌️ Thèmes

* Basé sur Material3
* Personnalisation via `styles.xml` :

  * `Widget.PsaImmoTool.PrimaryButton`
  * `Widget.PsaImmoTool.SecondaryButton`
  * `Widget.PsaImmoTool.FrameInput`
  * `Widget.PsaImmoTool.SectionTitle`

---

## 🔧 Permissions utilisées

* `android.permission.USB_PERMISSION`
* `android.permission.BLUETOOTH_CONNECT`
* `android.permission.BLUETOOTH`
* `android.permission.BLUETOOTH_ADMIN`
* `android.permission.INTERNET` (pour les mises à jour)

Demandées dynamiquement via `PermissionUtils`.

---

## 🔤 Langue & Redémarrage

* **Changement dynamique** par redémarrage contrôlé :

```kotlin
LocaleUtils.setLocaleAndRestart(activity, "fr")
```

---

## 🛠️ Compilation

* Android Studio Hedgehog ou plus récent
* API Target : `34` ou `36`
* Gradle : `8.2.1` minimum
* Kotlin `1.9.x`

---

## 🚗 Support Android Auto

> Module `automotive/` (facultatif, peut être ignoré si non utilisé)

* Utilise `androidx.car.app`
* S'enregistre via `CarService.kt`
* Déclaré dans le `AndroidManifest.xml` + `automotive_app_desc.xml`

---

## 🔄 Mise à jour automatique (UpdateManager)

* Vérifie la version en ligne via `version.txt` sur GitHub
* Télécharge la dernière APK `mobile-release.apk`
* Installation automatique via FileProvider
* Accessible dans le menu ActionBar > **Update**

---

## ✅ Statut des modules

| Module              | Type      | Statut        | Trames supportées    |
| ------------------- | --------- | ------------- | -------------------- |
| CanBusModule        | USB       | ✅ Fonctionnel | VIN, PIN, Custom CAN |
| CanBusUartModule    | UART      | ✅ Fonctionnel | VIN, PIN, Temp, etc. |
| KLineUsbModule      | USB       | ✅ Fonctionnel | VIN, Custom K-Line   |
| Obd2UsbModule       | USB       | ✅ Fonctionnel | VIN                  |
| Obd2BluetoothModule | Bluetooth | ✅ Fonctionnel | VIN, Custom OBD2     |

---

## 💬 Exemple de trame

```text
22 F1 90      → Requête PIN
09 02         → Requête VIN
FD 04 02 01   → Commande bouton volant
```

---

## 🧠 Architecture interne

* `UiUpdater`: mise à jour UI depuis modules (thread-safe)
* `FrameInterpreter`: décode trames vers chaînes utilisateur
* `LogExporter`: export texte brut
* `PermissionUtils`: centralisation permissions
* `LocaleUtils`: gestion changement de langue
* `UpdateManager`: gestion mise à jour automatique

---

## 🧼 Nettoyage des logs et rapports

* Bouton `clearLogsButton` remet la zone texte à zéro
* Export en `.txt` avec permissions vérifiées
* Rapport généré via `generateReportButton`

---

## ✍️ Contact / auteur

> Mickaël Z.
> Projet de diagnostic automobile privé à but pédagogique.

---

## 📦 ToDo ou Améliorations futures

* [ ] Support ISO-TP
* [ ] Interface tablette
* [ ] Graphiques temps réel
* [ ] Choix débit UART
* [ ] Gestion historique VIN
* [ ] Support OTA complet (GitHub API)

---

## 📘 Licence

Projet privé, non destiné à une diffusion publique, non opensource.
