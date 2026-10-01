package com.esp8266.reverseproxy

import android.Manifest
import android.animation.Animator
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private val bt = BtClient()
    private var devices: List<BluetoothDevice> = emptyList()
    private var firmwareUri: Uri? = null

    private lateinit var spinner: Spinner
    private lateinit var btnConnect: Button
    private lateinit var tvState: TextView
    private lateinit var etSsid: EditText
    private lateinit var etPass: EditText
    private lateinit var etHost: EditText
    private lateinit var etPort: EditText
    private lateinit var etListen: EditText
    private lateinit var tvFile: TextView
    private lateinit var progress: ProgressBar
    private lateinit var tvLog: TextView
    private lateinit var titleView: GlitchTextView
    private lateinit var link: LinkIndicatorView
    private val animators = mutableListOf<Animator>()
    private val logQueue = Channel<Pair<String, Boolean>>(Channel.UNLIMITED)

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) loadDevices() else log("Permiso Bluetooth denegado")
    }
    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            firmwareUri = uri
            tvFile.text = uri.lastPathSegment ?: uri.toString()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        spinner = findViewById(R.id.spinnerDevices)
        btnConnect = findViewById(R.id.btnConnect)
        tvState = findViewById(R.id.tvState)
        etSsid = findViewById(R.id.etSsid)
        etPass = findViewById(R.id.etPass)
        etHost = findViewById(R.id.etHost)
        etPort = findViewById(R.id.etPort)
        etListen = findViewById(R.id.etListen)
        tvFile = findViewById(R.id.tvFile)
        progress = findViewById(R.id.progress)
        tvLog = findViewById(R.id.tvLog)
        titleView = findViewById(R.id.tvTitle)
        link = findViewById(R.id.linkIndicator)

        // Consola con efecto maquina de escribir
        lifecycleScope.launch { for ((msg, err) in logQueue) typeLine(msg, err) }

        // Efectos: entrada escalonada, bordes que pulsan, linea neon, botones con rebote
        val content = findViewById<ViewGroup>(R.id.content)
        for (i in 0 until content.childCount) {
            val v = content.getChildAt(i)
            if (v.id != R.id.neonLine) v.enter(i)
            if (v is ViewGroup) v.pulseBorder(300L * i)?.let { animators.add(it) }
        }
        findViewById<View>(R.id.neonLine).pulseAlpha()?.let { animators.add(it) }
        applyPress(content)

        findViewById<Button>(R.id.btnRefresh).setOnClickListener { ensurePermission() }
        btnConnect.setOnClickListener { toggleConnection() }
        findViewById<Button>(R.id.btnStatus).setOnClickListener { withBt("Estado") { showStatus() } }
        findViewById<Button>(R.id.btnLoad).setOnClickListener { withBt("Leer config") { loadConfig() } }
        findViewById<Button>(R.id.btnScan).setOnClickListener { withBt("Buscar redes") { scanNetworks() } }
        findViewById<Button>(R.id.btnSave).setOnClickListener { withBt("Guardar config") { saveConfig() } }
        findViewById<Button>(R.id.btnPick).setOnClickListener { filePicker.launch(arrayOf("*/*")) }
        findViewById<Button>(R.id.btnUpload).setOnClickListener { withBt("Subir firmware") { uploadFirmware() } }
        findViewById<Button>(R.id.btnReboot).setOnClickListener { withBt("Reiniciar") { bt.command("REBOOT") } }

        log("BOOT NEURAL LINK v2.0")
        log("INICIALIZANDO BUS BLUETOOTH...")
        ensurePermission()
    }

    override fun onDestroy() {
        animators.forEach { it.cancel() }
        bt.disconnect()
        super.onDestroy()
    }

    private fun log(msg: String, error: Boolean = false) {
        logQueue.trySend(msg to error)
        if (error) tvLog.shake()
    }

    /** Escribe una linea en la consola caracter a caracter (verde; magenta si es error). */
    private suspend fun typeLine(msg: String, error: Boolean) {
        val color = if (error) Color.rgb(255, 43, 214) else Color.rgb(57, 255, 20)
        val text = "> " + msg + "\n"
        val animate = Fx.enabled(this)
        val chunk = if (animate) maxOf(2, text.length / 28) else text.length
        var i = 0
        while (i < text.length) {
            val end = minOf(text.length, i + chunk)
            val part = SpannableString(text.substring(i, end))
            part.setSpan(ForegroundColorSpan(color), 0, part.length, 0)
            tvLog.append(part)
            i = end
            if (animate && i < text.length) delay(14)
        }
    }

    private fun applyPress(v: View) {
        if (v is Button) v.neonPress()
        if (v is ViewGroup) for (i in 0 until v.childCount) applyPress(v.getChildAt(i))
    }

    // --- Bluetooth ---
    private fun ensurePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) permLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) else loadDevices()
    }

    @SuppressLint("MissingPermission")
    private fun loadDevices() {
        val adapter: BluetoothAdapter? = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (adapter == null || !adapter.isEnabled) {
            log("Activa el Bluetooth y vuelve a pulsar Actualizar")
            return
        }
        devices = adapter.bondedDevices.sortedBy { it.name ?: it.address }
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            devices.map { "${it.name ?: "?"} (${it.address})" })
        if (devices.isEmpty()) log("No hay dispositivos vinculados. Vincula ESP-Proxy en los ajustes de Android.")
    }

    private fun toggleConnection() {
        if (bt.isConnected) {
            bt.disconnect()
            setConnected(false)
            return
        }
        val dev = devices.getOrNull(spinner.selectedItemPosition) ?: return log("Selecciona un dispositivo")
        btnConnect.isEnabled = false
        lifecycleScope.launch {
            try {
                bt.connect(dev)
                setConnected(true)
                titleView.burst(14)
                log("ENLACE ESTABLECIDO")
                bt.command("PING")
                showStatus()
                loadConfig()
            } catch (e: Exception) {
                bt.disconnect()
                setConnected(false)
                log("Error de conexion: ${e.message}", true)
            } finally {
                btnConnect.isEnabled = true
            }
        }
    }

    private fun setConnected(c: Boolean) {
        btnConnect.text = if (c) "Desconectar" else "Conectar"
        tvState.text = if (c) "Conectado" else "Desconectado"
        link.setConnected(c)
        tvState.flicker()
    }

    private fun withBt(what: String, block: suspend () -> Unit) {
        if (!bt.isConnected) return log("No conectado")
        lifecycleScope.launch {
            try {
                block()
            } catch (e: Exception) {
                log("$what: ${e.message}", true)
            }
        }
    }

    // --- Comandos ---
    private suspend fun showStatus() {
        val l = bt.command("STATUS").firstOrNull { it.startsWith("STATUS") } ?: return
        val kv = l.removePrefix("STATUS").trim().split(' ').associate {
            it.substringBefore('=') to it.substringAfter('=', "")
        }
        val s = "WiFi: ${kv["wifi"]}  IP: ${kv["ip"]}  RSSI: ${kv["rssi"]}\n" +
            "Escucha :${kv["listen"]} -> ${kv["upstream"]}\n" +
            "Sesiones: ${kv["sessions"]}  Heap: ${kv["heap"]}  FW: ${kv["fw"]}"
        tvState.text = s
        tvState.flicker()
    }

    private suspend fun loadConfig() {
        for (l in bt.command("GET")) {
            if (!l.startsWith("CFG ")) continue
            val k = l.substring(4).substringBefore('=')
            val v = l.substringAfter('=', "")
            when (k) {
                "ssid" -> etSsid.setText(v)
                "host" -> etHost.setText(v)
                "port" -> etPort.setText(v)
                "listen" -> etListen.setText(v)
                "pass_set" -> etPass.hint = if (v == "1") "(guardada; deja vacio para no cambiar)" else "Contrasena WiFi"
            }
        }
        log("Configuracion leida")
    }

    private suspend fun scanNetworks() {
        log("Buscando redes (puede tardar unos segundos)...")
        val nets = bt.command("SCAN", 20000).filter { it.startsWith("NET ") }.map {
            val p = it.split(' ', limit = 4)
            Triple(p[3], p[1], p[2])
        }
        if (nets.isEmpty()) return log("No se encontraron redes")
        AlertDialog.Builder(this)
            .setTitle("Redes WiFi")
            .setItems(nets.map { "${it.first}  (${it.second} dBm, ${it.third})" }.toTypedArray()) { _, i ->
                etSsid.setText(nets[i].first)
            }.show()
    }

    private suspend fun saveConfig() {
        val port = etPort.text.toString().toIntOrNull()
        val listen = etListen.text.toString().toIntOrNull()
        if (port == null || listen == null) return log("Puertos invalidos")
        bt.command("SET ssid ${etSsid.text}")
        // Si la contrasena queda vacia y ya habia una guardada, no se toca.
        if (etPass.text.isNotEmpty() || etPass.hint.toString().startsWith("Contrasena")) {
            bt.command("SET pass ${etPass.text}")
        }
        bt.command("SET host ${etHost.text.toString().trim()}")
        bt.command("SET port $port")
        bt.command("SET listen $listen")
        bt.command("SAVE", 10000)
        titleView.burst(14)
        log("Configuracion guardada y aplicada")
        showStatus()
    }

    private suspend fun uploadFirmware() {
        val uri = firmwareUri ?: return log("Elige un archivo .bin primero")
        val bytes = withContext(Dispatchers.IO) {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } ?: return log("No se pudo leer el archivo")
        log("Subiendo ${bytes.size} bytes... no cierres la app ni apagues el ESP32")
        progress.progress = 0
        bt.uploadFirmware(bytes) { progress.setProgress(it, true) }
        titleView.burst(20)
        log("Firmware instalado; el ESP32 se reinicia. Vuelve a conectar en unos segundos.")
        bt.disconnect()
        setConnected(false)
    }
}
