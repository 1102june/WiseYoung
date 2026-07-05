package com.wiseyoung.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiseyoung.pro.ui.theme.AppColors
import com.wiseyoung.pro.ui.theme.Spacing

import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun BottomNavigationBarPreview() {
    BottomNavigationBar(
        currentScreen = "home",
        onNavigateHome = {},
        onNavigateCalendar = {},
        onNavigateBookmark = {},
        onNavigateProfile = {}
    )
}

@Composable
fun BottomNavigationBar(
    currentScreen: String,
    onNavigateHome: () -> Unit,
    onNavigateCalendar: () -> Unit,
    onNavigateBookmark: () -> Unit,
    onNavigateProfile: () -> Unit
) {
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .padding(bottom = navigationBarsPadding.calculateBottomPadding())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
            BottomNavButton(
                icon = Icons.Default.Home,
                filledIcon = Icons.Filled.Home,
                label = "홈",
                isSelected = currentScreen == "home",
                onClick = onNavigateHome
            )

            BottomNavButton(
                icon = Icons.Default.CalendarToday,
                filledIcon = Icons.Filled.CalendarToday,
                label = "캘린더",
                isSelected = currentScreen == "calendar",
                onClick = onNavigateCalendar
            )

            BottomNavButton(
                icon = Icons.Default.Favorite,
                filledIcon = Icons.Filled.Favorite,
                label = "좋아요",
                isSelected = currentScreen == "bookmark",
                onClick = onNavigateBookmark
            )

            BottomNavButton(
                icon = Icons.Default.Person,
                filledIcon = Icons.Filled.Person,
                label = "내정보",
                isSelected = currentScreen == "profile",
                onClick = onNavigateProfile
            )
            }
        }
    }
}

@Composable
private fun BottomNavButton(
    icon: ImageVector,
    filledIcon: ImageVector? = null,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (isSelected) 32.dp else 28.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) AppColors.LightBlue.copy(alpha = 0.1f) else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected && filledIcon != null) filledIcon else icon,
                contentDescription = label,
                modifier = Modifier.size(if (isSelected) 20.dp else 18.dp),
                tint = if (isSelected) AppColors.LightBlue else AppColors.TextSecondary
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isSelected) AppColors.LightBlue else AppColors.TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
