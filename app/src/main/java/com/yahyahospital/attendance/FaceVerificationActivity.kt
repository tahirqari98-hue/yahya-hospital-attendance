package com.yahyahospital.attendance

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class FaceVerificationActivity : AppCompatActivity() {
    private lateinit var preview: PreviewView
    private lateinit var message: TextView
    private val executor = Executors.newSingleThreadExecutor()
    private var recorded = false
    private var action = "CHECK_IN"
    private var employeeId = "EMP001"
    private var distance = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_verification)
        preview = findViewById(R.id.preview); message = findViewById(R.id.faceMessage)
        action = intent.getStringExtra("action") ?: "CHECK_IN"
        employeeId = intent.getStringExtra("employee_id") ?: "EMP001"
        distance = intent.getFloatExtra("distance_m", 0f)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera()
        else message.text = "Camera permission not granted."
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            val previewUseCase = Preview.Builder().build().also { it.surfaceProvider = preview.surfaceProvider }
            val detector = FaceDetection.getClient(FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).build())
            val analysis = ImageAnalysis.Builder().setTargetResolution(Size(640,480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            analysis.setAnalyzer(executor) { proxy ->
                val media = proxy.image
                if (media == null || recorded) { proxy.close(); return@setAnalyzer }
                detector.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
                    .addOnSuccessListener { faces ->
                        runOnUiThread {
                            when {
                                faces.size == 1 && !recorded -> { recorded = true; recordAttendance() }
                                faces.isEmpty() -> message.text = "Position your face in the camera."
                                else -> message.text = "Only one person should be visible."
                            }
                        }
                    }.addOnCompleteListener { proxy.close() }
            }
            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, previewUseCase, analysis)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun recordAttendance() {
        val prefs = getSharedPreferences("attendance", MODE_PRIVATE)
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val record = time + "|" + employeeId + "|" + action + "|" + distance.toInt() + "m"
        val old = prefs.getString("records", "").orEmpty()
        prefs.edit().putString("records", if (old.isBlank()) record else old + "\n" + record).apply()
        message.text = if (action == "CHECK_IN") "Check-in recorded successfully." else "Check-out recorded successfully."
        message.postDelayed({ finish() }, 1200)
    }
    override fun onDestroy() { super.onDestroy(); executor.shutdown() }
}
