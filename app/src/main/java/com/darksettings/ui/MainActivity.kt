package com.darksettings.ui

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.darksettings.R
import com.darksettings.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val categories = listOf(
        SettingsCategory("Permisos", "Conceder permisos del sistema", R.drawable.ic_security, R.color.icon_security, "permissions"),
        SettingsCategory("Wi-Fi", "Conexiones y red", R.drawable.ic_wifi, R.color.icon_wifi, "wifi"),
        SettingsCategory("Bluetooth", "Dispositivos conectados", R.drawable.ic_bluetooth, R.color.icon_bluetooth, "bluetooth"),
        SettingsCategory("Pantalla", "Brillo, fondo de pantalla", R.drawable.ic_display, R.color.icon_display, "display"),
        SettingsCategory("Sonido", "Volumen, tonos, vibración", R.drawable.ic_sound, R.color.icon_sound, "sound"),
        SettingsCategory("Batería", "Uso de batería, ahorro", R.drawable.ic_battery, R.color.icon_battery, "battery"),
        SettingsCategory("Almacenamiento", "Espacio del dispositivo", R.drawable.ic_storage, R.color.icon_storage, "storage"),
        SettingsCategory("Seguridad", "Bloqueo de pantalla, ubicación", R.drawable.ic_security, R.color.icon_security, "security"),
        SettingsCategory("Sistema", "Fecha, idioma, información", R.drawable.ic_system, R.color.icon_system, "system"),
        SettingsCategory("Aplicaciones", "Apps instaladas, permisos", R.drawable.ic_apps, R.color.icon_apps, "apps")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val layoutManager = LinearLayoutManager(this)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = SettingsAdapter(categories) { category ->
            val intent = if (category.action == "permissions") {
                Intent(this, PermissionsActivity::class.java)
            } else {
                Intent(this, SettingsActivity::class.java).apply {
                    putExtra("action", category.action)
                    putExtra("title", category.title)
                }
            }
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.recyclerView.itemAnimator?.apply {
            addDuration = 100
            removeDuration = 100
        }

        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.scale_in)
        binding.recyclerView.postDelayed({
            binding.recyclerView.scheduleLayoutAnimation()
        }, 100)
    }

    override fun onResume() {
        super.onResume()
        binding.recyclerView.scheduleLayoutAnimation()
    }
}
