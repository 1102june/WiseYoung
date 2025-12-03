package com.example.app.service

import android.content.Context
import com.example.app.NotificationSettings
import com.example.app.data.CalendarEvent
import com.example.app.data.CalendarRepository
import com.example.app.data.EventType
import com.example.app.work.CalendarNotificationScheduler
import com.example.app.network.NetworkModule
import com.example.app.data.model.CalendarEventRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 캘린더 서비스
 * 정책/임대주택을 캘린더에 추가하고 알림을 스케줄링
 */
class CalendarService(private val context: Context) {
    
    private val repository = CalendarRepository(context)
    private val auth = FirebaseAuth.getInstance()
    private val gson = Gson()
    
    /**
     * 날짜 문자열을 LocalDate로 파싱
     * 여러 형식 지원: yyyy-MM-dd, yyyy.MM.dd, yyyy/MM/dd
     */
    private fun parseDate(dateString: String): LocalDate? {
        val formatters = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE, // yyyy-MM-dd
            DateTimeFormatter.ofPattern("yyyy.MM.dd"), // yyyy.MM.dd
            DateTimeFormatter.ofPattern("yyyy/MM/dd"), // yyyy/MM/dd
            DateTimeFormatter.ofPattern("yyyyMMdd") // yyyyMMdd
        )
        
        for (formatter in formatters) {
            try {
                return LocalDate.parse(dateString, formatter)
            } catch (e: Exception) {
                // 다음 포맷 시도
                continue
            }
        }
        
        android.util.Log.w("CalendarService", "날짜 파싱 실패: $dateString (지원되는 형식: yyyy-MM-dd, yyyy.MM.dd, yyyy/MM/dd, yyyyMMdd)")
        return null
    }
    
    /**
     * 정책을 캘린더에 추가
     * @param title 정책 제목
     * @param organization 기관명
     * @param deadline 마감일 (yyyy-MM-dd, yyyy.MM.dd, yyyy/MM/dd 형식 지원)
     * @param policyId 정책 ID
     * @param notificationSettings 알림 설정
     */
    fun addPolicyToCalendar(
        title: String,
        organization: String?,
        deadline: String,
        policyId: String?,
        notificationSettings: NotificationSettings
    ) {
        val currentUser = auth.currentUser ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 마감일 파싱
                val endDate = parseDate(deadline) ?: run {
                    android.util.Log.e("CalendarService", "날짜 파싱 실패로 정책 추가 취소: $deadline")
                    return@launch
                }
                
                // 알림 설정을 JSON으로 변환
                val notificationJson = gson.toJson(notificationSettings)
                
                // 캘린더 이벤트 생성 (로컬 DB용)
                val event = CalendarEvent(
                    userId = currentUser.uid,
                    title = title,
                    eventType = EventType.POLICY,
                    endDate = endDate,
                    organization = organization,
                    policyId = policyId,
                    notificationSettings = notificationJson,
                    synced = false
                )
                
                // 로컬 DB에 저장
                val eventId = repository.insertEvent(event)
                
                // 로컬 알림 스케줄링 (백업용)
                scheduleNotifications(eventId, title, endDate, notificationSettings)
                
                // 백엔드 동기화 (FCM 알림용)
                val serverEventId = syncToBackend(
                    title = title,
                    eventType = "policy",
                    deadline = deadline,
                    notificationSettings = notificationSettings
                )
                
                // 서버 eventId 저장
                if (serverEventId != null) {
                    val savedEvent = repository.getEventById(eventId)
                    if (savedEvent != null) {
                        val updatedEvent = savedEvent.copy(serverEventId = serverEventId, synced = true)
                        repository.updateEvent(updatedEvent)
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("CalendarService", "캘린더에 정책 추가 실패: ${e.message}", e)
            }
        }
    }
    
    /**
     * 임대주택을 캘린더에 추가
     */
    fun addHousingToCalendar(
        title: String,
        organization: String?,
        deadline: String,
        housingId: String?,
        notificationSettings: NotificationSettings
    ) {
        val currentUser = auth.currentUser ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val endDate = parseDate(deadline) ?: run {
                    android.util.Log.e("CalendarService", "날짜 파싱 실패로 임대주택 추가 취소: $deadline")
                    return@launch
                }
                val notificationJson = gson.toJson(notificationSettings)
                
                val event = CalendarEvent(
                    userId = currentUser.uid,
                    title = title,
                    eventType = EventType.HOUSING,
                    endDate = endDate,
                    organization = organization,
                    housingId = housingId,
                    notificationSettings = notificationJson,
                    synced = false
                )
                
                val eventId = repository.insertEvent(event)
                
                scheduleNotifications(eventId, title, endDate, notificationSettings)
                
                // 백엔드 동기화 (FCM 알림용)
                val serverEventId = syncToBackend(
                    title = title,
                    eventType = "housing",
                    deadline = deadline,
                    notificationSettings = notificationSettings
                )
                
                // 서버 eventId 저장
                if (serverEventId != null) {
                    val savedEvent = repository.getEventById(eventId)
                    if (savedEvent != null) {
                        val updatedEvent = savedEvent.copy(serverEventId = serverEventId, synced = true)
                        repository.updateEvent(updatedEvent)
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("CalendarService", "캘린더에 임대주택 추가 실패: ${e.message}", e)
            }
        }
    }
    
    /**
     * 백엔드로 일정 정보 전송 (FCM 알림 스케줄링을 위해)
     * @return 서버 eventId, 실패 시 null
     */
    private suspend fun syncToBackend(
        title: String,
        eventType: String,
        deadline: String,
        notificationSettings: NotificationSettings
    ): Long? {
        val currentUser = auth.currentUser ?: return null
        
        try {
            // 날짜 형식을 ISO 형식(yyyy-MM-dd)으로 변환
            val endDate = parseDate(deadline)?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: deadline
            
            val request = CalendarEventRequest(
                userId = currentUser.uid,
                title = title,
                eventType = eventType,
                endDate = endDate, // ISO 형식으로 변환된 날짜 전송
                isSevenDaysAlert = notificationSettings.sevenDays,
                sevenDaysAlertTime = notificationSettings.sevenDaysTime,
                isOneDayAlert = notificationSettings.oneDay,
                oneDayAlertTime = notificationSettings.oneDayTime,
                isCustomAlert = notificationSettings.custom,
                customAlertDays = notificationSettings.customDays,
                customAlertTime = notificationSettings.customTime
            )
            
            android.util.Log.d("CalendarService", "백엔드 전송 요청: $request")
            
            val response = NetworkModule.apiService.addCalendarEvent(
                userId = currentUser.uid,
                request = request
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                val eventId = response.body()?.data?.eventId?.toLong()
                android.util.Log.d("CalendarService", "백엔드 동기화 성공: $title, eventId=$eventId")
                return eventId
            } else {
                android.util.Log.w("CalendarService", "백엔드 동기화 실패: ${response.code()} ${response.message()}")
                // 오류 본문 로깅 (있다면)
                response.errorBody()?.string()?.let { errorBody ->
                    android.util.Log.e("CalendarService", "백엔드 오류 상세: $errorBody")
                }
                return null
            }
        } catch (e: Exception) {
            android.util.Log.e("CalendarService", "백엔드 동기화 오류: ${e.message}", e)
            return null
        }
    }
    
    /**
     * 알림 스케줄링
     */
    fun scheduleNotifications(
        eventId: Long,
        title: String,
        endDate: LocalDate,
        notificationSettings: NotificationSettings
    ) {
        CalendarNotificationScheduler.scheduleMultipleNotifications(
            context = context,
            eventId = eventId,
            title = title,
            body = "마감일이 다가옵니다.",
            endDate = endDate,
            sevenDaysBefore = notificationSettings.sevenDays,
            sevenDaysTime = notificationSettings.sevenDaysTime,
            oneDayBefore = notificationSettings.oneDay,
            oneDayTime = notificationSettings.oneDayTime,
            customDays = if (notificationSettings.custom) notificationSettings.customDays else null,
            customTime = notificationSettings.customTime
        )
    }
    
    /**
     * 캘린더에서 일정 삭제
     */
    fun removeEventFromCalendar(eventId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                
                // 로컬 이벤트 조회 (서버 삭제를 위해 먼저 조회)
                val event = repository.getEventById(eventId)
                
                if (event == null) {
                    android.util.Log.w("CalendarService", "⚠️ 삭제할 이벤트를 찾을 수 없습니다: eventId=$eventId")
                    return@launch
                }
                
                // 서버에도 삭제 요청
                val serverEventId = event.serverEventId
                if (serverEventId != null) {
                    try {
                        android.util.Log.d("CalendarService", "서버 캘린더 이벤트 삭제 시도: serverEventId=$serverEventId, title=${event.title}")
                        val response = NetworkModule.apiService.deleteCalendarEvent(
                            userId = currentUser.uid,
                            eventId = serverEventId
                        )
                        if (response.isSuccessful && response.body()?.success == true) {
                            android.util.Log.d("CalendarService", "✅ 서버 캘린더 이벤트 삭제 성공: eventId=$serverEventId")
                        } else {
                            android.util.Log.w("CalendarService", "❌ 서버 캘린더 이벤트 삭제 실패: ${response.code()}, ${response.body()?.message}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CalendarService", "❌ 서버 캘린더 이벤트 삭제 오류: ${e.message}", e)
                    }
                } else {
                    android.util.Log.w("CalendarService", "⚠️ serverEventId가 null입니다. 서버 삭제를 건너뜁니다: title=${event.title}, endDate=${event.endDate}")
                }
                
                // 로컬 DB에서 삭제
                repository.deleteEventById(eventId)
                android.util.Log.d("CalendarService", "로컬 캘린더 이벤트 삭제 완료: eventId=$eventId")
                
                // 알림 취소
                CalendarNotificationScheduler.cancelNotification(context, eventId)
            } catch (e: Exception) {
                android.util.Log.e("CalendarService", "캘린더 일정 삭제 실패: ${e.message}", e)
            }
        }
    }
}
