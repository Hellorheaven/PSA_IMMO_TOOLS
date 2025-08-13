package com.helly.psaimmotool.automotive

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import com.helly.psaimmotool.ports.StatusPort

/**
 * Écran principal Android Auto (projection). 
 * Prêt à compiler; branchement au "moteur" à activer (voir TODO).
 */
class MainScreen(private val ctx: CarContext) : Screen(ctx) {

    // Stockage très simple pour l'exemple
    private val logs = mutableListOf<String>()
    private var status: String = "Prêt"

    // Port de statut qui recevra les messages des modules existants
    private val carStatusPort = object : StatusPort {
        override fun setStatus(text: String) {
            status = text
            invalidate()
        }
        override fun appendLog(line: String) {
            logs.add(0, line)
            if (logs.size > 200) logs.removeLast()
            invalidate()
        }

        override fun appendOutput(line: String) {
            logs.add(0, line)
            if (logs.size > 200) logs.removeLast()
            invalidate()
        }
    }

    // TODO: instancier ici votre module existant (sans renommer quoi que ce soit) et lui injecter le port :
    // private val engine = CanBusUartModule(ctx).withStatusPort(carStatusPort)
    // ou bien : private val engine = Obd2UsbModule(ctx).withStatusPort(carStatusPort)
    // Pour que ce fichier compile sans dépendances, on laisse un "engine" factice :
    private val engine = object {
        fun connect() { carStatusPort.appendLog("Connexion (stub)") }
        fun sendVinRequest() { carStatusPort.appendLog("VIN request (stub)") }
        fun sendPinRequest() { carStatusPort.appendLog("PIN request (stub)") }
        fun listenAll() { carStatusPort.appendLog("Listen all (stub)") }
    }

    override fun onGetTemplate(): Template {
        val list = ItemList.Builder()
            .addItem(
                Row.Builder()
                    .setTitle("Connexion")
                    .addText("Initialiser le lien (USB/BT)")
                    .setOnClickListener { engine.connect() }
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Lire VIN")
                    .addText("Envoie commande et lit la réponse")
                    .setOnClickListener { engine.sendVinRequest() }
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Lire PIN")
                    .addText("Lecture code PIN")
                    .setOnClickListener { engine.sendPinRequest() }
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Écouter CAN")
                    .addText("Écoute toutes trames")
                    .setOnClickListener { engine.listenAll() }
                    .build()
            )
            .build()

        val statusRow = Row.Builder()
            .setTitle("Statut")
            .addText(status)
            .build()

        val logItems = ItemList.Builder().apply {
            logs.take(10).forEach { addItem(Row.Builder().setTitle(it).build()) }
        }.build()

        return PaneTemplate.Builder(
            Pane.Builder()
                .addRow(statusRow)
                .addAction(Action.BACK)
                .build()
        )
            .setHeaderAction(Action.APP_ICON)
            .setTitle("PSA Immo Tool")
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(Action.Builder()
                        .setTitle("VIN")
                        .setOnClickListener { engine.sendVinRequest() }
                        .build())
                    .addAction(Action.Builder()
                        .setTitle("PIN")
                        .setOnClickListener { engine.sendPinRequest() }
                        .build())
                    .build()
            )
            .build()
    }
}
