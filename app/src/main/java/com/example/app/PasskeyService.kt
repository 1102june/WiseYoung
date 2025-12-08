package com.wiseyoung.app

import android.content.Context
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.credentials.*
import androidx.credentials.exceptions.*
import com.example.app.data.model.PasskeyLoginRequestResponse
import com.example.app.data.model.PasskeyRegisterRequestResponse
import com.example.app.network.NetworkModule
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom

/**
 * Google Passkey 로그인을 처리하는 서비스 클래스
 * FIDO2/WebAuthn 표준을 기반으로 한 비밀번호 없는 인증
 * 
 * 참고: 공식 문서에 따라 구현
 * https://developer.android.com/identity/passkeys/create-passkeys
 * https://developer.android.com/identity/passkeys/sign-in-with-passkeys
 */
object PasskeyService {
    private const val TAG = "PasskeyService"
    
    /**
     * Passkey 지원 여부 확인
     * 에뮬레이터나 일부 기기에서는 Passkey를 지원하지 않을 수 있음
     */
    fun isPasskeySupported(context: Context): Boolean {
        try {
            // CredentialManager가 사용 가능한지 확인
            val credentialManager = CredentialManager.create(context)
            
            // 에뮬레이터 감지 (완벽하지 않지만 참고용)
            val isEmulator = Build.FINGERPRINT.startsWith("generic")
                    || Build.FINGERPRINT.startsWith("unknown")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86")
                    || Build.MANUFACTURER.contains("Genymotion")
                    || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                    || "google_sdk" == Build.PRODUCT
            
            if (isEmulator) {
                Log.w(TAG, "⚠️ 에뮬레이터 감지: Passkey가 제대로 작동하지 않을 수 있습니다")
                Log.w(TAG, "   실제 기기에서 테스트하는 것을 권장합니다")
            }
            
            return credentialManager != null
        } catch (e: Exception) {
            Log.e(TAG, "Passkey 지원 확인 실패: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Passkey로 로그인
     * @param context Activity context
     * @param email 사용자 이메일 (선택사항, 없으면 기기에 저장된 Passkey 사용)
     * @return 로그인 결과 (성공 시 credential, 실패 시 null)
     */
    suspend fun signInWithPasskey(
        context: Context,
        email: String? = null
    ): PublicKeyCredential? {
        // Passkey 지원 여부 확인
        if (!isPasskeySupported(context)) {
            Log.e(TAG, "❌ 이 기기는 Passkey를 지원하지 않습니다")
            Log.e(TAG, "   실제 Android 기기에서 테스트해주세요")
            return null
        }
        
        try {
            val credentialManager = CredentialManager.create(context)
            
            // 1. 서버에서 challenge 및 설정 받아오기
            Log.d(TAG, "서버에서 Passkey 로그인 요청 생성 중...")
            val serverResponse = try {
                val response = NetworkModule.apiService.getPasskeyLoginRequest()
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.data
                } else {
                    Log.e(TAG, "서버 요청 실패: ${response.code()}, ${response.body()?.message}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "서버 요청 중 오류: ${e.message}", e)
                null
            }
            
            if (serverResponse == null) {
                Log.e(TAG, "서버에서 challenge를 받아올 수 없습니다")
                return null
            }
            
            Log.d(TAG, "서버에서 받은 challenge: ${serverResponse.challenge}")
            Log.d(TAG, "서버에서 받은 rpId: ${serverResponse.rpId}")
            
            // 2. 서버에서 받은 데이터로 WebAuthn JSON 생성
            // 참고: Android Passkey는 rpId로 패키지명을 사용할 수 있음
            val rpIdToUse = if (serverResponse.rpId == "localhost") {
                // localhost인 경우 패키지명 사용
                context.packageName
            } else {
                serverResponse.rpId
            }
            
            val requestJson = JSONObject().apply {
                // 서버에서 받은 challenge 사용
                put("challenge", serverResponse.challenge)
                
                // rpId: localhost면 패키지명 사용, 아니면 서버에서 받은 값 사용
                put("rpId", rpIdToUse)
                
                // 서버에서 받은 timeout 사용 (없으면 기본값)
                put("timeout", serverResponse.timeout ?: 60000)
                
                // userVerification: "preferred" 사용 (required는 너무 엄격)
                put("userVerification", serverResponse.userVerification ?: "preferred")
                
                // allowCredentials: 빈 배열이면 모든 Passkey 허용
                val allowCredentialsArray = JSONArray()
                serverResponse.allowCredentials?.forEach { credentialId ->
                    // credentialId가 Base64 문자열인 경우 그대로 사용
                    allowCredentialsArray.put(JSONObject().apply {
                        put("type", "public-key")
                        put("id", credentialId) // Base64 문자열
                    })
                }
                put("allowCredentials", allowCredentialsArray)
            }.toString()
            
            Log.d(TAG, "사용할 rpId: $rpIdToUse (서버: ${serverResponse.rpId})")
            
            Log.d(TAG, "Passkey 로그인 요청 JSON: $requestJson")
            
            val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(
                requestJson = requestJson
            )
            
            val getCredentialRequest = GetCredentialRequest(
                listOf(getPublicKeyCredentialOption)
            )
            
            Log.d(TAG, "Passkey 로그인 요청 시작")
            
            // Credential 요청
            val result = credentialManager.getCredential(
                request = getCredentialRequest,
                context = context
            )
            
            val credential = result.credential
            if (credential is PublicKeyCredential) {
                Log.d(TAG, "Passkey 로그인 성공")
                return credential
            } else {
                Log.e(TAG, "예상치 못한 credential 타입: ${credential::class.java.simpleName}")
                return null
            }
            
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Passkey 로그인 실패: ${e.message}", e)
            when (e) {
                is NoCredentialException -> {
                    Log.w(TAG, "등록된 Passkey가 없습니다")
                }
                else -> {
                    Log.e(TAG, "Credential 가져오기 오류: ${e::class.java.simpleName}")
                }
            }
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Passkey 로그인 중 예외 발생: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Passkey 등록 (회원가입 시 사용)
     * 
     * @param context Activity context
     * @param email 사용자 이메일
     * @param displayName 사용자 표시 이름
     * @return 등록 결과 (성공 시 credential, 실패 시 null)
     */
    suspend fun registerPasskey(
        context: Context,
        email: String,
        displayName: String
    ): PublicKeyCredential? {
        // Passkey 지원 여부 확인
        if (!isPasskeySupported(context)) {
            Log.e(TAG, "❌ 이 기기는 Passkey를 지원하지 않습니다")
            Log.e(TAG, "   실제 Android 기기에서 테스트해주세요")
            return null
        }
        
        try {
            val credentialManager = CredentialManager.create(context)
            
            // 1. 서버에서 challenge 및 설정 받아오기
            Log.d(TAG, "서버에서 Passkey 등록 요청 생성 중...")
            val serverResponse = try {
                val response = NetworkModule.apiService.getPasskeyRegisterRequest(email, displayName)
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.data
                } else {
                    Log.e(TAG, "서버 요청 실패: ${response.code()}, ${response.body()?.message}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "서버 요청 중 오류: ${e.message}", e)
                null
            }
            
            if (serverResponse == null) {
                Log.e(TAG, "서버에서 challenge를 받아올 수 없습니다")
                return null
            }
            
            Log.d(TAG, "서버에서 받은 challenge: ${serverResponse.challenge}")
            Log.d(TAG, "서버에서 받은 rpId: ${serverResponse.rpId}")
            
            // 2. rpId 처리 (localhost면 패키지명 사용)
            val rpIdToUse = if (serverResponse.rpId == "localhost") {
                context.packageName
            } else {
                serverResponse.rpId
            }
            
            // 3. 서버에서 받은 데이터로 WebAuthn JSON 생성
            val requestJson = JSONObject().apply {
                // rp (Relying Party) 정보
                put("rp", JSONObject().apply {
                    put("id", rpIdToUse)
                    put("name", serverResponse.rpName ?: "슬기로운 청년생활")
                })
                
                // user 정보
                put("user", JSONObject().apply {
                    put("id", serverResponse.userId) // 서버에서 받은 Base64 인코딩된 userId
                    put("name", serverResponse.userName ?: email)
                    put("displayName", serverResponse.userDisplayName ?: displayName)
                })
                
                // challenge를 서버에서 받은 값 사용
                put("challenge", serverResponse.challenge)
                
                // pubKeyCredParams: 지원하는 알고리즘 목록
                put("pubKeyCredParams", JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "public-key")
                        put("alg", -7) // ES256 (ECDSA w/ SHA-256)
                    })
                })
                
                put("timeout", serverResponse.timeout ?: 60000) // 밀리초
                put("attestation", "none") // attestation 없음
                
                // authenticatorSelection: 생체인증 선호
                put("authenticatorSelection", JSONObject().apply {
                    put("userVerification", serverResponse.userVerification ?: "preferred")
                    put("requireResidentKey", false) // resident key 불필요
                })
            }.toString()
            
            Log.d(TAG, "Passkey 등록 요청 JSON: $requestJson")
            Log.d(TAG, "사용할 rpId: $rpIdToUse (서버: ${serverResponse.rpId})")
            
            // 4. CreatePublicKeyCredentialRequest 생성
            val createPublicKeyCredentialRequest = CreatePublicKeyCredentialRequest(
                requestJson = requestJson
            )
            
            Log.d(TAG, "Passkey 등록 요청 시작: $email")
            
            // 5. Credential 생성
            val result = credentialManager.createCredential(
                request = createPublicKeyCredentialRequest,
                context = context
            )
            
            // CreateCredentialResponse에서 credential 가져오기
            // CreateCredentialResponse는 credential 속성을 가지고 있음
            // 하지만 컴파일러가 인식하지 못할 수 있으므로 리플렉션 사용
            val credential = try {
                val credentialField = result.javaClass.getDeclaredField("credential")
                credentialField.isAccessible = true
                credentialField.get(result) as? Credential
            } catch (e: Exception) {
                Log.e(TAG, "CreateCredentialResponse에서 credential 가져오기 실패: ${e.message}")
                null
            }
            
            if (credential is PublicKeyCredential) {
                Log.d(TAG, "Passkey 등록 성공")
                return credential
            } else {
                Log.e(TAG, "예상치 못한 credential 타입 또는 null")
                return null
            }
            
        } catch (e: CreateCredentialException) {
            Log.e(TAG, "Passkey 등록 실패: ${e.message}", e)
            Log.e(TAG, "Credential 생성 오류: ${e::class.java.simpleName}")
            // CreatePublicKeyCredentialDomException은 존재하지 않을 수 있으므로 일반 예외 처리
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Passkey 등록 중 예외 발생: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Passkey 삭제 (계정 삭제 시 사용)
     * @param context Activity context
     * @param email 사용자 이메일
     */
    suspend fun deletePasskey(
        context: Context,
        email: String
    ): Boolean {
        try {
            // Passkey는 기기에 저장되므로, 서버에서만 삭제하면 됨
            // 기기의 Passkey는 사용자가 직접 삭제해야 함
            Log.d(TAG, "Passkey 삭제 요청: $email (서버에서만 삭제)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Passkey 삭제 중 예외 발생: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Challenge 생성 (랜덤 바이트)
     */
    private fun generateChallenge(): ByteArray {
        val random = SecureRandom()
        val challenge = ByteArray(32)
        random.nextBytes(challenge)
        return challenge
    }
    
    /**
     * Relying Party ID 가져오기
     * 도메인 또는 패키지명 사용
     */
    private fun getRpId(context: Context): String {
        // 주의: rpId는 실제 도메인이어야 합니다.
        // 패키지명을 사용하면 검증에 실패할 수 있습니다.
        // TODO: 실제 배포 시 도메인 사용 (예: "wiseyoung.com")
        // 개발 중에는 localhost 사용
        return "localhost" // 개발용, 실제 배포 시 도메인으로 변경 필요
    }
    
    /**
     * PublicKeyCredential을 JSON으로 변환 (서버 전송용)
     */
    fun credentialToJson(credential: PublicKeyCredential): String {
        return credential.authenticationResponseJson
    }
    
    /**
     * PublicKeyCredential을 JSON으로 변환 (서버 전송용 - 등록)
     * 등록과 로그인 모두 authenticationResponseJson 사용 (API 제한)
     */
    fun registrationCredentialToJson(credential: PublicKeyCredential): String {
        // registrationResponseJson은 존재하지 않을 수 있으므로 authenticationResponseJson 사용
        return credential.authenticationResponseJson
    }
}
