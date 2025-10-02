package com.example.indoorscanner

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
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
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer
import java.util.*

class MainActivity : AppCompatActivity() {

    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private val TAG = "BLE_ADVERTISING"
    private val REQUEST_PERMISSIONS = 2
    private val TX_POWER_DBM = -62
    private val peerRSSI = mutableMapOf<Byte, Byte>()
    private val SERVICE_UUID = UUID.fromString("0000feed-0000-1000-8000-00805f9b34fb")
    private var studentByte: Byte = 0
    private var NUMBER_OF_PEERS = 10
    private var PAYLOAD_SIZE = 2 + (2 * NUMBER_OF_PEERS)
    private var payload: ByteArray = ByteArray(PAYLOAD_SIZE)

    private val permissions = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_ADVERTISE
    )

    private fun checkAndRequestPermissions() {
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1001)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        peerRSSI.clear()
        Log.d("Clear: ", "Clear")
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
        else {
            checkAndRequestPermissions()
        }

        val advertiseButton: Button = findViewById(R.id.btn_mark_attendance)
        advertiseButton.setOnClickListener {
            startOneTimeAdvertising()
        }
        checkStudentIdAndStart()
        val scanner = bluetoothAdapter.bluetoothLeScanner
        val scanFilter = android.bluetooth.le.ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID)).build()
        val scanSettings = android.bluetooth.le.ScanSettings.Builder()
            .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner.startScan(listOf(scanFilter), scanSettings, scanCallback)
    }

    private fun checkStudentIdAndStart() {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val studentId = sharedPref.getString("student_id", null)

        if (studentId == null) {
            showStudentIdDialog(sharedPref)
        } else {
            val studentIDText: TextView = findViewById(R.id.text_student_id)
            studentIDText.setText("Student Roll Number : " + studentId.toString())
        }
    }

    fun updatePayload(newPayload: ByteArray) {
        // Stop advertising
        bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)

        // Small delay before restarting
        Handler(Looper.getMainLooper()).postDelayed({
            startAdvertising()
        }, 500)  // 500 ms delay
    }

    private fun showStudentIdDialog(sharedPref: android.content.SharedPreferences) {
        val inputField = android.widget.EditText(this)

        AlertDialog.Builder(this)
            .setTitle("Enter Student Roll number")
            .setView(inputField)
            .setCancelable(false)
            .setPositiveButton("Submit") { _, _ ->
                val studentId = inputField.text.toString().trim()
                if (studentId.isNotEmpty()) {
                    sharedPref.edit().putString("student_id", studentId).apply()
                } else {
                    Toast.makeText(this, "Student Roll Number is required", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .show()
    }

    private fun updatePayload(studentByte: Byte) {
        val buffer = ByteBuffer.allocate(PAYLOAD_SIZE)  // txPower + ID + (max 10 peers * 2 bytes)

        buffer.put(TX_POWER_DBM.toByte())
        Log.d("TXPower: ", TX_POWER_DBM.toByte().toString())
        // buffer.put(studentBytes.size.toByte())
        buffer.put(studentByte)
        Log.d("studentBytes: ", studentByte.toString())

        // Add up to 10 strongest peers
        peerRSSI.entries.sortedBy { -it.value }.take(NUMBER_OF_PEERS).forEach { (hash, rssi) ->
            buffer.put(hash)
            buffer.put(rssi)
        }
        payload = buffer.array()
    }

    private fun startOneTimeAdvertising() {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val studentID = sharedPref.getString("student_id", null)
        Log.d("Student ID: ", studentID.toString())
        if (studentID == null) return
        studentByte = studentID.toByte()
        startAdvertising()
    }
    private fun startAdvertising() {
        updatePayload(studentByte)
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .addServiceData(ParcelUuid(SERVICE_UUID), payload)
            .build()
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)

        // Stop advertising after 10 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
            Log.d("BLE", "Advertising stopped")
            peerRSSI.clear()
            val attendanceText: TextView = findViewById<TextView>(R.id.text_status)
            attendanceText.setText("Status: Not Marked")
            attendanceText.setTextColor(Color.RED)
            val advertiseButton: Button = findViewById(R.id.btn_mark_attendance)
            advertiseButton.setEnabled(true)
            advertiseButton.getBackground().setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY)
        }, 30000)
    }


    private val scanCallback = object : android.bluetooth.le.ScanCallback() {
        override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
            result?.let {
                val data = it.scanRecord?.getServiceData(ParcelUuid(SERVICE_UUID)) ?: return
                if (data.size < 2) return

                val txPower = data[0]         // Not used here
                // val peerHash = data[1]        // First byte of student ID
                // val peerIdBytes = data.copyOfRange(1, data.size)
                val peerHash = data[1]
                val rssi = result.rssi.toByte()

                peerRSSI[peerHash] = rssi     // Store latest RSSI
                Log.d("Size: ", data.size.toString())
                Log.d("RSSI: ", rssi.toString())
                Log.d("peerHash: ", peerHash.toString())
            }
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Log.d(TAG, "Advertising started successfully")
            Toast.makeText(applicationContext, "Advertising started", Toast.LENGTH_LONG).show()
            val attendanceText: TextView = findViewById<TextView>(R.id.text_status)
            attendanceText.setText("Status: Present Marked")
            attendanceText.setTextColor(Color.GREEN)
            val advertiseButton: Button = findViewById(R.id.btn_mark_attendance)
            advertiseButton.setEnabled(false)
            advertiseButton.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY)
        // Stop advertising after 5 seconds (optional)
//            bluetoothLeAdvertiser?.let {
//                it.stopAdvertising(this)
//                Log.d(TAG, "Advertising stopped after one shot")
//            }
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e(TAG, "Advertising failed with error code: $errorCode")
            Toast.makeText(applicationContext, "Advertising failed: $errorCode", Toast.LENGTH_LONG).show()
        }
    }
}
