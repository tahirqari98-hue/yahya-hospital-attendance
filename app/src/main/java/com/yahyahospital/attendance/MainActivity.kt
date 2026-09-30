package com.yahyahospital.attendance

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.material.textfield.TextInputEditText
import kotlin.math.roundToInt

class MainActivity:AppCompatActivity(){
 companion object{const val HOSPITAL_LAT=33.99522;const val HOSPITAL_LON=72.92849;const val DEFAULT_RADIUS_M=100f}
 private val locationClient by lazy{LocationServices.getFusedLocationProviderClient(this)}
 private lateinit var status:android.widget.TextView;private var pendingAction="CHECK_IN";private var pendingEmployee=""
 private val permissions=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){r->if(r[Manifest.permission.ACCESS_FINE_LOCATION]==true&&r[Manifest.permission.CAMERA]==true)beginLocationCheck()else status.text="Camera and precise location permissions are required."}
 override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_main);status=findViewById(R.id.status);findViewById<com.google.android.material.button.MaterialButton>(R.id.checkIn).setOnClickListener{startAttendance("CHECK_IN")};findViewById<com.google.android.material.button.MaterialButton>(R.id.checkOut).setOnClickListener{startAttendance("CHECK_OUT")};findViewById<com.google.android.material.button.MaterialButton>(R.id.dashboard).setOnClickListener{openDashboard()};findViewById<com.google.android.material.button.MaterialButton>(R.id.admin).setOnClickListener{showAdminDialog()}}
 private fun openDashboard(){val id=findViewById<TextInputEditText>(R.id.employeeId).text?.toString()?.trim().orEmpty();val pin=findViewById<TextInputEditText>(R.id.pin).text?.toString()?.trim().orEmpty();val e=EmployeeStore.find(this,id);if(e==null||e.pin!=pin){status.text="Invalid employee credentials.";return};startActivity(Intent(this,DashboardActivity::class.java).putExtra("employee_id",e.id))}
 private fun startAttendance(action:String){val id=findViewById<TextInputEditText>(R.id.employeeId).text?.toString()?.trim().orEmpty();val pin=findViewById<TextInputEditText>(R.id.pin).text?.toString()?.trim().orEmpty();val e=EmployeeStore.find(this,id);if(e==null||e.pin!=pin){status.text="Invalid Employee ID or PIN.";return};if(e.faceSignature.isBlank()){status.text="Face is not enrolled. Ask admin to register your face.";return};pendingAction=action;pendingEmployee=e.id;val missing=mutableListOf<String>();if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)missing+=Manifest.permission.ACCESS_FINE_LOCATION;if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)missing+=Manifest.permission.CAMERA;if(missing.isNotEmpty())permissions.launch(missing.toTypedArray())else beginLocationCheck()}
 private fun beginLocationCheck(){status.text="Checking hospital location...";if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)return;locationClient.lastLocation.addOnSuccessListener{loc->if(loc==null){status.text="GPS unavailable. Enable Location and retry.";return@addOnSuccessListener};val radius=getSharedPreferences("settings",0).getFloat("radius",DEFAULT_RADIUS_M);val d=FloatArray(1);android.location.Location.distanceBetween(loc.latitude,loc.longitude,HOSPITAL_LAT,HOSPITAL_LON,d);if(d[0]>radius){status.text="Outside hospital geofence: "+d[0].roundToInt()+" m. Allowed: "+radius.roundToInt()+" m.";return@addOnSuccessListener};startActivity(Intent(this,FaceVerificationActivity::class.java).apply{putExtra("action",pendingAction);putExtra("employee_id",pendingEmployee);putExtra("distance_m",d[0])})}.addOnFailureListener{status.text="GPS error: "+it.message}}
 private fun showAdminDialog(){val input=TextInputEditText(this).apply{hint="Admin PIN"};AlertDialog.Builder(this).setTitle("Admin Access").setView(input).setPositiveButton("Open"){_,_->if(input.text?.toString()=="9999")startActivity(Intent(this,EmployeeAdminActivity::class.java))else Toast.makeText(this,"Invalid admin PIN",Toast.LENGTH_SHORT).show()}.setNegativeButton("Cancel",null).show()}
}