package com.darksettings.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.darksettings.R
import com.darksettings.databinding.ActivityPermissionsBinding

class PermissionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionsBinding

    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.NFC,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.CHANGE_NETWORK_STATE,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
        Manifest.permission.WAKE_LOCK,
        Manifest.permission.RECEIVE_BOOT_COMPLETED,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.WRITE_CALL_LOG,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.ANSWER_PHONE_CALLS,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.BODY_SENSORS,
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.VIBRATE
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.count { it }
        val denied = permissions.values.count { !it }
        Toast.makeText(this, "Concedidos: $granted, Denegados: $denied", Toast.LENGTH_SHORT).show()
        updatePermissionStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = "Permisos del Sistema"
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnRequestAll.setOnClickListener {
            val needed = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }.toTypedArray()
            if (needed.isNotEmpty()) {
                requestPermissionLauncher.launch(needed)
            } else {
                Toast.makeText(this, "Todos los permisos ya concedidos", Toast.LENGTH_SHORT).show()
            }
            updatePermissionStatus()
        }

        binding.btnGrantSecure.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", packageName, null)
            startActivity(intent)
        }

        binding.btnGrantSystem.setOnClickListener {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }

        binding.btnCopyAdb.setOnClickListener {
            val adbCommand = """#!/bin/bash
PACKAGE="com.darksettings"
adb shell pm grant \$PACKAGE android.permission.WRITE_SECURE_SETTINGS
adb shell pm grant \$PACKAGE android.permission.WRITE_SETTINGS
adb shell pm grant \$PACKAGE android.permission.DUMP
adb shell pm grant \$PACKAGE android.permission.PACKAGE_USAGE_STATS
adb shell pm grant \$PACKAGE android.permission.INSTALL_PACKAGES
adb shell pm grant \$PACKAGE android.permission.DELETE_PACKAGES
adb shell pm grant \$PACKAGE android.permission.DEVICE_POWER
adb shell pm grant \$PACKAGE android.permission.REBOOT
adb shell pm grant \$PACKAGE android.permission.STATUS_BAR
adb shell pm grant \$PACKAGE android.permission.EXPAND_STATUS_BAR
adb shell pm grant \$PACKAGE android.permission.CONNECTIVITY_INTERNAL
adb shell pm grant \$PACKAGE android.permission.MANAGE_NETWORK_POLICY
adb shell pm grant \$PACKAGE android.permission.TETHER_PRIVILEGED
adb shell pm grant \$PACKAGE android.permission.NETWORK_SETTINGS
adb shell pm grant \$PACKAGE android.permission.MODIFY_PHONE_STATE
adb shell pm grant \$PACKAGE android.permission.SET_TIME
adb shell pm grant \$PACKAGE android.permission.SET_TIME_ZONE
adb shell pm grant \$PACKAGE android.permission.WRITE_MEDIA_STORAGE
adb shell pm grant \$PACKAGE android.permission.CLEAR_APP_USER_DATA
adb shell pm grant \$PACKAGE android.permission.FORCE_STOP_PACKAGES
adb shell appops set \$PACKAGE android:write_settings allow
adb shell appops set \$PACKAGE android:manage_external_storage allow
adb shell appops set \$PACKAGE android:access_media_location allow
echo "Permisos concedidos!" """

            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ADB Command", adbCommand)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Script ADB copiado al portapapeles", Toast.LENGTH_LONG).show()
        }

        binding.btnCopySingleAdb.setOnClickListener {
            val command = "adb shell pm grant com.darksettings android.permission.WRITE_SECURE_SETTINGS"
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ADB Command", command)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Comando copiado", Toast.LENGTH_SHORT).show()
        }

        updatePermissionStatus()
    }

    private fun updatePermissionStatus() {
        val granted = permissions.count {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        binding.tvPermissionCount.text = "Permisos de aplicación: $granted / ${permissions.size}"

        val writeSettings = Settings.System.canWrite(this)
        binding.tvWriteSettings.text = if (writeSettings) "✓ Concedido" else "✗ Requiere ADB"

        binding.tvSecureSettings.text = "Requiere: adb shell pm grant com.darksettings android.permission.WRITE_SECURE_SETTINGS"
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }
}
