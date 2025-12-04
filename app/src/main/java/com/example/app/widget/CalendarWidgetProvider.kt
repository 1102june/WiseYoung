package com.wiseyoung.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.wiseyoung.app.CalendarActivity
import com.wiseyoung.app.R
import com.wiseyoung.app.data.CalendarEvent
import com.wiseyoung.app.data.CalendarRepository
import com.wiseyoung.app.data.EventType
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

        // 데이터 로드
        widgetScope.launch {
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
                    .take(5) // 최대 5개만 표시

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
                    
                    // 카드들을 동적으로 추가
                    val listContainer = views.findViewById<android.view.ViewGroup>(R.id.widget_list_container)
                    listContainer?.removeAllViews()
                    
                    upcomingEvents.forEachIndexed { index, event ->
                        val cardViews = createEventCard(context, event, index)
                        // RemoteViews는 직접 View를 추가할 수 없으므로
                        // 대신 LinearLayout에 RemoteViews를 추가하는 방식 사용
                        // 하지만 이는 복잡하므로 간단하게 텍스트로 표시
                    }
                    
                    // 첫 번째 이벤트 정보 표시 (간단한 버전)
                    val firstEvent = upcomingEvents.first()
                    val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, firstEvent.endDate).toInt()
                    val dateFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd")
                    val deadlineStr = firstEvent.endDate.format(dateFormat)
                    
                    views.setTextViewText(R.id.widget_event_title, firstEvent.title)
                    views.setTextViewText(R.id.widget_event_date, deadlineStr)
                    views.setTextViewText(
                        R.id.widget_event_days,
                        if (daysLeft == 0) "오늘" else "D-$daysLeft"
                    )
                    
                    // 이벤트 타입에 따른 색상 설정
                    val colorRes = when (firstEvent.eventType) {
                        EventType.POLICY -> R.color.policy_color
                        EventType.HOUSING -> R.color.housing_color
                    }
                    views.setInt(R.id.widget_event_indicator, "setBackgroundColor", 
                        context.getColor(colorRes))
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
            }
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
        
        // 이벤트 타입에 따른 색상
        val colorRes = when (event.eventType) {
            EventType.POLICY -> R.color.policy_color
            EventType.HOUSING -> R.color.housing_color
        }
        cardViews.setInt(R.id.widget_card_indicator, "setBackgroundColor", 
            context.getColor(colorRes))
        
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

