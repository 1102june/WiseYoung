package com.example.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * 캘린더 일정 DAO (Data Access Object)
 */
@Dao
interface CalendarDao {
    
    /**
     * 모든 일정 조회 (Flow)
     */
    @Query("SELECT * FROM calendar_events ORDER BY endDate ASC")
    fun getAllEvents(): Flow<List<CalendarEvent>>
    
    /**
     * 특정 날짜의 일정 조회
     */
    @Query("SELECT * FROM calendar_events WHERE endDate = :date ORDER BY endDate ASC")
    suspend fun getEventsByDate(date: LocalDate): List<CalendarEvent>
    
    /**
     * 특정 기간의 일정 조회
     */
    @Query("SELECT * FROM calendar_events WHERE endDate BETWEEN :startDate AND :endDate ORDER BY endDate ASC")
    suspend fun getEventsByDateRange(startDate: LocalDate, endDate: LocalDate): List<CalendarEvent>
    
    /**
     * 특정 사용자의 일정 조회
     */
    @Query("SELECT * FROM calendar_events WHERE userId = :userId ORDER BY endDate ASC")
    fun getEventsByUserId(userId: String): Flow<List<CalendarEvent>>
    
    /**
     * 특정 타입의 일정 조회
     */
    @Query("SELECT * FROM calendar_events WHERE eventType = :eventType ORDER BY endDate ASC")
    suspend fun getEventsByType(eventType: EventType): List<CalendarEvent>

    /**
     * ID로 일정 조회
     */
    @Query("SELECT * FROM calendar_events WHERE id = :id")
    suspend fun getEventById(id: Long): CalendarEvent?
    
    /**
     * ID로 일정 조회
     */
    @Query("SELECT * FROM calendar_events WHERE id = :id")
    suspend fun getEventById(id: Long): CalendarEvent?
    
    /**
     * 일정 삽입
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEvent): Long
    
    /**
     * 일정 삽입 (여러 개)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<CalendarEvent>)
    
    /**
     * 일정 업데이트
     */
    @Update
    suspend fun updateEvent(event: CalendarEvent)
    
    /**
     * 일정 삭제
     */
    @Delete
    suspend fun deleteEvent(event: CalendarEvent)
    
    /**
     * ID로 일정 삭제
     */
    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun deleteEventById(id: Long)
    
    /**
     * 모든 일정 삭제
     */
    @Query("DELETE FROM calendar_events")
    suspend fun deleteAllEvents()
    
    /**
     * 동기화되지 않은 일정 조회
     */
    @Query("SELECT * FROM calendar_events WHERE synced = 0")
    suspend fun getUnsyncedEvents(): List<CalendarEvent>
    
    /**
     * 동기화 상태 업데이트
     */
    @Query("UPDATE calendar_events SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)
}

