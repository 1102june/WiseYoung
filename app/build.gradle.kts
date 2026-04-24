plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")  // Firebase Plugin
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-kapt")  // Room 컴파일러를 위한 kapt 플러그인
}

android {
    namespace = "com.wiseyoung.pro"
    compileSdk = 36 // 안정 버전

    defaultConfig {
        applicationId = "com.wiseyoung.pro"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.0.1"  // manifest에서 AD_ID차단
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }

    buildTypes {
        debug {
            // 개발 환경: 로컬 개발용 (ADB 포트 포워딩 사용)
            buildConfigField("String", "BASE_URL", "\"http://127.0.0.1:8080/\"")
        }
        release {
            isMinifyEnabled = false
            // 배포 환경: 로컬 개발용 (테스트용)
            buildConfigField("String", "BASE_URL", "\"http://127.0.0.1:8080/\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true  // BuildConfig 사용 활성화
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"  // Compose Compiler Extension 버전 맞추기
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    // 🔹 Compose BOM (버전 자동 통일)
    implementation(platform("androidx.compose:compose-bom:2025.11.00"))

    // 🔹 Jetpack Compose 필수 패키지
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-text")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    // 🔹 AndroidX 기본 컴포넌트
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // 🔹 MultiDex (메모리 부족 문제 해결)
    implementation("androidx.multidex:multidex:2.0.1")

    // 🔹 Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-database:22.0.1")
    implementation("com.google.firebase:firebase-messaging") // FCM 알림

    // 🔹 Google 로그인
    implementation("com.google.android.gms:play-services-auth:21.4.0")
    
    // 🔹 Google Credential Manager (Passkey 지원)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    
    // 🔹 카카오맵 SDK v2 (Maven 의존성)
    // 참고: https://developers.kakao.com/
    implementation("com.kakao.maps.open:android:2.11.9")

    // 🔹 OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    
    // 🔹 Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")

    // 🔹 Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // 🔹 Activity result
    implementation("androidx.activity:activity-ktx:1.11.0")
    
    // 🔹 Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // 🔹 Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // 🔹 WorkManager (로컬 알림 스케줄링)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // 🔹 Gson (JSON 파싱)
    implementation("com.google.code.gson:gson:2.10.1")

    // 🔹 테스트
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.11.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.appcompat:appcompat:1.7.1")
}
