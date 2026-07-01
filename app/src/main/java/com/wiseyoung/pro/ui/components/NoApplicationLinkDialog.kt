package com.wiseyoung.pro.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun NoApplicationLinkDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                text = "알림",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Text(text = message, fontSize = 15.sp)
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF59ABF7))
            ) {
                Text("확인", color = Color.White)
            }
        }
    )
}

const val NO_POLICY_APPLICATION_LINK_MESSAGE = "정책정보에 신청링크가 없습니다!"
const val NO_HOUSING_APPLICATION_LINK_MESSAGE = "임대주택 정보에 신청링크가 없습니다!"
