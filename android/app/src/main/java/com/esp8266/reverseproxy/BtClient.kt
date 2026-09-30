package com.esp8266.reverseproxy

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

class BtException(msg: String) : IOException(msg)

/** Cliente Bluetooth Classic (SPP/RFCOMM) del protocolo de docs/PROTOCOL.md. */
class BtClient {
    private var socket: BluetoothSocket? = null
    private var readerJob: Job? = null
    private val lines = Channel<String>(Channel.UNLIMITED)
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val isConnected get() = socket?.isConnected == true

    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice) = withContext(Dispatchers.IO) {
        disconnect()
        BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
        val s = device.createRfcommSocketToServiceRecord(SPP_UUID)
        try {
            s.connect()
        } catch (e: IOException) {
            runCatching { s.close() }
            throw e
        }
        socket = s
        while (lines.tryReceive().isSuccess) { /* descarta restos */ }
        readerJob = scope.launch { readLoop(s) }
    }

    fun disconnect() {
        readerJob?.cancel()
        readerJob = null
        runCatching { socket?.close() }
        socket = null
    }

    private fun readLoop(s: BluetoothSocket) {
        val input = s.inputStream
        val sb = StringBuilder()
        val buf = ByteArray(256)
        try {
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                for (i in 0 until n) {
                    val c = (buf[i].toInt() and 0xFF).toChar()
                    if (c == '\n') {
                        val l = sb.toString().trim()
                        sb.setLength(0)
                        if (l.isNotEmpty()) lines.trySend(l)
                    } else sb.append(c)
                }
            }
        } catch (_: IOException) {
        }
        lines.trySend("ERR connection closed")
    }

    private suspend fun readLine(timeoutMs: Long): String =
        withTimeoutOrNull(timeoutMs) { lines.receive() } ?: throw BtException("Timeout esperando respuesta")

    private fun write(bytes: ByteArray) {
        val out = socket?.outputStream ?: throw BtException("No conectado")
        out.write(bytes)
        out.flush()
    }

    /** Envia un comando y devuelve las lineas de datos previas al `OK`. */
    suspend fun command(cmd: String, timeoutMs: Long = 5000): List<String> = mutex.withLock {
        withContext(Dispatchers.IO) {
            while (lines.tryReceive().isSuccess) { /* limpia basura anterior */ }
            write((cmd + "\n").toByteArray())
            val data = mutableListOf<String>()
            var done = false
            while (!done) {
                val l = readLine(timeoutMs)
                when {
                    l == "OK" || l.startsWith("OK ") -> done = true
                    l.startsWith("ERR") -> throw BtException(l.removePrefix("ERR").trim().ifEmpty { "error" })
                    else -> data.add(l)
                }
            }
            data.toList()
        }
    }

    /** Sube un firmware .bin por Bluetooth (comando OTA con ACK por bloque). */
    suspend fun uploadFirmware(fw: ByteArray, onProgress: (Int) -> Unit) = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (fw.isEmpty()) throw BtException("Archivo vacio")
            while (lines.tryReceive().isSuccess) { }
            val md5 = MessageDigest.getInstance("MD5").digest(fw)
                .joinToString("") { "%02x".format(it) }
            write("OTA ${fw.size} $md5\n".toByteArray())
            val first = readLine(15000)
            if (first.startsWith("ERR")) throw BtException(first.removePrefix("ERR").trim())
            val block = first.removePrefix("OK").trim().toIntOrNull() ?: 1024

            var off = 0
            while (off < fw.size) {
                val n = minOf(block, fw.size - off)
                write(fw.copyOfRange(off, off + n))
                off += n
                val r = readLine(20000)
                when {
                    r == "ACK" || r == "OTA_OK" -> {}
                    r.startsWith("ERR") -> throw BtException(r.removePrefix("ERR").trim())
                    else -> throw BtException("Respuesta inesperada: $r")
                }
                onProgress(off * 100 / fw.size)
            }
        }
    }

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
