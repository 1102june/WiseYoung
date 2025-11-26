package com.example.app.network

import com.example.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    // ========== 인증 ==========
    
    /**
     * 로그인
     * POST /auth/login
     * 백엔드에서 String을 반환 (LOGIN_SUCCESS, USER_NOT_FOUND 등)
     */
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<String>

    /**
     * 회원가입
     * POST /auth/register
     * 백엔드에서 String을 반환
     */
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<String>
    
    /**
     * 프로필 저장/업데이트
     * POST /auth/profile
     */
    @POST("auth/profile")
    suspend fun saveProfile(@Body request: ProfileRequest): Response<ApiResponse<String>>
    
    /**
     * FCM Push Token 저장/업데이트
     * POST /auth/push-token
     */
    @POST("auth/push-token")
    suspend fun savePushToken(@Body request: PushTokenRequest): Response<ApiResponse<String>>
    
    // ========== OTP (이메일 인증) ==========
    
    /**
     * 이메일 중복 확인
     * GET /auth/otp/email/check
     */
    @GET("auth/otp/email/check")
    suspend fun checkEmailDuplicate(
        @Query("email") email: String
    ): Response<ApiResponse<Boolean>>
    
    /**
     * 인증번호 발송
     * POST /auth/otp/send
     */
    @POST("auth/otp/send")
    suspend fun sendOtp(@Body request: OtpRequest): Response<ApiResponse<String>>
    
    /**
     * 인증번호 검증
     * POST /auth/otp/verify
     */
    @POST("auth/otp/verify")
    suspend fun verifyOtp(@Body request: OtpRequest): Response<ApiResponse<String>>
    
    /**
     * 이메일 인증 여부 확인
     * GET /auth/otp/status
     */
    @GET("auth/otp/status")
    suspend fun checkEmailVerified(
        @Query("email") email: String
    ): Response<ApiResponse<Boolean>>
    
    // ========== 챗봇 ==========
    
    /**
     * AI 챗봇 질의응답
     * POST /api/chat
     */
    @POST("api/chat")
    suspend fun chat(
        @Header("X-User-Id") userId: String? = null,
        @Body request: ChatRequest
    ): Response<ApiResponse<ChatResponse>>
    
    // ========== 메인 페이지 ==========
    
    /**
     * 메인 페이지 데이터 조회
     * GET /api/main
     */
    @GET("api/main")
    suspend fun getMainPage(
        @Header("X-User-Id") userId: String
    ): Response<ApiResponse<MainPageResponse>>
    
    // ========== 사용자 활동 로그 ==========
    
    /**
     * 사용자 활동 로그 저장
     * POST /api/activity
     */
    @POST("api/activity")
    suspend fun logActivity(
        @Header("X-User-Id") userId: String? = null,
        @Body request: UserActivityRequest
    ): Response<ApiResponse<Unit>>
    
    // ========== 정책 ==========
    
    /**
     * 활성 정책 목록 조회
     * GET /api/policy/active
     */
    @GET("api/policy/active")
    suspend fun getActivePolicies(
        @Query("userId") userId: String = "test-user"
    ): Response<ApiResponse<List<PolicyResponse>>>
    
    /**
     * 전체 정책 목록 조회
     * GET /api/policy/all
     */
    @GET("api/policy/all")
    suspend fun getAllPolicies(
        @Query("userId") userId: String = "test-user"
    ): Response<ApiResponse<List<PolicyResponse>>>
    
    /**
     * 정책 상세 조회
     * GET /api/policy/{policyId}
     */
    @GET("api/policy/{policyId}")
    suspend fun getPolicyById(
        @Path("policyId") policyId: String,
        @Query("userId") userId: String = "test-user"
    ): Response<ApiResponse<PolicyResponse>>
    
    // ========== 주택 ==========
    
    /**
     * 활성 주택 목록 조회
     * GET /api/housing/active
     */
    @GET("api/housing/active")
    suspend fun getActiveHousing(
        @Header("X-User-Id") userId: String? = null,
        @Query("userIdParam") userIdParam: String? = null
    ): Response<ApiResponse<List<HousingResponse>>>
    
    /**
     * 추천 주택 목록 조회 (위치 기반)
     * GET /api/housing/recommended
     */
    @GET("api/housing/recommended")
    suspend fun getRecommendedHousing(
        @Header("X-User-Id") userId: String? = null,
        @Query("userIdParam") userIdParam: String? = null,
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
        @Query("radius") radius: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<ApiResponse<List<HousingResponse>>>
    
    /**
     * 주택 상세 조회
     * GET /api/housing/{housingId}
     */
    @GET("api/housing/{housingId}")
    suspend fun getHousingById(
        @Path("housingId") housingId: String,
        @Header("X-User-Id") userId: String? = null,
        @Query("userIdParam") userIdParam: String? = null,
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null
    ): Response<ApiResponse<HousingResponse>>
    
    // ========== AI 추천 ==========
    
    /**
     * AI 추천 생성
     * POST /api/ai/recommendations/generate
     */
    @POST("api/ai/recommendations/generate")
    suspend fun generateRecommendations(
        @Header("X-User-Id") userId: String,
        @Query("maxRecommendations") maxRecommendations: Int = 10
    ): Response<ApiResponse<Unit>>
}

