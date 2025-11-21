package com.example.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.wiseyoung.app.MainActivity

/**
 * FCM 메시지를 받는 서비스
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val TAG = "FCMService"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "새 FCM 토큰: $token")
        // 토큰이 갱신되면 서버에 저장
        FcmTokenService.getAndSaveToken()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM 메시지 수신: ${remoteMessage.from}")

        // 알림 데이터 확인
        remoteMessage.notification?.let {
            Log.d(TAG, "알림 제목: ${it.title}")
            Log.d(TAG, "알림 내용: ${it.body}")
            showNotification(it.title ?: "알림", it.body ?: "")
        }

        // 데이터 페이로드 확인
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "데이터 페이로드: ${remoteMessage.data}")
        }
    }

    /**
     * 알림 표시
     */
    private fun showNotification(title: String, message: String) {
        val channelId = "wiseyoung_notification_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android O 이상에서는 알림 채널 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "WiseYoung 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "WiseYoung 앱 알림 채널"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 알림 클릭 시 MainActivity로 이동
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: 앱 아이콘으로 변경
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

