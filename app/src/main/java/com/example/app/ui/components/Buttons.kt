package com.example.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.ui.theme.AppColors
import com.example.app.ui.theme.AppShapes
import com.example.app.ui.theme.Spacing

/**
 * Material3 Primary Button - 주요 액션에 사용
 * Material3 스타일의 Filled Button
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.primary
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(Spacing.buttonHeight),
        enabled = enabled,
        shape = AppShapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.38f),
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Material3 Outlined Button - 보조 액션에 사용
 * Material3 스타일의 Outlined Button
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = MaterialTheme.colorScheme.primary
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(Spacing.buttonHeight),
        enabled = enabled,
        shape = AppShapes.button,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = textColor
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 1.dp
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Material3 FilledTonalButton - 강조가 필요한 보조 액션
 * Material3의 Filled Tonal Button 스타일
 */
@Composable
fun FilledTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(Spacing.buttonHeight),
        enabled = enabled,
        shape = AppShapes.button,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Success Button - 성공/완료 액션에 사용
 */
@Composable
fun SuccessButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    PrimaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        backgroundColor = AppColors.Success  // Success 색상 사용 (초록)
    )
}

/**
 * Gray Button - 취소/보조 액션에 사용
 */
@Composable
fun GrayButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    PrimaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        backgroundColor = AppColors.Gray
    )
}

/**
 * Toggle Button - 선택 가능한 버튼 (성별, 관심분야 등)
 * Row 내부에서만 사용 가능합니다 (weight 사용을 위해)
 */
@Composable
fun RowScope.ToggleButton(
    text: String,
    onClick: () -> Unit,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    if (isSelected) {
        Button(
            onClick = onClick,
            modifier = modifier
                .weight(1f)
                .height(Spacing.buttonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.LightBlue,  // 라이트 블루 (메인 컬러)
                contentColor = Color.White
            )
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
                .weight(1f)
                .height(Spacing.buttonHeight),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = AppColors.LightBlue  // 라이트 블루 (메인 컬러)
            ),
            border = androidx.compose.foundation.BorderStroke(2.dp, AppColors.LightBlue)  // 라이트 블루 테두리
        ) {
            Text(
                text = text,
                color = AppColors.LightBlue,  // 라이트 블루 (메인 컬러)
                fontSize = 16.sp
            )
        }
    }
}

/**
 * Square Button - 정사각형 아이콘 버튼 (재발송 등에 사용)
 * 작은 액션 버튼에 적합
 */
@Composable
fun SquareButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = AppColors.LightBlue,
    textColor: Color = Color.White,
    size: androidx.compose.ui.unit.Dp = 56.dp
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .size(size),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.4f),
            contentColor = textColor,
            disabledContentColor = textColor.copy(alpha = 0.6f)
        ),
        shape = AppShapes.buttonSmall
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

