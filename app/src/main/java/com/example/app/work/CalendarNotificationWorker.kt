package com.example.app.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.app.data.*
import com.google.firebase.auth.FirebaseAuth
import com.wiseyoung.app.CalendarActivity
import com.wiseyoung.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * 캘린더 알림 발송 Worker
 * WorkManager에서 스케줄된 알림을 발송
 */
class CalendarNotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    
    private val notificationRepository = NotificationRepository(context)
    private val calendarRepository = CalendarRepository(context)
    private val auth = FirebaseAuth.getInstance()
    
    override fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: "일정 알림"
        val body = inputData.getString(KEY_BODY) ?: "일정이 곧 마감됩니다."
        val eventId = inputData.getLong(KEY_EVENT_ID, -1)
        
        sendNotification(title, body, eventId)
        
        // 알림함에 저장
        saveNotificationToInbox(title, body, eventId)
        
        return Result.success()
    }
    
    private fun sendNotification(title: String, body: String, eventId: Long) {
        val notificationManager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Notification Channel 생성 (Android 8.0 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // PendingIntent 생성 (알림 클릭 시 CalendarActivity로 이동)
        val intent = Intent(applicationContext, CalendarActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            eventId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Notification 생성
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.wy_logo) // 앱 아이콘 사용
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        // 알림 발송
        notificationManager.notify(eventId.toInt(), notification)
    }
    
    /**
     * 알림을 알림함에 저장
     */
    private fun saveNotificationToInbox(title: String, body: String, eventId: Long) {
        val currentUser = auth.currentUser ?: return
        
        runBlocking {
            try {
                // 캘린더 이벤트 정보 가져오기
                val calendarEvent = calendarRepository.getEventById(eventId)
                
                // 알림 생성
                val notification = Notification(
                    userId = currentUser.uid,
                    title = title,
                    body = body,
                    notificationType = NotificationType.CALENDAR,
                    eventId = eventId,
                    eventType = calendarEvent?.eventType,
                    organization = calendarEvent?.organization,
                    policyId = calendarEvent?.policyId,
                    housingId = calendarEvent?.housingId,
                    isRead = false
                )
                
                notificationRepository.insertNotification(notification)
                android.util.Log.d("CalendarNotificationWorker", "알림함에 저장 완료: $title")
            } catch (e: Exception) {
                android.util.Log.e("CalendarNotificationWorker", "알림함 저장 실패: ${e.message}", e)
            }
        }
    }
    
    companion object {
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
        const val KEY_EVENT_ID = "event_id"
        const val CHANNEL_ID = "calendar_notifications"
        const val CHANNEL_NAME = "캘린더 알림"
        const val CHANNEL_DESCRIPTION = "정책 및 임대주택 마감일 알림"
    }
}

