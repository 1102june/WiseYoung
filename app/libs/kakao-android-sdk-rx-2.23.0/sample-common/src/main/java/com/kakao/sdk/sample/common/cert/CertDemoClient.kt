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
import com.kakao.sdk.sample.common.cert.model.CertDemoResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

class CertDemoClient(context: Context) {
    private val k2220DemoApi: K2220DemoApi
    private val k3220DemoApi: K3220DemoApi
    private val appCache: PersistentKVStore

    init {
        val preferences = context.getSharedPreferences("default", Context.MODE_PRIVATE)
        val phase = preferences.getString("phase", null)
        val baseUrl = phaseUrl(phase)

        val loggingInterceptor =
            HttpLoggingInterceptor { message -> SdkLog.i("log: $message") }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(OkHttpClient.Builder().addInterceptor(loggingInterceptor).build())
            .addConverterFactory(KakaoJson.json.asConverterFactory("application/json; charset=UTF8".toMediaType()))
            .build()
        k2220DemoApi = retrofit.create(K2220DemoApi::class.java)
        k3220DemoApi = retrofit.create(K3220DemoApi::class.java)
        appCache = SharedPrefsWrapper(KakaoSdk.applicationContextInfo.sharedPreferences)
    }

    fun getTxId(certType: CertType): String? {
        return appCache.getString(getTxIdKey(certType))
    }

    fun saveTxId(txId: String, certType: CertType) {
        appCache.putString(getTxIdKey(certType), txId).commit()
    }

    fun deleteTxId(certType: CertType) {
        appCache.remove(getTxIdKey(certType)).commit()
    }

    fun demoSign(publicKey: String, callback: (txId: String?, error: Throwable?) -> Unit) {
        val param = mapOf("public_key" to publicKey)

        k2220DemoApi.sign(param).enqueue(object : DemoCallback<CertDemoResponse>() {
            override fun onComplete(model: CertDemoResponse?, error: Throwable?) {
                callback(model?.txId, error)
            }
        })
    }

    fun demoSign(
        publicKey: String,
        returnUrl: String,
        validateAppKey: String,
        targetAppKey: String,
        callback: (txId: String?, error: Throwable?) -> Unit,
    ) {
        val param = mapOf(
            "public_key" to publicKey,
            "return_url" to returnUrl,
            "validate_app_key" to validateAppKey,
            "target_app_key" to targetAppKey,
        )

        k3220DemoApi.sign(param).enqueue(object : DemoCallback<CertDemoResponse>() {
            override fun onComplete(model: CertDemoResponse?, error: Throwable?) {
                callback(model?.txId, error)
            }
        })
    }

    fun demoVerify(
        txId: String,
        appUserId: Long,
        callback: (response: CertDemoResponse?, error: Throwable?) -> Unit,
    ) {
        val params = mapOf("tx_id" to txId, "app_user_id" to appUserId.toString())

        k2220DemoApi.verify(params).enqueue(object : DemoCallback<CertDemoResponse>() {
            override fun onComplete(model: CertDemoResponse?, error: Throwable?) {
                callback(model, error)
            }
        })
    }

    fun demoVerify(
        txId: String,
        targetAppKey: String,
        callback: (response: CertDemoResponse?, error: Throwable?) -> Unit,
    ) {
        val params = mapOf("tx_id" to txId, "target_app_key" to targetAppKey)

        k3220DemoApi.verify(params).enqueue(object : DemoCallback<CertDemoResponse>() {
            override fun onComplete(model: CertDemoResponse?, error: Throwable?) {
                callback(model, error)
            }
        })
    }

    fun demoReducedSign(
        txId: String,
        data: String,
        signature: String,
        certType: CertType,
        callback: (response: CertDemoResponse?, error: Throwable?) -> Unit,
    ) {
        val params = mapOf("tx_id" to txId, "data" to data, "signature" to signature)

        if (certType == CertType.K2220) {
            k2220DemoApi.reducedSign(params).enqueue(object : DemoCallback<CertDemoResponse>() {
                override fun onComplete(model: CertDemoResponse?, error: Throwable?) {
                    callback(model, error)
                }
            })
        } else if (certType == CertType.K3220) {
            k3220DemoApi.reducedSign(params).enqueue(object : DemoCallback<CertDemoResponse>() {
                override fun onComplete(model: CertDemoResponse?, error: Throwable?) {
                    callback(model, error)
                }
            })
        }
    }

    private fun phaseUrl(phase: String?): String {
        return when (phase) {
            "cbt" -> "https://cbt-zert-mock.dev.onkakao.net"
            "sandbox" -> "https://zert-mock.sandbox.onkakao.net"
            else -> "https://zert-mock.dev.onkakao.net"
        }
    }

    private fun getTxIdKey(certType: CertType) = "$TX_ID_KEY.${certType.value}"

    companion object {
        private var INSTANCE: CertDemoClient? = null

        @JvmStatic
        fun instance(context: Context): CertDemoClient {
            return INSTANCE ?: CertDemoClient(context)
        }

        private const val TX_ID_KEY = "com.kakao.sdk.txId"
    }
}
