package com.example.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.ui.theme.AppColors
import com.example.app.ui.theme.Spacing

/**
 * Primary Button - 주요 액션에 사용
 * 기본 색상: 라이트 블루 (메인 컬러)
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = AppColors.LightBlue  // 라이트 블루 (메인 컬러)
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(Spacing.buttonHeight),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.4f),
            contentColor = Color.White,
            disabledContentColor = Color.White.copy(alpha = 0.6f)
        )
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Secondary Button - 보조 액션에 사용
 * 기본 색상: 라이트 블루 (메인 컬러)
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = AppColors.LightBlue,  // 라이트 블루 (메인 컬러)
    borderColor: Color = AppColors.LightBlue  // 라이트 블루 (메인 컬러)
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(Spacing.buttonHeight),
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = textColor
        ),
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
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

