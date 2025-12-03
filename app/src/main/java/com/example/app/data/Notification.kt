package com.example.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * 알림 엔티티 (Room Database)
 * 알림함에 표시될 알림 데이터
 */
@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val userId: String, // Firebase UID
    
    val title: String, // 알림 제목
    
    val body: String, // 알림 내용
    
    val notificationType: NotificationType, // 알림 타입 (CALENDAR, FCM)
    
    val eventId: Long? = null, // 캘린더 이벤트 ID (캘린더 알림인 경우)
    
    val eventType: EventType? = null, // 정책(policy) 또는 임대주택(housing)
    
    val organization: String? = null, // 기관명
    
    val policyId: String? = null, // 정책 ID
    
    val housingId: String? = null, // 임대주택 ID
    
    val isRead: Boolean = false, // 읽음 여부
    
    val createdAt: LocalDateTime = LocalDateTime.now() // 알림 생성 시간
)

enum class NotificationType {
    CALENDAR,    // 캘린더 알림 (로컬 WorkManager)
    FCM          // FCM 알림 (서버 푸시)
}

