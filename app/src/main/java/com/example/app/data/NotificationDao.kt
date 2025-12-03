package com.example.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 알림 DAO
 * 알림 데이터베이스 작업
 */
@Dao
interface NotificationDao {
    
    /**
     * 모든 알림 조회 (최신순)
     */
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllNotifications(userId: String): Flow<List<Notification>>
    
    /**
     * 읽지 않은 알림 개수 조회
     */
    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    fun getUnreadCount(userId: String): Flow<Int>
    
    /**
     * 알림 삽입
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification): Long
    
    /**
     * 알림 업데이트
     */
    @Update
    suspend fun updateNotification(notification: Notification)
    
    /**
     * 알림 읽음 처리
     */
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: Long)
    
    /**
     * 모든 알림 읽음 처리
     */
    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: String)
    
    /**
     * 알림 삭제
     */
    @Delete
    suspend fun deleteNotification(notification: Notification)
    
    /**
     * 알림 ID로 삭제
     */
    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteNotificationById(notificationId: Long)
    
    /**
     * 모든 알림 삭제
     */
    @Query("DELETE FROM notifications WHERE userId = :userId")
    suspend fun deleteAllNotifications(userId: String)
    
    /**
     * 오래된 알림 삭제 (특정 날짜 이전의 알림)
     * @param beforeDate 삭제할 기준 날짜 (ISO 형식 문자열)
     */
    @Query("DELETE FROM notifications WHERE userId = :userId AND createdAt < :beforeDate")
    suspend fun deleteOldNotifications(userId: String, beforeDate: String)
}

