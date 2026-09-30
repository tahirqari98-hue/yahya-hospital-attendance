package com.yahyahospital.attendance

import com.google.mlkit.vision.face.Face
import kotlin.math.sqrt

object FaceSignature {
 fun fromFace(face:Face):FloatArray?{
  val points=face.allContours.flatMap{it.points}; if(points.size<100)return null
  val b=face.boundingBox;val w=b.width().toFloat().coerceAtLeast(1f);val h=b.height().toFloat().coerceAtLeast(1f)
  val out=FloatArray(points.size*2+2)
  points.forEachIndexed{i,p->out[i*2]=(p.x-b.left)/w;out[i*2+1]=(p.y-b.top)/h}
  out[out.size-2]=face.headEulerAngleY/45f;out[out.size-1]=face.headEulerAngleZ/45f
  return normalize(out)
 }
 private fun normalize(v:FloatArray):FloatArray{var cx=0f;var cy=0f;var n=0;for(i in v.indices step 2){if(i>=v.size-2)break;cx+=v[i];cy+=v[i+1];n++};cx/=n;cy/=n;val o=v.copyOf();var s=0f;for(i in 0 until o.size-2 step 2){o[i]-=cx;o[i+1]-=cy;s+=o[i]*o[i]+o[i+1]*o[i+1]};val scale=sqrt((s/n).coerceAtLeast(.000001f));for(i in 0 until o.size-2){o[i]/=scale};return o}
 fun encode(v:FloatArray)=v.joinToString(","){String.format(java.util.Locale.US,"%.6f",it)}
 fun decode(s:String):FloatArray?=try{s.split(",").map{it.toFloat()}.toFloatArray()}catch(_:Exception){null}
 fun distance(a:FloatArray,b:FloatArray):Float{val n=minOf(a.size,b.size);if(n<20)return Float.MAX_VALUE;var sum=0f;for(i in 0 until n){val d=a[i]-b[i];sum+=d*d};return sqrt(sum/n)}
}
