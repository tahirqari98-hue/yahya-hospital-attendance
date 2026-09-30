package com.yahyahospital.attendance

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Employee(val id:String,val name:String,val department:String,val designation:String,val phone:String,val pin:String,val faceSignature:String="")

object EmployeeStore {
 private const val PREFS="employees"; private const val KEY="list"
 fun all(c:Context):MutableList<Employee>{ val raw=c.getSharedPreferences(PREFS,0).getString(KEY,"[]")?:"[]"; return try{val a=JSONArray(raw); MutableList(a.length()){i->val o=a.getJSONObject(i); Employee(o.optString("id"),o.optString("name"),o.optString("department"),o.optString("designation"),o.optString("phone"),o.optString("pin"),o.optString("face"))}}catch(_:Exception){mutableListOf()} }
 fun find(c:Context,id:String)=all(c).firstOrNull{it.id.equals(id.trim(),true)}
 fun save(c:Context,e:Employee){val l=all(c);val i=l.indexOfFirst{it.id.equals(e.id,true)};if(i>=0)l[i]=e else l.add(e);write(c,l)}
 fun delete(c:Context,id:String){write(c,all(c).filterNot{it.id.equals(id,true)})}
 fun updateFace(c:Context,id:String,s:String){find(c,id)?.let{save(c,it.copy(faceSignature=s))}}
 private fun write(c:Context,l:List<Employee>){val a=JSONArray();l.forEach{e->a.put(JSONObject().apply{put("id",e.id);put("name",e.name);put("department",e.department);put("designation",e.designation);put("phone",e.phone);put("pin",e.pin);put("face",e.faceSignature)})};c.getSharedPreferences(PREFS,0).edit().putString(KEY,a.toString()).apply()}
}
