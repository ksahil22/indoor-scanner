package com.example.indoorscanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private val TAG = "BLE_ADVERTISING"
    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_PERMISSIONS = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Bluetooth is not available or not enabled", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        if (bluetoothLeAdvertiser == null) {
            Toast.makeText(this, "BLE Advertising not supported", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Request permissions for Android 10 and below
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSIONS
                )
            }
        }

        val advertiseButton: Button = findViewById(R.id.advertiseButton)
        advertiseButton.setOnClickListener {
            startOneTimeAdvertising()
        }
        checkStudentIdAndStart()
    }

    private fun checkStudentIdAndStart() {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val studentId = sharedPref.getString("student_id", null)

        if (studentId == null) {
            showStudentIdDialog(sharedPref)
        } else {
            val studentIDText: TextView = findViewById(R.id.studentID)
            studentIDText.setText("Student ID : " + studentId.toString())
        }
    }

    private fun showStudentIdDialog(sharedPref: android.content.SharedPreferences) {
        val inputField = android.widget.EditText(this)

        AlertDialog.Builder(this)
            .setTitle("Enter Student ID")
            .setView(inputField)
            .setCancelable(false)
            .setPositiveButton("Submit") { _, _ ->
                val studentId = inputField.text.toString().trim()
                if (studentId.isNotEmpty()) {
                    sharedPref.edit().putString("student_id", studentId).apply()
                } else {
                    Toast.makeText(this, "Student ID is required", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .show()
    }

    private fun startOneTimeAdvertising() {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val studentID = sharedPref.getString("student_id", null)
        val SERVICE_UUID = UUID.fromString("0000feed-0000-1000-8000-00805f9b34fb")
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .addServiceData(ParcelUuid(SERVICE_UUID), studentID.toString().toByteArray(Charsets.UTF_8))
            .build()

        bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)

        // Stop advertising after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
            Log.d("BLE", "Advertising stopped")
        }, 3000)
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Log.d(TAG, "Advertising started successfully")
            Toast.makeText(applicationContext, "Advertising started", Toast.LENGTH_SHORT).show()

            // Stop advertising after 5 seconds (optional)
//            bluetoothLeAdvertiser?.let {
//                it.stopAdvertising(this)
//                Log.d(TAG, "Advertising stopped after one shot")
//            }
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e(TAG, "Advertising failed with error code: $errorCode")
            Toast.makeText(applicationContext, "Advertising failed: $errorCode", Toast.LENGTH_SHORT).show()
        }
    }
}
