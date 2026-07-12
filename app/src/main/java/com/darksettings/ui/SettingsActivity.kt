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
        addToggle("Wi-Fi", "Conectar a redes Wi-Fi", wifiManager.isWifiEnabled) { enabled ->
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = enabled
        }
        addDivider()
        addInfo("Configuración de Wi-Fi", "Administrar redes y preferencias") {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
        addDivider()
        addInfo("Configuración de red móvil", "Datos móviles, APN") {
            startActivity(Intent(Settings.ACTION_DATA_ROAMING_SETTINGS))
        }
        addDivider()
        addInfo("Modo avión", "Desactivar todas las conexiones inalámbricas") {
            startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))
        }
    }

    private fun setupBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        val isEnabled = adapter?.isEnabled == true

        addToggle("Bluetooth", "Conectar dispositivos", isEnabled) { enabled ->
            if (enabled) adapter?.enable() else adapter?.disable()
        }
        addDivider()
        addInfo("Dispositivos Bluetooth", "Emparejar y gestionar dispositivos") {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }
    }

    private fun setupDisplay() {
        val currentBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        val brightnessPercent = (currentBrightness * 100 / 255)

        addInfo("Brillo de pantalla", "Ajustar brillo manualmente", "${brightnessPercent}%") {
            startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
        }
        addDivider()

        val autoBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0)
        addToggle("Brillo automático", "Ajustar según ambiente", autoBrightness == 1) { enabled ->
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                if (enabled) 1 else 0)
        }
        addDivider()

        val timeout = Settings.System.getInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 30000)
        val timeoutSec = timeout / 1000
        addInfo("Temporización de pantalla", "Tiempo antes de apagar", "${timeoutSec}s") {
            startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
        }
        addDivider()

        val fontScale = Settings.System.getFloat(contentResolver, Settings.System.FONT_SCALE, 1.0f)
        addInfo("Tamaño de fuente", "Escalar texto del sistema", "${fontScale}x") {
            startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
        }
        addDivider()

        val rotation = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
        addToggle("Rotación automática", "Girar pantalla según orientación", rotation == 1) { enabled ->
            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION,
                if (enabled) 1 else 0)
        }
    }

    private fun setupSound() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager

        val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        addInfo("Volumen de multimedia", "Ajustar nivel de volumen", "${currentVolume}/${maxVolume}") {
            startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
        }
        addDivider()

        val ringVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_RING)
        val ringMax = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_RING)
        addInfo("Volumen de tono", "Ajustar tono de llamada", "${ringVolume}/${ringMax}") {
            startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
        }
        addDivider()

        val notifVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION)
        val notifMax = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_NOTIFICATION)
        addInfo("Volumen de notificaciones", "Ajustar volumen de alertas", "${notifVolume}/${notifMax}") {
            startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
        }
        addDivider()

        val vibrateOnCall = Settings.System.getInt(contentResolver, "vibrate_when_ringing", 0)
        addToggle("Vibrar al recibir llamadas", "Vibrar junto con el tono", vibrateOnCall == 1) { enabled ->
            Settings.System.putInt(contentResolver, "vibrate_when_ringing", if (enabled) 1 else 0)
        }
        addDivider()

        val soundEffects = Settings.System.getInt(contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, 1)
        addToggle("Efectos de sonido", "Sonidos de interfaz", soundEffects == 1) { enabled ->
            Settings.System.putInt(contentResolver, Settings.System.SOUND_EFFECTS_ENABLED,
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
        addInfo("Ahorro de batería", "Optimizar consumo de energía") {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        addDivider()
        addInfo("Uso de batería", "Ver qué consume batería") {
            val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
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
        addInfo("Gestionar almacenamiento", "Ver detalles y liberar espacio") {
            startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
        }
    }

    private fun setupSecurity() {
        addInfo("Bloqueo de pantalla", "Configurar PIN, patrón o huella") {
            startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
        }
        addDivider()
        addInfo("Ubicación", "Servicios de ubicación y permisos") {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
        addDivider()
        addInfo("Privacidad", "Permisos de apps y datos") {
            startActivity(Intent(Settings.ACTION_PRIVACY_SETTINGS))
        }
        addDivider()
        addInfo("Cifrado del dispositivo", "Proteger datos con cifrado") {
            startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
        }
    }

    private fun setupSystem() {
        addInfo("Fecha y hora", "Configurar zona horaria y formato") {
            startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
        }
        addDivider()
        addInfo("Idioma y entrada", "Cambiar idioma del teclado y sistema") {
            startActivity(Intent(Settings.ACTION_LOCALE_SETTINGS))
        }
        addDivider()
        addInfo("Accesibilidad", "Opciones de accesibilidad") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        addDivider()
        addInfo("Opciones del desarrollador", "Herramientas avanzadas") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        }
        addDivider()
        addInfo("Acerca del teléfono", "Información del dispositivo") {
            startActivity(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
        }
    }

    private fun setupApps() {
        addInfo("Todas las aplicaciones", "Gestionar apps instaladas") {
            startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS))
        }
        addDivider()
        addInfo("Aplicaciones predeterminas", "Apps por defecto para cada acción") {
            startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        }
        addDivider()
        addInfo("Permisos de apps", "Controlar permisos por app") {
            startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS))
        }
    }
}
