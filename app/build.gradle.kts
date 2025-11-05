plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services") // Firebase Plugin
}

android {
    namespace = "com.example.wiseyoung"
    compileSdk = 35 // 안정 버전

    defaultConfig {
        applicationId = "com.example.wiseyoung"
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    dependencies {
        // Jetpack Compose
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.activity.compose)
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.material3)

        // Firebase SDK (firebase-bom 사용)
        implementation (platform("com.google.firebase:firebase-bom:33.5.1"))

        // Firebase Auth (firebase-auth만 사용)
        implementation("com.google.firebase:firebase-auth")

        // Firebase Firestore
        implementation("com.google.firebase:firebase-firestore")

        // Firebase Analytics
        implementation("com.google.firebase:firebase-analytics")

        // Google Sign-In
        implementation("com.google.android.gms:play-services-auth:20.0.1")

        // Firebase Realtime Database
        implementation("com.google.firebase:firebase-database:20.0.3")

        // AppCompat
        implementation("androidx.appcompat:appcompat:1.7.1")

        // ActivityResultContracts 의존성
        implementation("androidx.activity:activity-ktx:1.11.0")

        // JetpackCompose
        implementation("androidx.compose.material3:material3")

        // 기타 의존성들
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(platform(libs.androidx.compose.bom))
        androidTestImplementation(libs.androidx.ui.test.junit4)
        debugImplementation(libs.androidx.ui.tooling)
        debugImplementation(libs.androidx.ui.test.manifest)
    }
}