package com.yahyahospital.attendance

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class DashboardActivity : AppCompatActivity() {
    private lateinit var employeeId: String
    private lateinit var list: LinearLayout
    private val prefs by lazy { getSharedPreferences("attendance", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        employeeId = intent.getStringExtra("employee_id") ?: "EMP001"
        list = findViewById(R.id.dashboardList)
        findViewById<Button>(R.id.applyLeave).setOnClickListener { applyLeave() }
        findViewById<Button>(R.id.applyShortLeave).setOnClickListener { applyShortLeave() }
        findViewById<Button>(R.id.refreshDashboard).setOnClickListener { render() }
        findViewById<Button>(R.id.adminRequests).setOnClickListener { adminRequests() }
        render()
    }

    private fun render() {
        list.removeAllViews()
        val records = prefs.getString("records", "").orEmpty()
        addText("Employee: $employeeId")
        addText("Attendance records: " + if (records.isBlank()) 0 else records.lines().size)
        addText("Pending requests: " + pending().size)
        addText("Recent activity")
        if (records.isBlank()) addText("No attendance recorded yet.")
        else records.lines().takeLast(8).reversed().forEach { addText(it) }
        addText("Leave / Short Leave Requests")
        val req = prefs.getString("leave_requests", "").orEmpty()
        if (req.isBlank()) addText("No leave requests.")
        else req.lines().filter { it.contains("|$employeeId|") }.takeLast(10).reversed()
            .forEach { addText(it.replace("|", "  •  ")) }
    }

    private fun applyLeave() {
        val types = arrayOf("Casual Leave", "Sick Leave", "Annual Leave", "Other")
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 8, 20, 0) }
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@DashboardActivity, android.R.layout.simple_spinner_dropdown_item, types)
        }
        val reason = EditText(this).apply { hint = "Reason (optional)" }
        box.addView(spinner); box.addView(reason)
        AlertDialog.Builder(this).setTitle("Apply Leave").setView(box)
            .setPositiveButton("Select Date") { _, _ ->
                chooseDate { date -> saveRequest("LEAVE", types[spinner.selectedItemPosition], date, "", reason.text.toString()) }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun applyShortLeave() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 8, 20, 0) }
        val hours = EditText(this).apply { hint = "Duration in hours (e.g. 2)"; inputType = 2 }
        val reason = EditText(this).apply { hint = "Reason" }
        box.addView(hours); box.addView(reason)
        AlertDialog.Builder(this).setTitle("Apply Short Leave").setView(box)
            .setPositiveButton("Select Date") { _, _ ->
                chooseDate { date -> saveRequest("SHORT_LEAVE", "Short Leave", date, hours.text.toString(), reason.text.toString()) }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun chooseDate(onDate: (String) -> Unit) {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            onDate(String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d))
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveRequest(kind: String, type: String, date: String, duration: String, reason: String) {
        val old = prefs.getString("leave_requests", "").orEmpty()
        val row = System.currentTimeMillis().toString() + "|" + employeeId + "|" + kind + "|" + type + "|" + date + "|" + duration + "|" + reason + "|PENDING"
        prefs.edit().putString("leave_requests", if (old.isBlank()) row else old + "\n" + row).apply()
        Toast.makeText(this, "Request submitted for admin approval.", Toast.LENGTH_LONG).show()
        render()
    }

    private fun pending(): List<String> =
        prefs.getString("leave_requests", "").orEmpty().lines().filter { it.isNotBlank() && it.endsWith("|PENDING") }

    private fun adminRequests() {
        val pin = EditText(this).apply { hint = "Admin PIN"; inputType = 2 }
        AlertDialog.Builder(this).setTitle("Admin Access").setView(pin)
            .setPositiveButton("Open") { _, _ ->
                if (pin.text.toString() == "9999") showRequests()
                else Toast.makeText(this, "Invalid admin PIN", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun showRequests() {
        val rows = pending()
        if (rows.isEmpty()) {
            AlertDialog.Builder(this).setTitle("Leave Requests").setMessage("No pending requests.")
                .setPositiveButton("OK", null).show()
            return
        }
        val labels = rows.map {
            val p = it.split("|")
            val duration = if (p.size > 5 && p[5].isNotBlank()) p[5] + "h" else ""
            p[1] + " • " + p[3] + " • " + p[4] + " • " + duration + "\n" + p[6]
        }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Pending Requests").setItems(labels) { _, which ->
            decide(rows[which])
        }.show()
    }

    private fun decide(row: String) {
        val parts = row.split("|")
        AlertDialog.Builder(this).setTitle("Request Decision")
            .setMessage(parts[1] + "\n" + parts[3] + "\n" + parts[4] + "\n" + parts[6])
            .setPositiveButton("Approve") { _, _ -> replaceStatus(parts[0], "APPROVED") }
            .setNegativeButton("Reject") { _, _ -> replaceStatus(parts[0], "REJECTED") }
            .setNeutralButton("Cancel", null).show()
    }

    private fun replaceStatus(id: String, status: String) {
        val rows = prefs.getString("leave_requests", "").orEmpty().lines().map {
            if (it.startsWith(id + "|")) it.substringBeforeLast("|") + "|" + status else it
        }.filter { it.isNotBlank() }
        prefs.edit().putString("leave_requests", rows.joinToString("\n")).apply()
        Toast.makeText(this, "Request $status.", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun addText(value: String) {
        list.addView(TextView(this).apply { text = value; textSize = 15f; setPadding(8, 10, 8, 10) })
    }
}
