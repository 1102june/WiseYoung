package com.wiseyoung.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

const val POLICY_DATA_SOURCE_NOTICE_MESSAGE =
    "📋 본 서비스는 공공데이터를 기반으로 맞춤 청년정책 정보를 제공합니다.\n\n" +
        "⚠️ 원본 데이터의 누락이나 업데이트 지연으로 인해, 일부 정책의 신청 링크 및 상세 조건 확인이 어려울 수 있습니다.\n\n" +
        "🔗 정확한 내용은 주관 기관의 공식 공고를 한 번 더 확인해 주세요!"

const val HOUSING_DATA_SOURCE_NOTICE_MESSAGE =
    "🏠 본 서비스는 임대주택 관련 공공데이터를 기반으로 정보를 제공합니다.\n\n" +
        "⚠️ 원본 데이터에 주소·좌표가 누락된 단지는 지도에 표시되지 않거나 상세 공고 연결이 어려울 수 있습니다.\n\n" +
        "🔗 정확한 공급 정보는 주관 기관의 공식 공고를 한 번 더 확인해 주세요!"

private const val NOTICE_CLOSING_TEXT = "감사합니다.\u00A0🙏"

@Composable
fun DataSourceNoticeDialog(
    message: String,
    onConfirm: (hideForOneDay: Boolean) -> Unit
) {
    var hideForOneDay by remember { mutableStateOf(false) }
    val primaryBlue = Color(0xFF59ABF7)

    Dialog(
        onDismissRequest = { onConfirm(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 24.dp, vertical = 22.dp)
            ) {
                Text(
                    text = "📌 알림",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF1A1A1A)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = message,
                    fontSize = 16.sp,
                    lineHeight = 26.sp,
                    color = Color(0xFF444444)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = NOTICE_CLOSING_TEXT,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { hideForOneDay = !hideForOneDay }
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = hideForOneDay,
                            onCheckedChange = { hideForOneDay = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = primaryBlue,
                                uncheckedColor = Color(0xFFBDBDBD),
                                checkmarkColor = Color.White
                            )
                        )
                        Text(
                            text = "오늘 하루 동안 보지 않기",
                            fontSize = 13.sp,
                            color = Color(0xFF666666)
                        )
                    }

                    Button(
                        onClick = { onConfirm(hideForOneDay) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                        contentPadding = ButtonDefaults.ContentPadding
                    ) {
                        Text("확인", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
