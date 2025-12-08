package com.example.app.network

import com.wiseyoung.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    // Base URL 설정
    // ⚠️ 로컬 개발용 (에뮬레이터):
    private const val BASE_URL = "http://127.0.0.1:8080/" // 로컬 개발용
    
    // 실제 기기 테스트: PC의 로컬 IP 주소 사용
    // private const val BASE_URL = "http://192.168.39.204:8080/" // 실제 기기 테스트용 (PC의 로컬 IP)
    
    // 배포 시:
    // private const val BASE_URL = "http://210.104.76.139:8080/" // 서버 배포용
    
    // BuildConfig 자동 전환 (나중에 사용 가능):
    // private const val BASE_URL = BuildConfig.BASE_URL
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val apiService: ApiService = retrofit.create(ApiService::class.java)
}

