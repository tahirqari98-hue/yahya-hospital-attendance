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
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class FaceVerificationActivity:AppCompatActivity(){
 private lateinit var preview:PreviewView;private lateinit var message:TextView;private val executor=Executors.newSingleThreadExecutor();private var done=false;private var action="CHECK_IN";private var employeeId="";private var distance=0f
 override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_face_verification);preview=findViewById(R.id.preview);message=findViewById(R.id.faceMessage);action=intent.getStringExtra("action")?:"CHECK_IN";employeeId=intent.getStringExtra("employee_id").orEmpty();distance=intent.getFloatExtra("distance_m",0f);if(EmployeeStore.find(this,employeeId)==null){message.text="Employee not found.";return};startCamera()}
 private fun startCamera(){val f=ProcessCameraProvider.getInstance(this);f.addListener({val p=f.get();val pu=Preview.Builder().build().also{it.surfaceProvider=preview.surfaceProvider};val o=FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE).setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL).build();val d=FaceDetection.getClient(o);val a=ImageAnalysis.Builder().setTargetResolution(Size(640,480)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();a.setAnalyzer(executor){proxy->val media=proxy.image;if(media==null){proxy.close();return@setAnalyzer};d.process(InputImage.fromMediaImage(media,proxy.imageInfo.rotationDegrees)).addOnSuccessListener{faces->if(done)return@addOnSuccessListener;runOnUiThread{when{faces.size!=1->message.text=if(faces.isEmpty())"Position your face in the camera." else "Only one person should be visible.";else->{val sig=FaceSignature.fromFace(faces[0]);val stored=EmployeeStore.find(this,employeeId)?.faceSignature.orEmpty();val ref=FaceSignature.decode(stored);if(sig==null||ref==null){message.text="Unable to read face. Try again."}else{val score=FaceSignature.distance(sig,ref);if(score<0.55f){done=true;recordAttendance(score)}else message.text="Face not recognized. Please look directly at the camera."}}}}}.addOnCompleteListener{proxy.close()}};p.unbindAll();p.bindToLifecycle(this,CameraSelector.DEFAULT_FRONT_CAMERA,pu,a)},ContextCompat.getMainExecutor(this))}
 private fun recordAttendance(score:Float){val prefs=getSharedPreferences("attendance",0);val time=SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault()).format(Date());val e=EmployeeStore.find(this,employeeId);val row=listOf(time,employeeId,e?.name.orEmpty(),action,distance.toInt().toString()+"m",String.format(Locale.US,"%.3f",score)).joinToString("|");val old=prefs.getString("records","").orEmpty();prefs.edit().putString("records",if(old.isBlank())row else old+"\n"+row).apply();message.text=if(action=="CHECK_IN")"Face verified. Check-in recorded." else "Face verified. Check-out recorded.";message.postDelayed({finish()},1400)}
 override fun onDestroy(){super.onDestroy();executor.shutdown()}
}