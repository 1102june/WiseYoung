package com.example.app.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    // Base URL 설정
    // 에뮬레이터: 10.0.2.2
    // 실제 기기: PC의 로컬 IP 주소 (예: 192.168.0.100)
    //private const val BASE_URL = "http://10.0.2.2:8080/"  // 에뮬레이터용
     private const val BASE_URL = "http://127.0.0.1:8080/" // 실제 기기용 (PC IP 주소로 변경 필요)
    
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

