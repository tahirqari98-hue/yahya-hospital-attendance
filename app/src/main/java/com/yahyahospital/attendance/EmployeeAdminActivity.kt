package com.yahyahospital.attendance

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class EmployeeAdminActivity : AppCompatActivity() {
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employee_admin)
        list = findViewById(R.id.employeeList)
        findViewById<Button>(R.id.addEmployee).setOnClickListener { showEditor(null) }
        findViewById<Button>(R.id.exportAll).setOnClickListener { exportDialog() }
        render()
    }

    private fun render() {
        list.removeAllViews()
        val employees = EmployeeStore.all(this)
        if (employees.isEmpty()) {
            addText("No employees registered.")
            return
        }

        employees.forEach { employee ->
            val row = LinearLayout(this)
            row.orientation = LinearLayout.VERTICAL
            row.setPadding(8, 12, 8, 12)

            val info = TextView(this)
            info.text = employee.id + " — " + employee.name +
                    "\n" + employee.designation + " | " + employee.department +
                    "\nFace: " + if (employee.faceSignature.isBlank()) "Not enrolled" else "Enrolled"
            info.textSize = 16f
            row.addView(info)

            val buttons = LinearLayout(this)

            val edit = Button(this)
            edit.text = "Edit"
            edit.setOnClickListener { showEditor(employee) }
            buttons.addView(edit)

            val face = Button(this)
            face.text = "Register Face"
            face.setOnClickListener {
                startActivity(
                    Intent(this, FaceEnrollmentActivity::class.java)
                        .putExtra("employee_id", employee.id)
                )
            }
            buttons.addView(face)

            val delete = Button(this)
            delete.text = "Delete"
            delete.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Delete " + employee.name + "?")
                    .setMessage("Attendance records will remain.")
                    .setPositiveButton("Delete") { _, _ ->
                        EmployeeStore.delete(this, employee.id)
                        render()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            buttons.addView(delete)

            row.addView(buttons)
            list.addView(row)
        }
    }

    private fun showEditor(existing: Employee?) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(20, 8, 20, 0)

        fun field(hint: String, value: String = ""): EditText {
            return EditText(this).apply {
                this.hint = hint
                setText(value)
            }
        }

        val id = field("Employee ID", existing?.id.orEmpty())
        val name = field("Full name", existing?.name.orEmpty())
        val department = field("Department", existing?.department.orEmpty())
        val designation = field("Designation", existing?.designation.orEmpty())
        val phone = field("Phone", existing?.phone.orEmpty())
        val pin = field("Employee PIN", existing?.pin.orEmpty())
        pin.inputType = 2

        if (existing != null) id.isEnabled = false

        listOf(id, name, department, designation, phone, pin).forEach {
            box.addView(it)
        }

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Add Employee" else "Edit Employee")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                val employeeId = id.text.toString().trim()
                val employeeName = name.text.toString().trim()
                if (employeeId.isBlank() || employeeName.isBlank()) {
                    Toast.makeText(
                        this,
                        "Employee ID and name are required.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    EmployeeStore.save(
                        this,
                        Employee(
                            employeeId,
                            employeeName,
                            department.text.toString().trim(),
                            designation.text.toString().trim(),
                            phone.text.toString().trim(),
                            pin.text.toString().trim(),
                            existing?.faceSignature.orEmpty()
                        )
                    )
                    render()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exportDialog() {
        val items = arrayOf(
            "Today's attendance",
            "This month's attendance",
            "All attendance"
        )
        AlertDialog.Builder(this)
            .setTitle("Excel Attendance Report")
            .setItems(items) { _, which ->
                val mode = when (which) {
                    0 -> "daily"
                    1 -> "monthly"
                    else -> "all"
                }
                val file = ExcelExporter.create(this, mode)
                if (file != null) {
                    ExcelExporter.share(this, file)
                } else {
                    Toast.makeText(
                        this,
                        "No attendance records to export.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }

    private fun addText(value: String) {
        list.addView(
            TextView(this).apply {
                text = value
                textSize = 16f
                setPadding(8, 20, 8, 20)
            }
        )
    }
}
