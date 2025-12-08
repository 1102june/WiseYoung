package com.example.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.app.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.wiseyoung.app.MainActivity
import com.wiseyoung.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val repository by lazy { NotificationRepository(applicationContext) }
    private val auth = FirebaseAuth.getInstance()

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // 토큰이 갱신되면 서버로 전송
        FcmTokenService.getAndSaveToken()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // 데이터 메시지 처리
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleNow(remoteMessage.data)
        }

        // 알림 메시지 처리
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            val title = it.title ?: "알림"
            val body = it.body ?: ""
            
            // 상단 알림 표시
            sendNotification(title, body, remoteMessage.data)
            
            // 알림함에 로그 저장
            saveNotificationToInbox(title, body, remoteMessage.data)
        }
    }

    private fun handleNow(data: Map<String, String>) {
        val title = data["title"] ?: "알림"
        val body = data["body"] ?: ""
        if (body.isNotEmpty()) {
            sendNotification(title, body, data)
            saveNotificationToInbox(title, body, data)
        }
    }

    private fun sendNotification(title: String, messageBody: String, data: Map<String, String> = emptyMap()) {
        // 알림 클릭 시 이동할 화면 설정 (앱 로고/메인으로 이동)
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        
        // 필요한 경우 데이터 전달
        for ((key, value) in data) {
            intent.putExtra(key, value)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.wy_logo)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 오레오 이상 버전에서는 채널이 필요합니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "기본 알림 채널",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
    
    /**
     * 알림을 내부 DB 알림함에 저장 (로그 기능)
     */
    private fun saveNotificationToInbox(title: String, body: String, data: Map<String, String>) {
        val currentUser = auth.currentUser ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 데이터에서 이벤트 정보 추출 (서버에서 보내주는 키값에 맞춤)
                val eventId = data["eventId"]?.toLongOrNull()
                val eventTypeStr = data["eventType"]
                val eventType = when (eventTypeStr?.lowercase()) {
                    "policy" -> EventType.POLICY
                    "housing" -> EventType.HOUSING
                    else -> null
                }
                val organization = data["organization"]
                val policyId = data["policyId"]
                val housingId = data["housingId"]
                
                // Notification 엔티티 생성
                val notification = Notification(
                    userId = currentUser.uid,
                    title = title,
                    body = body,
                    notificationType = NotificationType.FCM,
                    eventId = eventId,
                    eventType = eventType,
                    organization = organization,
                    policyId = policyId,
                    housingId = housingId,
                    isRead = false
                    // createdAt은 기본값인 현재 시간으로 자동 설정됨
                )
                
                repository.insertNotification(notification)
                Log.d(TAG, "알림함에 저장 완료: $title")
            } catch (e: Exception) {
                Log.e(TAG, "알림함 저장 실패: ${e.message}", e)
            }
        }
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}
