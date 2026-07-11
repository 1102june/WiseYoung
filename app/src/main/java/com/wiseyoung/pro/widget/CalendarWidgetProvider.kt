package com.wiseyoung.pro.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.wiseyoung.pro.CalendarActivity
import com.wiseyoung.pro.R
import com.wiseyoung.pro.data.CalendarEvent
import com.wiseyoung.pro.data.CalendarRepository
import com.wiseyoung.pro.data.EventType
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 캘린더 위젯 Provider
 * 홈 화면에 다가오는 마감일 카드들을 표시
 */
class CalendarWidgetProvider : AppWidgetProvider() {

    private val widgetScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 각 위젯 인스턴스 업데이트
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        // 위젯 업데이트 요청 시
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CalendarWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            appWidgetIds.forEach { appWidgetId ->
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid

        if (userId == null) {
            // 로그인되지 않은 경우
            val views = RemoteViews(context.packageName, R.layout.widget_calendar)
            views.setTextViewText(R.id.widget_title, "로그인이 필요합니다")
            views.setViewVisibility(R.id.widget_empty_message, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_list_container, android.view.View.GONE)
            
            // 앱 열기 Intent
            val intent = Intent(context, CalendarActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
            return
        }

        // 데이터 로드 (동기적으로 처리)
        try {
            val repository = CalendarRepository(context)
            val allEvents = runBlocking { 
                repository.getEventsByUserId(userId).first() 
            }
            val today = LocalDate.now()
            
            // 오늘 이후의 이벤트만 필터링하고 날짜순 정렬
            val upcomingEvents = allEvents
                .filter { it.endDate >= today }
                .sortedBy { it.endDate }
            
            // 정책과 임대주택으로 분리 (HOUSING_ANNOUNCEMENT도 포함)
            val policyEvents = upcomingEvents.filter { it.eventType == EventType.POLICY }.take(2)
            val housingEvents = upcomingEvents.filter { 
                it.eventType == EventType.HOUSING || it.eventType == EventType.HOUSING_ANNOUNCEMENT 
            }.take(2)

            val views = RemoteViews(context.packageName, R.layout.widget_calendar)
            
            if (upcomingEvents.isEmpty()) {
                // 일정이 없는 경우
                views.setTextViewText(R.id.widget_title, "다가오는 마감일")
                views.setViewVisibility(R.id.widget_empty_message, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.widget_list_container, android.view.View.GONE)
                views.setTextViewText(R.id.widget_empty_message, "등록된 일정이 없습니다.")
            } else {
                // 일정이 있는 경우
                views.setTextViewText(R.id.widget_title, "다가오는 마감일")
                views.setViewVisibility(R.id.widget_empty_message, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_list_container, android.view.View.VISIBLE)
                
                val dateFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd")
                
                // 정책 섹션 표시
                if (policyEvents.isNotEmpty()) {
                    views.setViewVisibility(R.id.widget_policy_section_title, android.view.View.VISIBLE)
                    
                    // 정책 카드 1
                    if (policyEvents.size >= 1) {
                        val event = policyEvents[0]
                        val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, event.endDate).toInt()
                        val deadlineStr = event.endDate.format(dateFormat)
                        
                        views.setViewVisibility(R.id.widget_policy_card_1, android.view.View.VISIBLE)
                        views.setTextViewText(R.id.widget_policy_title_1, event.title)
                        views.setTextViewText(R.id.widget_policy_date_1, deadlineStr)
                        views.setTextViewText(
                            R.id.widget_policy_days_1,
                            if (daysLeft == 0) "오늘" else "D-$daysLeft"
                        )
                    } else {
                        views.setViewVisibility(R.id.widget_policy_card_1, android.view.View.GONE)
                    }
                    
                    // 정책 카드 2
                    if (policyEvents.size >= 2) {
                        val event = policyEvents[1]
                        val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, event.endDate).toInt()
                        val deadlineStr = event.endDate.format(dateFormat)
                        
                        views.setViewVisibility(R.id.widget_policy_card_2, android.view.View.VISIBLE)
                        views.setTextViewText(R.id.widget_policy_title_2, event.title)
                        views.setTextViewText(R.id.widget_policy_date_2, deadlineStr)
                        views.setTextViewText(
                            R.id.widget_policy_days_2,
                            if (daysLeft == 0) "오늘" else "D-$daysLeft"
                        )
                    } else {
                        views.setViewVisibility(R.id.widget_policy_card_2, android.view.View.GONE)
                    }
                } else {
                    views.setViewVisibility(R.id.widget_policy_section_title, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_policy_card_1, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_policy_card_2, android.view.View.GONE)
                }
                
                // 임대주택 섹션 표시
                if (housingEvents.isNotEmpty()) {
                    views.setViewVisibility(R.id.widget_housing_section_title, android.view.View.VISIBLE)
                    
                    // 임대주택 카드 1
                    if (housingEvents.size >= 1) {
                        val event = housingEvents[0]
                        val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, event.endDate).toInt()
                        val deadlineStr = event.endDate.format(dateFormat)
                        
                        views.setViewVisibility(R.id.widget_housing_card_1, android.view.View.VISIBLE)
                        views.setTextViewText(R.id.widget_housing_title_1, event.title)
                        views.setTextViewText(R.id.widget_housing_date_1, deadlineStr)
                        views.setTextViewText(
                            R.id.widget_housing_days_1,
                            if (daysLeft == 0) "오늘" else "D-$daysLeft"
                        )
                    } else {
                        views.setViewVisibility(R.id.widget_housing_card_1, android.view.View.GONE)
                    }
                    
                    // 임대주택 카드 2
                    if (housingEvents.size >= 2) {
                        val event = housingEvents[1]
                        val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, event.endDate).toInt()
                        val deadlineStr = event.endDate.format(dateFormat)
                        
                        views.setViewVisibility(R.id.widget_housing_card_2, android.view.View.VISIBLE)
                        views.setTextViewText(R.id.widget_housing_title_2, event.title)
                        views.setTextViewText(R.id.widget_housing_date_2, deadlineStr)
                        views.setTextViewText(
                            R.id.widget_housing_days_2,
                            if (daysLeft == 0) "오늘" else "D-$daysLeft"
                        )
                    } else {
                        views.setViewVisibility(R.id.widget_housing_card_2, android.view.View.GONE)
                    }
                } else {
                    views.setViewVisibility(R.id.widget_housing_section_title, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_housing_card_1, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_housing_card_2, android.view.View.GONE)
                }
            }
            
            // 앱 열기 Intent
            val intent = Intent(context, CalendarActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            android.util.Log.e("CalendarWidget", "위젯 업데이트 실패: ${e.message}", e)
            // 오류 발생 시 기본 레이아웃 표시
            val views = RemoteViews(context.packageName, R.layout.widget_calendar)
            views.setTextViewText(R.id.widget_title, "다가오는 마감일")
            views.setViewVisibility(R.id.widget_empty_message, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_list_container, android.view.View.GONE)
            views.setTextViewText(R.id.widget_empty_message, "위젯을 불러올 수 없습니다.")
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun createEventCard(context: Context, event: CalendarEvent, index: Int): RemoteViews {
        val cardViews = RemoteViews(context.packageName, R.layout.widget_event_card)
        
        val today = LocalDate.now()
        val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, event.endDate).toInt()
        val dateFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        val deadlineStr = event.endDate.format(dateFormat)
        
        cardViews.setTextViewText(R.id.widget_card_title, event.title)
        cardViews.setTextViewText(R.id.widget_card_date, deadlineStr)
        cardViews.setTextViewText(
            R.id.widget_card_days,
            if (daysLeft == 0) "오늘" else "D-$daysLeft"
        )
        
        // 이벤트 타입에 따른 색상 (drawable 리소스 사용)
        val indicatorDrawable = when (event.eventType) {
            EventType.POLICY -> R.drawable.widget_indicator_policy
            EventType.HOUSING -> R.drawable.widget_indicator_housing
            else -> R.drawable.widget_indicator_policy // 기본값
        }
        cardViews.setInt(R.id.widget_card_indicator, "setBackgroundResource", indicatorDrawable)
        
        return cardViews
    }

    companion object {
        /**
         * 위젯 수동 업데이트
         */
        fun updateWidgets(context: Context) {
            val intent = Intent(context, CalendarWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            context.sendBroadcast(intent)
        }
    }
}

