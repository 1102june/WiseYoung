package com.example.app.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context

/**
 * Room Database
 * 앱의 로컬 데이터베이스
 */
@Database(
    entities = [CalendarEvent::class, Notification::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun calendarDao(): CalendarDao
    
    abstract fun notificationDao(): NotificationDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wiseyoung_database"
                )
                    .fallbackToDestructiveMigration() // 개발 중에는 스키마 변경 시 데이터 삭제
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

