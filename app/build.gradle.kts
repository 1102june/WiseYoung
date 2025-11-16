plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")  // Firebase Plugin
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.wiseyoung.app"
    compileSdk = 36 // 안정 버전

    defaultConfig {
        applicationId = "com.wiseyoung.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"  // Compose Compiler Extension 버전 맞추기
    }

    dependencies {

        // 🔹 Compose BOM (버전 자동 통일)
        implementation(platform("androidx.compose:compose-bom:2025.11.00"))

        // 🔹 Jetpack Compose 필수 패키지
        implementation("androidx.compose.ui:ui")
        implementation("androidx.compose.ui:ui-tooling-preview")
        implementation("androidx.compose.foundation:foundation")
        implementation("androidx.compose.material3:material3")
        implementation("androidx.compose.ui:ui-graphics")
        implementation("androidx.compose.ui:ui-text")

        // 🔹 AndroidX 기본 컴포넌트
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.activity.compose)

        // 🔹 Firebase BOM
        implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
        implementation("com.google.firebase:firebase-auth")
        implementation("com.google.firebase:firebase-firestore")
        implementation("com.google.firebase:firebase-analytics")
        implementation("com.google.firebase:firebase-database:22.0.1")

        // 🔹 Google 로그인
        implementation("com.google.android.gms:play-services-auth:21.4.0")

        // 🔹 OkHttp
        implementation("com.squareup.okhttp3:okhttp:4.9.3")

        // 🔹 Activity result
        implementation("androidx.activity:activity-ktx:1.11.0")

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
}
