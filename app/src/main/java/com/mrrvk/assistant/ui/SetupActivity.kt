package com.mrrvk.assistant.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mrrvk.assistant.MrRvkApp
import com.mrrvk.assistant.R

class SetupActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val OVERLAY_REQUEST_CODE = 101
        private const val BATTERY_OPT_REQUEST_CODE = 102
        private const val WRITE_SETTINGS_REQUEST_CODE = 103
    }

    private lateinit var nameInput: EditText
    private lateinit var btnNext: Button
    private lateinit var btnGrantPermissions: Button
    private lateinit var btnFinish: Button
    private lateinit var setupStep1: LinearLayout
    private lateinit var setupStep2: LinearLayout
    private lateinit var setupStep3: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar

    private var userName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        nameInput = findViewById(R.id.nameInput)
        btnNext = findViewById(R.id.btnNext)
        btnGrantPermissions = findViewById(R.id.btnGrantPermissions)
        btnFinish = findViewById(R.id.btnFinish)
        setupStep1 = findViewById(R.id.setupStep1)
        setupStep2 = findViewById(R.id.setupStep2)
        setupStep3 = findViewById(R.id.setupStep3)
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)

        btnNext.setOnClickListener {
            val name = nameInput.text.toString().trim()
            if (name.isEmpty()) {
                nameInput.error = "Please enter your name"
                return@setOnClickListener
            }
            userName = name
            showStep2()
        }

        btnGrantPermissions.setOnClickListener {
            requestAllPermissions()
        }

        btnFinish.setOnClickListener {
            finishSetup()
        }
    }

    private fun showStep2() {
        setupStep1.visibility = View.GONE
        setupStep2.visibility = View.VISIBLE
        statusText.text = "Step 2: Grant Permissions"
    }

    private fun showStep3() {
        setupStep2.visibility = View.GONE
        setupStep3.visibility = View.VISIBLE
        statusText.text = "Step 3: Final Setup"
    }

    private fun requestAllPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_CALL_LOG
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            requestSpecialPermissions()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            requestSpecialPermissions()
        }
    }

    private fun requestSpecialPermissions() {
        // Request overlay permission
        if (!Settings.canDrawOverlays(this)) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, OVERLAY_REQUEST_CODE)
                return
            } catch (e: Exception) {
                // Continue if not available
            }
        }

        // Request battery optimization exemption
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivityForResult(intent, BATTERY_OPT_REQUEST_CODE)
                return
            } catch (e: Exception) {
                // Continue if not available
            }
        }

        // Request write settings permission
        if (!Settings.System.canWrite(this)) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivityForResult(intent, WRITE_SETTINGS_REQUEST_CODE)
                return
            } catch (e: Exception) {
                // Continue
            }
        }

        showStep3()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            OVERLAY_REQUEST_CODE, BATTERY_OPT_REQUEST_CODE, WRITE_SETTINGS_REQUEST_CODE -> {
                requestSpecialPermissions()
            }
        }
    }

    private fun finishSetup() {
        progressBar.visibility = View.VISIBLE
        btnFinish.isEnabled = false
        statusText.text = "Setting up Mr. RVK..."

        MrRvkApp.setSetupComplete(this, userName)

        // Short delay for effect, then go to main
        window.decorView.postDelayed({
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }, 1500)
    }
}
