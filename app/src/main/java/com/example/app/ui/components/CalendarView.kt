package com.example.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.data.CalendarEvent
import com.example.app.data.EventType
import com.example.app.ui.theme.AppColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * 달력 컴포넌트
 * Jetpack Compose로 직접 구현한 달력 UI
 */
@Composable
fun CalendarView(
    selectedDate: LocalDate,
    events: List<CalendarEvent>,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: ((LocalDate) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember(selectedDate) { mutableStateOf(YearMonth.from(selectedDate)) }
    val firstDayOfMonth = currentMonth.atDay(1)
    val lastDayOfMonth = currentMonth.atEndOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek
    val daysInMonth = lastDayOfMonth.dayOfMonth
    
    // 이전 달의 마지막 날들
    val daysFromPreviousMonth = (firstDayOfWeek.value % 7)
    val previousMonth = currentMonth.minusMonths(1)
    val daysInPreviousMonth = previousMonth.lengthOfMonth()
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // 월/년 표시 및 네비게이션
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    currentMonth = currentMonth.minusMonths(1)
                    onMonthChange?.invoke(currentMonth.atDay(1))
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "이전 달",
                    tint = AppColors.TextPrimary
                )
            }
            
            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("yyyy년 MM월")),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            
            IconButton(
                onClick = {
                    currentMonth = currentMonth.plusMonths(1)
                    onMonthChange?.invoke(currentMonth.atDay(1))
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "다음 달",
                    tint = AppColors.TextPrimary
                )
            }
        }
        
        // 요일 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
                Text(
                    text = day,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (day == "일") Color(0xFFEF4444) else if (day == "토") Color(0xFF3B82F6) else AppColors.TextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 날짜 그리드
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            var currentWeek = mutableListOf<DayItem>()
            
            // 이전 달의 마지막 날짜들
            if (daysFromPreviousMonth > 0) {
                for (i in daysFromPreviousMonth - 1 downTo 0) {
                    val day = daysInPreviousMonth - i
                    currentWeek.add(
                        DayItem(
                            day = day,
                            isCurrentMonth = false,
                            isSelected = false,
                            hasEvent = false
                        )
                    )
                }
            }
            
            // 현재 달의 날짜들
            for (day in 1..daysInMonth) {
                val date = currentMonth.atDay(day)
                val dayEvents = events.filter { it.endDate == date }
                val hasEvent = dayEvents.isNotEmpty()
                val isSelected = date == selectedDate
                val isToday = date == LocalDate.now()
                
                // 이벤트 타입별 색상 결정
                val eventType = when {
                    dayEvents.any { it.eventType == EventType.POLICY } -> EventType.POLICY
                    dayEvents.any { it.eventType == EventType.HOUSING } -> EventType.HOUSING
                    else -> null
                }
                
                currentWeek.add(
                    DayItem(
                        day = day,
                        isCurrentMonth = true,
                        isSelected = isSelected,
                        hasEvent = hasEvent,
                        isToday = isToday,
                        eventType = eventType
                    )
                )
                
                if (currentWeek.size == 7) {
                    CalendarWeekRow(
                        week = currentWeek,
                        onDayClick = { dayNum ->
                            if (dayNum <= daysInMonth) {
                                onDateSelected(currentMonth.atDay(dayNum))
                            }
                        }
                    )
                    currentWeek = mutableListOf()
                }
            }
            
            // 다음 달의 날짜들 (마지막 주 완성)
            if (currentWeek.isNotEmpty()) {
                var nextMonthDay = 1
                while (currentWeek.size < 7) {
                    currentWeek.add(
                        DayItem(
                            day = nextMonthDay,
                            isCurrentMonth = false,
                            isSelected = false,
                            hasEvent = false
                        )
                    )
                    nextMonthDay++
                }
                CalendarWeekRow(
                    week = currentWeek,
                    onDayClick = { }
                )
            }
        }
    }
}

private data class DayItem(
    val day: Int,
    val isCurrentMonth: Boolean,
    val isSelected: Boolean,
    val hasEvent: Boolean,
    val isToday: Boolean = false,
    val eventType: EventType? = null
)

@Composable
private fun CalendarWeekRow(
    week: List<DayItem>,
    onDayClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        week.forEach { dayItem ->
            CalendarDayCell(
                dayItem = dayItem,
                onClick = { onDayClick(dayItem.day) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayItem: DayItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    dayItem.isSelected -> AppColors.BackgroundGradientStart.copy(alpha = 0.3f)
                    dayItem.isToday -> AppColors.Info.copy(alpha = 0.1f)
                    else -> Color.Transparent
                }
            )
            .then(
                if (dayItem.isCurrentMonth) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayItem.day.toString(),
                fontSize = 14.sp,
                fontWeight = if (dayItem.isSelected || dayItem.isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    !dayItem.isCurrentMonth -> AppColors.TextTertiary.copy(alpha = 0.3f)
                    dayItem.isSelected -> AppColors.BackgroundGradientStart
                    dayItem.isToday -> AppColors.Info
                    else -> AppColors.TextPrimary
                }
            )
            
            // 일정이 있는 날 표시 (이벤트 타입별 색상)
            if (dayItem.hasEvent && dayItem.isCurrentMonth) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            when (dayItem.eventType) {
                                EventType.POLICY -> Color(0xFF59ABF7) // 정책 - 파란색
                                EventType.HOUSING -> Color(0xFFFF9800) // 임대주택 - 주황색
                                null -> AppColors.BackgroundGradientStart
                            }
                        )
                )
            }
        }
    }
}

