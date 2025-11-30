package com.example.app.work

import android.content.Context
import androidx.work.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * 캘린더 알림 스케줄러
 * WorkManager를 사용하여 알림을 스케줄링
 */
object CalendarNotificationScheduler {
    
    private const val WORK_NAME_PREFIX = "calendar_notification_"
    
    /**
     * 일정 알림 스케줄링
     * @param context Context
     * @param eventId 일정 ID
     * @param title 알림 제목
     * @param body 알림 내용
     * @param notificationDate 알림을 발송할 날짜와 시간
     */
    fun scheduleNotification(
        context: Context,
        eventId: Long,
        title: String,
        body: String,
        notificationDate: LocalDateTime
    ) {
        val workManager = WorkManager.getInstance(context)
        
        // 알림 시간까지의 지연 시간 계산
        val now = LocalDateTime.now()
        val delay = java.time.Duration.between(now, notificationDate)
        
        if (delay.isNegative || delay.isZero) {
            // 이미 지난 시간이면 스케줄링하지 않음
            return
        }
        
        // WorkRequest 생성
        val inputData = Data.Builder()
            .putString(CalendarNotificationWorker.KEY_TITLE, title)
            .putString(CalendarNotificationWorker.KEY_BODY, body)
            .putLong(CalendarNotificationWorker.KEY_EVENT_ID, eventId)
            .build()
        
        val workRequest = OneTimeWorkRequestBuilder<CalendarNotificationWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag("$WORK_NAME_PREFIX$eventId")
            .build()
        
        // WorkManager에 등록
        workManager.enqueue(workRequest)
    }
    
    /**
     * 여러 알림 스케줄링 (7일 전, 1일 전, 커스텀 등)
     * @param context Context
     * @param eventId 일정 ID
     * @param title 알림 제목
     * @param body 알림 내용
     * @param endDate 마감일
     * @param sevenDaysBefore 7일 전 알림 여부
     * @param sevenDaysTime 7일 전 알림 시간 (HH:mm)
     * @param oneDayBefore 1일 전 알림 여부
     * @param oneDayTime 1일 전 알림 시간 (HH:mm)
     * @param customDays 커스텀 일수
     * @param customTime 커스텀 알림 시간 (HH:mm)
     */
    fun scheduleMultipleNotifications(
        context: Context,
        eventId: Long,
        title: String,
        body: String,
        endDate: LocalDate,
        sevenDaysBefore: Boolean = false,
        sevenDaysTime: String = "09:00",
        oneDayBefore: Boolean = false,
        oneDayTime: String = "10:00",
        customDays: Int? = null,
        customTime: String = "09:00"
    ) {
        // 7일 전 알림
        if (sevenDaysBefore) {
            val sevenDaysDate = endDate.minusDays(7)
            val time = parseTime(sevenDaysTime)
            val notificationDateTime = LocalDateTime.of(sevenDaysDate, time)
            scheduleNotification(context, eventId, title, "$body (7일 전)", notificationDateTime)
        }
        
        // 1일 전 알림
        if (oneDayBefore) {
            val oneDayDate = endDate.minusDays(1)
            val time = parseTime(oneDayTime)
            val notificationDateTime = LocalDateTime.of(oneDayDate, time)
            scheduleNotification(context, eventId, title, "$body (1일 전)", notificationDateTime)
        }
        
        // 커스텀 알림
        if (customDays != null && customDays > 0) {
            val customDate = endDate.minusDays(customDays.toLong())
            val time = parseTime(customTime)
            val notificationDateTime = LocalDateTime.of(customDate, time)
            scheduleNotification(context, eventId, title, "$body (${customDays}일 전)", notificationDateTime)
        }
    }
    
    /**
     * 일정 알림 취소
     */
    fun cancelNotification(context: Context, eventId: Long) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag("$WORK_NAME_PREFIX$eventId")
    }
    
    /**
     * 시간 문자열 파싱 (HH:mm)
     */
    private fun parseTime(timeString: String): LocalTime {
        val parts = timeString.split(":")
        val hour = parts[0].toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return LocalTime.of(hour, minute)
    }
}

