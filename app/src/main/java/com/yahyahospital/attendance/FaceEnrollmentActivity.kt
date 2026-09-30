package com.yahyahospital.attendance

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
import java.util.concurrent.Executors

class FaceEnrollmentActivity:AppCompatActivity(){
 private lateinit var message:TextView;private lateinit var preview:PreviewView;private val executor=Executors.newSingleThreadExecutor();private val samples=mutableListOf<FloatArray>();private var employeeId=""
 override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_face_enrollment);employeeId=intent.getStringExtra("employee_id").orEmpty();message=findViewById(R.id.enrollMessage);preview=findViewById(R.id.enrollPreview);if(employeeId.isBlank()||EmployeeStore.find(this,employeeId)==null){message.text="Employee not found.";return};message.text="Look straight at camera. Sample 1 of 3.";startCamera()}
 private fun startCamera(){val f=ProcessCameraProvider.getInstance(this);f.addListener({val p=f.get();val pu=Preview.Builder().build().also{it.surfaceProvider=preview.surfaceProvider};val o=FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE).setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL).build();val d=FaceDetection.getClient(o);val a=ImageAnalysis.Builder().setTargetResolution(Size(640,480)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();var last=0L;a.setAnalyzer(executor){proxy->val media=proxy.image;if(media==null){proxy.close();return@setAnalyzer};d.process(InputImage.fromMediaImage(media,proxy.imageInfo.rotationDegrees)).addOnSuccessListener{faces->if(faces.size==1&&System.currentTimeMillis()-last>1400){FaceSignature.fromFace(faces[0])?.let{s->samples.add(s);last=System.currentTimeMillis();runOnUiThread{if(samples.size>=3)finishEnrollment()else message.text="Sample saved. "+(samples.size+1)+" of 3 — slightly turn your head."}}}else if(faces.isEmpty())runOnUiThread{message.text="Place your face inside the frame."}else if(faces.size>1)runOnUiThread{message.text="Only one face should be visible."}}.addOnCompleteListener{proxy.close()}};p.unbindAll();p.bindToLifecycle(this,CameraSelector.DEFAULT_FRONT_CAMERA,pu,a)},ContextCompat.getMainExecutor(this))}
 private fun finishEnrollment(){val len=samples.minOfOrNull{it.size}?:return;val avg=FloatArray(len);samples.forEach{s->for(i in 0 until len)avg[i]+=s[i]};for(i in avg.indices)avg[i]/=samples.size;EmployeeStore.updateFace(this,employeeId,FaceSignature.encode(avg));message.text="Face registration complete.";message.postDelayed({finish()},1000)}
 override fun onDestroy(){super.onDestroy();executor.shutdown()}
}
