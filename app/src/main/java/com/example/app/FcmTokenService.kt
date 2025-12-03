package com.example.app

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject

object FcmTokenService {
    private const val TAG = "FcmTokenService"
    
    /**
     * FCM 토큰을 가져와서 서버에 저장
     */
    fun getAndSaveToken() {
        getFcmToken { token ->
            // 토큰이 있으면 서버에 저장
            if (token != null) {
                val auth = FirebaseAuth.getInstance()
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    currentUser.getIdToken(true)
                        .addOnSuccessListener { tokenResult ->
                            val idToken = tokenResult.token ?: return@addOnSuccessListener
                            saveTokenToServer(idToken, token)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "ID Token 가져오기 실패", e)
                        }
                }
            }
        }
    }
    
    /**
     * FCM 토큰을 가져오기만 함 (서버 저장 없이)
     * @param callback 토큰을 받을 콜백 함수
     */
    fun getFcmToken(callback: (String?) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        
        if (currentUser == null) {
            Log.w(TAG, "사용자가 로그인하지 않았습니다. FCM 토큰을 가져올 수 없습니다.")
            callback(null)
            return
        }
        
        // FCM 토큰 가져오기
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e(TAG, "FCM 토큰 가져오기 실패", task.exception)
                callback(null)
                return@addOnCompleteListener
            }
            
            val fcmToken = task.result
            Log.d(TAG, "FCM 토큰: $fcmToken")
            Log.i(TAG, "═══════════════════════════════════════════════════════")
            Log.i(TAG, "FCM 등록 토큰 (Firebase 콘솔에 복사하세요):")
            Log.i(TAG, fcmToken)
            Log.i(TAG, "═══════════════════════════════════════════════════════")
            callback(fcmToken)
        }
    }
    
    /**
     * 기존 getAndSaveToken 메서드 (하위 호환성 유지)
     */
    @Deprecated("getAndSaveToken()을 사용하세요", ReplaceWith("getAndSaveToken()"))
    fun getAndSaveTokenOld() {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        
        if (currentUser == null) {
            Log.w(TAG, "사용자가 로그인하지 않았습니다. FCM 토큰 저장을 건너뜁니다.")
            return
        }
        
        // FCM 토큰 가져오기
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e(TAG, "FCM 토큰 가져오기 실패", task.exception)
                return@addOnCompleteListener
            }
            
            val fcmToken = task.result
            // FCM 토큰을 명확하게 로그에 출력 (Firebase 콘솔 테스트용)
            Log.i(TAG, "═══════════════════════════════════════════════════════")
            Log.i(TAG, "FCM 등록 토큰 (Firebase 콘솔에 복사하세요):")
            Log.i(TAG, fcmToken)
            Log.i(TAG, "═══════════════════════════════════════════════════════")
            
            // ID Token 가져오기
            currentUser.getIdToken(true)
                .addOnSuccessListener { tokenResult ->
                    val idToken = tokenResult.token ?: return@addOnSuccessListener
                    // 서버에 FCM 토큰 저장
                    saveTokenToServer(idToken, fcmToken)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "ID Token 가져오기 실패", e)
                }
        }
    }
    
    /**
     * 서버에 FCM 토큰 저장
     */
    private fun saveTokenToServer(idToken: String, fcmToken: String) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        
        val json = JSONObject().apply {
            put("idToken", idToken)
            put("fcmToken", fcmToken)
        }
        
        val body = RequestBody.create(
            "application/json".toMediaType(),
            json.toString()
        )
        
        val request = Request.Builder()
            .url(Config.getUrl(Config.Api.PUSH_TOKEN))
            .post(body)
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                Log.e(TAG, "FCM 토큰 저장 네트워크 오류", e)
            }
            
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d(TAG, "FCM 토큰 저장 성공")
                } else {
                    val errorBody = response.body?.string() ?: "응답 없음"
                    Log.e(TAG, "FCM 토큰 저장 실패: ${response.code}, 응답: $errorBody")
                }
            }
        })
    }
}
