package com.example.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 캘린더 일정 엔티티 (Room Database)
 * 앱 내부 DB에 저장되는 일정 데이터
 */
@Entity(tableName = "calendar_events")
data class CalendarEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val userId: String, // Firebase UID
    
    val title: String, // 일정 제목
    
    val eventType: EventType, // 정책(policy) 또는 임대주택(housing)
    
    val endDate: LocalDate, // 마감일 또는 일정 날짜
    
    val organization: String? = null, // 기관명
    
    val policyId: String? = null, // 정책 ID (서버 동기화용)
    
    val housingId: String? = null, // 임대주택 ID (서버 동기화용)
    
    val notificationSettings: String? = null, // 알림 설정 JSON 문자열
    
    val synced: Boolean = false, // 서버 동기화 여부
    
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class EventType {
    POLICY,    // 정책
    HOUSING    // 임대주택
}

