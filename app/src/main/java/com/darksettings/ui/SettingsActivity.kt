package com.darksettings.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.darksettings.R
import com.darksettings.databinding.ActivitySettingsBinding
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val action = intent.getStringExtra("action") ?: "main"
        val title = intent.getStringExtra("title") ?: "Configuración"

        binding.toolbar.title = title
        binding.toolbar.setNavigationOnClickListener { finish() }

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

    private fun addDivider() {
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply { setMargins(16, 8, 16, 8) }
            setBackgroundColor(getColor(R.color.divider))
        }
        binding.content.addView(divider)
    }

    private fun setupWifi() {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        addToggle("Wi-Fi", "Encender/apagar Wi-Fi", wifiManager.isWifiEnabled) { enabled ->
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = enabled
        }
        addDivider()
        addInfo("Red Wi-Fi actual", "SSID", wifiManager.connectionInfo?.ssid?.replace("\"", "") ?: "Desconectado")
        addDivider()
        val scanEnabled = Settings.Global.getInt(contentResolver, Settings.Global.WIFI_SCAN_ALWAYS_ENABLED, 0)
        addToggle("Escaneo Wi-Fi siempre activo", "Escanear redes en segundo plano", scanEnabled == 1) { enabled ->
            Settings.Global.putInt(contentResolver, Settings.Global.WIFI_SCAN_ALWAYS_ENABLED, if (enabled) 1 else 0)
        }
        addDivider()
        val hotspotEnabled = Settings.Global.getInt(contentResolver, "soft_ap_enabled", 0)
        addToggle("Punto de acceso", "Compartir conexión móvil", hotspotEnabled == 1) { enabled ->
            Settings.Global.putInt(contentResolver, "soft_ap_enabled", if (enabled) 1 else 0)
        }
    }

    private fun setupBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        val isEnabled = adapter?.isEnabled == true

        addToggle("Bluetooth", "Encender/apagar Bluetooth", isEnabled) { enabled ->
            if (enabled) adapter?.enable() else adapter?.disable()
        }
        addDivider()
        val visible = adapter?.scanMode == android.bluetooth.BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
        addToggle("Visibilidad", "Visible para otros dispositivos", visible) { enabled ->
            if (enabled) {
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                startActivity(intent)
            }
        }
        addDivider()
        addInfo("Nombre del dispositivo", adapter?.name ?: "Desconocido")
        addDivider()
        val btAutoConnect = Settings.Global.getInt(contentResolver, "bluetooth_disabled_profiles", 0)
        addToggle("A2DP", "Audio de alta calidad por Bluetooth", btAutoConnect == 0) { enabled ->
            Settings.Global.putInt(contentResolver, "bluetooth_disabled_profiles", if (enabled) 0 else 1)
        }
    }

    private fun setupDisplay() {
        val currentBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        val brightnessPercent = (currentBrightness * 100 / 255)

        addInfo("Brillo de pantalla", "Nivel actual", "${brightnessPercent}%")
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
        val timeoutSec = timeout / 1000
        addInfo("Temporización de pantalla", "Tiempo antes de apagar", "${timeoutSec}s") {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
            startActivity(intent)
        }
        addDivider()

        val fontScale = Settings.System.getFloat(contentResolver, Settings.System.FONT_SCALE, 1.0f)
        addInfo("Tamaño de fuente", "Escalar texto del sistema", "${fontScale}x") {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
            startActivity(intent)
        }
        addDivider()

        val rotation = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
        addToggle("Rotación automática", "Girar pantalla según orientación", rotation == 1) { enabled ->
            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION,
                if (enabled) 1 else 0)
        }
        addDivider()

        val screenOffTimeout = Settings.System.getInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 30000)
        addInfo("Tiempo de espera", "Antes de apagar pantalla", "${screenOffTimeout / 1000}s")
    }

    private fun setupSound() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager

        val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        addInfo("Volumen multimedia", "Nivel actual", "${currentVolume}/${maxVolume}")
        addDivider()

        val ringVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_RING)
        val ringMax = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_RING)
        addInfo("Volumen de tono", "Llamadas entrantes", "${ringVolume}/${ringMax}")
        addDivider()

        val notifVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION)
        val notifMax = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_NOTIFICATION)
        addInfo("Volumen notificaciones", "Alertas", "${notifVolume}/${notifMax}")
        addDivider()

        val alarmVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_ALARM)
        val alarmMax = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM)
        addInfo("Volumen alarma", "Alarmas", "${alarmVolume}/${alarmMax}")
        addDivider()

        addToggle("Silencio", "Modo silencio", audioManager.ringerMode == android.media.AudioManager.RINGER_MODE_SILENT) { enabled ->
            audioManager.ringerMode = if (enabled) android.media.AudioManager.RINGER_MODE_SILENT else android.media.AudioManager.RINGER_MODE_NORMAL
        }
        addDivider()

        addToggle("Vibrar al recibir llamadas", "Vibrar junto con el tono",
            Settings.System.getInt(contentResolver, "vibrate_when_ringing", 0) == 1) { enabled ->
            Settings.System.putInt(contentResolver, "vibrate_when_ringing", if (enabled) 1 else 0)
        }
        addDivider()

        val soundEffects = Settings.System.getInt(contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, 1)
        addToggle("Efectos de sonido", "Sonidos de interfaz", soundEffects == 1) { enabled ->
            Settings.System.putInt(contentResolver, Settings.System.SOUND_EFFECTS_ENABLED,
                if (enabled) 1 else 0)
        }
        addDivider()

        val hapticFeedback = Settings.System.getInt(contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1)
        addToggle("Retroalimentación háptica", "Vibración al tocar", hapticFeedback == 1) { enabled ->
            Settings.System.putInt(contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED,
                if (enabled) 1 else 0)
        }
    }

    private fun setupBattery() {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = batteryManager.isCharging
        val status = if (isCharging) "Cargando" else "Desconectado"

        addInfo("Nivel de batería", status, "${level}%")
        addDivider()

        val health = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        addInfo("Salud de batería", "Capacidad restante", "${health}%")
        addDivider()

        val temperature = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        addInfo("Temperatura", "Batería", "${temperature}°C")
        addDivider()

        val batterySaver = Settings.Global.getInt(contentResolver, "low_power", 0)
        addToggle("Ahorro de batería", "Reducir rendimiento para ahorrar energía", batterySaver == 1) { enabled ->
            Settings.Global.putInt(contentResolver, "low_power", if (enabled) 1 else 0)
        }
        addDivider()

        val optimizeBattery = Settings.Secure.getInt(contentResolver, "user_restriction_modify_apps", 0)
        addToggle("Restricción de apps en segundo plano", "Limitar apps mientras se usa batería", optimizeBattery == 1) { enabled ->
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        }
    }

    private fun setupStorage() {
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.totalBytes
        val freeBytes = stat.availableBytes
        val usedBytes = totalBytes - freeBytes
        val totalGB = totalBytes / (1024 * 1024 * 1024)
        val usedGB = usedBytes / (1024 * 1024 * 1024)
        val freeGB = freeBytes / (1024 * 1024 * 1024)

        addInfo("Almacenamiento del dispositivo", "${usedGB} GB usados de ${totalGB} GB", "${freeGB} GB libres")
        addDivider()

        val cacheSize = cacheDir?.let {
            val cacheUsed = it.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            cacheUsed / (1024 * 1024)
        } ?: 0
        addInfo("Caché de la app", "Datos temporales", "${cacheSize} MB") {
            cacheDir?.deleteRecursively()
            Toast.makeText(this, "Caché eliminada", Toast.LENGTH_SHORT).show()
        }
        addDivider()

        val automaticallyCleanup = Settings.Global.getInt(contentResolver, "automatic_storage_manager_enabled", 0)
        addToggle("Limpieza automática de almacenamiento", "Eliminar archivos antiguos automáticamente", automaticallyCleanup == 1) { enabled ->
            Settings.Global.putInt(contentResolver, "automatic_storage_manager_enabled", if (enabled) 1 else 0)
        }
    }

    private fun setupSecurity() {
        addInfo("Bloqueo de pantalla", "Configurar PIN, patrón o huella") {
            val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
            startActivity(intent)
        }
        addDivider()

        addInfo("Ubicación", "Servicios de ubicación y permisos") {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
        addDivider()

        addInfo("Privacidad", "Permisos de apps y datos") {
            val intent = Intent(Settings.ACTION_PRIVACY_SETTINGS)
            startActivity(intent)
        }
        addDivider()

        val nfcEnabled = Settings.Global.getInt(contentResolver, "nfc_on", 0)
        addToggle("NFC", "Pagos sin contacto y lectura de etiquetas", nfcEnabled == 1) { enabled ->
            Settings.Global.putInt(contentResolver, "nfc_on", if (enabled) 1 else 0)
        }
        addDivider()

        val lockScreenVisible = Settings.Secure.getInt(contentResolver, "lock_screen_allow_private_notifications", 1)
        addToggle("Notificaciones en pantalla de bloqueo", "Mostrar contenido de notificaciones", lockScreenVisible == 1) { enabled ->
            Settings.Secure.putInt(contentResolver, "lock_screen_allow_private_notifications", if (enabled) 1 else 0)
        }
    }

    private fun setupSystem() {
        val autoTime = Settings.Global.getInt(contentResolver, Settings.Global.AUTO_TIME, 1)
        addToggle("Fecha y hora automática", "Obtener de la red", autoTime == 1) { enabled ->
            Settings.Global.putInt(contentResolver, Settings.Global.AUTO_TIME, if (enabled) 1 else 0)
        }
        addDivider()

        val autoTimeZone = Settings.Global.getInt(contentResolver, Settings.Global.AUTO_TIME_ZONE, 1)
        addToggle("Zona horaria automática", "Obtener de la red", autoTimeZone == 1) { enabled ->
            Settings.Global.putInt(contentResolver, Settings.Global.AUTO_TIME_ZONE, if (enabled) 1 else 0)
        }
        addDivider()

        val animationScale = Settings.Global.getFloat(contentResolver, "window_animation_scale", 1.0f)
        addInfo("Escala de animación", "Velocidad de animaciones", "${animationScale}x") {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            startActivity(intent)
        }
        addDivider()

        val transitionScale = Settings.Global.getFloat(contentResolver, "transition_animation_scale", 1.0f)
        addInfo("Escala de transición", "Velocidad de transiciones", "${transitionScale}x") {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            startActivity(intent)
        }
        addDivider()

        val windowAnimationScale = Settings.Global.getFloat(contentResolver, "window_animation_scale", 1.0f)
        addInfo("Escala de ventanas", "Velocidad de ventanas", "${windowAnimationScale}x") {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            startActivity(intent)
        }
        addDivider()

        val airplaneMode = Settings.Global.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0)
        addToggle("Modo avión", "Desactivar todas las conexiones", airplaneMode == 1) { enabled ->
            Settings.Global.putInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, if (enabled) 1 else 0)
            val intent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            sendBroadcast(intent)
        }
        addDivider()

        val hotspotEnabled = Settings.Global.getInt(contentResolver, "soft_ap_enabled", 0)
        addToggle("Punto de acceso", "Compartir conexión móvil", hotspotEnabled == 1) { enabled ->
            Settings.Global.putInt(contentResolver, "soft_ap_enabled", if (enabled) 1 else 0)
        }
    }

    private fun setupApps() {
        val pm = packageManager
        val packages = pm.getInstalledPackages(0)
        val systemApps = packages.filter { pm.getApplicationInfo(it.packageName, 0).flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0 }
        val userApps = packages.filter { pm.getApplicationInfo(it.packageName, 0).flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0 }

        addInfo("Apps instaladas", "Aplicaciones totales", "${packages.size}")
        addDivider()

        addInfo("Apps del sistema", "Apps preinstaladas", "${systemApps.size}")
        addDivider()

        addInfo("Apps del usuario", "Apps instaladas por ti", "${userApps.size}")
        addDivider()

        addInfo("Gestionar apps", "Ver lista completa de apps") {
            val intent = Intent(Settings.ACTION_APPLICATION_SETTINGS)
            startActivity(intent)
        }
        addDivider()

        addInfo("Apps predeterminas", "Apps por defecto para cada acción") {
            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            startActivity(intent)
        }
        addDivider()

        val backgroundCheck = Settings.Secure.getInt(contentResolver, "bg_activity_summary", 0)
        addToggle("Actividad en segundo plano", "Permitir apps en segundo plano", backgroundCheck == 0) { enabled ->
            Settings.Secure.putInt(contentResolver, "bg_activity_summary", if (enabled) 0 else 1)
        }
        addDivider()

        addInfo("Permisos de apps", "Controlar permisos por app") {
            val intent = Intent(Settings.ACTION_APPLICATION_SETTINGS)
            startActivity(intent)
        }
    }
}
