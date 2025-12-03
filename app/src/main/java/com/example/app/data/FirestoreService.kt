package com.example.app.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Firestore 데이터베이스 서비스
 * MariaDB의 user와 user_profile 테이블 구조를 Firestore에 매핑
 */
object FirestoreService {
    private val db = FirebaseFirestore.getInstance()
    
    // 컬렉션 이름
    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_USER_PROFILES = "user_profiles"
    
    /**
     * User 데이터 모델 (MariaDB user 테이블과 동일한 구조)
     */
    data class User(
        val userId: String = "", // Firebase UID
        val email: String = "",
        val emailVerified: Boolean = false,
        val passwordHash: String? = null,
        val loginType: String? = null, // "GOOGLE", "EMAIL" 등
        val osType: String? = null, // "ANDROID", "IOS" 등
        val appVersion: String? = null,
        val deviceId: String? = null,
        val pushToken: String? = null,
        val createdAt: Date? = null
    )
    
    /**
     * UserProfile 데이터 모델 (MariaDB user_profile 테이블과 동일한 구조)
     */
    data class UserProfile(
        val userId: String = "", // Firebase UID (user와 1:1 관계)
        val birthYear: String? = null, // "1999-01-01" 형식의 문자열
        val nickname: String? = null, // 닉네임
        val gender: String? = null, // "male", "female"
        val region: String? = null, // "서울", "경기" 등
        val education: String? = null, // "대학교 재학" 등
        val jobStatus: String? = null // "학생", "직장인" 등
    )
    
    /**
     * User 정보를 Firestore에 저장/업데이트
     */
    fun saveUser(
        user: User,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val userMap = hashMapOf(
            "userId" to user.userId,
            "email" to user.email,
            "emailVerified" to user.emailVerified,
            "passwordHash" to (user.passwordHash ?: ""),
            "loginType" to (user.loginType ?: ""),
            "osType" to (user.osType ?: ""),
            "appVersion" to (user.appVersion ?: ""),
            "deviceId" to (user.deviceId ?: ""),
            "pushToken" to (user.pushToken ?: ""),
            "createdAt" to (user.createdAt ?: Date())
        )
        
        db.collection(COLLECTION_USERS)
            .document(user.userId)
            .set(userMap, SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
    
    /**
     * User 정보를 Firestore에서 읽기
     */
    fun getUser(
        userId: String,
        onSuccess: (User?) -> Unit,
        onFailure: (Exception) -> Unit = {}
    ) {
        db.collection(COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = documentToUser(document)
                    onSuccess(user)
                } else {
                    onSuccess(null)
                }
            }
            .addOnFailureListener { onFailure(it) }
    }
    
    /**
     * DocumentSnapshot을 User 객체로 변환
     */
    private fun documentToUser(document: DocumentSnapshot): User {
        return User(
            userId = document.getString("userId") ?: document.id,
            email = document.getString("email") ?: "",
            emailVerified = document.getBoolean("emailVerified") ?: false,
            passwordHash = document.getString("passwordHash"),
            loginType = document.getString("loginType"),
            osType = document.getString("osType"),
            appVersion = document.getString("appVersion"),
            deviceId = document.getString("deviceId"),
            pushToken = document.getString("pushToken"),
            createdAt = document.getDate("createdAt")
        )
    }
    
    /**
     * UserProfile 정보를 Firestore에 저장/업데이트
     */
    fun saveUserProfile(
        profile: UserProfile,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val profileMap = hashMapOf(
            "userId" to profile.userId,
            "birthYear" to profile.birthYear,
            "nickname" to (profile.nickname ?: ""),
            "gender" to (profile.gender ?: ""),
            "region" to (profile.region ?: ""),
            "education" to (profile.education ?: ""),
            "jobStatus" to (profile.jobStatus ?: "")
        )
        
        db.collection(COLLECTION_USER_PROFILES)
            .document(profile.userId)
            .set(profileMap, SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
    
    /**
     * UserProfile 정보를 Firestore에서 읽기
     */
    fun getUserProfile(
        userId: String,
        onSuccess: (UserProfile?) -> Unit,
        onFailure: (Exception) -> Unit = {}
    ) {
        db.collection(COLLECTION_USER_PROFILES)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val profile = documentToUserProfile(document)
                    onSuccess(profile)
                } else {
                    onSuccess(null)
                }
            }
            .addOnFailureListener { onFailure(it) }
    }
    
    /**
     * DocumentSnapshot을 UserProfile 객체로 변환
     */
    private fun documentToUserProfile(document: DocumentSnapshot): UserProfile {
        // birthYear가 Date인 경우 문자열로 변환
        val birthYearStr = when (val birthYear = document.get("birthYear")) {
            is Date -> {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.format(birthYear)
            }
            is String -> birthYear
            else -> null
        }
        
        return UserProfile(
            userId = document.getString("userId") ?: document.id,
            birthYear = birthYearStr,
            nickname = document.getString("nickname"),
            gender = document.getString("gender"),
            region = document.getString("region"),
            education = document.getString("education"),
            jobStatus = document.getString("jobStatus")
        )
    }
    
    /**
     * User와 UserProfile을 함께 읽기
     */
    fun getUserWithProfile(
        userId: String,
        onSuccess: (User?, UserProfile?) -> Unit,
        onFailure: (Exception) -> Unit = {}
    ) {
        var user: User? = null
        var profile: UserProfile? = null
        var userLoaded = false
        var profileLoaded = false
        
        fun checkComplete() {
            if (userLoaded && profileLoaded) {
                onSuccess(user, profile)
            }
        }
        
        // User 읽기
        getUser(userId,
            onSuccess = {
                user = it
                userLoaded = true
                checkComplete()
            },
            onFailure = { onFailure(it) }
        )
        
        // UserProfile 읽기
        getUserProfile(userId,
            onSuccess = {
                profile = it
                profileLoaded = true
                checkComplete()
            },
            onFailure = { onFailure(it) }
        )
    }
}

