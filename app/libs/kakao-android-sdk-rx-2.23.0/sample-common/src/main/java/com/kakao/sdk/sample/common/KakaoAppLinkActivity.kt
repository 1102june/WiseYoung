/*
  Copyright 2020 Kakao Corp.

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
package com.kakao.sdk.sample.common

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * 카카오톡 공유와 카카오톡 메시지에서 앱 링크 클릭 시 실행되는 Activity 예제
 *
 *  AndroidManifest.xml 에 intent-filter 설정이 필요하므로 샘플앱의 설정을 참고합니다.
 *
 *  실행되는 URL 형태:
 *    - kakao{NATIVE_APP_KEY}://kakaolink?{executionParams}
 */
class KakaoAppLinkActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.data?.let { uri ->

            // 공유 시 전달한 파라미터
            val executionParams = mutableMapOf<String, String>()

            uri.queryParameterNames.forEach { name ->
                executionParams[name] = uri.getQueryParameter(name)!!
            }

            Toast.makeText(this, "${uri.authority}\n\n$executionParams", Toast.LENGTH_SHORT).show()
        }
    }
}
