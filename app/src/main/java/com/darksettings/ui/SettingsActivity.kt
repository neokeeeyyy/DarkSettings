package com.neoconfigurator.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.neoconfigurator.R
import com.neoconfigurator.databinding.ActivitySettingsBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val wifiManager by lazy { applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    private val bluetoothManager by lazy { getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val action = intent.getStringExtra("action") ?: "main"
        val title = intent.getStringExtra("title") ?: "Configuración"

        binding.toolbar.title = title
        binding.toolbar.setNavigationOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_bottom)
        }

        when (action) {
            "wifi" -> setupWifi()
            "bluetooth" -> setupBluetooth()
            "display" -> setupDisplay()
            "sound" -> setupSound()
            "battery" -> setupBattery()
            "storage" -> setupStorage()
            "security" -> setupSecurity()
            "system" -> setupSystem()
            "apps" -> setupApps()
        }
    }

    private fun addToggle(title: String, summary: String? = null, isChecked: Boolean, onToggle: (Boolean) -> Unit) {
        val view = layoutInflater.inflate(R.layout.item_toggle, binding.content, false)
        view.findViewById<TextView>(R.id.title).text = title
        val summaryView = view.findViewById<TextView>(R.id.summary)
        if (summary != null) {
            summaryView.text = summary
            summaryView.visibility = View.VISIBLE
        }
        val switch = view.findViewById<MaterialSwitch>(R.id.switchWidget)
        switch.isChecked = isChecked
        switch.setOnCheckedChangeListener { _, checked -> onToggle(checked) }
        binding.content.addView(view)
    }

    private fun addInfo(title: String, summary: String? = null, value: String? = null, onClick: (() -> Unit)? = null) {
        val view = layoutInflater.inflate(R.layout.item_info, binding.content, false)
        view.findViewById<TextView>(R.id.title).text = title
        val summaryView = view.findViewById<TextView>(R.id.summary)
        if (summary != null) {
            summaryView.text = summary
        }
        val valueView = view.findViewById<TextView>(R.id.value)
        if (value != null) {
            valueView.text = value
            valueView.visibility = View.VISIBLE
        }
        if (onClick != null) {
            view.setOnClickListener { onClick() }
        }
        binding.content.addView(view)
    }

    private fun addButton(title: String, onClick: () -> Unit) {
        val button = MaterialButton(this).apply {
            text = title
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                52.dp
            ).apply { setMargins(0, 8, 0, 8) }
            textSize = 14f
            cornerRadius = 12.dp
        }
        binding.content.addView(button)
    }

    private fun addSectionTitle(title: String) {
        val textView = TextView(this).apply {
            text = title
            setTextColor(getColor(R.color.md_theme_primary))
            textSize = 12f
            letterSpacing = 0.1f
            setPadding(0, 24, 0, 8)
        }
        binding.content.addView(textView)
    }

    private fun addDivider() {
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply { setMargins(0, 4, 0, 4) }
            setBackgroundColor(getColor(R.color.divider))
        }
        binding.content.addView(divider)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private fun setupWifi() {
        @Suppress("DEPRECATION")
        addToggle("Wi-Fi", "Encender/apagar Wi-Fi", wifiManager.isWifiEnabled) { enabled ->
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = enabled
        }
        addDivider()

        addSectionTitle("Red actual")
        val ssid = wifiManager.connectionInfo?.ssid?.replace("\"", "") ?: "Desconectado"
        addInfo("SSID", ssid)
        val rssi = wifiManager.connectionInfo?.rssi ?: 0
        val signalStrength = WifiManager.calculateSignalLevel(rssi, 5)
        addInfo("Señal", "$signalStrength/4")
        addDivider()

        addSectionTitle("Configuración")
        val scanEnabled = Settings.Global.getInt(contentResolver, "wifi_scan_always_enabled", 0)
        addToggle("Escaneo siempre activo", "Escanear redes en segundo plano", scanEnabled == 1) { enabled ->
            Settings.Global.putInt(contentResolver, "wifi_scan_always_enabled", if (enabled) 1 else 0)
        }
        addDivider()

        val hotspotEnabled = Settings.Global.getInt(contentResolver, "soft_ap_enabled", 0)
        addToggle("Punto de acceso", "Compartir conexión", hotspotEnabled == 1) { enabled ->
            Settings.Global.putInt(contentResolver, "soft_ap_enabled", if (enabled) 1 else 0)
        }
        addDivider()

        addSectionTitle("Redes disponibles")
        addButton("Escanear redes") { scanWifiNetworks() }
    }

    private fun scanWifiNetworks() {
        @Suppress("DEPRECATION")
        wifiManager.startScan()
        val results = wifiManager.scanResults
        val ssids = results.map { it.SSID }.filter { it.isNotEmpty() }.distinct()

        if (ssids.isEmpty()) {
            Toast.makeText(this, "No se encontraron redes", Toast.LENGTH_SHORT).show()
            return
        }

        val items = ssids.toTypedArray()
        AlertDialog.Builder(this, R.style.Theme_DarkSettings)
            .setTitle("Redes Wi-Fi")
            .setItems(items) { _, which ->
                connectToWifi(results.first { it.SSID == ssids[which] })
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun connectToWifi(scanResult: ScanResult) {
        if (scanResult.capabilities.contains("WPA") || scanResult.capabilities.contains("WEP")) {
            val input = EditText(this).apply {
                hint = "Contraseña"
                inputType = EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
            }
            AlertDialog.Builder(this, R.style.Theme_DarkSettings)
                .setTitle("Conectar a ${scanResult.SSID}")
                .setView(input)
                .setPositiveButton("Conectar") { _, _ ->
                    val password = input.text.toString()
                    val config = WifiConfiguration().apply {
                        SSID = "\"${scanResult.SSID}\""
                        preSharedKey = "\"$password\""
                    }
                    @Suppress("DEPRECATION")
                    val networkId = wifiManager.addNetwork(config)
                    if (networkId != -1) {
                        @Suppress("DEPRECATION")
                        wifiManager.enableNetwork(networkId, true)
                        Toast.makeText(this, "Conectando...", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } else {
            val config = WifiConfiguration().apply {
                SSID = "\"${scanResult.SSID}\""
            }
            @Suppress("DEPRECATION")
            val networkId = wifiManager.addNetwork(config)
            if (networkId != -1) {
                @Suppress("DEPRECATION")
                wifiManager.enableNetwork(networkId, true)
                Toast.makeText(this, "Conectando...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBluetooth() {
        val adapter = bluetoothManager.adapter
        val isEnabled = adapter?.isEnabled == true

        addToggle("Bluetooth", "Encender/apagar Bluetooth", isEnabled) { enabled ->
            if (enabled) adapter?.enable() else adapter?.disable()
        }
        addDivider()

        val visible = adapter?.scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
        addToggle("Visibilidad", "Visible para otros dispositivos", visible) { enabled ->
            if (enabled) {
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                startActivity(intent)
            }
        }
        addDivider()

        addSectionTitle("Dispositivo")
        addInfo("Nombre", adapter?.name ?: "Desconocido")
        addInfo("Dirección MAC", adapter?.address ?: "Desconocida")
        addDivider()

        addSectionTitle("Perfiles")
        val a2dpEnabled = Settings.Global.getInt(contentResolver, "bluetooth_disabled_profiles", 0)
        addToggle("A2DP", "Audio de alta calidad", a2dpEnabled == 0) { enabled ->
            Settings.Global.putInt(contentResolver, "bluetooth_disabled_profiles", if (enabled) 0 else 1)
        }
        addDivider()

        addSectionTitle("Dispositivos")
        addButton("Escanear dispositivos") { scanBluetoothDevices() }
    }

    private fun scanBluetoothDevices() {
        val adapter = bluetoothManager.adapter
        if (adapter?.isEnabled != true) {
            Toast.makeText(this, "Activa Bluetooth primero", Toast.LENGTH_SHORT).show()
            return
        }

        val pairedDevices = adapter.bondedDevices?.toList() ?: emptyList()
        val items = pairedDevices.map { "${it.name ?: "Desconocido"} (${it.address})" }.toTypedArray()

        if (items.isEmpty()) {
            Toast.makeText(this, "No hay dispositivos emparejados", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this, R.style.Theme_DarkSettings)
            .setTitle("Dispositivos emparejados")
            .setItems(items) { _, which ->
                val device = pairedDevices[which]
                Toast.makeText(this, "Seleccionado: ${device.name}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setupDisplay() {
        val currentBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        val brightnessPercent = (currentBrightness * 100 / 255)
        addInfo("Brillo", "Nivel actual", "${brightnessPercent}%")
        addDivider()

        val autoBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0)
        addToggle("Brillo automático", "Ajustar según ambiente", autoBrightness == 1) { enabled ->
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                if (enabled) 1 else 0)
        }
        addDivider()

        val nightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        addToggle("Modo oscuro", "Tema oscuro del sistema", nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) { enabled ->
            @Suppress("DEPRECATION")
            val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as android.app.UiModeManager
            uiModeManager.nightMode = if (enabled) android.app.UiModeManager.MODE_NIGHT_YES else android.app.UiModeManager.MODE_NIGHT_NO
        }
        addDivider()

        val timeout = Settings.System.getInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 30000)
        addInfo("Temporización de pantalla", "Tiempo antes de apagar", "${timeout / 1000}s") {
            showTimeoutDialog()
        }
        addDivider()

        val rotation = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
        addToggle("Rotación automática", "Girar pantalla", rotation == 1) { enabled ->
            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (enabled) 1 else 0)
        }
        addDivider()

        val fontScale = Settings.System.getFloat(contentResolver, Settings.System.FONT_SCALE, 1.0f)
        addInfo("Tamaño de fuente", "Escalar texto", "${fontScale}x") {
            showFontScaleDialog()
        }
    }

    private fun showTimeoutDialog() {
        val options = arrayOf("15 segundos", "30 segundos", "1 minuto", "2 minutos", "5 minutos", "10 minutos")
        val values = intArrayOf(15000, 30000, 60000, 120000, 300000, 600000)
        AlertDialog.Builder(this, R.style.Theme_DarkSettings)
            .setTitle("Temporización de pantalla")
            .setItems(options) { _, which ->
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, values[which])
            }
            .show()
    }

    private fun showFontScaleDialog() {
        val options = arrayOf("Pequeño (0.85x)", "Normal (1.0x)", "Grande (1.15x)", "Extra grande (1.3x)")
        val values = floatArrayOf(0.85f, 1.0f, 1.15f, 1.3f)
        AlertDialog.Builder(this, R.style.Theme_DarkSettings)
            .setTitle("Tamaño de fuente")
            .setItems(options) { _, which ->
                Settings.System.putFloat(contentResolver, Settings.System.FONT_SCALE, values[which])
            }
            .show()
    }

    private fun setupSound() {
        val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        addInfo("Volumen multimedia", "Nivel actual", "${currentVolume}/${maxVolume}")
        addDivider()

        val ringVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_RING)
        val ringMax = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_RING)
        addInfo("Volumen de tono", "Llamadas", "${ringVolume}/${ringMax}")
        addDivider()

        val notifVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION)
        val notifMax = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_NOTIFICATION)
        addInfo("Volumen notificaciones", "Alertas", "${notifVolume}/${notifMax}")
        addDivider()

        val alarmVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_ALARM)
        val alarmMax = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM)
        addInfo("Volumen alarma", "Alarmas", "${alarmVolume}/${alarmMax}")
        addDivider()

        addSectionTitle("Modo de sonido")
        addToggle("Silencio", "Sin sonido", audioManager.ringerMode == android.media.AudioManager.RINGER_MODE_SILENT) { enabled ->
            audioManager.ringerMode = if (enabled) android.media.AudioManager.RINGER_MODE_SILENT else android.media.AudioManager.RINGER_MODE_NORMAL
        }
        addDivider()

        addToggle("Vibrar", "Solo vibración", audioManager.ringerMode == android.media.AudioManager.RINGER_MODE_VIBRATE) { enabled ->
            audioManager.ringerMode = if (enabled) android.media.AudioManager.RINGER_MODE_VIBRATE else android.media.AudioManager.RINGER_MODE_NORMAL
        }
        addDivider()

        addSectionTitle("Opciones")
        addToggle("Vibrar al recibir llamadas", "Vibrar junto con el tono",
            Settings.System.getInt(contentResolver, "vibrate_when_ringing", 0) == 1) { enabled ->
            Settings.System.putInt(contentResolver, "vibrate_when_ringing", if (enabled) 1 else 0)
        }
        addDivider()

        val soundEffects = Settings.System.getInt(contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, 1)
        addToggle("Efectos de sonido", "Sonidos de interfaz", soundEffects == 1) { enabled ->
            Settings.System.putInt(contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, if (enabled) 1 else 0)
        }
        addDivider()

        val hapticFeedback = Settings.System.getInt(contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1)
        addToggle("Retroalimentación háptica", "Vibración al tocar", hapticFeedback == 1) { enabled ->
            Settings.System.putInt(contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, if (enabled) 1 else 0)
        }
    }

    private fun setupBattery() {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = batteryManager.isCharging
        val status = if (isCharging) "Cargando" else "Desconectado"

        addInfo("Nivel de batería", status, "${level}%")
        addDivider()

        addSectionTitle("Ahorro de energía")
        val batterySaver = Settings.Global.getInt(contentResolver, "low_power", 0)
        addToggle("Ahorro de batería", "Reducir rendimiento", batterySaver == 1) { enabled ->
            Settings.Global.putInt(contentResolver, "low_power", if (enabled) 1 else 0)
        }
        addDivider()

        val batterySaverAuto = Settings.Global.getInt(contentResolver, "low_power_trigger_level", 0)
        addInfo("Activación automática", "Porcentaje mínimo", "${batterySaverAuto}%") {
            showBatteryThresholdDialog()
        }
    }

    private fun showBatteryThresholdDialog() {
        val options = arrayOf("5%", "15%", "20%", "30%", "50%")
        val values = intArrayOf(5, 15, 20, 30, 50)
        AlertDialog.Builder(this, R.style.Theme_DarkSettings)
            .setTitle("Umbral de ahorro automático")
            .setItems(options) { _, which ->
                Settings.Global.putInt(contentResolver, "low_power_trigger_level", values[which])
            }
            .show()
    }

    private fun setupStorage() {
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.totalBytes
        val freeBytes = stat.availableBytes
        val usedBytes = totalBytes - freeBytes
        val totalGB = totalBytes / (1024 * 1024 * 1024)
        val usedGB = usedBytes / (1024 * 1024 * 1024)
        val freeGB = freeBytes / (1024 * 1024 * 1024)

        addInfo("Almacenamiento total", "${usedGB} GB usados", "${freeGB} GB libres")
        addDivider()

        addSectionTitle("Uso de espacio")
        val cacheSize = cacheDir?.let {
            val cacheUsed = it.walkTopDown().filter { f -> f.isFile }.sumOf { f -> f.length() }
            cacheUsed / (1024 * 1024)
        } ?: 0
        addInfo("Caché de la app", "Datos temporales", "${cacheSize} MB") {
            cacheDir?.deleteRecursively()
            Toast.makeText(this, "Caché eliminada", Toast.LENGTH_SHORT).show()
        }
        addDivider()

        addSectionTitle("Opciones")
        val automaticallyCleanup = Settings.Global.getInt(contentResolver, "automatic_storage_manager_enabled", 0)
        addToggle("Limpieza automática", "Eliminar archivos antiguos", automaticallyCleanup == 1) { enabled ->
            Settings.Global.putInt(contentResolver, "automatic_storage_manager_enabled", if (enabled) 1 else 0)
        }
    }

    private fun setupSecurity() {
        addSectionTitle("Bloqueo de pantalla")
        addInfo("Configurar bloqueo", "PIN, patrón o huella") {
            showLockScreenDialog()
        }
        addDivider()

        addSectionTitle("Ubicación")
        val locationEnabled = Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE, 0) != 0
        addToggle("Servicios de ubicación", "Activar/desactivar GPS", locationEnabled) { enabled ->
            Settings.Secure.putInt(contentResolver, Settings.Secure.LOCATION_MODE,
                if (enabled) Settings.Secure.LOCATION_MODE_HIGH_ACCURACY else Settings.Secure.LOCATION_MODE_OFF)
        }
        addDivider()

        val locationAccuracy = Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE, 0)
        addInfo("Modo de ubicación", when (locationAccuracy) {
            3 -> "Alta precisión"
            2 -> "Balanceado"
            1 -> "Ahorro de energía"
            else -> "Desactivado"
        })
        addDivider()

        addSectionTitle("NFC y conexiones")
        val nfcEnabled = Settings.Global.getInt(contentResolver, "nfc_on", 0)
        addToggle("NFC", "Pagos sin contacto", nfcEnabled == 1) { enabled ->
            Settings.Global.putInt(contentResolver, "nfc_on", if (enabled) 1 else 0)
        }
        addDivider()

        val lockScreenVisible = Settings.Secure.getInt(contentResolver, "lock_screen_allow_private_notifications", 1)
        addToggle("Notificaciones en bloqueo", "Mostrar contenido", lockScreenVisible == 1) { enabled ->
            Settings.Secure.putInt(contentResolver, "lock_screen_allow_private_notifications", if (enabled) 1 else 0)
        }
    }

    private fun showLockScreenDialog() {
        val options = arrayOf("Configurar PIN", "Configurar patrón", "Configurar huella", "Sin seguridad")
        AlertDialog.Builder(this, R.style.Theme_DarkSettings)
            .setTitle("Bloqueo de pantalla")
            .setItems(options) { _, which ->
                when (which) {
                    0, 1, 2 -> {
                        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                        startActivity(intent)
                    }
                    3 -> {
                        Toast.makeText(this, "Selecciona 'Ninguno' en la configuración", Toast.LENGTH_SHORT).show()
                        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                        startActivity(intent)
                    }
                }
            }
            .show()
    }

    private fun setupSystem() {
        addSectionTitle("Fecha y hora")
        val autoTime = Settings.Global.getInt(contentResolver, Settings.Global.AUTO_TIME, 1)
        addToggle("Hora automática", "Obtener de la red", autoTime == 1) { enabled ->
            Settings.Global.putInt(contentResolver, Settings.Global.AUTO_TIME, if (enabled) 1 else 0)
        }
        addDivider()

        val autoTimeZone = Settings.Global.getInt(contentResolver, Settings.Global.AUTO_TIME_ZONE, 1)
        addToggle("Zona horaria automática", "Obtener de la red", autoTimeZone == 1) { enabled ->
            Settings.Global.putInt(contentResolver, Settings.Global.AUTO_TIME_ZONE, if (enabled) 1 else 0)
        }
        addDivider()

        addSectionTitle("Animaciones")
        val animationScale = Settings.Global.getFloat(contentResolver, "window_animation_scale", 1.0f)
        addInfo("Escala de animación", "Velocidad de ventanas", "${animationScale}x") {
            showAnimationDialog("window_animation_scale")
        }
        addDivider()

        val transitionScale = Settings.Global.getFloat(contentResolver, "transition_animation_scale", 1.0f)
        addInfo("Escala de transición", "Velocidad de transiciones", "${transitionScale}x") {
            showAnimationDialog("transition_animation_scale")
        }
        addDivider()

        val animatorScale = Settings.Global.getFloat(contentResolver, "animator_duration_scale", 1.0f)
        addInfo("Escala de duración", "Velocidad de animaciones del sistema", "${animatorScale}x") {
            showAnimationDialog("animator_duration_scale")
        }
        addDivider()

        addSectionTitle("Conexiones")
        val airplaneMode = Settings.Global.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0)
        addToggle("Modo avión", "Desactivar todas las conexiones", airplaneMode == 1) { enabled ->
            Settings.Global.putInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, if (enabled) 1 else 0)
            val intent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            sendBroadcast(intent)
        }
        addDivider()

        val hotspotEnabled = Settings.Global.getInt(contentResolver, "soft_ap_enabled", 0)
        addToggle("Punto de acceso", "Compartir conexión", hotspotEnabled == 1) { enabled ->
            Settings.Global.putInt(contentResolver, "soft_ap_enabled", if (enabled) 1 else 0)
        }
        addDivider()

        val usbTethering = Settings.Global.getInt(contentResolver, "usb_tethering", 0)
        addToggle("Tethering USB", "Compartir por USB", usbTethering == 1) { enabled ->
            Settings.Global.putInt(contentResolver, "usb_tethering", if (enabled) 1 else 0)
        }
        addDivider()

        val bluetoothTethering = Settings.Global.getInt(contentResolver, "bluetooth_tethering", 0)
        addToggle("Tethering Bluetooth", "Compartir por Bluetooth", bluetoothTethering == 1) { enabled ->
            Settings.Global.putInt(contentResolver, "bluetooth_tethering", if (enabled) 1 else 0)
        }
    }

    private fun showAnimationDialog(key: String) {
        val options = arrayOf("Desactivado (0x)", "Lento (0.5x)", "Normal (1x)", "Rápido (1.5x)", "Muy rápido (2x)")
        val values = floatArrayOf(0f, 0.5f, 1f, 1.5f, 2f)
        AlertDialog.Builder(this, R.style.Theme_DarkSettings)
            .setTitle("Velocidad de animación")
            .setItems(options) { _, which ->
                Settings.Global.putFloat(contentResolver, key, values[which])
            }
            .show()
    }

    private fun setupApps() {
        val pm = packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
        val systemApps = packages.filter { pm.getApplicationInfo(it.packageName, 0).flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0 }
        val userApps = packages.filter { pm.getApplicationInfo(it.packageName, 0).flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0 }

        addSectionTitle("Resumen")
        addInfo("Total de apps", "Apps instaladas", "${packages.size}")
        addDivider()

        addInfo("Apps del sistema", "Preinstaladas", "${systemApps.size}")
        addDivider()

        addInfo("Apps del usuario", "Instaladas por ti", "${userApps.size}")
        addDivider()

        addSectionTitle("Gestión")
        addButton("Ver todas las apps") { showAllAppsDialog() }
        addDivider()

        addButton("Forzar detener una app") { showForceStopDialog() }
        addDivider()

        addButton("Desinstalar una app") { showUninstallDialog() }

        val backgroundCheck = Settings.Secure.getInt(contentResolver, "bg_activity_summary", 0)
        addToggle("Actividad en segundo plano", "Permitir apps en segundo plano", backgroundCheck == 0) { enabled ->
            Settings.Secure.putInt(contentResolver, "bg_activity_summary", if (enabled) 0 else 1)
        }
    }

    private fun showAllAppsDialog() {
        val pm = packageManager
        val packages = pm.getInstalledPackages(0)
        val items = packages.map {
            val appInfo = pm.getApplicationInfo(it.packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        }.toTypedArray()

        AlertDialog.Builder(this, R.style.Theme_DarkSettings)
            .setTitle("Todas las apps (${packages.size})")
            .setItems(items) { _, which ->
                val pkg = packages[which]
                Toast.makeText(this, pkg.packageName, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun showForceStopDialog() {
        val pm = packageManager
        val packages = pm.getInstalledPackages(0).filter {
            pm.getApplicationInfo(it.packageName, 0).flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0
        }
        val items = packages.map {
            pm.getApplicationLabel(pm.getApplicationInfo(it.packageName, 0)).toString()
        }.toTypedArray()

        AlertDialog.Builder(this, R.style.Theme_DarkSettings)
            .setTitle("Forzar detener")
            .setItems(items) { _, which ->
                val pkg = packages[which]
                forceStopPackage(pkg.packageName)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showUninstallDialog() {
        val pm = packageManager
        val packages = pm.getInstalledPackages(0).filter {
            pm.getApplicationInfo(it.packageName, 0).flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0
        }
        val items = packages.map {
            pm.getApplicationLabel(pm.getApplicationInfo(it.packageName, 0)).toString()
        }.toTypedArray()

        AlertDialog.Builder(this, R.style.Theme_DarkSettings)
            .setTitle("Desinstalar")
            .setItems(items) { _, which ->
                val pkg = packages[which]
                uninstallPackage(pkg.packageName)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun forceStopPackage(packageName: String) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "am force-stop $packageName"))
            process.waitFor()
            Toast.makeText(this, "App forzada a detener", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uninstallPackage(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = android.net.Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
