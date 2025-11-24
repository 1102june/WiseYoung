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
package com.kakao.sdk.sample.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.auth.TokenManageable
import com.kakao.sdk.auth.TokenManager
import com.kakao.sdk.auth.TokenManagerProvider
import com.kakao.sdk.auth.model.CertType
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.auth.model.Prompt
import com.kakao.sdk.cert.CertApiClient
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.AuthError
import com.kakao.sdk.common.model.AuthErrorCause
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.common.model.KakaoSdkError
import com.kakao.sdk.common.util.KakaoCustomTabsClient
import com.kakao.sdk.common.util.KakaoJson
import com.kakao.sdk.friend.client.selectFriend
import com.kakao.sdk.friend.core.PickerClient
import com.kakao.sdk.friend.core.model.OpenPickerFriendRequestParams
import com.kakao.sdk.friend.core.model.ViewType
import com.kakao.sdk.navi.Constants
import com.kakao.sdk.navi.NaviClient
import com.kakao.sdk.navi.model.CoordType
import com.kakao.sdk.navi.model.Location
import com.kakao.sdk.navi.model.NaviOption
import com.kakao.sdk.sample.common.cert.CertDemoClient
import com.kakao.sdk.sample.common.cert.model.CertDemoError
import com.kakao.sdk.sample.common.internal.ApiAdapter
import com.kakao.sdk.sample.common.internal.FriendsActivity
import com.kakao.sdk.sample.common.internal.Log
import com.kakao.sdk.sample.common.internal.ParameterChoiceDialogFragment
import com.kakao.sdk.sample.common.internal.PickerItem
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.share.WebSharerClient
import com.kakao.sdk.talk.TalkApiClient
import com.kakao.sdk.talk.model.Friend
import com.kakao.sdk.talk.model.Friends
import com.kakao.sdk.talk.model.FriendsContext
import com.kakao.sdk.talk.model.Order
import com.kakao.sdk.template.model.Button
import com.kakao.sdk.template.model.CalendarTemplate
import com.kakao.sdk.template.model.Commerce
import com.kakao.sdk.template.model.CommerceTemplate
import com.kakao.sdk.template.model.Content
import com.kakao.sdk.template.model.FeedTemplate
import com.kakao.sdk.template.model.IdType
import com.kakao.sdk.template.model.ItemContent
import com.kakao.sdk.template.model.ItemInfo
import com.kakao.sdk.template.model.Link
import com.kakao.sdk.template.model.ListTemplate
import com.kakao.sdk.template.model.LocationTemplate
import com.kakao.sdk.template.model.Social
import com.kakao.sdk.template.model.TextTemplate
import com.kakao.sdk.user.UserApiClient
import com.kakao.sdk.user.model.LoginUiMode
import java.io.File
import java.io.FileOutputStream

class OpenFragment : Fragment() {

    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        Log.view = view

        viewManager = LinearLayoutManager(context)
        viewAdapter = ApiAdapter(
            listOf(
                ApiAdapter.Item.Header("User API"),
                ApiAdapter.Item.ApiItem("isKakaoTalkLoginAvailable()") {

                    // 카카오톡 설치여부 확인
                    if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                        Log.i(TAG, "카카오톡으로 로그인 가능")
                    } else {
                        Log.i(TAG, "카카오톡 미설치: 카카오계정으로 로그인 사용 권장")
                    }
                },
                ApiAdapter.Item.ApiItem("+loginWithKakaoTalk()") {
                    val dialogFragment =
                        ParameterChoiceDialogFragment.newInstance("loginWithKakaoTalk") { result ->
                            if (result == null) return@newInstance

                            // 카카오톡으로 로그인
                            UserApiClient.instance.loginWithKakaoTalk(
                                context,
                                nonce = result.nonce,
                                channelPublicIds = result.channelPublicIds,
                                serviceTerms = result.serviceTerms,
                            ) { token, error ->
                                if (error != null) {
                                    Log.e(TAG, "로그인 실패", error)
                                } else if (token != null) {
                                    Log.i(TAG, "로그인 성공 ${token.accessToken}")
                                }
                            }
                        }
                    dialogFragment.show(parentFragmentManager, "loginWithKakaoTalk")
                },
                ApiAdapter.Item.ApiItem("+certLoginWithKakaoTalk()") {
                    val settleId = bundle.getString("settleId")
                    val dialogFragment = ParameterChoiceDialogFragment.newInstance(
                        "certLoginWithKakaoTalk",
                        settleId
                    ) { result ->
                        if (result == null) return@newInstance

                        // 카카오톡으로 인증서 로그인
                        CertApiClient.instance.certLoginWithKakaoTalk(
                            context,
                            CertType.K2100,
                            prompts = result.prompts,
                            identifyItems = result.identifyItems,
                            signData = result.state,
                            nonce = result.nonce,
                            settleId = result.settleId,
                            channelPublicIds = result.channelPublicIds,
                            serviceTerms = result.serviceTerms,
                        ) { certTokenInfo, error ->
                            if (error != null) {
                                Log.e(TAG, "로그인 실패", error)
                            } else if (certTokenInfo != null) {
                                Log.i(
                                    TAG,
                                    "로그인 성공 ${certTokenInfo.token.accessToken} ${certTokenInfo.txId}"
                                )
                            }
                        }
                    }
                    dialogFragment.show(parentFragmentManager, "certLoginWithKakaoTalk")
                },
                ApiAdapter.Item.ApiItem("+loginWithKakaoAccount()") {
                    val dialogFragment =
                        ParameterChoiceDialogFragment.newInstance("loginWithKakaoAccount") { result ->
                            if (result == null) return@newInstance

                            // 카카오계정으로 로그인
                            UserApiClient.instance.loginWithKakaoAccount(
                                context,
                                prompts = result.prompts,
                                loginHint = result.loginHints,
                                nonce = result.nonce,
                                channelPublicIds = result.channelPublicIds,
                                serviceTerms = result.serviceTerms,
                            ) { token, error ->
                                if (error != null) {
                                    Log.e(TAG, "로그인 실패", error)
                                } else if (token != null) {
                                    Log.i(TAG, "로그인 성공 ${token.accessToken}")
                                }
                            }
                        }
                    dialogFragment.show(parentFragmentManager, "loginWithKakaoAccount")
                },
                ApiAdapter.Item.ApiItem("+certLoginWithKakaoAccount()") {
                    val settleId = bundle.getString("settleId")
                    val dialogFragment =
                        ParameterChoiceDialogFragment.newInstance(
                            "certLoginWithKakaoAccount",
                            settleId
                        ) { result ->
                            if (result == null) return@newInstance

                            // 카카오계정으로 인증서 로그인
                            CertApiClient.instance.certLoginWithKakaoAccount(
                                context,
                                certType = CertType.K2100,
                                prompts = result.prompts,
                                identifyItems = result.identifyItems,
                                loginHint = result.loginHints,
                                signData = result.state,
                                nonce = result.nonce,
                                settleId = result.settleId,
                                channelPublicIds = result.channelPublicIds,
                                serviceTerms = result.serviceTerms,
                            ) { certTokenInfo, error ->
                                if (error != null) {
                                    Log.e(TAG, "로그인 실패", error)
                                } else if (certTokenInfo != null) {
                                    Log.i(
                                        TAG,
                                        "로그인 성공 ${certTokenInfo.token.accessToken} ${certTokenInfo.txId}"
                                    )
                                }
                            }
                        }
                    dialogFragment.show(parentFragmentManager, "certLoginWithKakaoAccount")
                },
                ApiAdapter.Item.ApiItem("+loginWithNewScopes()") {
                    val dialogFragment =
                        ParameterChoiceDialogFragment.newInstance("loginWithNewScopes") { result ->
                            if (result == null) return@newInstance

                            // 새로운 동의항목으로 로그인
                            UserApiClient.instance.loginWithNewScopes(
                                context,
                                scopes = result.scopes,
                                nonce = result.nonce
                            ) { token, error ->
                                if (error != null) {
                                    Log.e(TAG, "로그인 실패", error)
                                } else if (token != null) {
                                    Log.i(TAG, "로그인 성공 ${token.accessToken}")
                                }
                            }
                        }
                    dialogFragment.show(parentFragmentManager, "loginWithKakaoAccount")
                },
                ApiAdapter.Item.ApiItem("loginWithKakao") {
                    UserApiClient.instance.loginWithKakao(context, uiMode= LoginUiMode.AUTO) { token, error ->
                        if (error != null) {
                            Log.e(TAG, "로그인 실패", error)
                        } else if (token != null) {
                            Log.i(TAG, "로그인 성공 ${token.accessToken}")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("loginWithKakao - Lignt") {
                    UserApiClient.instance.loginWithKakao(context, uiMode= LoginUiMode.LIGHT) { token, error ->
                        if (error != null) {
                            Log.e(TAG, "로그인 실패", error)
                        } else if (token != null) {
                            Log.i(TAG, "로그인 성공 ${token.accessToken}")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("loginWithKakao - Dark") {
                    UserApiClient.instance.loginWithKakao(context, uiMode= LoginUiMode.DARK) { token, error ->
                        if (error != null) {
                            Log.e(TAG, "로그인 실패", error)
                        } else if (token != null) {
                            Log.i(TAG, "로그인 성공 ${token.accessToken}")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("loginWithKakaoTalk()") {

                    // 카카오톡으로 로그인
                    UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                        if (error != null) {
                            Log.e(TAG, "로그인 실패", error)
                        } else if (token != null) {
                            Log.i(TAG, "로그인 성공 ${token.accessToken}")
                        }
                    }
                },

                ApiAdapter.Item.ApiItem("certLoginWithKakaoTalk()") {

                    // 카카오톡으로 인증서 로그인
                    CertApiClient.instance.certLoginWithKakaoTalk(
                        context,
                        CertType.K2100,
                        signData = "test"
                    ) { certTokenInfo, error ->
                        if (error != null) {
                            Log.e(TAG, "로그인 실패", error)
                        } else if (certTokenInfo != null) {
                            Log.i(
                                TAG,
                                "로그인 성공 ${certTokenInfo.token.accessToken} ${certTokenInfo.txId}"
                            )
                        }
                    }
                },

                ApiAdapter.Item.ApiItem("certLoginWithKakaoTalk(settleId)") {
                    val settleId = bundle.getString("settleId")

                    // 카카오톡으로 인증서 로그인
                    CertApiClient.instance.certLoginWithKakaoTalk(
                        context,
                        CertType.K2100,
                        signData = "test",
                        settleId = settleId,
                    ) { certTokenInfo, error ->
                        if (error != null) {
                            Log.e(TAG, "로그인 실패", error)
                        } else if (certTokenInfo != null) {
                            Log.i(
                                TAG,
                                "로그인 성공 ${certTokenInfo.token.accessToken} ${certTokenInfo.txId}"
                            )
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("loginWithKakaoAccount()") {
                    // 카카오계정으로 로그인
                    UserApiClient.instance.loginWithKakaoAccount(context) { token, error ->
                        if (error != null) {
                            Log.e(TAG, "로그인 실패", error)
                        } else if (token != null) {
                            Log.i(TAG, "로그인 성공 ${token.accessToken}")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("loginWithKakaoAccount(prompts:select_account)") {
                    // 카카오계정으로 로그인 - 계정 간편 로그인
                    UserApiClient.instance.loginWithKakaoAccount(
                        context,
                        prompts = listOf(Prompt.SELECT_ACCOUNT)
                    ) { token, error ->
                        if (error != null) {
                            Log.e(TAG, "로그인 실패", error)
                        } else if (token != null) {
                            Log.i(TAG, "로그인 성공 ${token.accessToken}")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("loginWithKakaoAccount(prompts:create)") {

                    // 카카오계정 가입 후 로그인
                    UserApiClient.instance.loginWithKakaoAccount(
                        context,
                        prompts = listOf(Prompt.CREATE)
                    ) { token, error ->
                        if (error != null) {
                            Log.e(TAG, "로그인 실패", error)
                        } else if (token != null) {
                            Log.i(TAG, "로그인 성공 ${token.accessToken}")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("loginWithKakaoAccount(prompts:login)") {

                    // 카카오계정으로 로그인 - 재인증
                    UserApiClient.instance.loginWithKakaoAccount(
                        context,
                        prompts = listOf(Prompt.LOGIN)
                    ) { token, error ->
                        if (error != null) {
                            Log.e(TAG, "로그인 실패", error)
                        } else if (token != null) {
                            Log.i(TAG, "로그인 성공 ${token.accessToken}")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("certLoginWithKakaoAccount()") {

                    // 카카오계정으로 인증서 로그인
                    CertApiClient.instance.certLoginWithKakaoAccount(
                        context,
                        certType = CertType.K2100,
                        signData = "test"
                    ) { certTokenInfo, error ->
                        if (error != null) {
                            Log.e(TAG, "로그인 실패", error)
                        } else if (certTokenInfo != null) {
                            Log.i(
                                TAG,
                                "로그인 성공 ${certTokenInfo.token.accessToken} ${certTokenInfo.txId}"
                            )
                        }
                    }
                },

                ApiAdapter.Item.ApiItem("certLoginWithKakaoAccount(settleId)") {
                    val settleId = bundle.getString("settleId")
                    // 카카오계정으로 인증서 로그인
                    CertApiClient.instance.certLoginWithKakaoAccount(
                        context,
                        certType = CertType.K2100,
                        signData = "test",
                        settleId = settleId,
                    ) { certTokenInfo, error ->
                        if (error != null) {
                            Log.e(TAG, "로그인 실패", error)
                        } else if (certTokenInfo != null) {
                            Log.i(
                                TAG,
                                "로그인 성공 ${certTokenInfo.token.accessToken} ${certTokenInfo.txId}"
                            )
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("Combination Login") {

                    // 로그인 조합 예제

                    // 카카오계정으로 로그인 공통 callback 구성
                    val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
                        if (error != null) {
                            Log.e(TAG, "카카오계정으로 로그인 실패", error)
                        } else if (token != null) {
                            Log.i(TAG, "카카오계정으로 로그인 성공 ${token.accessToken}")
                        }
                    }

                    // 카카오톡이 설치되어 있으면 카카오톡으로 로그인, 아니면 카카오계정으로 로그인
                    if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                        UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                            if (error != null) {
                                Log.e(TAG, "카카오톡으로 로그인 실패", error)

                                // 유저에 의해서 카카오톡으로 로그인이 취소된 경우 카카오계정으로 로그인 생략 (ex 뒤로가기)
                                if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                                    return@loginWithKakaoTalk
                                }

                                // 카카오톡에 로그인이 안되어있는 경우 카카오계정으로 로그인
                                UserApiClient.instance.loginWithKakaoAccount(
                                    context,
                                    callback = callback
                                )
                            } else if (token != null) {
                                Log.i(TAG, "카카오톡으로 로그인 성공 ${token.accessToken}")
                            }
                        }
                    } else {
                        UserApiClient.instance.loginWithKakaoAccount(
                            context,
                            callback = callback
                        )
                    }
                },
                ApiAdapter.Item.ApiItem("Combination Login (Verbose Callback)") {

                    // 로그인 조합 예제 + 상세한 에러처리 콜백

                    // 로그인 공통 callback 구성
                    val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
                        if (error != null) {
                            // 로그인 실패
                            // 아래는 로그인 요청의 대표적인 에러케이스 처리 예제입니다. 서비스에 최적화된 형태로 간소화하세요.
                            when (error) {
                                is ClientError ->
                                    when (error.reason) {
                                        ClientErrorCause.Cancelled ->
                                            Log.d(TAG, "취소됨 (back button)", error)

                                        ClientErrorCause.NotSupported ->
                                            Log.e(TAG, "지원되지 않음 (카톡 미설치)", error)

                                        else ->
                                            Log.e(TAG, "기타 클라이언트 에러", error)
                                    }

                                is AuthError ->
                                    when (error.reason) {
                                        AuthErrorCause.AccessDenied ->
                                            Log.d(TAG, "취소됨 (동의 취소)", error)

                                        AuthErrorCause.Misconfigured ->
                                            Log.e(
                                                TAG,
                                                "개발자사이트 앱 설정에 키해시를 등록하세요. 현재 값: ${KakaoSdk.keyHash}",
                                                error
                                            )

                                        else ->
                                            Log.e(TAG, "기타 인증 에러", error)
                                    }

                                else ->
                                    // 에러처리에 대한 개선사항이 필요하면 데브톡(https://devtalk.kakao.com)으로 문의해주세요.
                                    Log.e(TAG, "기타 에러 (네트워크 장애 등..)", error)
                            }
                        } else if (token != null) {
                            Log.i(TAG, "로그인 성공 ${token.accessToken}")
                        }
                    }

                    // 카카오톡이 설치되어 있으면 카카오톡으로 로그인, 아니면 카카오계정으로 로그인
                    if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                        UserApiClient.instance.loginWithKakaoTalk(context, callback = callback)
                    } else {
                        UserApiClient.instance.loginWithKakaoAccount(
                            context,
                            callback = callback
                        )
                    }
                },
                ApiAdapter.Item.ApiItem("me()") {

                    // 사용자 정보 요청 (기본)
                    UserApiClient.instance.me { user, error ->
                        if (error != null) {
                            Log.e(TAG, "사용자 정보 요청 실패", error)
                        } else if (user != null) {
                            Log.i(
                                TAG, "사용자 정보 요청 성공" +
                                        "\n회원번호: ${user.id}" +
                                        "\n이메일: ${user.kakaoAccount?.email}" +
                                        "\n닉네임: ${user.kakaoAccount?.profile?.nickname}" +
                                        "\n프로필사진: ${user.kakaoAccount?.profile?.thumbnailImageUrl}"
                            )
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("me() - new scopes") {

                    // 사용자 정보 요청 (추가 동의)

                    // 사용자가 로그인 시 제3자 정보제공에 동의하지 않은 개인정보 항목 중 어떤 정보가 반드시 필요한 시나리오에 진입한다면
                    // 다음과 같이 추가 동의를 받고 해당 정보를 획득할 수 있습니다.

                    //  * 주의: 선택 동의항목은 사용자가 거부하더라도 서비스 이용에 지장이 없어야 합니다.

                    // 추가 권한 요청 시나리오 예제
                    UserApiClient.instance.me { user, e ->
                        if (e != null) {
                            Log.e(TAG, "사용자 정보 요청 실패", e)
                        } else if (user != null) {
                            val scopes = mutableListOf<String>()

                            if (user.kakaoAccount?.emailNeedsAgreement == true) {
                                scopes.add("account_email")
                            }
                            if (user.kakaoAccount?.birthdayNeedsAgreement == true) {
                                scopes.add("birthday")
                            }
                            if (user.kakaoAccount?.birthyearNeedsAgreement == true) {
                                scopes.add("birthyear")
                            }
                            if (user.kakaoAccount?.phoneNumberNeedsAgreement == true) {
                                scopes.add("phone_number")
                            }
                            if (user.kakaoAccount?.profileNeedsAgreement == true) {
                                scopes.add("profile")
                            }
                            if (user.kakaoAccount?.ageRangeNeedsAgreement == true) {
                                scopes.add("age_range")
                            }

                            if (scopes.isNotEmpty()) {
                                Log.d(TAG, "사용자에게 추가 동의를 받아야 합니다.")

                                UserApiClient.instance.loginWithNewScopes(
                                    context,
                                    scopes
                                ) { token, err ->
                                    if (err != null) {
                                        Log.e(TAG, "사용자 추가 동의 실패", err)
                                    } else {
                                        Log.d(TAG, "allowed scopes: ${token!!.scopes}")

                                        // 사용자 정보 재요청
                                        UserApiClient.instance.me { user, error ->
                                            if (error != null) {
                                                Log.e(TAG, "사용자 정보 요청 실패", error)
                                            } else if (user != null) {
                                                Log.i(TAG, "사용자 정보 요청 성공")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("signup()") {
                    UserApiClient.instance.signup { error ->
                        if (error != null) {
                            Log.e(TAG, "signup 실패", error)
                        } else {
                            Log.i(TAG, "signup 성공")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("scopes()") {
                    UserApiClient.instance.scopes { scopeInfo, error ->
                        if (error != null) {
                            Log.e(TAG, "동의 정보 조회 실패", error)
                        } else if (scopeInfo != null) {
                            Log.i(TAG, "동의 정보 조회 성공\n 현재 가지고 있는 동의 항목 $scopeInfo")
                        }
                    }
                },
                // 특정 동의 항목 조회
                ApiAdapter.Item.ApiItem("scopes() - optional") {
                    val scopes = mutableListOf("account_email", "friends")
                    UserApiClient.instance.scopes(scopes) { scopeInfo, error ->
                        if (error != null) {
                            Log.e(TAG, "동의 정보 조회 실패", error)
                        } else if (scopeInfo != null) {
                            Log.i(TAG, "동의 정보 조회 성공\n 현재 가지고 있는 동의 항목 $scopeInfo")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("revokeScopes()") {
                    val scopes = mutableListOf("account_email", "friends")
                    UserApiClient.instance.revokeScopes(scopes) { scopeInfo, error ->
                        if (error != null) {
                            Log.e(TAG, "동의 철회 실패", error)
                        } else if (scopeInfo != null) {
                            Log.i(TAG, "동의 철회 성공\n 현재 가지고 있는 동의 항목 $scopeInfo")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("accessTokenInfo()") {

                    // 토큰 정보 보기
                    UserApiClient.instance.accessTokenInfo { tokenInfo, error ->
                        if (error != null) {
                            Log.e(TAG, "토큰 정보 보기 실패", error)
                        } else if (tokenInfo != null) {
                            Log.i(
                                TAG, "토큰 정보 보기 성공" +
                                        "\n회원번호: ${tokenInfo.id}" +
                                        "\n만료시간: ${tokenInfo.expiresIn} 초"
                            )
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("updateProfile()") {

                    // 사용자 정보 저장

                    // 변경할 내용
                    val properties = mapOf("custom_key" to "${System.currentTimeMillis()}")

                    UserApiClient.instance.updateProfile(properties) { error ->
                        if (error != null) {
                            Log.e(TAG, "사용자 정보 저장 실패", error)
                        } else {
                            Log.i(TAG, "사용자 정보 저장 성공")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("selectShippingAddress()") {
                    UserApiClient.instance.selectShippingAddress(context) { addressId, error ->
                        if (error != null) {
                            Log.i(TAG, "배송지 선택 실패 $error")
                            return@selectShippingAddress
                        }

                        Log.i(TAG, "배송지 선택 성공 $addressId")
                    }
                },
                ApiAdapter.Item.ApiItem("shippingAddresses()") {

                    // 배송지 조회 (추가 동의)
                    UserApiClient.instance.shippingAddresses { userShippingAddresses, e ->
                        if (e != null) {
                            Log.e(TAG, "배송지 조회 실패", e)
                        } else if (userShippingAddresses != null) {
                            if (userShippingAddresses.shippingAddresses != null) {
                                Log.i(
                                    TAG, "배송지 조회 성공" +
                                            "\n회원번호: ${userShippingAddresses.userId}" +
                                            "\n배송지: \n${
                                                userShippingAddresses.shippingAddresses?.joinToString(
                                                    "\n"
                                                )
                                            }"
                                )
                            } else if (userShippingAddresses.needsAgreement == false) {
                                Log.e(TAG, "사용자 계정에 배송지 없음. 꼭 필요하다면 동의항목 설정에서 수집 기능을 활성화 해보세요.")
                            } else if (userShippingAddresses.needsAgreement == true) {
                                Log.d(TAG, "사용자에게 배송지 제공 동의를 받아야 합니다.")

                                val scopes = listOf("shipping_address")

                                // 사용자에게 배송지 제공 동의 요청
                                UserApiClient.instance.loginWithNewScopes(
                                    context,
                                    scopes
                                ) { token, err ->
                                    if (err != null) {
                                        Log.e(TAG, "배송지 제공 동의 실패", err)
                                    } else if (token != null) {
                                        Log.d(TAG, "allowed scopes: ${token.scopes}")

                                        // 배송지 조회 재요청
                                        UserApiClient.instance.shippingAddresses { userShippingAddresses, error ->
                                            if (error != null) {
                                                Log.e(TAG, "배송지 조회 실패", error)
                                            } else if (userShippingAddresses != null) {
                                                Log.i(
                                                    TAG, "배송지 조회 성공" +
                                                            "\n회원번호: ${userShippingAddresses.userId}" +
                                                            "\n배송지: \n${
                                                                userShippingAddresses.shippingAddresses?.joinToString(
                                                                    "\n"
                                                                )
                                                            }"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("serviceTerms()") {

                    // 동의한 약관 조회
                    UserApiClient.instance.serviceTerms { userServiceTerms, error ->
                        if (error != null) {
                            Log.e(TAG, "동의한 약관 조회 실패", error)
                        } else if (userServiceTerms != null) {
                            Log.i(
                                TAG, "동의한 약관 조회 성공" +
                                        "\n회원번호: ${userServiceTerms.id}" +
                                        "\n동의한 약관: \n${userServiceTerms.serviceTerms?.joinToString("\n")}"
                            )
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("serviceTerms(app_service_terms)") {

                    // 전체 약관 조회
                    UserApiClient.instance.serviceTerms(result = "app_service_terms") { userServiceTerms, error ->
                        if (error != null) {
                            Log.e(TAG, "전체 약관 조회 실패", error)
                        } else if (userServiceTerms != null) {
                            Log.i(
                                TAG, "전체 약관 조회 성공" +
                                        "\n회원번호: ${userServiceTerms.id}" +
                                        "\n약관: \n${userServiceTerms.serviceTerms?.joinToString("\n")}"
                            )
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("serviceTerms(tags)") {

                    // 특정 약관 조회
                    UserApiClient.instance.serviceTerms(tags = listOf("policy")) { userServiceTerms, error ->
                        if (error != null) {
                            Log.e(TAG, "약관 조회 실패", error)
                        } else if (userServiceTerms != null) {
                            Log.i(
                                TAG, "약관 조회 성공" +
                                        "\n회원번호: ${userServiceTerms.id}" +
                                        "\n약관: \n${userServiceTerms.serviceTerms?.joinToString("\n")}"
                            )
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("revokeServiceTerms()") {

                    // 약관 철회
                    UserApiClient.instance.revokeServiceTerms(tags = listOf("revoke_test")) { userRevokedServiceTerms, error ->
                        if (error != null) {
                            Log.e(TAG, "약관 철회 실패", error)
                        } else if (userRevokedServiceTerms != null) {
                            Log.i(
                                TAG, "약관 철회 성공" +
                                        "\n회원번호: ${userRevokedServiceTerms.id}" +
                                        "\n철회한 약관: \n${
                                            userRevokedServiceTerms.revokedServiceTerms?.joinToString(
                                                "\n"
                                            )
                                        }"
                            )
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("refreshToken()") {
                    AuthApiClient.instance.refreshToken { _, error ->
                        if (error != null) {
                            Log.e(TAG, "토큰 갱신 실패", error)
                        } else {
                            Log.i(TAG, "토큰 갱신 성공")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("logout()") {

                    // 로그아웃
                    UserApiClient.instance.logout { error ->
                        if (error != null) {
                            Log.e(TAG, "로그아웃 실패. SDK에서 토큰 폐기 됨", error)
                        } else {
                            Log.i(TAG, "로그아웃 성공. SDK에서 토큰 폐기 됨")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("unlink()") {

                    // 연결 해제
                    UserApiClient.instance.unlink { error ->
                        if (error != null) {
                            Log.e(TAG, "연결 해제 실패", error)
                        } else {
                            Log.i(TAG, "연결 해제 성공. SDK에서 토큰 폐기 됨")
                        }
                    }
                },
                ApiAdapter.Item.Header("KakaoTalk API"),
                ApiAdapter.Item.ApiItem("profile()") {

                    // 카카오톡 프로필 조회
                    TalkApiClient.instance.profile { profile, error ->
                        if (error != null) {
                            Log.e(TAG, "카카오톡 프로필 조회 실패", error)
                        } else if (profile != null) {
                            Log.i(
                                TAG, "카카오톡 프로필 조회 성공" +
                                        "\n닉네임: ${profile.nickname}" +
                                        "\n프로필사진: ${profile.thumbnailUrl}" +
                                        "\n국가코드: ${profile.countryISO}"
                            )
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("sendCustomMemo()") {

                    // 커스텀 템플릿으로 나에게 보내기

                    // 메시지 템플릿 아이디
                    //  * 만들기 가이드: https://developers.kakao.com/docs/latest/ko/message-template/common
                    val templateId = bundle.getLong("customMessage")

                    TalkApiClient.instance.sendCustomMemo(templateId) { error ->
                        if (error != null) {
                            Log.e(TAG, "나에게 보내기 실패", error)
                        } else {
                            Log.i(TAG, "나에게 보내기 성공")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("sendDefaultMemo()") {

                    // 디폴트 템플릿으로 나에게 보내기 - Feed
                    TalkApiClient.instance.sendDefaultMemo(defaultFeed) { error ->
                        if (error != null) {
                            Log.e(TAG, "나에게 보내기 실패", error)
                        } else {
                            Log.i(TAG, "나에게 보내기 성공")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("sendScrapMemo()") {

                    // 스크랩 템플릿으로 나에게 보내기

                    // 공유할 웹페이지 URL
                    //  * 주의: 개발자사이트 Web 플랫폼 설정에 공유할 URL의 도메인이 등록되어 있어야 합니다.
                    val url = "https://developers.kakao.com"

                    TalkApiClient.instance.sendScrapMemo(url) { error ->
                        if (error != null) {
                            Log.e(TAG, "나에게 보내기 실패", error)
                        } else {
                            Log.i(TAG, "나에게 보내기 성공")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("friends()") {

                    // 카카오톡 친구 목록 조회 (기본)
                    TalkApiClient.instance.friends { friends, error ->
                        if (error != null) {
                            Log.e(TAG, "카카오톡 친구 목록 조회 실패", error)
                        } else if (friends != null) {
                            Log.i(
                                TAG,
                                "카카오톡 친구 목록 조회 성공 \n${friends.elements?.joinToString("\n")}"
                            )

                            // 친구의 UUID 로 메시지 보내기 가능
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("friends(order:) - desc") {

                    // 카카오톡 친구 목록 조회 (파라미터)

                    // 내림차순으로 받기
                    TalkApiClient.instance.friends(order = Order.DESC) { friends, error ->
                        if (error != null) {
                            Log.e(TAG, "카카오톡 친구 목록 조회 실패", error)
                        } else if (friends != null) {
                            Log.i(
                                TAG,
                                "카카오톡 친구 목록 조회 성공 \n${friends.elements?.joinToString("\n")}"
                            )

                            // 친구의 UUID 로 메시지 보내기 가능
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("friends(context:) - reculsive") {

                    var nextFriendsContext =
                        FriendsContext(offset = 0, limit = 3, order = Order.DESC)

                    recursiveAppFriendsCompletion =
                        recursiveAppFriendsCompletion@{ nextFriends, err ->
                            if (err == null) {
                                if (nextFriends != null) {
                                    try {
                                        nextFriends.afterUrl?.let {
                                            nextFriendsContext = FriendsContext(url = it)
                                        } ?: return@recursiveAppFriendsCompletion

                                    } catch (e: IllegalArgumentException) {
                                        return@recursiveAppFriendsCompletion
                                    }
                                }

                                TalkApiClient.instance.friends(context = nextFriendsContext) { friends, error ->
                                    if (error != null) {
                                        Log.e(TAG, "카카오톡 친구 목록 조회 실패", error)
                                    } else if (friends != null) {
                                        Log.i(
                                            TAG,
                                            "카카오톡 친구 목록 조회 성공 \n${
                                                friends.elements?.joinToString(
                                                    "\n"
                                                )
                                            }"
                                        )
                                        recursiveAppFriendsCompletion?.let { it(friends, null) }
                                    }
                                }
                            }
                        }

                    recursiveAppFriendsCompletion?.let { it(null, null) }
                },
                ApiAdapter.Item.ApiItem("friends(context:) - FriendsContext") {
                    TalkApiClient.instance.friends(
                        context = FriendsContext(
                            offset = 0,
                            limit = 1,
                            order = Order.DESC
                        )
                    ) { friends, error ->
                        if (error != null) {
                            Log.e(TAG, "카카오톡 친구 목록 조회 실패", error)
                        } else if (friends != null) {
                            Log.i(
                                TAG,
                                "카카오톡 친구 목록 조회 성공 \n${friends.elements?.joinToString("\n")}"
                            )
                        }
                    }

                },
                ApiAdapter.Item.ApiItem("sendCustomMessage()") {

                    // 커스텀 템플릿으로 친구에게 메시지 보내기

                    // 카카오톡 친구 목록 조회
                    TalkApiClient.instance.friends { friends, error ->
                        if (error != null) {
                            Log.e(TAG, "카카오톡 친구 목록 조회 실패", error)
                        } else if (friends != null) {
                            friends.elements?.let {
                                Log.d(TAG, "카카오톡 친구 목록 조회 성공 \n${it.joinToString("\n")}")

                                if (it.isEmpty()) {
                                    Log.e(TAG, "메시지 보낼 친구가 하나도 없어요 ㅠㅠ")
                                } else {
                                    // 서비스에 상황에 맞게 메시지 보낼 친구의 UUID 를 가져오세요.
                                    // 이 샘플에서는 친구 목록을 화면에 보여주고 체크박스로 선택된 친구들의 UUID 를 수집하도록 구현했습니다.
                                    FriendsActivity.startForResult(
                                        context,
                                        it.map { friend ->
                                            PickerItem(
                                                friend.uuid,
                                                friend.profileNickname ?: "",
                                                friend.profileThumbnailImage
                                            )
                                        }
                                    ) { selectedItems ->
                                        if (selectedItems.isEmpty()) return@startForResult
                                        Log.d(
                                            TAG,
                                            "선택된 친구:\n${selectedItems.joinToString("\n")}"
                                        )


                                        // 메시지 보낼 친구의 UUID 목록
                                        val receiverUuids = selectedItems

                                        // 메시지 템플릿 아이디
                                        //  * 만들기 가이드: https://developers.kakao.com/docs/latest/ko/message-template/common
                                        val templateId = bundle.getLong("customMessage")

                                        // 메시지 보내기
                                        TalkApiClient.instance.sendCustomMessage(
                                            receiverUuids,
                                            templateId
                                        ) { result, error ->
                                            if (error != null) {
                                                Log.e(TAG, "메시지 보내기 실패", error)
                                            } else if (result != null) {
                                                Log.i(
                                                    TAG,
                                                    "메시지 보내기 성공 ${result.successfulReceiverUuids}"
                                                )

                                                if (result.failureInfos != null) {
                                                    Log.d(
                                                        TAG,
                                                        "메시지 보내기에 일부 성공했으나, 일부 대상에게는 실패 \n${result.failureInfos}"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("sendDefaultMessage()") {

                    // 디폴트 템플릿으로 친구에게 메시지 보내기 - Feed

                    // 카카오톡 친구 목록 조회
                    TalkApiClient.instance.friends { friends, error ->
                        if (error != null) {
                            Log.e(TAG, "카카오톡 친구 목록 조회 실패", error)
                        } else if (friends != null) {
                            friends.elements?.let {
                                Log.d(TAG, "카카오톡 친구 목록 조회 성공 \n${it.joinToString("\n")}")

                                if (it.isEmpty()) {
                                    Log.e(TAG, "메시지 보낼 친구가 하나도 없어요 ㅠㅠ")
                                } else {

                                    // 서비스에 상황에 맞게 메시지 보낼 친구의 UUID를 가져오세요.
                                    // 이 샘플에서는 친구 목록을 화면에 보여주고 체크박스로 선택된 친구들의 UUID 를 수집하도록 구현했습니다.
                                    FriendsActivity.startForResult(
                                        context,
                                        it.map { friend ->
                                            PickerItem(
                                                friend.uuid,
                                                friend.profileNickname ?: "",
                                                friend.profileThumbnailImage
                                            )
                                        }
                                    ) { selectedItems ->
                                        if (selectedItems.isEmpty()) return@startForResult
                                        Log.d(
                                            TAG,
                                            "선택된 친구:\n${selectedItems.joinToString("\n")}"
                                        )


                                        // 메시지 보낼 친구의 UUID 목록
                                        val receiverUuids = selectedItems

                                        // Feed 메시지
                                        val template = defaultFeed

                                        // 메시지 보내기
                                        TalkApiClient.instance.sendDefaultMessage(
                                            receiverUuids,
                                            template
                                        ) { result, error ->
                                            if (error != null) {
                                                Log.e(TAG, "메시지 보내기 실패", error)
                                            } else if (result != null) {
                                                Log.i(
                                                    TAG,
                                                    "메시지 보내기 성공 ${result.successfulReceiverUuids}"
                                                )

                                                if (result.failureInfos != null) {
                                                    Log.d(
                                                        TAG,
                                                        "메시지 보내기에 일부 성공했으나, 일부 대상에게는 실패 \n${result.failureInfos}"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("sendScrapMessage()") {

                    // 스크랩 템플릿으로 친구에게 메시지 보내기

                    // 카카오톡 친구 목록 조회
                    TalkApiClient.instance.friends { friends, error ->
                        if (error != null) {
                            Log.e(TAG, "카카오톡 친구 목록 조회 실패", error)
                        } else if (friends != null) {
                            friends.elements?.let {
                                Log.d(TAG, "카카오톡 친구 목록 조회 성공 \n${it.joinToString("\n")}")

                                if (it.isEmpty()) {
                                    Log.e(TAG, "메시지 보낼 친구가 하나도 없어요 ㅠㅠ")
                                } else {

                                    // 서비스에 상황에 맞게 메시지 보낼 친구의 UUID를 가져오세요.
                                    // 이 샘플에서는 친구 목록을 화면에 보여주고 체크박스로 선택된 친구들의 UUID 를 수집하도록 구현했습니다.
                                    FriendsActivity.startForResult(
                                        context,
                                        it.map { friend ->
                                            PickerItem(
                                                friend.uuid,
                                                friend.profileNickname ?: "",
                                                friend.profileThumbnailImage
                                            )
                                        }
                                    ) { selectedItems ->
                                        if (selectedItems.isEmpty()) return@startForResult
                                        Log.d(
                                            TAG,
                                            "선택된 친구:\n${selectedItems.joinToString("\n")}"
                                        )


                                        // 메시지 보낼 친구의 UUID 목록
                                        val receiverUuids = selectedItems

                                        // 공유할 웹페이지 URL
                                        //  * 주의: 개발자사이트 Web 플랫폼 설정에 공유할 URL의 도메인이 등록되어 있어야 합니다.
                                        val url = "https://developers.kakao.com"

                                        // 메시지 보내기
                                        TalkApiClient.instance.sendScrapMessage(
                                            receiverUuids,
                                            url
                                        ) { result, error ->
                                            if (error != null) {
                                                Log.e(TAG, "메시지 보내기 실패", error)
                                            } else if (result != null) {
                                                Log.i(
                                                    TAG,
                                                    "메시지 보내기 성공 ${result.successfulReceiverUuids}"
                                                )

                                                if (result.failureInfos != null) {
                                                    Log.d(
                                                        TAG,
                                                        "메시지 보내기에 일부 성공했으나, 일부 대상에게는 실패 \n${result.failureInfos}"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("channels()") {

                    // 카카오톡 채널 관계 조회
                    TalkApiClient.instance.channels { relations, error ->
                        if (error != null) {
                            Log.e(TAG, "채널 관계 조회 실패", error)
                        } else if (relations != null) {
                            Log.i(TAG, "채널 관계 조회 성공 \n${relations.channels}")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("followChannel()") {
                    val channelPublicId = bundle.getString("channelId")!!

                    TalkApiClient.instance.followChannel(
                        context,
                        channelPublicId
                    ) { result, error ->
                        if (error != null) {
                            Log.i(TAG, "채널 추가 실패 $error")
                            return@followChannel
                        }
                        Log.i(TAG, "채널 추가 성공 $result")
                    }
                },
                ApiAdapter.Item.ApiItem("addChannel()") {
                    // 카카오톡 채널 추가
                    val channelPublicId = bundle.getString("channelId")

                    TalkApiClient.instance.addChannel(context, channelPublicId!!) { error ->
                        if (error != null) {
                            Log.i(TAG, "카카오톡 채널 추가 실패 $error")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("chatChannel()") {
                    // 카카오톡 채널 채팅
                    val channelPublicId = bundle.getString("channelId")

                    TalkApiClient.instance.chatChannel(context, channelPublicId!!) { error ->
                        if (error != null) {
                            Log.i(TAG, "카카오톡 채널 채팅 실패 $error")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("addChannelUrl()") {
                    // 카카오톡 채널 추가 URL
                    val channelPublicId = bundle.getString("channelId")
                    val url = TalkApiClient.instance.addChannelUrl(channelPublicId!!)

                    // CustomTabsServiceConnection 지원 브라우저 열기
                    try {
                        KakaoCustomTabsClient.openWithDefault(context, url)
                    } catch (e: UnsupportedOperationException) {
                        Log.e(
                            TAG,
                            "CustomTabsServiceConnection 지원 브라우저 없음: 디바이스에 설치된 다른 인터넷 브라우저 사용 권장"
                        )

                        // 디바이스에 설치된 다른 브라우저 열기
                        try {
                            KakaoCustomTabsClient.open(context, url)
                        } catch (e: ActivityNotFoundException) {
                            Log.e(TAG, "인터넷 브라우저 미설치: 인터넷 브라우저를 설치해주세요")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("chatChannelUrl()") {

                    // 카카오톡 채널 채팅 URL
                    val channelPublicId = bundle.getString("channelId")
                    val url = TalkApiClient.instance.chatChannelUrl(channelPublicId!!)

                    // CustomTabsServiceConnection 지원 브라우저 열기
                    try {
                        KakaoCustomTabsClient.openWithDefault(context, url)
                    } catch (e: UnsupportedOperationException) {
                        Log.e(
                            TAG,
                            "CustomTabsServiceConnection 지원 브라우저 없음: 디바이스에 설치된 다른 인터넷 브라우저 사용 권장"
                        )

                        // 디바이스에 설치된 다른 브라우저 열기
                        try {
                            KakaoCustomTabsClient.open(context, url)
                        } catch (e: ActivityNotFoundException) {
                            Log.e(TAG, "인터넷 브라우저 미설치: 인터넷 브라우저를 설치해주세요")
                        }
                    }
                },
                ApiAdapter.Item.Header("Friend API"),
                ApiAdapter.Item.ApiItem("멀티 친구 피커") {
                    val params = OpenPickerFriendRequestParams(title = "멀티 친구 피커")

                    PickerClient.instance.selectFriend(
                        context,
                        params,
                        ViewType.FULL,
                    ) { selectedUsers, error ->
                        if (error != null) {
                            Log.e(TAG, "친구 선택 실패", error)
                        } else {
                            Log.i(TAG, "친구 선택 성공 $selectedUsers")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("싱글 친구 피커") {
                    val params = OpenPickerFriendRequestParams(title = "싱글 친구 피커")

                    PickerClient.instance.selectFriend(
                        context,
                        params,
                        ViewType.FULL,
                        false,
                    ) { selectedUsers, error ->
                        if (error != null) {
                            Log.e(TAG, "친구 선택 실패", error)
                        } else {
                            Log.i(TAG, "친구 선택 성공 $selectedUsers")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("멀티 친구 피커 팝업") {
                    val params = OpenPickerFriendRequestParams(title = "멀티 친구 피커")

                    PickerClient.instance.selectFriend(
                        context,
                        params,
                        ViewType.POPUP,
                    ) { selectedUsers, error ->
                        if (error != null) {
                            Log.e(TAG, "친구 선택 실패", error)
                        } else {
                            Log.i(TAG, "친구 선택 성공 $selectedUsers")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("싱글 친구 피커 팝업") {
                    val params = OpenPickerFriendRequestParams(title = "싱글 친구 피커")

                    PickerClient.instance.selectFriend(
                        context,
                        params,
                        ViewType.POPUP,
                        false,
                    ) { selectedUsers, error ->
                        if (error != null) {
                            Log.e(TAG, "친구 선택 실패", error)
                        } else {
                            Log.i(TAG, "친구 선택 성공 $selectedUsers")
                        }
                    }
                },
                ApiAdapter.Item.Header("KakaoTalk Sharing API"),
                ApiAdapter.Item.ApiItem("isKakaoTalkSharingAvailable()") {

                    // 카카오톡 설치여부 확인
                    if (ShareClient.instance.isKakaoTalkSharingAvailable(context)) {
                        Log.i(TAG, "카카오톡 공유 가능")
                    } else {
                        Log.i(TAG, "카카오톡 미설치: 웹 공유 사용 권장")
                    }
                },
                ApiAdapter.Item.ApiItem("customTemplate()") {

                    // 커스텀 템플릿으로 카카오톡 공유 메시지 발송
                    //  * 만들기 가이드: https://developers.kakao.com/docs/latest/ko/message-template/common
                    val templateId = bundle.getLong("customMemo")

                    ShareClient.instance.shareCustom(
                        context,
                        templateId
                    ) { sharingResult, error ->
                        if (error != null) {
                            Log.e(TAG, "카카오톡 공유 실패", error)
                        } else if (sharingResult != null) {
                            Log.d(TAG, "카카오톡 공유 성공 ${sharingResult.intent}")
                            startActivity(sharingResult.intent)

                            // 카카오톡 공유에 성공했지만 아래 경고 메시지가 존재할 경우 일부 컨텐츠가 정상 동작하지 않을 수 있습니다.
                            Log.w(TAG, "Warning Msg: ${sharingResult.warningMsg}")
                            Log.w(TAG, "Argument Msg: ${sharingResult.argumentMsg}")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("scrapTemplate()") {

                    // 스크랩 템플릿으로 카카오톡 공유 메시지 발송

                    // 공유할 웹페이지 URL
                    //  * 주의: 개발자사이트 Web 플랫폼 설정에 공유할 URL의 도메인이 등록되어 있어야 합니다.
                    val url = "https://developers.kakao.com"

                    ShareClient.instance.shareScrap(context, url) { sharingResult, error ->
                        if (error != null) {
                            Log.e(TAG, "카카오톡 공유 실패", error)
                        } else if (sharingResult != null) {
                            Log.d(TAG, "카카오톡 공유 성공 ${sharingResult.intent}")
                            startActivity(sharingResult.intent)

                            // 카카오톡 공유에 성공했지만 아래 경고 메시지가 존재할 경우 일부 컨텐츠가 정상 동작하지 않을 수 있습니다.
                            Log.w(TAG, "Warning Msg: ${sharingResult.warningMsg}")
                            Log.w(TAG, "Argument Msg: ${sharingResult.argumentMsg}")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("defaultTemplate() - feed") {

                    // 디폴트 템플릿으로 카카오톡 공유 메시지 발송 - Feed
                    ShareClient.instance.shareDefault(
                        context,
                        defaultFeed
                    ) { sharingResult, error ->
                        if (error != null) {
                            Log.e(TAG, "카카오톡 공유 실패", error)
                        } else if (sharingResult != null) {
                            Log.d(TAG, "카카오톡 공유 성공 ${sharingResult.intent}")
                            startActivity(sharingResult.intent)

                            // 카카오톡 공유에 성공했지만 아래 경고 메시지가 존재할 경우 일부 컨텐츠가 정상 동작하지 않을 수 있습니다.
                            Log.w(TAG, "Warning Msg: ${sharingResult.warningMsg}")
                            Log.w(TAG, "Argument Msg: ${sharingResult.argumentMsg}")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("defaultTemplate() - list") {

                    // 디폴트 템플릿으로 카카오톡 공유 메시지 발송 - List
                    ShareClient.instance.shareDefault(
                        context,
                        defaultList
                    ) { sharingResult, error ->
                        if (error != null) {
                            Log.e(TAG, "카카오톡 공유 실패", error)
                        } else if (sharingResult != null) {
                            Log.d(TAG, "카카오톡 공유 성공 ${sharingResult.intent}")
                            startActivity(sharingResult.intent)

                            // 카카오톡 공유에 성공했지만 아래 경고 메시지가 존재할 경우 일부 컨텐츠가 정상 동작하지 않을 수 있습니다.
                            Log.w(TAG, "Warning Msg: ${sharingResult.warningMsg}")
                            Log.w(TAG, "Argument Msg: ${sharingResult.argumentMsg}")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("defaultTemplate() - location") {

                    // 디폴트 템플릿으로 카카오톡 공유 메시지 발송 - Location
                    ShareClient.instance.shareDefault(
                        context,
                        defaultLocation
                    ) { sharingResult, error ->
                        if (error != null) {
                            Log.e(TAG, "카카오톡 공유 실패", error)
                        } else if (sharingResult != null) {
                            Log.d(TAG, "카카오톡 공유 성공 ${sharingResult.intent}")
                            startActivity(sharingResult.intent)

                            // 카카오톡 공유에 성공했지만 아래 경고 메시지가 존재할 경우 일부 컨텐츠가 정상 동작하지 않을 수 있습니다.
                            Log.w(TAG, "Warning Msg: ${sharingResult.warningMsg}")
                            Log.w(TAG, "Argument Msg: ${sharingResult.argumentMsg}")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("defaultTemplate() - commerce") {

                    // 디폴트 템플릿으로 카카오톡 공유 메시지 발송 - Commerce
                    ShareClient.instance.shareDefault(
                        context,
                        defaultCommerce
                    ) { sharingResult, error ->
                        if (error != null) {
                            Log.e(TAG, "카카오톡 공유 실패", error)
                        } else if (sharingResult != null) {
                            Log.d(TAG, "카카오톡 공유 성공 ${sharingResult.intent}")
                            startActivity(sharingResult.intent)

                            // 카카오톡 공유에 성공했지만 아래 경고 메시지가 존재할 경우 일부 컨텐츠가 정상 동작하지 않을 수 있습니다.
                            Log.w(TAG, "Warning Msg: ${sharingResult.warningMsg}")
                            Log.w(TAG, "Argument Msg: ${sharingResult.argumentMsg}")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("defaultTemplate() - text") {

                    // 디폴트 템플릿으로 카카오톡 공유 메시지 발송 - Text
                    ShareClient.instance.shareDefault(
                        context,
                        defaultText
                    ) { sharingResult, error ->
                        if (error != null) {
                            Log.e(TAG, "카카오톡 공유 실패", error)
                        } else if (sharingResult != null) {
                            Log.d(TAG, "카카오톡 공유 성공 ${sharingResult.intent}")
                            startActivity(sharingResult.intent)

                            // 카카오톡 공유에 성공했지만 아래 경고 메시지가 존재할 경우 일부 컨텐츠가 정상 동작하지 않을 수 있습니다.
                            Log.w(TAG, "Warning Msg: ${sharingResult.warningMsg}")
                            Log.w(TAG, "Argument Msg: ${sharingResult.argumentMsg}")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("defaultTemplate() - calendar") {
                    val defaultCalendar = createCalendarTemplate(bundle)

                    // 디폴트 템플릿으로 카카오톡 공유 메시지 발송 - Text
                    ShareClient.instance.shareDefault(
                        context,
                        defaultCalendar
                    ) { sharingResult, error ->
                        if (error != null) {
                            Log.e(TAG, "카카오톡 공유 실패", error)
                        } else if (sharingResult != null) {
                            Log.d(TAG, "카카오톡 공유 성공 ${sharingResult.intent}")
                            startActivity(sharingResult.intent)

                            // 카카오톡 공유에 성공했지만 아래 경고 메시지가 존재할 경우 일부 컨텐츠가 정상 동작하지 않을 수 있습니다.
                            Log.w(TAG, "Warning Msg: ${sharingResult.warningMsg}")
                            Log.w(TAG, "Argument Msg: ${sharingResult.argumentMsg}")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("customTemplateUri() - web sharer") {
                    // 커스텀 템플릿으로 웹에서 카카오톡 공유 메시지 발송

                    // 메시지 템플릿 아이디
                    //  * 만들기 가이드: https://developers.kakao.com/docs/latest/ko/message-template/common
                    val templateId = bundle.getLong("customMemo")

                    val sharerUrl = WebSharerClient.instance.makeCustomUrl(
                        templateId,
                        mapOf("DESCRIPTION" to "value1")
                    )

                    // CustomTabsServiceConnection 지원 브라우저 열기
                    try {
                        KakaoCustomTabsClient.openWithDefault(context, sharerUrl)
                    } catch (e: UnsupportedOperationException) {
                        Log.e(
                            TAG,
                            "CustomTabsServiceConnection 지원 브라우저 없음: 디바이스에 설치된 다른 인터넷 브라우저 사용 권장"
                        )

                        // 디바이스에 설치된 다른 브라우저 열기
                        try {
                            KakaoCustomTabsClient.open(context, sharerUrl)
                        } catch (e: ActivityNotFoundException) {
                            Log.e(TAG, "인터넷 브라우저 미설치: 인터넷 브라우저를 설치해주세요")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("scrapTemplateUri() - web sharer") {
                    // 스크랩 템플릿으로 웹에서 카카오톡 공유 메시지 발송

                    // 공유할 웹페이지 URL
                    //  * 주의: 개발자사이트 Web 플랫폼 설정에 공유할 URL의 도메인이 등록되어 있어야 합니다.
                    val url = "https://developers.kakao.com"

                    val sharerUrl = WebSharerClient.instance.makeScrapUrl(
                        url,
                        templateArgs = mapOf("key1" to "value1")
                    )

                    // CustomTabsServiceConnection 지원 브라우저 열기
                    try {
                        KakaoCustomTabsClient.openWithDefault(context, sharerUrl)
                    } catch (e: UnsupportedOperationException) {
                        Log.e(
                            TAG,
                            "CustomTabsServiceConnection 지원 브라우저 미설치: 디바이스에 설치된 인터넷 브라우저 사용 권장"
                        )

                        // 디바이스에 설치된 다른 브라우저 열기
                        try {
                            KakaoCustomTabsClient.open(context, sharerUrl)
                        } catch (e: ActivityNotFoundException) {
                            Log.e(TAG, "인터넷 브라우저 미설치: 인터넷 브라우저를 설치해주세요")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("defaultTemplateUri() - web sharer - feed") {
                    // 디폴트 템플릿으로 웹에서 카카오톡 공유 메시지 발송 - Feed
                    val sharerUrl = WebSharerClient.instance.makeDefaultUrl(defaultFeed)

                    // CustomTabsServiceConnection 지원 브라우저 열기
                    try {
                        KakaoCustomTabsClient.openWithDefault(context, sharerUrl)
                    } catch (e: UnsupportedOperationException) {
                        Log.e(
                            TAG,
                            "CustomTabsServiceConnection 지원 브라우저 없음: 디바이스에 설치된 다른 인터넷 브라우저 사용 권장"
                        )

                        // 디바이스에 설치된 다른 브라우저 열기
                        try {
                            KakaoCustomTabsClient.open(context, sharerUrl)
                        } catch (e: ActivityNotFoundException) {
                            Log.e(TAG, "인터넷 브라우저 미설치: 인터넷 브라우저를 설치해주세요")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("defaultTemplateUri() - web sharer - location") {
                    // 디폴트 템플릿으로 웹에서 카카오톡 공유 메시지 발송 - Location
                    val sharerUrl = WebSharerClient.instance.makeDefaultUrl(defaultLocation)

                    // CustomTabsServiceConnection 지원 브라우저 열기
                    try {
                        KakaoCustomTabsClient.openWithDefault(context, sharerUrl)
                    } catch (e: UnsupportedOperationException) {
                        Log.e(
                            TAG,
                            "CustomTabsServiceConnection 지원 브라우저 없음: 디바이스에 설치된 다른 인터넷 브라우저 사용 권장"
                        )

                        // 디바이스에 설치된 다른 브라우저 열기
                        try {
                            KakaoCustomTabsClient.open(context, sharerUrl)
                        } catch (e: ActivityNotFoundException) {
                            Log.e(TAG, "인터넷 브라우저 미설치: 인터넷 브라우저를 설치해주세요")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("uploadImage()") {

                    // 이미지 업로드

                    // 로컬 이미지 파일
                    // 이 샘플에서는 프로젝트 리소스로 추가한 이미지 파일을 사용했습니다. 갤러리 등 서비스 니즈에 맞는 사진 파일을 준비하세요.
                    val bitmap = BitmapFactory.decodeResource(resources, R.drawable.sample1)
                    val file = File(context.cacheDir, "sample1.png")
                    val stream = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    stream.close()

                    // 카카오 이미지 서버로 업로드
                    ShareClient.instance.uploadImage(file) { imageUploadResult, error ->
                        if (error != null) {
                            Log.e(TAG, "이미지 업로드 실패", error)
                        } else if (imageUploadResult != null) {
                            Log.i(TAG, "이미지 업로드 성공 \n${imageUploadResult.infos.original}")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("scrapImage()") {

                    // 이미지 스크랩

                    // 원본 원격 이미지 URL
                    val url =
                        "https://t1.kakaocdn.net/kakaocorp/Service/KakaoTalk/pc/slide/talkpc_theme_01.jpg"

                    // 카카오 이미지 서버로 업로드
                    ShareClient.instance.scrapImage(url) { imageUploadResult, error ->
                        if (error != null) {
                            Log.e(TAG, "이미지 스크랩 실패", error)
                        } else if (imageUploadResult != null) {
                            Log.i(TAG, "이미지 스크랩 성공 \n${imageUploadResult.infos.original}")
                        }
                    }
                },
                ApiAdapter.Item.Header("KakaoNavi API"),
                ApiAdapter.Item.ApiItem("isKakaoNaviInstalled()") {
                    // 카카오내비 설치여부 확인
                    if (NaviClient.instance.isKakaoNaviInstalled(context)) {
                        Log.i(TAG, "카카오내비 앱으로 길안내 가능")
                    } else {
                        Log.i(TAG, "카카오내비 미설치")
                    }
                },
                ApiAdapter.Item.ApiItem("shareDestinationIntent() - KATEC") {
                    if (NaviClient.instance.isKakaoNaviInstalled(context)) {
                        // 카카오내비 앱으로 목적지 공유 - KATEC
                        startActivity(
                            NaviClient.instance.shareDestinationIntent(
                                Location("카카오 판교오피스", "321286", "533707")
                            )
                        )
                    } else {
                        // 카카오내비 설치 페이지로 이동
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(Constants.WEB_NAVI_INSTALL)
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        )
                    }
                },
                ApiAdapter.Item.ApiItem("shareDestinationIntent() - WGS84") {
                    if (NaviClient.instance.isKakaoNaviInstalled(context)) {
                        // 카카오내비 앱으로 목적지 공유 - WGS84
                        startActivity(
                            NaviClient.instance.shareDestinationIntent(
                                Location("카카오 판교오피스", "127.108640", "37.402111"),
                                NaviOption(coordType = CoordType.WGS84)
                            )
                        )
                    } else {
                        // 카카오내비 설치 페이지로 이동
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(Constants.WEB_NAVI_INSTALL)
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        )
                    }
                },
                ApiAdapter.Item.ApiItem("navigateIntent() - KATEC") {
                    if (NaviClient.instance.isKakaoNaviInstalled(context)) {
                        // 카카오내비 앱으로 길안내 - KATEC
                        startActivity(
                            NaviClient.instance.navigateIntent(
                                Location("카카오 판교오피스", "321286", "533707")
                            )
                        )
                    } else {
                        // 카카오내비 설치 페이지로 이동
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(Constants.WEB_NAVI_INSTALL)
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        )
                    }
                },
                ApiAdapter.Item.ApiItem("navigateIntent() - WGS84") {
                    if (NaviClient.instance.isKakaoNaviInstalled(context)) {
                        // 카카오내비 앱으로 길안내 - WGS84
                        startActivity(
                            NaviClient.instance.navigateIntent(
                                Location("카카오 판교오피스", "127.108640", "37.402111"),
                                NaviOption(coordType = CoordType.WGS84)
                            )
                        )
                    } else {
                        // 카카오내비 설치 페이지로 이동
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(Constants.WEB_NAVI_INSTALL)
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        )
                    }
                },
                ApiAdapter.Item.ApiItem("navigateIntent() - KATEC - viaList") {
                    if (NaviClient.instance.isKakaoNaviInstalled(context)) {
                        // 카카오내비 앱으로 길안내 - KATEC - 경유지 추가
                        startActivity(
                            NaviClient.instance.navigateIntent(
                                Location("카카오 판교오피스", "321286", "533707"),
                                viaList = listOf(
                                    Location("판교역 1번출구", "321525", "532951")
                                )
                            )
                        )
                    } else {
                        // 카카오내비 설치 페이지로 이동
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(Constants.WEB_NAVI_INSTALL)
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        )
                    }
                },
                ApiAdapter.Item.ApiItem("navigateIntent() - WGS84 - viaList") {
                    if (NaviClient.instance.isKakaoNaviInstalled(context)) {
                        // 카카오내비 앱으로 길안내 - WGS84 - 경유지 추가
                        startActivity(
                            NaviClient.instance.navigateIntent(
                                Location("카카오 판교오피스", "127.108640", "37.402111"),
                                NaviOption(coordType = CoordType.WGS84),
                                listOf(
                                    Location("판교역 1번출구", "127.111492", "37.395225")
                                )
                            )
                        )
                    } else {
                        // 카카오내비 설치 페이지로 이동
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(Constants.WEB_NAVI_INSTALL)
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        )
                    }
                },
                ApiAdapter.Item.Header("Kakao Sync"),
                ApiAdapter.Item.ApiItem("login(serviceTerms:) - select one") {

                    // 서비스 약관 선택해 동의 요청

                    // 개발자사이트 간편가입 설정에 등록한 약관 목록 중, 동의를 요청할 약관의 태그 값을 지정합니다.
                    val serviceTerms = listOf("service")

                    // serviceTerms 파라미터와 함께 카카오톡으로 로그인 요청 (카카오계정으로 로그인도 사용법 동일)
                    UserApiClient.instance.loginWithKakaoTalk(
                        context = context,
                        serviceTerms = serviceTerms
                    ) { token, error ->
                        if (error != null) {
                            Log.e(TAG, "로그인 실패", error)
                        } else if (token != null) {
                            Log.i(TAG, "로그인 성공 ${token.accessToken}")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("login(serviceTerms:) - empty") {

                    // 약관 동의 받지 않기

                    // serviceTerms 파라미터에 empty list 전달해서 카카오톡으로 로그인 요청 (카카오계정으로 로그인도 사용법 동일)
                    UserApiClient.instance.loginWithKakaoTalk(
                        context = context,
                        serviceTerms = listOf()
                    ) { token, error ->
                        if (error != null) {
                            Log.e(TAG, "로그인 실패", error)
                        } else if (token != null) {
                            Log.i(TAG, "로그인 성공 ${token.accessToken}")
                        }
                    }
                },
                ApiAdapter.Item.Header("OIDC"),
                ApiAdapter.Item.ApiItem("loginWithKakaoTalk(nonce:openidtest)") {

                    // 카카오톡으로 로그인 - openId
                    UserApiClient.instance.loginWithKakaoTalk(
                        context,
                        nonce = "openidtest"
                    ) { token, error ->
                        if (error != null) {
                            Log.e(TAG, "로그인 실패", error)
                        } else if (token != null) {
                            Log.i(TAG, "로그인 성공 id_token: ${token.idToken}")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("certLoginWithKakaoTalk(nonce:openidtest)") {

                    // 카카오톡으로 인증서 로그인 - openId
                    CertApiClient.instance.certLoginWithKakaoTalk(
                        context,
                        CertType.K2100,
                        signData = "test",
                        nonce = "openidtest"
                    ) { certTokenInfo, error ->
                        if (error != null) {
                            Log.e(TAG, "로그인 실패", error)
                        } else if (certTokenInfo != null) {
                            Log.i(TAG, "로그인 성공 id_token: ${certTokenInfo.token.idToken}")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("loginWithKakaoAccount(nonce:openidtest)") {

                    // 카카오계정으로 로그인 - openId
                    UserApiClient.instance.loginWithKakaoAccount(
                        context,
                        nonce = "openidtest"
                    ) { token, error ->
                        if (error != null) {
                            Log.e(TAG, "로그인 실패", error)
                        } else if (token != null) {
                            Log.i(TAG, "로그인 성공 id_token: ${token.idToken}")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("certLoginWithKakaoAccount(nonce:openidtest)") {

                    // 카카오계정으로 인증서 로그인 - openId
                    CertApiClient.instance.certLoginWithKakaoAccount(
                        context,
                        certType = CertType.K2100,
                        signData = "test",
                        nonce = "openidtest"
                    ) { certTokenInfo, error ->
                        if (error != null) {
                            Log.e(TAG, "로그인 실패", error)
                        } else if (certTokenInfo != null) {
                            Log.i(TAG, "로그인 성공 id_token: ${certTokenInfo.token.idToken}")
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("me() - new scopes(nonce:openidtest)") {

                    // 사용자 정보 요청 (추가 동의)

                    // 사용자가 로그인 시 제3자 정보제공에 동의하지 않은 개인정보 항목 중 어떤 정보가 반드시 필요한 시나리오에 진입한다면
                    // 다음과 같이 추가 동의를 받고 해당 정보를 획득할 수 있습니다.

                    //  * 주의: 선택 동의항목은 사용자가 거부하더라도 서비스 이용에 지장이 없어야 합니다.

                    // 추가 권한 요청 시나리오 예제
                    UserApiClient.instance.me { user, e ->
                        if (e != null) {
                            Log.e(TAG, "사용자 정보 요청 실패", e)
                        } else if (user != null) {
                            val scopes = mutableListOf<String>()

                            if (user.kakaoAccount?.emailNeedsAgreement == true) {
                                scopes.add("account_email")
                            }
                            if (user.kakaoAccount?.birthdayNeedsAgreement == true) {
                                scopes.add("birthday")
                            }
                            if (user.kakaoAccount?.birthyearNeedsAgreement == true) {
                                scopes.add("birthyear")
                            }
                            if (user.kakaoAccount?.phoneNumberNeedsAgreement == true) {
                                scopes.add("phone_number")
                            }
                            if (user.kakaoAccount?.profileNeedsAgreement == true) {
                                scopes.add("profile")
                            }
                            if (user.kakaoAccount?.ageRangeNeedsAgreement == true) {
                                scopes.add("age_range")
                            }

                            if (scopes.isNotEmpty()) {
                                Log.d(TAG, "사용자에게 추가 동의를 받아야 합니다.")

                                // OpenID 활성화 후
                                // - scope 파라메터에 openid 항목을 포함하여 요청할 경우 OIDC로 동작
                                // - scope 파라메터에 openid 항목을 미포함 시 OAuth2.0 으로 동작

                                // OIDC 요청이므로 "openid" 항목을 추가한다
                                scopes.add("openid")

                                UserApiClient.instance.loginWithNewScopes(
                                    context,
                                    scopes
                                ) { token, err ->
                                    if (err != null) {
                                        Log.e(TAG, "사용자 추가 동의 실패", err)
                                    } else {
                                        Log.d(TAG, "allowed scopes: ${token!!.scopes}")

                                        // 사용자 정보 재요청
                                        UserApiClient.instance.me { user, error ->
                                            if (error != null) {
                                                Log.e(TAG, "사용자 정보 요청 실패", error)
                                            } else if (user != null) {
                                                Log.i(TAG, "사용자 정보 요청 성공")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                ApiAdapter.Item.Header("ETC"),
                ApiAdapter.Item.ApiItem("Get Current Token") {

                    // 현재 토큰 저장소에서 토큰 조회
                    Log.i(TAG, "${TokenManagerProvider.instance.manager.getToken()}")
                },
                ApiAdapter.Item.ApiItem("Set Custom TokenManager") {

                    // 커스텀 토큰 저장소 설정
                    TokenManagerProvider.instance.manager = object : TokenManageable {
                        val preferences =
                            context.getSharedPreferences(
                                "test_preferences",
                                Context.MODE_PRIVATE
                            )
                        val tokenKey = "test_token_key"

                        override fun getToken(): OAuthToken? =
                            preferences.getString(tokenKey, "")?.let {
                                KakaoJson.decodeFromString(it)
                            }

                        override fun setToken(token: OAuthToken) {
                            Log.d(TAG, "토큰 암호화를 권장합니다.")
                            preferences.edit(true) {
                                putString(tokenKey, KakaoJson.encodeToString(token))
                            }
                        }

                        override fun clear() {
                            preferences.edit(true) {
                                remove(tokenKey)
                            }
                        }
                    }
                    Log.i(TAG, "커스텀 토큰 저장소 사용")
                },
                ApiAdapter.Item.ApiItem("Set Default TokenManager") {

                    // 기본 저장소 재설정
                    TokenManagerProvider.instance.manager = TokenManager.instance
                    Log.i(TAG, "use default token manager")
                },
                ApiAdapter.Item.ApiItem("hasToken() usage") {
                    if (AuthApiClient.instance.hasToken()) {
                        UserApiClient.instance.accessTokenInfo { _, err ->
                            if (err != null) {
                                if (err is KakaoSdkError && err.isInvalidTokenError()) {
                                    Log.e(TAG, "토큰이 만료되었습니다.", err)

                                } else {
                                    Log.e(TAG, "토큰 정보를 가져오는데 실패했습니다.", err)
                                }

                                // 카카오계정으로 로그인
                                UserApiClient.instance.loginWithKakaoAccount(context) { token, error ->
                                    if (error != null) {
                                        Log.e(TAG, "로그인 실패", error)
                                    } else if (token != null) {
                                        Log.i(TAG, "로그인 성공 ${token.accessToken}")
                                    }
                                }
                            } else {
                                Log.i(TAG, "토큰 유효성 체크 성공")
                            }
                        }
                    } else {
                        Log.i(TAG, "토큰이 없습니다.")

                        // 카카오계정으로 로그인
                        UserApiClient.instance.loginWithKakaoAccount(context) { token, error ->
                            if (error != null) {
                                Log.e(TAG, "로그인 실패", error)
                            } else if (token != null) {
                                Log.i(TAG, "로그인 성공 ${token.accessToken}")
                            }
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("loginWithAccount()") {
                    UserApiClient.instance.loginWithKakaoAccount(context) { token, _ ->
                        Log.i(TAG, "${token?.accessToken}")
                    }
                },
                ApiAdapter.Item.ApiItem("me()") {
                    UserApiClient.instance.me { user, _ ->
                        Log.i(TAG, "${user?.id} ${user?.kakaoAccount?.email}")
                    }
                },
                ApiAdapter.Item.ApiItem("logout()") {
                    UserApiClient.instance.logout {
                        Log.i(TAG, "logout")
                    }
                },
                ApiAdapter.Item.ApiItem("openWebView()") {
                    startActivity(Intent(context, SampleWebViewActivity::class.java))
                },
                ApiAdapter.Item.Header("Cert - K2220"),
                ApiAdapter.Item.ApiItem("[SDK] publicKey()") {
                    val publicKey = CertApiClient.instance.publicKey(CertType.K2220)
                    ApiResultDialog.show(context, publicKey, "PublicKey")
                },
                ApiAdapter.Item.ApiItem("[SDK] sessionInfo request") {
                    val title = "SessionInfo"
                    val txId = CertDemoClient.instance(context).getTxId(CertType.K2220) ?: run {
                        val error = CertDemoError("txId is null. login first")
                        ApiResultDialog.show(context, error, title)
                        return@ApiItem
                    }

                    CertApiClient.instance.sessionInfo(CertType.K2220, txId) { sessionInfo, error ->
                        if (error != null) {
                            ApiResultDialog.show(context, error, title)
                            return@sessionInfo
                        }

                        ApiResultDialog.show(context, sessionInfo, title)
                    }
                },
                ApiAdapter.Item.ApiItem("[SDK] sessionInfo()") {
                    val sessionInfo = CertApiClient.instance.sessionInfo(CertType.K2220)
                    ApiResultDialog.show(context, sessionInfo, "SessionInfo")
                },
                ApiAdapter.Item.ApiItem("[SDK] isValidSession()") {
                    val isValidSession = CertApiClient.instance.isValidSession(CertType.K2220)
                    ApiResultDialog.show(context, isValidSession, "IsValidSession")
                },
                ApiAdapter.Item.ApiItem("[SDK] deleteKeyPair()") {
                    AlertDialog.Builder(context)
                        .setTitle("deleteKeyPair()")
                        .setMessage("이전에 생성된 키쌍과 저장되어있는 세션 정보가 삭제됩니다")
                        .setPositiveButton("OK") { dialog, _ ->
                            CertApiClient.instance.deleteKeyPair(CertType.K2220)

                            // 이용기관 txId 삭제 필요
                            CertDemoClient.instance(context).deleteTxId(CertType.K2220)

                            dialog.dismiss()

                            ApiResultDialog.show(
                                context,
                                "이전에 생성된 키쌍과 저장되어있던 세션 정보가 삭제되었고 이후 publicKey()를 호출하면 키쌍이 재생성 됩니다",
                                "DeleteKeyPair"
                            )
                        }
                        .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                        .show()
                },
                ApiAdapter.Item.ApiItem("[이용기관] demoSign(publicKey) -> [SDK] certLogin(..)") {
                    if (!UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                        val error = CertDemoError("KakaoTalk is not installed")
                        ApiResultDialog.show(context, error)

                        // 카카오톡 설치 호출 예시
                        // val uri = Uri.parse("market://launch?id=com.kakao.talk")
                        // startActivity(Intent(Intent.ACTION_VIEW, uri))
                        return@ApiItem
                    }

                    val publicKey = CertApiClient.instance.publicKey(CertType.K2220) ?: run {
                        CertDemoClient.instance(context).deleteTxId(CertType.K2220)
                        val error = CertDemoError("publicKey is null")
                        ApiResultDialog.show(context, error)
                        return@ApiItem
                    }

                    CertDemoClient.instance(context).demoSign(publicKey) { txId, error ->
                            if (error != null) {
                                ApiResultDialog.show(context, error)
                                return@demoSign
                            }

                            val settleId = bundle.getString("settleId")

                            CertApiClient.instance.certLoginWithKakaoTalk(
                                context,
                                CertType.K2220,
                                txId = txId,
                                settleId = settleId,
                            ) { certTokenInfo, loginError ->
                                if (loginError != null) {
                                    ApiResultDialog.show(context, loginError)

                                    if (loginError is KakaoSdkError) {
                                        // 실패시 키생성, txId발급 로그인을 다시 해야함
                                        CertApiClient.instance.deleteKeyPair(CertType.K2220)
                                        CertDemoClient.instance(context).deleteTxId(CertType.K2220)
                                    }
                                } else if (certTokenInfo != null) {
                                    CertDemoClient.instance(context)
                                        .saveTxId(txId!!, CertType.K2220)
                                    ApiResultDialog.show(context, certTokenInfo)
                                }
                            }
                        }
                },
                ApiAdapter.Item.ApiItem("[이용기관] demoVerify(txId, appUserId)") {
                    val txId = CertDemoClient.instance(context).getTxId(CertType.K2220) ?: run {
                        val error = CertDemoError("txId is null. login first.")
                        ApiResultDialog.show(context, error, "Verify")
                        return@ApiItem
                    }

                    UserApiClient.instance.me { user, error ->
                        if (error != null) {
                            ApiResultDialog.show(context, error, "Verify")
                            return@me
                        }

                        val userId = user?.id ?: run {
                            ApiResultDialog.show(context, "user.id is null", "Verify")
                            return@me
                        }

                        CertDemoClient.instance(context)
                            .demoVerify(txId, userId) { response, verifyError ->
                                if (verifyError != null) {
                                    ApiResultDialog.show(context, verifyError, "Verify")
                                    return@demoVerify
                                }

                                ApiResultDialog.show(context, response, "Verify")
                            }
                    }
                },
                ApiAdapter.Item.ApiItem("[이용기관] demoReducedSign(txId, data, signature)") {
                    val txId = CertDemoClient.instance(context).getTxId(CertType.K2220) ?: run {
                        val error = CertDemoError("txId is null. login first.")
                        ApiResultDialog.show(context, error, "reducedSign")
                        return@ApiItem
                    }
                    val signData = "hello"

                    CertApiClient.instance.reducedSign(
                        CertType.K2220,
                        signData
                    ) { signature, error ->
                        if (error != null) {
                            ApiResultDialog.show(context, error, "reducedSign")
                            return@reducedSign
                        }

                        CertDemoClient.instance(context).demoReducedSign(
                            txId,
                            signData,
                            signature!!,
                            CertType.K2220,
                        ) { response, signError ->
                            if (signError != null) {
                                ApiResultDialog.show(context, signError, "reducedSign")
                                return@demoReducedSign
                            }

                            ApiResultDialog.show(context, response, "reducedSign")
                        }
                    }
                },
                ApiAdapter.Item.Header("Cert - K3220"),
                ApiAdapter.Item.ApiItem("[SDK] isKakaoTalkSignAvailable") {
                    val result = CertApiClient.instance.isKakaoTalkSignAvailable(context)
                    ApiResultDialog.show(context, result, "PublicKey")
                },
                ApiAdapter.Item.ApiItem("[SDK] publicKey()") {
                    val publicKey = CertApiClient.instance.publicKey(CertType.K3220)
                    ApiResultDialog.show(context, publicKey, "PublicKey")
                },
                ApiAdapter.Item.ApiItem("[SDK] sessionInfoByAppKey request") {
                    val title = "SessionInfo"
                    val targetAppKey = bundle.getString("targetAppKey") ?: ""

                    val txId = CertDemoClient.instance(context).getTxId(CertType.K3220) ?: run {
                        val error = CertDemoError("txId is null. sign first")
                        ApiResultDialog.show(context, error, title)
                        return@ApiItem
                    }

                    CertApiClient.instance.sessionInfoByAppKey(
                        certType = CertType.K3220,
                        txId = txId,
                        targetAppKey = targetAppKey
                    ) { sessionInfo, error ->
                        if (error != null) {
                            ApiResultDialog.show(context, error, title)
                            return@sessionInfoByAppKey
                        }

                        ApiResultDialog.show(context, sessionInfo, title)
                    }
                },
                ApiAdapter.Item.ApiItem("[SDK] sessionInfo()") {
                    val sessionInfo = CertApiClient.instance.sessionInfo(certType = CertType.K3220)
                    ApiResultDialog.show(context, sessionInfo, "SessionInfo")
                },
                ApiAdapter.Item.ApiItem("[SDK] isValidSession()") {
                    val isValidSession =
                        CertApiClient.instance.isValidSession(certType = CertType.K3220)
                    ApiResultDialog.show(context, isValidSession, "IsValidSession")
                },
                ApiAdapter.Item.ApiItem("[SDK] deleteKeyPair()") {
                    AlertDialog.Builder(context)
                        .setTitle("deleteKeyPair()")
                        .setMessage("이전에 생성된 키쌍과 저장되어있는 세션 정보가 삭제됩니다")
                        .setPositiveButton("OK") { dialog, _ ->
                            CertApiClient.instance.deleteKeyPair(certType = CertType.K3220)

                            // 이용기관 txId 삭제 필요
                            CertDemoClient.instance(context).deleteTxId(CertType.K3220)

                            dialog.dismiss()

                            ApiResultDialog.show(
                                context,
                                "이전에 생성된 키쌍과 저장되어있던 세션 정보가 삭제되었고 이후 publicKey()를 호출하면 키쌍이 재생성 됩니다",
                                "DeleteKeyPair"
                            )
                        }
                        .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                        .show()
                },
                ApiAdapter.Item.ApiItem("[이용기관] demoSign(publicKey) -> [SDK] signWithKakaoTalk(..)") {
                    val targetAppKey = bundle.getString("targetAppKey") ?: ""

                    if (!CertApiClient.instance.isKakaoTalkSignAvailable(context)) {
                        val error = CertDemoError("KakaoTalk Sign is not available")
                        ApiResultDialog.show(context, error)

                        // 카카오톡 설치 호출 예시
                        // val uri = Uri.parse("market://launch?id=com.kakao.talk")
                        // startActivity(Intent(Intent.ACTION_VIEW, uri))
                        return@ApiItem
                    }

                    val publicKey =
                        CertApiClient.instance.publicKey(certType = CertType.K3220) ?: run {
                            CertDemoClient.instance(context).deleteTxId(CertType.K3220)
                            val error = CertDemoError("publicKey is null")
                            ApiResultDialog.show(context, error)
                            return@ApiItem
                        }

                    CertDemoClient.instance(context).demoSign(
                        publicKey = publicKey,
                        returnUrl = "${KakaoSdk.applicationContextInfo.customScheme}://cert",
                        validateAppKey = KakaoSdk.appKey,
                        targetAppKey = targetAppKey
                    ) { txId, error ->
                        if (error != null) {
                            ApiResultDialog.show(context, error)
                            return@demoSign
                        }

                        CertApiClient.instance.signWithKakaoTalk(
                            context = context,
                            certType = CertType.K3220,
                            txId = txId!!,
                            targetAppKey = targetAppKey,
                        ) { signStatusInfo, signError ->
                            if (signError != null) {
                                ApiResultDialog.show(context, signError)

                                if (signError is KakaoSdkError) {
                                    // 실패시 키생성, txId발급 로그인을 다시 해야함
                                    CertApiClient.instance.deleteKeyPair(certType = CertType.K3220)
                                    CertDemoClient.instance(context).deleteTxId(CertType.K3220)
                                }
                            } else if (signStatusInfo != null) {
                                CertDemoClient.instance(context)
                                    .saveTxId(signStatusInfo.txId, CertType.K3220)
                                ApiResultDialog.show(context, signStatusInfo)
                            }
                        }
                    }
                },
                ApiAdapter.Item.ApiItem("[이용기관] demoVerify(txId, appUserId)") {
                    val targetAppKey = bundle.getString("targetAppKey") ?: ""

                    val txId = CertDemoClient.instance(context).getTxId(CertType.K3220) ?: run {
                        val error = CertDemoError("txId is null. sign first.")
                        ApiResultDialog.show(context, error, "Verify")
                        return@ApiItem
                    }

                    CertDemoClient.instance(context)
                        .demoVerify(
                            txId = txId,
                            targetAppKey = targetAppKey,
                        ) { response, verifyError ->
                            if (verifyError != null) {
                                ApiResultDialog.show(context, verifyError, "Verify")
                                return@demoVerify
                            }

                            ApiResultDialog.show(context, response, "Verify")
                        }
                },
                ApiAdapter.Item.ApiItem("[이용기관] demoReducedSign(txId, data, signature)") {
                    val txId = CertDemoClient.instance(context).getTxId(CertType.K3220) ?: run {
                        val error = CertDemoError("txId is null. sign first.")
                        ApiResultDialog.show(context, error, "reducedSign")
                        return@ApiItem
                    }
                    val signData = "hello"

                    CertApiClient.instance.reducedSign(
                        data = signData,
                        certType = CertType.K3220,
                    ) { signature, error ->
                        if (error != null) {
                            ApiResultDialog.show(context, error, "reducedSign")
                            return@reducedSign
                        }

                        CertDemoClient.instance(context).demoReducedSign(
                            txId = txId,
                            data = signData,
                            signature = signature!!,
                            certType = CertType.K3220,
                        ) { response, signError ->
                            if (signError != null) {
                                ApiResultDialog.show(context, signError, "reducedSign")
                                return@demoReducedSign
                            }

                            ApiResultDialog.show(context, response, "reducedSign")
                        }
                    }
                },
            )
        )
        view.findViewById<RecyclerView>(R.id.recyclerView).apply {
            setHasFixedSize(true)
            addItemDecoration(
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            )
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            bundle = it
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_open, container, false)
    }

    private fun createCalendarTemplate(bundle: Bundle) = CalendarTemplate(
        idType = IdType.EVENT,
        id = bundle.getString("calendarEventId")!!,
        content = Content(
            title = "1월 신작 평론 모임",
            description = "따끈한 신작 감상평을 나누는 월간 모임에 초대합니다.",
            imageUrl = "http://k.kakaocdn.net/dn/dFUqwp/bl3SUTqb2VV/VFSqyPpKUzZVVMcmotN9A0/kakaolink40_original.png",
            link = Link(
                webUrl = "https://developers.kakao.com",
                mobileWebUrl = "https://developers.kakao.com",
            )
        ),
        buttons = listOf(
            Button(
                title = "모임 주제 보기",
                link = Link(
                    webUrl = "https://developers.kakao.com",
                    mobileWebUrl = "https://developers.kakao.com",
                ),
            )
        )
    )

    private lateinit var bundle: Bundle

    private var recursiveAppFriendsCompletion: ((Friends<Friend>?, Error?) -> Unit)? = null

    companion object {
        const val TAG = "KakaoSDKSample"

        fun newInstance(bundle: Bundle): OpenFragment =
            OpenFragment().apply {
                arguments = bundle
            }

        val defaultFeed = FeedTemplate(
            content = Content(
                title = "딸기 치즈 케익",
                description = "#케익 #딸기 #삼평동 #카페 #분위기 #소개팅",
                imageUrl = "http://mud-kage.kakao.co.kr/dn/Q2iNx/btqgeRgV54P/VLdBs9cvyn8BJXB3o7N8UK/kakaolink40_original.png",
                link = Link(
                    webUrl = "https://developers.kakao.com",
                    mobileWebUrl = "https://developers.kakao.com"
                )
            ),
            itemContent = ItemContent(
                profileText = "Kakao",
                profileImageUrl = "http://mud-kage.kakao.co.kr/dn/Q2iNx/btqgeRgV54P/VLdBs9cvyn8BJXB3o7N8UK/kakaolink40_original.png",
                titleImageUrl = "http://mud-kage.kakao.co.kr/dn/Q2iNx/btqgeRgV54P/VLdBs9cvyn8BJXB3o7N8UK/kakaolink40_original.png",
                titleImageText = "Cheese cake",
                titleImageCategory = "cake",
                items = listOf(
                    ItemInfo(item = "cake1", itemOp = "1000원"),
                    ItemInfo(item = "cake2", itemOp = "2000원"),
                    ItemInfo(item = "cake3", itemOp = "3000원"),
                    ItemInfo(item = "cake4", itemOp = "4000원"),
                    ItemInfo(item = "cake5", itemOp = "5000원")
                ),
                sum = "total",
                sumOp = "15000원"
            ),
            social = Social(
                likeCount = 286,
                commentCount = 45,
                sharedCount = 845
            ),
            buttons = listOf(
                Button(
                    "웹으로 보기",
                    Link(
                        webUrl = "https://developers.kakao.com",
                        mobileWebUrl = "https://developers.kakao.com"
                    )
                ),
                Button(
                    "앱으로 보기",
                    Link(
                        androidExecutionParams = mapOf("key1" to "value1", "key2" to "value2"),
                        iosExecutionParams = mapOf("key1" to "value1", "key2" to "value2")
                    )
                )
            )
        )

        val defaultList = ListTemplate(
            headerTitle = "WEEKLY MAGAZINE",
            headerLink = Link(
                webUrl = "https://developers.kakao.com",
                mobileWebUrl = "https://developers.kakao.com"
            ),
            contents = listOf(
                Content(
                    title = "취미의 특징, 탁구",
                    description = "스포츠",
                    imageUrl = "http://mud-kage.kakao.co.kr/dn/bDPMIb/btqgeoTRQvd/49BuF1gNo6UXkdbKecx600/kakaolink40_original.png",
                    link = Link(
                        webUrl = "https://developers.kakao.com",
                        mobileWebUrl = "https://developers.kakao.com"
                    )
                ),
                Content(
                    title = "크림으로 이해하는 커피이야기",
                    description = "음식",
                    imageUrl = "http://mud-kage.kakao.co.kr/dn/QPeNt/btqgeSfSsCR/0QJIRuWTtkg4cYc57n8H80/kakaolink40_original.png",
                    link = Link(
                        webUrl = "https://developers.kakao.com",
                        mobileWebUrl = "https://developers.kakao.com"
                    )
                ),
                Content(
                    title = "감성이 가득한 분위기",
                    description = "사진",
                    imageUrl = "http://mud-kage.kakao.co.kr/dn/c7MBX4/btqgeRgWhBy/ZMLnndJFAqyUAnqu4sQHS0/kakaolink40_original.png",
                    link = Link(
                        webUrl = "https://developers.kakao.com",
                        mobileWebUrl = "https://developers.kakao.com"
                    )
                )
            ),
            buttons = listOf(
                Button(
                    "웹으로 보기",
                    Link(
                        webUrl = "https://developers.kakao.com",
                        mobileWebUrl = "https://developers.kakao.com"
                    )
                ),
                Button(
                    "앱으로 보기",
                    Link(
                        androidExecutionParams = mapOf("key1" to "value1", "key2" to "value2"),
                        iosExecutionParams = mapOf("key1" to "value1", "key2" to "value2")
                    )
                )
            )
        )

        val defaultLocation = LocationTemplate(
            address = "경기 성남시 분당구 판교역로 235 에이치스퀘어 N동 8층",
            addressTitle = "카카오 판교오피스 카페톡",
            content = Content(
                title = "신메뉴 출시❤️ 체리블라썸라떼",
                description = "이번 주는 체리블라썸라떼 1+1",
                imageUrl = "http://mud-kage.kakao.co.kr/dn/bSbH9w/btqgegaEDfW/vD9KKV0hEintg6bZT4v4WK/kakaolink40_original.png",
                link = Link(
                    webUrl = "https://developers.com",
                    mobileWebUrl = "https://developers.kakao.com"
                )
            ),
            social = Social(
                likeCount = 286,
                commentCount = 45,
                sharedCount = 845
            )
        )

        val defaultCommerce = CommerceTemplate(
            content = Content(
                title = "Ivory long dress (4 Color)",
                imageUrl = "http://mud-kage.kakao.co.kr/dn/RY8ZN/btqgOGzITp3/uCM1x2xu7GNfr7NS9QvEs0/kakaolink40_original.png",
                link = Link(
                    webUrl = "https://developers.kakao.com",
                    mobileWebUrl = "https://developers.kakao.com"
                )
            ),
            commerce = Commerce(
                regularPrice = 208800,
                discountPrice = 146160,
                discountRate = 30,
                productName = "Ivory long dress",
                currencyUnit = "₩",
                currencyUnitPosition = 1
            ),
            buttons = listOf(
                Button(
                    "구매하기",
                    Link(
                        webUrl = "https://developers.kakao.com",
                        mobileWebUrl = "https://developers.kakao.com"
                    )
                ),
                Button(
                    "공유하기",
                    Link(
                        androidExecutionParams = mapOf("key1" to "value1", "key2" to "value2"),
                        iosExecutionParams = mapOf("key1" to "value1", "key2" to "value2")
                    )
                )
            )
        )

        val defaultText = TextTemplate(
            text = """
                카카오톡 공유는 카카오 플랫폼 서비스의 대표 기능으로써 사용자의 모바일 기기에 설치된 카카오 플랫폼과 연동하여 다양한 기능을 실행할 수 있습니다.
                현재 이용할 수 있는 카카오톡 공유는 다음과 같습니다.
                카카오톡링크
                카카오톡을 실행하여 사용자가 선택한 채팅방으로 메시지를 전송합니다.
            """.trimIndent(),
            link = Link(
                webUrl = "https://developers.kakao.com",
                mobileWebUrl = "https://developers.kakao.com"
            )
        )

    }
}
