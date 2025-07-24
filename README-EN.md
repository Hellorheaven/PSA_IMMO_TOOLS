# PSA Immo Tool

> 🔧 Android diagnostic application dedicated to PSA vehicles (Peugeot, Citroën, DS).
> Supports multiple communication modules (CAN, K-Line, OBD2) via USB, UART, and Bluetooth.
> Designed for mobile use and Android Auto Automotive OS.

---

## 📁 Project Structure

```
psa_immo_usb_tool_project/
├── automotive/              # Android Auto module
├── core/                    # (optional) Shared utilities
├── mobile/                  # Main module (mobile application)
│   ├── res/                 # UI resources (layout, drawables, strings...)
│   ├── java/com/helly/psaimmotool/
│   │   ├── MainActivity.kt  # Main activity
│   │   ├── modules/         # All communication modules
│   │   ├── utils/           # Generic tools (logs, permissions, locale, UI, update)
│   │   └── can/             # CAN interfaces and frames
│   └── AndroidManifest.xml
├── settings.gradle
├── build.gradle
└── README.md                # This file
```

---

## 🧩 Features by Module

### 1. CanBusModule (USB)

* 📡 CAN communication via USB port
* 🔑 `sendPinRequest()` → Read PIN code
* 🚗 `sendVinRequest()` → Read VIN
* 🧪 `listenAll()` → Continuous reception
* 🧾 `sendCustomFrame(frame)` → Send custom frame

### 2. CanBusUartModule (UART)

* Uses `usb-serial-for-android`
* 🔌 `connectUsb(context)`
* Includes all the above functions +:

  * 📈 `sendTripDataCar(...)` : distance, consumption, speed
  * 🔧 `sendCarInfo(...)` : speed, RPM, fuel level
  * 🎛️ `sendButtonCode(code)` : steering wheel button commands
  * 🌡️ `sendTemperature(temp)`

### 3. KLineUsbModule (USB)

* For older ECUs
* Supports:

  * `connectUsb(context)`
  * `sendVinRequest()`
  * `sendCommand(frame)`

### 4. Obd2UsbModule (USB)

* OBD2 via USB port
* `connectUsb(context)`
* `sendVinRequest()`

### 5. Obd2BluetoothModule

* OBD2 communication via classic Bluetooth
* 🔎 Scans and pairs with devices
* 🔄 Connects via `createRfcommSocketToServiceRecord()`
* `sendVinRequest()` or `sendCommand(frame)`

---

## 🎨 User Interface (activity\_main.xml)

* Module selector (USB / UART / Bluetooth)
* List of Bluetooth devices
* Buttons:

  * Connect
  * Read VIN
  * Read PIN
  * Start CAN listening
  * Send custom frame
  * Export / clear logs
  * **Generate report** (full diagnostic)
  * **Automatic update** (UpdateManager)
* Light/Dark theme toggle
* Language: 🇫🇷 / 🇬🇧
* Log area with scroll (ScrollView)

---

## 🌐 Translations (strings.xml)

* Supports French and English
* Handled via `LocaleUtils.setLocaleAndRestart(...)`

---

## 🖌️ Themes

* Based on Material3
* Customized via `styles.xml`:

  * `Widget.PsaImmoTool.PrimaryButton`
  * `Widget.PsaImmoTool.SecondaryButton`
  * `Widget.PsaImmoTool.FrameInput`
  * `Widget.PsaImmoTool.SectionTitle`

---

## 🔧 Permissions Used

* `android.permission.USB_PERMISSION`
* `android.permission.BLUETOOTH_CONNECT`
* `android.permission.BLUETOOTH`
* `android.permission.BLUETOOTH_ADMIN`
* `android.permission.INTERNET` (for updates)

Managed dynamically via `PermissionUtils`.

---

## 🔤 Language & Restart

* **Dynamic language change** with controlled restart:

```kotlin
LocaleUtils.setLocaleAndRestart(activity, "en")
```

---

## 🛠️ Build

* Android Studio Hedgehog or newer
* Target API: `34` or `36`
* Gradle: `8.2.1` minimum
* Kotlin `1.9.x`

---

## 🚗 Android Auto Support

> `automotive/` module (optional, can be ignored if unused)

* Uses `androidx.car.app`
* Registers through `CarService.kt`
* Declared in `AndroidManifest.xml` + `automotive_app_desc.xml`

---

## 🔄 Automatic Update (UpdateManager)

* Checks online version via `version.txt` on GitHub
* Downloads the latest APK `mobile-release.apk`
* Installs automatically via FileProvider
* Accessible in ActionBar menu > **Update**

---

## ✅ Module Status

| Module              | Type      | Status       | Supported Frames     |
| ------------------- | --------- | ------------ | -------------------- |
| CanBusModule        | USB       | ✅ Functional | VIN, PIN, Custom CAN |
| CanBusUartModule    | UART      | ✅ Functional | VIN, PIN, Temp, etc. |
| KLineUsbModule      | USB       | ✅ Functional | VIN, Custom K-Line   |
| Obd2UsbModule       | USB       | ✅ Functional | VIN                  |
| Obd2BluetoothModule | Bluetooth | ✅ Functional | VIN, Custom OBD2     |

---

## 💬 Example Frames

```text
22 F1 90      → PIN request
09 02         → VIN request
FD 04 02 01   → Steering wheel button command
```

---

## 🧠 Internal Architecture

* `UiUpdater`: thread-safe UI updates
* `FrameInterpreter`: decode frames to user-readable text
* `LogExporter`: export raw logs
* `PermissionUtils`: centralized permissions
* `LocaleUtils`: language switching
* `UpdateManager`: automatic update handling

---

## 🧼 Log and Report Management

* `clearLogsButton` resets the log area
* Export to `.txt` with permission checks
* Reports generated via `generateReportButton`

---

## ✍️ Contact / Author

> Mickaël Z.
> Private automotive diagnostic project for educational purposes.

---

## 📦 ToDo or Future Improvements

* [ ] ISO-TP support
* [ ] Tablet interface
* [ ] Real-time charts
* [ ] UART speed selection
* [ ] VIN history management
* [ ] Full OTA support (GitHub API)

---

## 📘 License

Private project, not intended for public distribution, not open-source.
