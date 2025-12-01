package com.example.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * 캘린더 일정 Repository
 * Room Database와의 통신을 담당
 */
class CalendarRepository(context: Context) {
    
    private val calendarDao = AppDatabase.getDatabase(context).calendarDao()
    
    /**
     * 모든 일정 조회 (Flow)
     */
    fun getAllEvents(): Flow<List<CalendarEvent>> = calendarDao.getAllEvents()
    
    /**
     * 특정 날짜의 일정 조회
     */
    suspend fun getEventsByDate(date: LocalDate): List<CalendarEvent> {
        return calendarDao.getEventsByDate(date)
    }
    
    /**
     * 특정 기간의 일정 조회
     */
    suspend fun getEventsByDateRange(startDate: LocalDate, endDate: LocalDate): List<CalendarEvent> {
        return calendarDao.getEventsByDateRange(startDate, endDate)
    }
    
    /**
     * 특정 사용자의 일정 조회 (Flow)
     */
    fun getEventsByUserId(userId: String): Flow<List<CalendarEvent>> {
        return calendarDao.getEventsByUserId(userId)
    }
    
    /**
     * 특정 타입의 일정 조회
     */
    suspend fun getEventsByType(eventType: EventType): List<CalendarEvent> {
        return calendarDao.getEventsByType(eventType)
    }
    
    /**
     * ID로 일정 조회
     */
    suspend fun getEventById(id: Long): CalendarEvent? {
        return calendarDao.getEventById(id)
    }
    
    /**
     * 일정 추가
     */
    suspend fun insertEvent(event: CalendarEvent): Long {
        return calendarDao.insertEvent(event)
    }
    
    /**
     * 일정 추가 (여러 개)
     */
    suspend fun insertEvents(events: List<CalendarEvent>) {
        calendarDao.insertEvents(events)
    }
    
    /**
     * 일정 업데이트
     */
    suspend fun updateEvent(event: CalendarEvent) {
        calendarDao.updateEvent(event)
    }
    
    /**
     * 일정 삭제
     */
    suspend fun deleteEvent(event: CalendarEvent) {
        calendarDao.deleteEvent(event)
    }
    
    /**
     * ID로 일정 삭제
     */
    suspend fun deleteEventById(id: Long) {
        calendarDao.deleteEventById(id)
    }
    
    /**
     * 동기화되지 않은 일정 조회
     */
    suspend fun getUnsyncedEvents(): List<CalendarEvent> {
        return calendarDao.getUnsyncedEvents()
    }
    
    /**
     * 동기화 상태 업데이트
     */
    suspend fun markAsSynced(id: Long) {
        calendarDao.markAsSynced(id)
    }
}

