/*
  Copyright 2024 Kakao Corp.

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

import com.kakao.sdk.common.util.KakaoJson
import com.kakao.sdk.network.origin
import com.kakao.sdk.sample.common.cert.model.CertDemoError
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

internal abstract class DemoCallback<T> : Callback<T> {
    abstract fun onComplete(model: T?, error: Throwable?)

    override fun onResponse(call: Call<T>, response: Response<T>) {
        if (!response.isSuccessful) {
            val rawError = response.errorBody()?.string() ?: ""
            val error = KakaoJson.decodeFromString<CertDemoError>(rawError)
            onComplete(null, error)
            return
        }

        onComplete(response.body(), null)
    }

    override fun onFailure(call: Call<T>, t: Throwable) {
        t.origin.also {
            onComplete(null, it)
        }
    }
}
