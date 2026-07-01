package com.wiseyoung.pro.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

/**
 * 기기 기본 캘린더(Google Calendar 등)에 일정 추가.
 */
object DeviceCalendarHelper {

    fun addDeadlineEvent(
        context: Context,
        title: String,
        description: String?,
        endDate: LocalDate
    ): Boolean {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR)
            != PackageManager.PERMISSION_GRANTED
        ) {
            android.util.Log.w("DeviceCalendarHelper", "WRITE_CALENDAR 권한 없음 — 기기 캘린더 연동 생략")
            return false
        }

        return try {
            val calendarId = queryDefaultCalendarId(context) ?: return false
            val startMillis = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description ?: "")
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.ALL_DAY, 1)
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            android.util.Log.d("DeviceCalendarHelper", "기기 캘린더 추가: $title -> $uri")
            uri != null
        } catch (e: Exception) {
            android.util.Log.e("DeviceCalendarHelper", "기기 캘린더 추가 실패: ${e.message}", e)
            false
        }
    }

    private fun queryDefaultCalendarId(context: Context): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY
        )
        val uri = CalendarContract.Calendars.CONTENT_URI
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                if (idIndex >= 0) return cursor.getLong(idIndex)
            }
        }
        return null
    }
}
