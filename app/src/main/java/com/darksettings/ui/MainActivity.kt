package com.darksettings.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.darksettings.R
import com.darksettings.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val categories = listOf(
        SettingsCategory("Permisos", "Conceder permisos del sistema", R.drawable.ic_security, R.color.security_color, "permissions"),
        SettingsCategory("Wi-Fi", "Conexiones y red", R.drawable.ic_wifi, R.color.wifi_color, "wifi"),
        SettingsCategory("Bluetooth", "Dispositivos conectados", R.drawable.ic_bluetooth, R.color.bluetooth_color, "bluetooth"),
        SettingsCategory("Pantalla", "Brillo, fondo de pantalla", R.drawable.ic_display, R.color.display_color, "display"),
        SettingsCategory("Sonido", "Volumen, tonos, vibración", R.drawable.ic_sound, R.color.sound_color, "sound"),
        SettingsCategory("Batería", "Uso de batería, ahorro", R.drawable.ic_battery, R.color.battery_color, "battery"),
        SettingsCategory("Almacenamiento", "Espacio del dispositivo", R.drawable.ic_storage, R.color.storage_color, "storage"),
        SettingsCategory("Seguridad", "Bloqueo de pantalla, ubicación", R.drawable.ic_security, R.color.security_color, "security"),
        SettingsCategory("Sistema", "Fecha, idioma, información", R.drawable.ic_system, R.color.system_color, "system"),
        SettingsCategory("Aplicaciones", "Apps instaladas, permisos", R.drawable.ic_apps, R.color.apps_color, "apps")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = SettingsAdapter(categories) { category ->
            if (category.action == "permissions") {
                val intent = Intent(this, PermissionsActivity::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(this, SettingsActivity::class.java).apply {
                    putExtra("action", category.action)
                    putExtra("title", category.title)
                }
                startActivity(intent)
            }
        }
    }
}
