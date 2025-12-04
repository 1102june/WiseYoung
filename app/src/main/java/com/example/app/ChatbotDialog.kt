package com.wiseyoung.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.app.data.model.ChatRequest
import com.example.app.data.model.ChatResponse
import com.example.app.network.NetworkModule
import com.example.app.ui.theme.AppColors
import com.example.app.ui.theme.Spacing
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class ChatbotContext {
    POLICY, HOUSING, NONE
}

data class ChatMessage(
    val id: Int,
    val text: String,
    val sender: MessageSender,
    val timestamp: Date = Date()
)

enum class MessageSender {
    USER, BOT
}

data class QuickChip(
    val id: Int,
    val label: String,
    val icon: String
)

val quickChips = listOf(
    QuickChip(1, "AI 추천", "🤖"),
    QuickChip(2, "정책 검색", "🔍"),
    QuickChip(3, "임대주택", "🏠")
)

@Composable
fun ChatbotDialog(
    isOpen: Boolean,
    onClose: () -> Unit,
    context: ChatbotContext = ChatbotContext.NONE
) {
    if (!isOpen) return
    
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid
    
    Dialog(onDismissRequest = onClose) {
        ChatbotContent(
            onClose = onClose,
            context = context,
            userId = userId
        )
    }
}

@Composable
private fun ChatbotContent(
    onClose: () -> Unit,
    context: ChatbotContext,
    userId: String? = null
) {
    var messages by remember(context) {
        mutableStateOf<List<ChatMessage>>(
            listOf(
                ChatMessage(
                    id = 1,
                    text = getInitialMessage(context),
                    sender = MessageSender.BOT
                )
            )
        )
    }
    
    var inputValue by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    
    // Context 변경 시 메시지 초기화 및 자동 응답
    LaunchedEffect(context) {
        val initialMessage = ChatMessage(
            id = 1,
            text = getInitialMessage(context),
            sender = MessageSender.BOT
        )
        messages = listOf(initialMessage)
        
        // Context에 따라 자동으로 질문 전송
        when (context) {
            ChatbotContext.POLICY -> {
                delay(500)
                val userMessage = ChatMessage(
                    id = 2,
                    text = "정책 검색",
                    sender = MessageSender.USER
                )
                messages = listOf(initialMessage, userMessage)
                
                delay(800)
                val botResponse = ChatMessage(
                    id = 3,
                    text = "회원님의 관심분야인 취업, 복지, 주거 분야의 추천 정책을 안내해드릴게요!\n\n💼 취업 분야\n• 청년 일자리 도약 장려금: 중소기업 취업 시 3년간 최대 1,200만원 지원\n• 청년 취업 성공패키지: 진로설정, 직업훈련, 취업알선 및 참여수당 지급\n\n💰 복지 분야\n• 청년 복지 지원금: 저소득 청년층 대상 월 30만원 지원\n• 청년 건강검진 지원: 종합 건강검진 비용 전액 지원\n\n🏠 주거 분야\n• 청년 월세 한시 특별지원: 월 20만원 최대 12개월 지원 (마감임박!)\n• 청년 전월세 보증금 대출: 최대 1억원 저금리 대출\n\n더 자세한 정보가 필요하시면 말씀해주세요!",
                    sender = MessageSender.BOT
                )
                messages = listOf(initialMessage, userMessage, botResponse)
            }
            ChatbotContext.HOUSING -> {
                delay(500)
                val userMessage = ChatMessage(
                    id = 2,
                    text = "임대주택",
                    sender = MessageSender.USER
                )
                messages = listOf(initialMessage, userMessage)
                
                delay(800)
                val botResponse = ChatMessage(
                    id = 3,
                    text = "회원님 근처의 임대주택 정보를 찾아드릴게요. 현재 수원시 인근에 LH 임대주택 2곳, SH 임대주택 1곳이 있습니다.",
                    sender = MessageSender.BOT
                )
                messages = listOf(initialMessage, userMessage, botResponse)
            }
            else -> {}
        }
    }
    
    // 스크롤을 최하단으로
    LaunchedEffect(messages.size) {
        delay(100)
        scrollState.animateScrollTo(scrollState.maxValue)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            ChatbotHeader(onClose = onClose)
            
            // Messages
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                messages.forEach { message ->
                    MessageBubble(message = message)
                }
            }
            
            // Quick Chips - 3개 버튼 한 줄로 표시
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                quickChips.take(3).forEach { chip ->
                    QuickChipButton(
                        chip = chip,
                        onClick = {
                            handleSend(
                                chip.label,
                                messages,
                                coroutineScope,
                                userId,
                                onMessagesChange = { messages = it }
                            )
                        }
                    )
                }
            }
            
            // Input
            ChatbotInput(
                value = inputValue,
                onValueChange = { inputValue = it },
                onSend = {
                    if (inputValue.trim().isNotEmpty()) {
                        handleSend(
                            inputValue,
                            messages,
                            coroutineScope,
                            userId,
                            onMessagesChange = { messages = it }
                        )
                        inputValue = ""
                    }
                }
            )
        }
    }
}

@Composable
private fun ChatbotHeader(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF59ABF7)) // 메인 컬러로 통일
            .padding(Spacing.md),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Wisebot",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.sender == MessageSender.USER
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(16.dp))
                .then(
                    if (isUser) {
                        Modifier.background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    AppColors.LightBlue,  // 메인 컬러 #59abf7
                                    Color(0xFF6EBBFF)  // 연한 블루 계열로 통일
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    } else {
                        Modifier.background(
                            color = AppColors.Border,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                )
                .padding(Spacing.md)
        ) {
            Column {
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = if (isUser) Color.White else AppColors.TextPrimary,
                    lineHeight = 20.sp
                )
                Text(
                    text = dateFormat.format(message.timestamp),
                    fontSize = 11.sp,
                    color = if (isUser) Color.White.copy(alpha = 0.7f) else AppColors.TextTertiary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun RowScope.QuickChipButton(
    chip: QuickChip,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = AppColors.Border,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.BorderDark)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = chip.icon,
                fontSize = 12.sp
            )
            Text(
                text = chip.label,
                fontSize = 12.sp,
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun ChatbotInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("메시지를 입력하세요...", fontSize = 14.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Border,
                    unfocusedBorderColor = AppColors.Border
                ),
                shape = RoundedCornerShape(24.dp)
            )
            
            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                AppColors.LightBlue,  // 메인 컬러 #59abf7
                                Color(0xFF6EBBFF)  // 연한 블루 계열로 통일
                            )
                        ),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun getInitialMessage(context: ChatbotContext): String {
    return when (context) {
        ChatbotContext.POLICY -> {
            "안녕하세요! Wisebot입니다. 청년정책에 대해 무엇이든 물어보세요! 취업지원, 주거지원, 창업지원 등 다양한 정책을 안내해드릴게요."
        }
        ChatbotContext.HOUSING -> {
            "안녕하세요! Wisebot입니다. 임대주택에 대해 무엇이든 물어보세요! 원하시는 지역, 가격대, 면적 등을 알려주시면 맞춤 임대주택을 찾아드릴게요."
        }
        else -> {
            "안녕하세요! Wisebot입니다. 청년정책 및 주거정보에 대해 무엇이든 물어보세요!"
        }
    }
}

private fun handleSend(
    text: String,
    currentMessages: List<ChatMessage>,
    coroutineScope: CoroutineScope,
    userId: String?,
    onMessagesChange: (List<ChatMessage>) -> Unit
) {
    if (text.trim().isEmpty()) return
    
    val userMessage = ChatMessage(
        id = currentMessages.size + 1,
        text = text,
        sender = MessageSender.USER
    )
    
    val updatedMessages = currentMessages + userMessage
    onMessagesChange(updatedMessages)
    
    // Gemini API 호출
    coroutineScope.launch {
        try {
            // 대화 ID는 마지막 메시지에서 추출하거나 새로 생성
            val conversationId = null // 필요시 구현
            
            val request = ChatRequest(
                message = text,
                userId = userId,
                conversationId = conversationId
            )
            
            val response = NetworkModule.apiService.chat(userId, request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val chatResponse = response.body()?.data
                chatResponse?.let {
                    val botMessage = ChatMessage(
                        id = updatedMessages.size + 1,
                        text = it.response,
                        sender = MessageSender.BOT
                    )
                    
                    var finalMessages = updatedMessages + botMessage
                    
                    // ActionLink가 있으면 추가 메시지로 표시
                    if (it.actionLinks.isNotEmpty()) {
                        val linksText = it.actionLinks.joinToString("\n") { link ->
                            "📌 ${link.title} (${link.type}: ${link.id})"
                        }
                        val linkMessage = ChatMessage(
                            id = finalMessages.size + 1,
                            text = "관련 정보:\n$linksText",
                            sender = MessageSender.BOT
                        )
                        finalMessages = finalMessages + linkMessage
                    }
                    
                    onMessagesChange(finalMessages)
                }
            } else {
                // API 실패 시 기본 응답
                val errorMessage = ChatMessage(
                    id = updatedMessages.size + 1,
                    text = "죄송합니다. 일시적인 오류가 발생했습니다. 다시 시도해주세요.",
                    sender = MessageSender.BOT
                )
                onMessagesChange(updatedMessages + errorMessage)
            }
        } catch (e: Exception) {
            // 네트워크 오류 시 기본 응답
            val errorMessage = ChatMessage(
                id = updatedMessages.size + 1,
                text = "네트워크 연결을 확인해주세요. 잠시 후 다시 시도해주세요.",
                sender = MessageSender.BOT
            )
            onMessagesChange(updatedMessages + errorMessage)
        }
    }
}

