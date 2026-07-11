package com.wiseyoung.pro.data.model

/**
 * 회원탈퇴 요청
 * Google 재로그인으로 받은 idToken으로 본인 확인 후 탈퇴 처리
 */
data class DeleteAccountRequest(
    val idToken: String
)
