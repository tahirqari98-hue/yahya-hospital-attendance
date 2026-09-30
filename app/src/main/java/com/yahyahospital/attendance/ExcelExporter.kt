package com.yahyahospital.attendance

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExporter {
    fun create(context: Context, mode: String): File? {
        val raw = context.getSharedPreferences("attendance", 0).getString("records", "").orEmpty()
        if (raw.isBlank()) return null
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val month = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        val rows = raw.lines().filter { it.isNotBlank() }.mapNotNull { line ->
            val p = line.split("|")
            if (p.size >= 4) p else null
        }.filter { p -> when (mode) {
            "daily" -> p[0].startsWith(today)
            "monthly" -> p[0].startsWith(month)
            else -> true
        }}
        if (rows.isEmpty()) return null
        val file = File(context.cacheDir, "Yahya_Attendance_${mode}_$today.csv")
        file.writeText(buildString {
            appendLine("Date/Time,Employee ID,Employee Name,Action,GPS Distance,Face Score")
            rows.forEach { p -> appendLine(p.take(6).joinToString(",") { csv(it) }) }
        })
        return file
    }
    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share attendance Excel/CSV"))
    }
    private fun csv(s: String): String = "\"" + s.replace("\"", "\"\"") + "\""
}