package com.wiseyoung.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.data.model.ChatRequest
import com.example.app.data.model.ChatResponse
import com.example.app.network.NetworkModule
import com.example.app.ui.theme.WiseYoungTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ChatActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val userId = auth.currentUser?.uid
        
        setContent {
            WiseYoungTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ChatScreen(
                        userId = userId,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

data class ChatActivityMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    userId: String?,
    modifier: Modifier = Modifier
) {
    var messages by remember { mutableStateOf<List<ChatActivityMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var conversationId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // 메시지 추가 시 스크롤
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }
    
    fun sendMessage() {
        if (inputText.isBlank() || isLoading) return
        
        val userMessage = inputText.trim()
        inputText = ""
        
        // 사용자 메시지 추가
        messages = messages + ChatActivityMessage(text = userMessage, isUser = true)
        isLoading = true
        
        // API 호출
        scope.launch {
            try {
                val request = ChatRequest(
                    message = userMessage,
                    userId = userId,
                    conversationId = conversationId
                )
                
                val response = NetworkModule.apiService.chat(userId, request)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val chatResponse = response.body()?.data
                    chatResponse?.let {
                        // 챗봇 응답 추가
                        messages = messages + ChatActivityMessage(text = it.response, isUser = false)
                        conversationId = it.conversationId
                        
                        // ActionLink가 있으면 메시지로 표시
                        if (it.actionLinks.isNotEmpty()) {
                            val linksText = it.actionLinks.joinToString("\n") { link ->
                                "📌 ${link.title} (${link.type}: ${link.id})"
                            }
                            messages = messages + ChatActivityMessage(
                                text = "관련 정보:\n$linksText",
                                isUser = false
                            )
                        }
                    }
                } else {
                    messages = messages + ChatActivityMessage(
                        text = "오류: ${response.body()?.message ?: "응답을 받을 수 없습니다."}",
                        isUser = false
                    )
                }
            } catch (e: Exception) {
                messages = messages + ChatActivityMessage(
                    text = "네트워크 오류: ${e.message}",
                    isUser = false
                )
            } finally {
                isLoading = false
            }
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 헤더
        TopAppBar(
            title = { Text("AI 챗봇") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        
        // 메시지 목록
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message = message)
            }
            
            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("응답 중...", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
        
        // 입력 필드
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("메시지를 입력하세요...") },
                enabled = !isLoading,
                singleLine = true
            )
            Button(
                onClick = { sendMessage() },
                enabled = inputText.isNotBlank() && !isLoading
            ) {
                Text("전송")
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatActivityMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (message.isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

