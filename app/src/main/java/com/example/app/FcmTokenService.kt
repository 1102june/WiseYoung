package com.example.app

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.example.app.Config
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException

/**
 * FCM 토큰을 관리하고 서버에 저장하는 서비스
 */
object FcmTokenService {
    private const val TAG = "FcmTokenService"
    private val client = OkHttpClient()

    /**
     * FCM 토큰을 가져와서 서버에 저장
     */
    fun getAndSaveToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "FCM 토큰 가져오기 실패", task.exception)
                return@addOnCompleteListener
            }

            // FCM 토큰 가져오기 성공
            val token = task.result
            Log.d(TAG, "FCM 토큰: $token")
            
            // 로그인된 사용자가 있으면 서버에 저장
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                saveTokenToServer(token)
            } else {
                Log.d(TAG, "로그인되지 않은 사용자 - 토큰 저장 건너뜀")
            }
        }
    }

    /**
     * FCM 토큰을 서버에 저장
     */
    private fun saveTokenToServer(fcmToken: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        
        currentUser.getIdToken(true).addOnSuccessListener { tokenResult ->
            val idToken = tokenResult.token ?: return@addOnSuccessListener
            
            val json = """
                {
                    "idToken": "$idToken",
                    "pushToken": "$fcmToken"
                }
            """.trimIndent()
            
            val requestBody = RequestBody.create("application/json".toMediaType(), json)
            val request = Request.Builder()
                .url(Config.getUrl(Config.Api.PUSH_TOKEN))
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()
            
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "FCM 토큰 서버 저장 실패: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        Log.d(TAG, "FCM 토큰 서버 저장 성공")
                    } else {
                        Log.e(TAG, "FCM 토큰 서버 저장 실패: ${response.code} - ${response.message}")
                    }
                    response.close()
                }
            })
        }.addOnFailureListener {
            Log.e(TAG, "ID 토큰 발급 실패: ${it.message}")
        }
    }
}

