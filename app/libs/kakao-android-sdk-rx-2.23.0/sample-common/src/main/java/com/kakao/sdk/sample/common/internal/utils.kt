package com.kakao.sdk.sample.common.internal

import com.kakao.sdk.common.model.ApiError
import com.kakao.sdk.common.model.AuthError
import com.kakao.sdk.common.model.ClientError

fun handleError(tag: String, error: Throwable?) {
    if(error == null){
        return
    }

    when (error) {
        is AuthError -> when (error.reason) {
            else -> Log.e(tag, "auth error: $error")
        }
        is ApiError -> when (error.reason) {
            else -> Log.e(tag, "api error: $error")
        }
        is ClientError -> when (error.reason) {
            else -> Log.e(tag, "client error: $error")
        }
        else -> Log.e(tag, "normal error: $error")
    }
}