package com.example.app.data

import com.google.firebase.auth.FirebaseAuth
import java.util.Date

/**
 * FirestoreService 사용 예제
 * 
 * 이 파일은 참고용 예제입니다. 실제 사용 시에는 각 Activity나 ViewModel에서 호출하세요.
 */
object FirestoreUsageExample {
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * 예제 1: 현재 로그인한 사용자의 정보 읽기
     */
    fun loadCurrentUserInfo() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // 로그인되지 않음
            return
        }
        
        val userId = currentUser.uid
        
        // User 정보만 읽기
        FirestoreService.getUser(
            userId = userId,
            onSuccess = { user ->
                if (user != null) {
                    println("이메일: ${user.email}")
                    println("로그인 타입: ${user.loginType}")
                    println("앱 버전: ${user.appVersion}")
                } else {
                    println("사용자 정보가 없습니다.")
                }
            },
            onFailure = { exception ->
                println("에러: ${exception.message}")
            }
        )
        
        // UserProfile 정보만 읽기
        FirestoreService.getUserProfile(
            userId = userId,
            onSuccess = { profile ->
                if (profile != null) {
                    println("성별: ${profile.gender}")
                    println("지역: ${profile.region}")
                    println("학력: ${profile.education}")
                } else {
                    println("프로필 정보가 없습니다.")
                }
            },
            onFailure = { exception ->
                println("에러: ${exception.message}")
            }
        )
        
        // User와 UserProfile을 함께 읽기
        FirestoreService.getUserWithProfile(
            userId = userId,
            onSuccess = { user, profile ->
                println("사용자: ${user?.email}")
                println("프로필: ${profile?.gender}")
            },
            onFailure = { exception ->
                println("에러: ${exception.message}")
            }
        )
    }
    
    /**
     * 예제 2: User 정보 저장하기
     */
    fun saveUserExample() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid
        
        val user = FirestoreService.User(
            userId = userId,
            email = currentUser.email ?: "",
            emailVerified = currentUser.isEmailVerified,
            loginType = "GOOGLE", // 또는 "EMAIL"
            osType = "ANDROID",
            appVersion = "1.0",
            deviceId = "device123",
            createdAt = Date()
        )
        
        FirestoreService.saveUser(
            user = user,
            onSuccess = {
                println("사용자 정보 저장 성공")
            },
            onFailure = { exception ->
                println("저장 실패: ${exception.message}")
            }
        )
    }
    
    /**
     * 예제 3: UserProfile 정보 저장하기
     */
    fun saveUserProfileExample() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid
        
        val profile = FirestoreService.UserProfile(
            userId = userId,
            birthYear = "1999-01-01", // "yyyy-MM-dd" 형식
            gender = "male", // 또는 "female"
            region = "서울",
            education = "대학교 재학",
            jobStatus = "학생"
        )
        
        FirestoreService.saveUserProfile(
            profile = profile,
            onSuccess = {
                println("프로필 저장 성공")
            },
            onFailure = { exception ->
                println("저장 실패: ${exception.message}")
            }
        )
    }
}
















