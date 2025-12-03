# 서버 구현 가이드: 비밀번호 BCrypt 해시화 및 JWT 토큰 관리

## 요구사항
- 비밀번호 단방향 암호화 (BCrypt)
- JWT 토큰 발급 및 만료관리

## Android 앱에서 전송하는 데이터

### 1. 회원가입 (`/auth/signup`)
```json
{
  "idToken": "Firebase ID Token",
  "password": "사용자가 입력한 평문 비밀번호"
}
```

### 2. 이메일 로그인 (`/auth/login`)
```json
{
  "idToken": "Firebase ID Token",
  "password": "사용자가 입력한 평문 비밀번호"
}
```

### 3. Google 로그인 (`/auth/login`)
```json
{
  "idToken": "Firebase ID Token"
  // password 필드 없음 (Google 로그인은 비밀번호 없음)
}
```

## 서버 구현 방법

### 1. 의존성 추가 (Spring Boot)

**build.gradle (Maven의 경우 pom.xml)**
```gradle
dependencies {
    // BCrypt
    implementation 'org.springframework.security:spring-security-crypto'
    
    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.11.5'
    implementation 'io.jsonwebtoken:jjwt-impl:0.11.5'
    implementation 'io.jsonwebtoken:jjwt-jackson:0.11.5'
}
```

### 2. 회원가입 처리 (`/auth/signup`)

```java
@PostMapping("/auth/signup")
public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
    // 1. Firebase ID Token 검증
    FirebaseToken decodedToken = FirebaseAuth.getInstance()
        .verifyIdToken(request.getIdToken());
    String uid = decodedToken.getUid();
    String email = decodedToken.getEmail();
    
    // 2. 비밀번호 BCrypt 해시화
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    String passwordHash = encoder.encode(request.getPassword());
    
    // 3. MariaDB에 저장
    User user = new User();
    user.setUserId(uid);
    user.setEmail(email);
    user.setPasswordHash(passwordHash); // BCrypt 해시 저장
    user.setLoginType(LoginType.email);
    user.setOsType(OSType.android);
    // ... 기타 필드
    
    userRepository.save(user);
    
    // 4. JWT 토큰 발급
    String jwtToken = jwtTokenProvider.generateToken(uid, email);
    
    return ResponseEntity.ok(new SignupResponse(jwtToken));
}
```

### 3. 로그인 처리 (`/auth/login`)

```java
@PostMapping("/auth/login")
public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    // 1. Firebase ID Token 검증
    FirebaseToken decodedToken = FirebaseAuth.getInstance()
        .verifyIdToken(request.getIdToken());
    String uid = decodedToken.getUid();
    String email = decodedToken.getEmail();
    
    // 2. 사용자 조회
    User user = userRepository.findByUserId(uid)
        .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    
    // 3. 비밀번호 검증 (이메일 로그인인 경우)
    if (request.getPassword() != null && !request.getPassword().isEmpty()) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("비밀번호가 일치하지 않습니다.");
        }
    }
    // Google 로그인인 경우 password가 null이므로 검증 생략
    
    // 4. JWT 토큰 발급
    String jwtToken = jwtTokenProvider.generateToken(uid, email);
    
    return ResponseEntity.ok(new LoginResponse(jwtToken));
}
```

### 4. BCrypt 설정

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 5. JWT 토큰 제공자 구현

```java
@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String secretKey;
    
    @Value("${jwt.expiration:86400000}") // 기본 24시간
    private long expiration;
    
    // JWT 토큰 생성
    public String generateToken(String userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
            .setSubject(userId)
            .claim("email", email)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, secretKey)
            .compact();
    }
    
    // JWT 토큰 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    // JWT 토큰에서 사용자 ID 추출
    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(secretKey)
            .parseClaimsJws(token)
            .getBody();
        return claims.getSubject();
    }
}
```

### 6. application.properties 설정

```properties
# JWT 설정
jwt.secret=your-secret-key-here-minimum-512-bits
jwt.expiration=86400000  # 24시간 (밀리초)
```

## MariaDB 테이블 구조

### user 테이블
```sql
CREATE TABLE user (
    user_id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),  -- BCrypt 해시 저장 (Google 로그인은 NULL)
    login_type VARCHAR(20),      -- 'email', 'google'
    os_type VARCHAR(20),         -- 'android', 'ios'
    app_version VARCHAR(50),
    device_id VARCHAR(255),
    push_token VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 보안 고려사항

1. **HTTPS 사용**: 비밀번호 평문 전송 시 반드시 HTTPS 사용
2. **BCrypt 강도**: 기본 강도(10) 사용 (더 높은 보안이 필요하면 증가 가능)
3. **JWT Secret Key**: 최소 512비트 이상의 강력한 키 사용
4. **토큰 만료 시간**: 적절한 만료 시간 설정 (기본 24시간)
5. **비밀번호 정책**: 클라이언트에서 검증하되, 서버에서도 재검증 권장

## 테스트 예시

### 회원가입 테스트
```bash
curl -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "firebase-id-token-here",
    "password": "userPassword123!"
  }'
```

### 로그인 테스트
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "firebase-id-token-here",
    "password": "userPassword123!"
  }'
```

## 참고사항

- Google 로그인 사용자는 `password_hash`가 `NULL`로 저장됩니다
- 이메일 로그인 사용자는 BCrypt 해시가 저장됩니다
- JWT 토큰은 모든 인증된 요청에 사용됩니다
- 토큰 만료 시 재발급 로직 구현 필요

