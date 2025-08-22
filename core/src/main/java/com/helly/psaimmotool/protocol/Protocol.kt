package com.helly.psaimmotool.protocol

interface Protocol {
    fun withReporter(r: Reporter?): Protocol

    fun connect()
    fun disconnect()

    fun requestVin()
    fun requestPin()
    fun requestDtc()

    fun sendFrame(frame: String)
    fun startListening()
}