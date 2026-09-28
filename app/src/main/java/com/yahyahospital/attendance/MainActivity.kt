package com.yahyahospital.attendance

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.material.textfield.TextInputEditText
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    companion object {
        const val HOSPITAL_LAT = 33.99522
        const val HOSPITAL_LON = 72.92849
        const val DEFAULT_RADIUS_M = 100f
        const val DEMO_ID = "EMP001"
        const val DEMO_PIN = "1234"
    }
    private val locationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private lateinit var status: android.widget.TextView
    private var pendingAction = "CHECK_IN"
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true && result[Manifest.permission.CAMERA] == true) beginLocationCheck()
        else status.text = "Camera and precise location permissions are required."
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        findViewById<com.google.android.material.button.MaterialButton>(R.id.checkIn).setOnClickListener { startAttendance("CHECK_IN") }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.checkOut).setOnClickListener { startAttendance("CHECK_OUT") }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.dashboard).setOnClickListener { openDashboard() }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.admin).setOnClickListener { showAdminDialog() }
    }
    private fun openDashboard() {
        val id = findViewById<TextInputEditText>(R.id.employeeId).text?.toString()?.trim().orEmpty()
        val pin = findViewById<TextInputEditText>(R.id.pin).text?.toString()?.trim().orEmpty()
        if (!id.equals(DEMO_ID, true) || pin != DEMO_PIN) {
            status.text = "Invalid credentials. Demo: EMP001 / 1234"
            return
        }
        startActivity(Intent(this, DashboardActivity::class.java).putExtra("employee_id", id))
    }

    private fun startAttendance(action: String) {
        val id = findViewById<TextInputEditText>(R.id.employeeId).text?.toString()?.trim().orEmpty()
        val pin = findViewById<TextInputEditText>(R.id.pin).text?.toString()?.trim().orEmpty()
        if (id.isBlank() || pin.isBlank()) { status.text = "Enter Employee ID and PIN."; return }
        if (!id.equals(DEMO_ID, true) || pin != DEMO_PIN) { status.text = "Invalid credentials. Demo: EMP001 / 1234"; return }
        pendingAction = action
        val missing = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) missing += Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) missing += Manifest.permission.CAMERA
        if (missing.isNotEmpty()) permissions.launch(missing.toTypedArray()) else beginLocationCheck()
    }
    private fun beginLocationCheck() {
        status.text = "Checking hospital location..."
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        locationClient.lastLocation.addOnSuccessListener { location ->
            if (location == null) { status.text = "GPS location unavailable. Enable Location and retry."; return@addOnSuccessListener }
            val radius = getSharedPreferences("settings", MODE_PRIVATE).getFloat("radius", DEFAULT_RADIUS_M)
            val distance = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, HOSPITAL_LAT, HOSPITAL_LON, distance)
            if (distance[0] > radius) {
                status.text = "Outside hospital geofence: " + distance[0].roundToInt() + " m. Allowed: " + radius.roundToInt() + " m."
                return@addOnSuccessListener
            }
            startActivity(Intent(this, FaceVerificationActivity::class.java).apply {
                putExtra("action", pendingAction); putExtra("employee_id", DEMO_ID); putExtra("distance_m", distance[0])
            })
        }.addOnFailureListener { status.text = "GPS error: " + it.message }
    }
    private fun showAdminDialog() {
        val input = TextInputEditText(this).apply { hint = "Admin PIN" }
        AlertDialog.Builder(this).setTitle("Admin Access").setView(input)
            .setPositiveButton("Open") { _, _ -> if (input.text?.toString() == "9999") showSettings() else Toast.makeText(this, "Invalid admin PIN", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("Cancel", null).show()
    }
    private fun showSettings() {
        val current = getSharedPreferences("settings", MODE_PRIVATE).getFloat("radius", DEFAULT_RADIUS_M)
        val input = TextInputEditText(this).apply { hint = "Geofence radius (metres)"; setText(current.toInt().toString()) }
        AlertDialog.Builder(this).setTitle("Hospital Settings")
            .setMessage("Hospital coordinates: 33.99522, 72.92849\nDefault radius: 100 m")
            .setView(input).setPositiveButton("Save") { _, _ ->
                input.text?.toString()?.toFloatOrNull()?.takeIf { it in 20f..1000f }?.let {
                    getSharedPreferences("settings", MODE_PRIVATE).edit().putFloat("radius", it).apply()
                    Toast.makeText(this, "Geofence saved: " + it.toInt() + " m", Toast.LENGTH_SHORT).show()
                }
            }.setNegativeButton("Cancel", null).show()
    }
}
