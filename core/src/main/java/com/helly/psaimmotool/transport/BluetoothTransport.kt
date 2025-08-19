// core/transport/BluetoothTransport.kt
package com.helly.psaimmotool.transport

import com.helly.psaimmotool.can.CanFrame
import com.helly.psaimmotool.protocol.TransportInterface

class BluetoothTransport : TransportInterface {
    override fun connect() { /* Connexion BT */ }
    override fun disconnect() { /* Déconnexion BT */ }
    override fun sendFrame(frame: String) { /* Envoi sur BT */ }
    override fun startListening(callback: (CanFrame) -> Unit) {
        // Callback avec trames reçues
    }
}
