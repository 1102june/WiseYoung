package com.wiseyoung.pro.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.wiseyoung.pro.NotificationSettings
import com.wiseyoung.pro.data.CalendarEvent
import com.wiseyoung.pro.data.CalendarRepository
import com.wiseyoung.pro.data.EventType
import com.wiseyoung.pro.data.model.CalendarEventRequest
import com.wiseyoung.pro.data.model.CalendarEventResponse
import com.wiseyoung.pro.network.NetworkModule
import com.wiseyoung.pro.util.CalendarPermissionHelper
import com.wiseyoung.pro.util.DeviceCalendarHelper
import com.wiseyoung.pro.util.HousingDisplayUtils
import com.wiseyoung.pro.work.CalendarNotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 캘린더 서비스 — 앱(Room) · 서버 · 휴대폰 캘린더 연동.
 * 앱 캘린더 표시는 Room DB 기준이며, 서버 일정은 진입 시 동기화한다.
 */
class CalendarService(private val context: Context) {

    private val repository = CalendarRepository(context)
    private val auth = FirebaseAuth.getInstance()
    private val gson = Gson()

    private fun parseDate(dateString: String): LocalDate? {
        val formatters = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
        )
        for (formatter in formatters) {
            try {
                return LocalDate.parse(dateString, formatter)
            } catch (_: Exception) {
            }
        }
        android.util.Log.w("CalendarService", "날짜 파싱 실패: $dateString")
        return null
    }

    fun addPolicyToCalendar(
        title: String,
        organization: String?,
        deadline: String,
        policyId: String?,
        notificationSettings: NotificationSettings
    ) {
        val currentUser = auth.currentUser ?: return
        CoroutineScope(Dispatchers.IO).launch {
            addEventInternal(
                userId = currentUser.uid,
                title = title,
                organization = organization,
                deadline = deadline,
                policyId = policyId,
                housingId = null,
                eventType = EventType.POLICY,
                backendEventType = "policy",
                notificationSettings = notificationSettings
            )
        }
    }

    fun addHousingToCalendar(
        title: String,
        organization: String?,
        deadline: String,
        housingId: String?,
        notificationSettings: NotificationSettings,
        isAnnouncement: Boolean = false
    ) {
        val currentUser = auth.currentUser ?: return
        CoroutineScope(Dispatchers.IO).launch {
            addEventInternal(
                userId = currentUser.uid,
                title = title,
                organization = organization,
                deadline = deadline,
                policyId = null,
                housingId = housingId,
                eventType = if (isAnnouncement) EventType.HOUSING_ANNOUNCEMENT else EventType.HOUSING,
                backendEventType = "housing",
                notificationSettings = notificationSettings
            )
        }
    }

    /**
     * 북마크 저장 후 마감일을 조회·해석해 D-day 캘린더(Room + 서버 + 알림)에 추가.
     */
    fun addHousingBookmarkToCalendar(
        userId: String,
        title: String,
        organization: String?,
        contentId: String,
        preferredDeadline: String?,
        notificationSettings: NotificationSettings,
        isAnnouncement: Boolean = false,
        showToast: Boolean = true
    ) {
        if (auth.currentUser == null) return
        CoroutineScope(Dispatchers.IO).launch {
            val deadline = resolveHousingDeadline(userId, contentId, title, preferredDeadline)
            if (deadline == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "마감일 정보가 없어 D-day 캘린더에 추가하지 못했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                android.util.Log.w(
                    "CalendarService",
                    "임대주택 캘린더 추가 스kip: contentId=$contentId, title=$title"
                )
                return@launch
            }
            val added = addEventInternal(
                userId = userId,
                title = title,
                organization = organization,
                deadline = deadline,
                policyId = null,
                housingId = contentId,
                eventType = if (isAnnouncement) EventType.HOUSING_ANNOUNCEMENT else EventType.HOUSING,
                backendEventType = "housing",
                notificationSettings = notificationSettings
            )
            if (added && showToast) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "D-day 캘린더에 마감일이 추가되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun resolveHousingDeadline(
        userId: String,
        contentId: String,
        title: String,
        preferredDeadline: String?
    ): String? {
        preferredDeadline?.takeIf { it.isNotBlank() }?.let { formatDeadlineForStorage(it) }?.let { return it }

        try {
            val housingResponse = NetworkModule.apiService.getHousingById(
                housingId = contentId,
                userIdParam = userId
            )
            if (housingResponse.isSuccessful && housingResponse.body()?.success == true) {
                housingResponse.body()?.data?.applicationEnd
                    ?.let { formatDeadlineForStorage(it) }
                    ?.let { return it }
            }
        } catch (e: Exception) {
            android.util.Log.w("CalendarService", "getHousingById 마감일 조회 실패: ${e.message}")
        }

        try {
            val noticesResponse = NetworkModule.apiService.getHousingNotices(
                userId = userId,
                limit = 300
            )
            if (noticesResponse.isSuccessful && noticesResponse.body()?.success == true) {
                val notices = noticesResponse.body()?.data ?: emptyList()
                notices.find { it.noticeId == contentId || it.panId == contentId }
                    ?.applicationEnd
                    ?.let { formatDeadlineForStorage(it) }
                    ?.let { return it }
                HousingDisplayUtils.nearestDeadlineForComplex(
                    complexId = contentId,
                    complexName = title,
                    notices = notices
                ).takeIf { it.isNotBlank() }
                    ?.let { formatDeadlineForStorage(it) }
                    ?.let { return it }
            }
        } catch (e: Exception) {
            android.util.Log.w("CalendarService", "공고 API 마감일 조회 실패: ${e.message}")
        }
        return null
    }

    private fun formatDeadlineForStorage(raw: String): String? {
        val normalized = raw.trim().take(10)
        parseDate(normalized)?.let {
            return it.format(DateTimeFormatter.ISO_LOCAL_DATE)
        }
        parseDate(normalized.replace(".", "-"))?.let {
            return it.format(DateTimeFormatter.ISO_LOCAL_DATE)
        }
        return null
    }

    /** 서버 활성 일정 → Room DB 동기화 (앱 캘린더 표시용) */
    suspend fun syncFromBackend(userId: String) = withContext(Dispatchers.IO) {
        try {
            val response = NetworkModule.apiService.getCalendarEvents(userId = userId)
            if (!response.isSuccessful || response.body()?.success != true) {
                android.util.Log.w("CalendarService", "서버 캘린더 동기화 실패: ${response.code()}")
                return@withContext
            }
            val serverEvents = response.body()?.data ?: emptyList()
            for (serverEvent in serverEvents) {
                upsertFromServer(userId, serverEvent)
            }
            android.util.Log.d("CalendarService", "서버 캘린더 동기화 완료: ${serverEvents.size}건")
        } catch (e: Exception) {
            android.util.Log.e("CalendarService", "서버 캘린더 동기화 오류: ${e.message}", e)
        }
    }

    private suspend fun upsertFromServer(userId: String, serverEvent: CalendarEventResponse) {
        val serverId = serverEvent.eventId
        if (repository.getEventByServerId(serverId) != null) {
            return
        }
        val endDate = parseDate(serverEvent.endDate) ?: return
        val eventType = when {
            serverEvent.eventType.contains("policy", ignoreCase = true) -> EventType.POLICY
            serverEvent.eventType.contains("announcement", ignoreCase = true) -> EventType.HOUSING_ANNOUNCEMENT
            else -> EventType.HOUSING
        }
        repository.insertEvent(
            CalendarEvent(
                userId = userId,
                title = serverEvent.title,
                eventType = eventType,
                endDate = endDate,
                serverEventId = serverId,
                synced = true
            )
        )
    }

    private suspend fun addEventInternal(
        userId: String,
        title: String,
        organization: String?,
        deadline: String,
        policyId: String?,
        housingId: String?,
        eventType: EventType,
        backendEventType: String,
        notificationSettings: NotificationSettings
    ): Boolean {
        try {
            val endDate = parseDate(deadline) ?: run {
                android.util.Log.e("CalendarService", "날짜 파싱 실패로 일정 추가 취소: $deadline")
                return false
            }

            if (policyId != null) {
                val dup = findDuplicate(userId, policyId = policyId, housingId = null, title, endDate)
                if (dup != null) {
                    syncDeviceCalendarIfPermitted(title, organization, endDate)
                    return false
                }
            }
            if (housingId != null) {
                val dup = findDuplicate(userId, policyId = null, housingId = housingId, title, endDate)
                if (dup != null) {
                    syncDeviceCalendarIfPermitted(title, organization, endDate)
                    return false
                }
            }

            val notificationJson = gson.toJson(notificationSettings)
            val event = CalendarEvent(
                userId = userId,
                title = title,
                eventType = eventType,
                endDate = endDate,
                organization = organization,
                policyId = policyId,
                housingId = housingId,
                notificationSettings = notificationJson,
                synced = false
            )

            val eventId = repository.insertEvent(event)
            scheduleNotifications(eventId, title, endDate, notificationSettings)

            val serverEventId = syncToBackend(
                title = title,
                eventType = backendEventType,
                deadline = deadline,
                notificationSettings = notificationSettings
            )
            if (serverEventId != null) {
                repository.getEventById(eventId)?.let { saved ->
                    repository.updateEvent(saved.copy(serverEventId = serverEventId, synced = true))
                }
            }

            syncDeviceCalendarIfPermitted(title, organization, endDate)
            return true
        } catch (e: Exception) {
            android.util.Log.e("CalendarService", "캘린더 일정 추가 실패: ${e.message}", e)
            return false
        }
    }

    private suspend fun findDuplicate(
        userId: String,
        policyId: String?,
        housingId: String?,
        title: String,
        endDate: LocalDate
    ): CalendarEvent? {
        val events = repository.getEventsByUserIdOnce(userId)
        return events.find { event ->
            event.userId == userId && (
                (policyId != null && event.policyId == policyId) ||
                (housingId != null && event.housingId == housingId) ||
                (event.title == title && event.endDate == endDate)
            )
        }
    }

    private fun syncDeviceCalendarIfPermitted(title: String, organization: String?, endDate: LocalDate) {
        if (!CalendarPermissionHelper.hasCalendarPermission(context)) {
            return
        }
        val added = DeviceCalendarHelper.addDeadlineEvent(
            context = context,
            title = title,
            description = organization,
            endDate = endDate
        )
        notifyDeviceCalendarResult(added)
    }

    private fun notifyDeviceCalendarResult(added: Boolean) {
        if (!added) return
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context,
                "휴대폰 캘린더에 마감일이 추가되었습니다.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private suspend fun syncToBackend(
        title: String,
        eventType: String,
        deadline: String,
        notificationSettings: NotificationSettings
    ): Long? {
        val currentUser = auth.currentUser ?: return null
        return try {
            val endDate = parseDate(deadline)?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: deadline
            val request = CalendarEventRequest(
                userId = currentUser.uid,
                title = title,
                eventType = eventType,
                endDate = endDate,
                isSevenDaysAlert = notificationSettings.sevenDays,
                sevenDaysAlertTime = notificationSettings.sevenDaysTime,
                isOneDayAlert = notificationSettings.oneDay,
                oneDayAlertTime = notificationSettings.oneDayTime,
                isCustomAlert = notificationSettings.custom,
                customAlertDays = notificationSettings.customDays,
                customAlertTime = notificationSettings.customTime
            )
            val response = NetworkModule.apiService.addCalendarEvent(
                userId = currentUser.uid,
                request = request
            )
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data?.eventId
            } else {
                android.util.Log.w("CalendarService", "백엔드 동기화 실패: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("CalendarService", "백엔드 동기화 오류: ${e.message}", e)
            null
        }
    }

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

    fun rescheduleNotifications(
        eventId: Long,
        title: String,
        endDate: LocalDate,
        notificationSettings: NotificationSettings
    ) {
        CalendarNotificationScheduler.cancelNotification(context, eventId)
        scheduleNotifications(eventId, title, endDate, notificationSettings)
    }

    fun removeEventFromCalendar(eventId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val event = repository.getEventById(eventId) ?: return@launch
                event.serverEventId?.let { serverEventId ->
                    try {
                        NetworkModule.apiService.deleteCalendarEvent(
                            userId = currentUser.uid,
                            eventId = serverEventId
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("CalendarService", "서버 삭제 오류: ${e.message}", e)
                    }
                }
                repository.deleteEventById(eventId)
                CalendarNotificationScheduler.cancelNotification(context, eventId)
            } catch (e: Exception) {
                android.util.Log.e("CalendarService", "캘린더 일정 삭제 실패: ${e.message}", e)
            }
        }
    }
}
