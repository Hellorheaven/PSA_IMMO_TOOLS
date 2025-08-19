// core/transport/WifiTransport.kt
package com.helly.psaimmotool.transport

import com.helly.psaimmotool.can.CanFrame
import com.helly.psaimmotool.protocol.TransportInterface

class WifiTransport : TransportInterface {
    override fun connect() { /* Connexion WiFi */ }
    override fun disconnect() { /* Déconnexion WiFi */ }
    override fun sendFrame(frame: String) { /* Envoi TCP/UDP */ }
    override fun startListening(callback: (CanFrame) -> Unit) { }
}
