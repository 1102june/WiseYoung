package com.wiseyoung.pro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wiseyoung.pro.ui.theme.ThemeWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import java.nio.file.WatchEvent


class AuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeWrapper {
                AuthScreen(
                    onLogin = {
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                    },
                    onRegister = {
                        val intent = Intent(this, RegisterActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun AuthScreen(onLogin: () -> Unit, onRegister: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // WiseYoung Logo
        Image(
            painter = painterResource(id = R.drawable.wy_logo),
            contentDescription = "WY Logo",
            modifier = Modifier.size(180.dp)  // 100.dp -> 180.dp로 크게 변경
        )

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "슬기로운 청년생활을 위한 첫걸음\n간편한 가입으로 문을 열어주세요!",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        //Local 로그인만 남기기
        Button(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor =  Color(0xFF59ABF7))
        ) {
            Text("구글계정으로 시작",color = Color.White)
        }
    }
}