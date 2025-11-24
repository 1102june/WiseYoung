/*
  Copyright 2023 Kakao Corp.

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
package com.kakao.sdk.sample.common.cert

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.kakao.sdk.auth.model.CertType
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.util.KakaoJson
import com.kakao.sdk.common.util.PersistentKVStore
import com.kakao.sdk.common.util.SdkLog
import com.kakao.sdk.common.util.SharedPrefsWrapper
import com.kakao.sdk.sample.common.cert.model.CertDemoError
import com.kakao.sdk.sample.common.cert.model.CertDemoResponse
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleTransformer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory

class RxCertDemoClient(context: Context) {
    private val k2220DemoApi: K2220RxDemoApi
    private val k3220DemoApi: K3220RxDemoApi
    private val appCache: PersistentKVStore

    init {
        val preferences = context.getSharedPreferences("default", Context.MODE_PRIVATE)
        val phase = preferences.getString("phase", null)
        val baseUrl = phaseUrl(phase)

        val interceptor: HttpLoggingInterceptor =
            HttpLoggingInterceptor { message -> SdkLog.i("log: $message") }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(OkHttpClient.Builder().addInterceptor(interceptor).build())
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .addConverterFactory(KakaoJson.json.asConverterFactory("application/json; charset=UTF8".toMediaType()))
            .build()
        k2220DemoApi = retrofit.create(K2220RxDemoApi::class.java)
        k3220DemoApi = retrofit.create(K3220RxDemoApi::class.java)
        appCache = SharedPrefsWrapper(KakaoSdk.applicationContextInfo.sharedPreferences)
    }

    fun getTxId(certType: CertType): String? {
        return appCache.getString("${TX_ID_KEY}.${certType.value}")
    }

    fun saveTxId(txId: String, certType: CertType) {
        appCache.putString("${TX_ID_KEY}.${certType.value}", txId).commit()
    }

    fun deleteTxId(certType: CertType) {
        appCache.remove("${TX_ID_KEY}.${certType.value}").commit()
    }

    fun demoSign(publicKey: String): Single<String> {
        val param = mapOf("public_key" to publicKey)
        return k2220DemoApi.sign(param).map { it.txId!! }.compose(handleDemoError())
    }

    fun demoSign(
        publicKey: String,
        returnUrl: String,
        validateAppKey: String,
        targetAppKey: String,
    ): Single<String> {
        val param = mapOf(
            "public_key" to publicKey,
            "return_url" to returnUrl,
            "validate_app_key" to validateAppKey,
            "target_app_key" to targetAppKey,
        )
        return k3220DemoApi.sign(param).map { it.txId!! }.compose(handleDemoError())
    }

    fun demoVerify(
        txId: String,
        appUserId: Long,
    ): Single<CertDemoResponse> {
        val params = mapOf("tx_id" to txId, "app_user_id" to appUserId.toString())
        return k2220DemoApi.verify(params).compose(handleDemoError())
    }

    fun demoVerify(
        txId: String,
        targetAppKey: String,
    ): Single<CertDemoResponse> {
        val params = mapOf("tx_id" to txId, "target_app_key" to targetAppKey)
        return k3220DemoApi.verify(params).compose(handleDemoError())
    }

    fun demoReducedSign(
        txId: String,
        data: String,
        signature: String,
        certType: CertType,
    ): Single<CertDemoResponse> {
        val params = mapOf("tx_id" to txId, "data" to data, "signature" to signature)

        return if (certType == CertType.K2220) {
            k2220DemoApi.reducedSign(params).compose(handleDemoError())
        } else {
            k3220DemoApi.reducedSign(params).compose(handleDemoError())
        }
    }

    private fun phaseUrl(phase: String?): String {
        return when (phase) {
            "cbt" -> "https://cbt-zert-mock.dev.onkakao.net"
            "sandbox" -> "https://zert-mock.sandbox.onkakao.net"
            else -> "https://zert-mock.dev.onkakao.net"
        }
    }

    private fun <T : Any> handleDemoError(): SingleTransformer<T, T> = SingleTransformer { single ->
        single.onErrorResumeNext { error ->
            if (error is HttpException) {
                val rawError = error.response()?.errorBody()?.string() ?: ""
                val certDemoError = KakaoJson.decodeFromString<CertDemoError>(rawError)
                return@onErrorResumeNext Single.error(certDemoError)
            }

            Single.error(error)
        }
    }

    companion object {
        private var INSTANCE: RxCertDemoClient? = null

        @JvmStatic
        fun instance(context: Context): RxCertDemoClient {
            return INSTANCE ?: RxCertDemoClient(context)
        }

        private const val TX_ID_KEY = "com.kakao.sdk.txId"
    }
}
