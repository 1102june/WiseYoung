plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
}

buildscript {
    repositories {
        google()  // Google 저장소 추가
        mavenCentral()  // Maven 저장소 추가
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.25")  // Kotlin 최신 버전
        classpath("com.google.gms:google-services:4.4.4")  // Firebase 플러그인
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
