/*
  Copyright 2019 Kakao Corp.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
object Versions {
    const val kotlin = "1.8.0"
    const val gradle = "8.4.2"
    const val retrofit = "2.11.0"
    const val kotlinxSerializationConverter = "1.0.0"

    const val okhttp = "4.9.2"

    const val json = "1.6.3"

    const val mockitoVersion = "4.3.0"
}

object SdkVersions {
    const val versionCode = 1064
    const val version = "2.23.0"

    const val minSdkVersion = 23
    const val compileSdkVersion = 35
    const val buildToolsVersion = "35.0.1"
    const val targetSdkVersion = 35
}

object Dependencies {
    const val core = "androidx.core:core-ktx:1.13.1"
    const val appCompat = "androidx.appcompat:appcompat:1.4.2"
    const val lifecycleProcess = "androidx.lifecycle:lifecycle-process:2.4.0"
    const val constraintLayout = "androidx.constraintlayout:constraintlayout:2.1.3"
    const val material = "com.google.android.material:material:1.5.0"
    const val viewPager2 = "androidx.viewpager2:viewpager2:1.0.0"
    const val browser = "androidx.browser:browser:1.4.0"

    const val json = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.json}"

    const val rxJava = "io.reactivex.rxjava3:rxjava:3.1.3"
    const val rxAndroid = "io.reactivex.rxjava3:rxandroid:3.0.0"
    const val rxKotlin = "io.reactivex.rxjava3:rxkotlin:3.0.1"

    const val okhttp = "com.squareup.okhttp3:okhttp:${Versions.okhttp}"
    const val retrofit = "com.squareup.retrofit2:retrofit:${Versions.retrofit}"
    const val retrofitRx = "com.squareup.retrofit2:adapter-rxjava3:${Versions.retrofit}"
    const val retrofitKotlinSerialization =
        "com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:${Versions.kotlinxSerializationConverter}"

    const val mockwebserver = "com.squareup.okhttp3:mockwebserver:${Versions.okhttp}"
    const val loggingInterceptor = "com.squareup.okhttp3:logging-interceptor:${Versions.okhttp}"

    const val mockitoCore = "org.mockito:mockito-core:${Versions.mockitoVersion}"
    const val mockitoInline = "org.mockito:mockito-inline:${Versions.mockitoVersion}"
    const val mockitoAndroid = "org.mockito:mockito-android:${Versions.mockitoVersion}"
}

object SampleDependencies {
    const val appCompat = "androidx.appcompat:appcompat:1.4.2"
    const val core = "androidx.core:core-ktx:1.8.0"
    const val constraintLayout = "androidx.constraintlayout:constraintlayout:2.1.4"
    const val material = "com.google.android.material:material:1.6.0"

    const val junit = "junit:junit:4.13.2"
    const val testRunner = "androidx.test:runner:1.5.2"
    const val espressoCore = "androidx.test.espresso:espresso-core:3.5.1"
}
