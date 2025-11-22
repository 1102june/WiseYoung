package com.wiseyoung.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.app.ui.theme.ThemeWrapper

class PasswordResetActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeWrapper {
                PasswordResetScreen(
                    onBack = { finish() },
                    onComplete = { finish() }
                )
            }
        }
    }
}

@Composable
fun PasswordResetScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }
    var codeVerified by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val isPasswordMatch = confirmPassword.isNotEmpty() && confirmPassword == password
    val accentColor = Color(0xFF59ABF7)  // 라이트 블루 (메인 컬러)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "비밀번호 찾기", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "이메일", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("") },
                    singleLine = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = { codeSent = true },
                    border = BorderStroke(2.dp, accentColor),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
                ) {
                    Text("인증번호 발송")
                }
            }

            if (codeSent) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "인증번호를 입력하세요", style = MaterialTheme.typography.labelLarge)
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("") },
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { codeVerified = true },
                            border = BorderStroke(2.dp, accentColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
                        ) {
                            Text("확인")
                        }
                    }
                }
            }

            if (codeVerified) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "새 비밀번호", style = MaterialTheme.typography.labelLarge)
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("8자리이상, 특수문자 필수") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "비밀번호 확인", style = MaterialTheme.typography.labelLarge)
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("비밀번호 확인") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            trailingIcon = {
                                if (isPasswordMatch) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "match",
                                            tint = accentColor
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "비밀번호가 일치합니다!",
                                            color = accentColor,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        )
                    }

                    Button(
                        onClick = onComplete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text(text = "완료", color = Color.White)
                    }
                }
            }
        }
    }
}