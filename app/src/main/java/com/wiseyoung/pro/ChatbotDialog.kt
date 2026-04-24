package com.wiseyoung.pro

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import com.wiseyoung.pro.data.model.ChatRequest
import com.wiseyoung.pro.data.model.ChatResponse
import com.wiseyoung.pro.network.NetworkModule
import com.wiseyoung.pro.ui.theme.AppColors
import com.wiseyoung.pro.ui.theme.Spacing
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
    val timestamp: Date = Date(),
    val actionLink: ChatResponse.ActionLink? = null
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

    var isLoading by remember { mutableStateOf(false) }
    var inputValue by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Context 변경 시 메시지 초기화
    LaunchedEffect(context) {
        val initialMessage = ChatMessage(
            id = 1,
            text = getInitialMessage(context),
            sender = MessageSender.BOT
        )
        messages = listOf(initialMessage)
    }

    // 스크롤을 최하단으로
    LaunchedEffect(messages.size) {
        delay(100)
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(0.99f)
            .fillMaxHeight(0.95f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            ChatbotHeader(onClose = onClose)

            // Messages
            val context = LocalContext.current
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                messages.forEach { message ->
                    MessageBubble(
                        message = message,
                        onActionLinkClick = { link ->
                            // URL이 있으면 웹 브라우저로 열기
                            link.url?.let { url ->
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                context.startActivity(intent)
                            }
                        }
                    )
                }

                // 로딩 상태 표시
                if (isLoading) {
                    LoadingMessage()
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
                                onMessagesChange = { messages = it },
                                onLoadingChange = { isLoading = it }
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
                            onMessagesChange = { messages = it },
                            onLoadingChange = { isLoading = it }
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
private fun MessageBubble(
    message: ChatMessage,
    onActionLinkClick: (ChatResponse.ActionLink) -> Unit = {}
) {
    val isUser = message.sender == MessageSender.USER
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    color = if (isUser) AppColors.LightBlue else Color(0xFFE5E5E5),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = if (isUser) Color.White else AppColors.TextPrimary,
                    modifier = Modifier.fillMaxWidth()
                )

                // ActionLink가 있으면 클릭 가능한 버튼 표시
                message.actionLink?.let { link ->
                    Spacer(modifier = Modifier.height(8.dp))
                    if (link.url != null && link.url.isNotBlank()) {
                        Button(
                            onClick = { onActionLinkClick(link) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.LightBlue
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "신청하러 가기",
                                fontSize = 12.sp
                            )
                        }
                    }
                }

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
private fun LoadingMessage() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    color = AppColors.Border,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(Spacing.md)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = AppColors.LightBlue
                )
                Text(
                    text = "사용자에 맞는 정보를 찾는 중입니다...",
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary
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
        shape = RoundedCornerShape(20.dp),
        color = AppColors.Border,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.BorderDark)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = chip.icon,
                fontSize = 10.sp
            )
            Text(
                text = chip.label,
                fontSize = 10.sp,
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
                        color = AppColors.LightBlue,
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
    onMessagesChange: (List<ChatMessage>) -> Unit,
    onLoadingChange: (Boolean) -> Unit
) {
    if (text.trim().isEmpty()) return

    val userMessage = ChatMessage(
        id = currentMessages.size + 1,
        text = text,
        sender = MessageSender.USER
    )

    val updatedMessages = currentMessages + userMessage
    onMessagesChange(updatedMessages)
    onLoadingChange(true)

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

            onLoadingChange(false)

            if (response.isSuccessful && response.body()?.success == true) {
                val chatResponse = response.body()?.data
                chatResponse?.let {
                    // ActionLink가 있으면 각각 별도 메시지로 추가
                    if (it.actionLinks.isNotEmpty()) {
                        // 먼저 응답 텍스트 메시지 추가
                        val responseMessage = ChatMessage(
                            id = updatedMessages.size + 1,
                            text = it.response,
                            sender = MessageSender.BOT
                        )
                        var newMessages = updatedMessages + responseMessage
                        
                        // 각 ActionLink를 별도 메시지로 추가 (버튼 표시용)
                        it.actionLinks.forEachIndexed { index, link ->
                            val actionMessage = ChatMessage(
                                id = updatedMessages.size + 1 + index + 1,
                                text = link.title + (if (link.summary != null) "\n${link.summary}" else ""),
                                sender = MessageSender.BOT,
                                actionLink = link
                            )
                            newMessages = newMessages + actionMessage
                        }
                        onMessagesChange(newMessages)
                    } else {
                        // ActionLink가 없으면 일반 메시지만 추가
                        val botMessage = ChatMessage(
                            id = updatedMessages.size + 1,
                            text = it.response,
                            sender = MessageSender.BOT
                        )
                        onMessagesChange(updatedMessages + botMessage)
                    }
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
            onLoadingChange(false)
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


