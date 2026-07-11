package com.wiseyoung.pro.data.model

data class ChatRequest(
    val message: String,
    val userId: String? = null,
    val conversationId: String? = null
)

