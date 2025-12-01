package com.example.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 알림 Repository
 * Room Database와의 통신을 담당
 */
class NotificationRepository(context: Context) {
    
    private val notificationDao = AppDatabase.getDatabase(context).notificationDao()
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    /**
     * 모든 알림 조회 (Flow)
     */
    fun getAllNotifications(userId: String): Flow<List<Notification>> {
        return notificationDao.getAllNotifications(userId)
    }
    
    /**
     * 읽지 않은 알림 개수 조회
     */
    fun getUnreadCount(userId: String): Flow<Int> {
        return notificationDao.getUnreadCount(userId)
    }
    
    /**
     * 알림 추가
     */
    suspend fun insertNotification(notification: Notification): Long {
        return notificationDao.insertNotification(notification)
    }
    
    /**
     * 알림 업데이트
     */
    suspend fun updateNotification(notification: Notification) {
        notificationDao.updateNotification(notification)
    }
    
    /**
     * 알림 읽음 처리
     */
    suspend fun markAsRead(notificationId: Long) {
        notificationDao.markAsRead(notificationId)
    }
    
    /**
     * 모든 알림 읽음 처리
     */
    suspend fun markAllAsRead(userId: String) {
        notificationDao.markAllAsRead(userId)
    }
    
    /**
     * 알림 삭제
     */
    suspend fun deleteNotification(notification: Notification) {
        notificationDao.deleteNotification(notification)
    }
    
    /**
     * 알림 ID로 삭제
     */
    suspend fun deleteNotificationById(notificationId: Long) {
        notificationDao.deleteNotificationById(notificationId)
    }
    
    /**
     * 모든 알림 삭제
     */
    suspend fun deleteAllNotifications(userId: String) {
        notificationDao.deleteAllNotifications(userId)
    }
    
    /**
     * 오래된 알림 삭제 (30일 이상 된 알림)
     */
    suspend fun deleteOldNotifications(userId: String) {
        val thirtyDaysAgo = LocalDateTime.now().minusDays(30)
        val beforeDate = thirtyDaysAgo.format(dateTimeFormatter)
        notificationDao.deleteOldNotifications(userId, beforeDate)
    }
}

