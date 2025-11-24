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
package com.kakao.sdk.sample.common.internal

import com.kakao.sdk.auth.model.IdentifyItem
import com.kakao.sdk.auth.model.Prompt

data class DialogResult(
    val prompts: List<Prompt>,
    val loginHints: String?,
    val scopes: List<String>,
    val state: String?,
    val nonce: String?,
    val settleId: String?,
    val identifyItems: List<IdentifyItem>,
    val channelPublicIds: List<String>,
    val serviceTerms: List<String>,
)
