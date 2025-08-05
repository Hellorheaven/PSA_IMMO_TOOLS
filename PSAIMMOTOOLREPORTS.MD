# 📋 Audit Complet – PSA Immo Tool (Baseline Immobile)

## **1. Résumé exécutif**

Cet audit est basé **exclusivement** sur l'analyse des fichiers fournis dans la baseline.  
Aucune supposition ou extrapolation n'a été faite.  
Seuls les éléments réellement présents et identifiés comme **non utilisés** sont listés.

### **Constats principaux**
- Plusieurs **méthodes et classes Kotlin** existent mais ne sont **jamais appelées** dans le code.
- Des **ressources Android** (layouts, icônes, chaînes, arrays) sont présentes mais **non référencées** dans le code ou les layouts.
- Certaines logiques sont **dupliquées** dans différents modules (CAN, export, permissions).
- La structure des dépendances montre des **modules isolés** non intégrés dans le flux principal.

### **Objectif**
- Identifier les éléments inutilisés **réels** pour pouvoir les intégrer ou nettoyer dans les futurs développements.
- Préparer le terrain pour une utilisation complète de toutes les fonctionnalités disponibles.

## **2. Code Kotlin – Éléments inutilisés**

| Élément | Localisation | État | Explication | Proposition |
|---------|--------------|------|-------------|-------------|
| `VehicleCapabilitiesEditor` | `ui/VehicleCapabilitiesEditor.kt` | Classe non instanciée | Prévue pour édition des capacités mais non branchée | L’intégrer dans un futur menu “Capacités véhicule” |
| `UiUpdater.autoScrollIfEnabled()` | `utils/UiUpdater.kt` | Méthode inutilisée | Prévue pour le log CAN mais non appelée | Lier au recyclage du log CAN dans `DiagnosticsFragment` |
| `PermissionUtils.checkLocationPermission()` | `utils/PermissionUtils.kt` | Méthode inutilisée | Gestion permission localisation non implémentée | Intégrer dans `MainActivity` avant connexion Bluetooth |
| `PsaKeyCalculator.calculateKeyFromPin()` | `modules/PsaKeyCalculator.kt` | Méthode inutilisée | Prévue pour calcul de clé PIN mais non branchée | Ajouter dans le flux d’authentification |
| `LogExporter.exportLogsAsText()` | `utils/LogExporter.kt` | Méthode inutilisée | Version CSV utilisée à la place | Prévoir option d’export texte dans `MainActivity` |

---

## **3. Ressources Android inutilisées**

### **Layouts**
- `res/layout/item_can_log.xml` → jamais gonflé (`inflate`) dans le code.

### **Drawables**
- `res/drawable/ic_car.xml` → icône jamais utilisée.
- `res/drawable/ic_exit.xml` → icône non utilisée.

### **Arrays**
- `baud_rates` (`arrays.xml`) → non utilisé dans préférences ni code.
- `vehicle_types` (`arrays.xml`) → non utilisé.

### **Chaînes (`strings.xml`)**
*(Localisation + ligne exacte disponible dans la version détaillée finale)*
- `pref_autoscroll_title`
- `pref_autoscroll_summary`
- `pref_vehicle_title`
- `pref_vehicle_summary`
- `pref_theme_title`
- `pref_theme_summary`
- `pref_language_title`
- `pref_language_summary`
- `menu_exit`
- `dialog_confirm_exit`
- `toast_export_success`
- `toast_export_failure`
- `label_vehicle_editor`
- `label_capabilities_editor`
- `status_connected`
- `status_disconnected`

---

## **4. Redondances détectées**
- **Gestion CAN** : plusieurs modules (`CanBusModule`, `GenericCanDemoModule`) réimplémentent la même logique de parsing → mutualisable.
- **Permissions** : vérifications redondantes dans `MainActivity` et `PermissionUtils`.
- **Export** : logique similaire entre `ReportGenerator` et `LogExporter`.

---

## **5. Carte des dépendances simplifiée (Mermaid)**

```mermaid
graph TD
    MainActivity --> DiagnosticsFragment
    MainActivity --> VehicleEditorActivity
    MainActivity --> SettingsActivity
    SettingsActivity --> SettingsFragment
    DiagnosticsFragment --> UiUpdater
    DiagnosticsFragment --> CanBusModule
    CanBusModule --> UsbCanInterface
    CanBusModule --> CanFrame
    Obd2UsbModule --> UsbCanInterface
    Obd2BluetoothModule --> CanCommunicationInterface
    KLineUsbModule --> CanCommunicationInterface
    VehicleManager --> VehicleCapabilities
    ReportGenerator --> VehicleManager
    LogExporter --> VehicleManager
