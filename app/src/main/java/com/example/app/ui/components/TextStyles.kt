package com.example.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.app.ui.theme.AppColors

/**
 * Label Text - 입력 필드 라벨에 사용
 */
@Composable
fun LabelText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = AppColors.TextPrimary,
        modifier = modifier
    )
}

/**
 * Body Text - 일반 본문 텍스트
 */
@Composable
fun BodyText(
    text: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = AppColors.TextPrimary,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        textAlign = textAlign,
        modifier = modifier
    )
}

/**
 * Secondary Text - 보조 텍스트
 */
@Composable
fun SecondaryText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.TextSecondary,
        textAlign = textAlign,
        modifier = modifier
    )
}

/**
 * Heading Text - 제목 텍스트
 */
@Composable
fun HeadingText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 18.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = AppColors.TextPrimary,
        textAlign = textAlign,
        modifier = modifier
    )
}

