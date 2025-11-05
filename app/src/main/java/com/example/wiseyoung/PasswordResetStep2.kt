package com.example.wiseyoung

import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun PasswordResetStep2(onComplete: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isMatch by remember { mutableStateOf(false) }

    val handleConfirmPasswordChange = { value: String ->
        confirmPassword = value
        isMatch = password == confirmPassword
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 타이틀
        Text("재설정 비밀번호", style = MaterialTheme.typography.h6)

        Spacer(modifier = Modifier.height(24.dp))

        // 비밀번호 입력 필드
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("비밀번호") },
            placeholder = { Text("8자리 이상, 특수문자 포함") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 비밀번호 확인 입력 필드
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { handleConfirmPasswordChange(it) },
            label = { Text("비밀번호 확인") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        // 비밀번호 일치 여부 표시
        if (isMatch && confirmPassword.isNotEmpty()) {
            Text("비밀번호가 일치합니다!", color = Color.Green)
        } else if (confirmPassword.isNotEmpty()) {
            Text("비밀번호가 일치하지 않습니다", color = Color.Red)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 완료 버튼
        Button(
            onClick = onComplete,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("완료", color = Color.White)
        }
    }
}
