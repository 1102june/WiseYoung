package com.wiseyoung.pro.data.model

data class UserProfileResponse(
    val userId: String,
    val nickname: String?,
    val age: Int?,
    val region: String?,
    val education: String?,
    val jobStatus: String?,
    val interests: List<String> = emptyList()
)


