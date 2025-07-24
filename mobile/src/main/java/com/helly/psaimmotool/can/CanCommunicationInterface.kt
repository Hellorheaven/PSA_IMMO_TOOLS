package com.helly.psaimmotool.can

interface CanCommunicationInterface {
    fun connect()
    fun disconnect()
    fun sendFrame(frame: CanFrame)
    fun startListening(callback: (CanFrame) -> Unit)
}
