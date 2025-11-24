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
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.auth.TokenManageable
import com.kakao.sdk.auth.TokenManager
import com.kakao.sdk.auth.TokenManagerProvider
import com.kakao.sdk.auth.model.CertType
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.auth.model.Prompt
import com.kakao.sdk.auth.network.RxAuthOperations
import com.kakao.sdk.cert.CertApiClient
import com.kakao.sdk.cert.rx
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.ApiError
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.common.model.KakaoSdkError
import com.kakao.sdk.common.util.KakaoCustomTabsClient
import com.kakao.sdk.common.util.KakaoJson
import com.kakao.sdk.friend.core.model.OpenPickerFriendRequestParams
import com.kakao.sdk.friend.client.rx
import com.kakao.sdk.friend.core.PickerClient
import com.kakao.sdk.friend.core.model.ViewType
import com.kakao.sdk.navi.Constants
import com.kakao.sdk.navi.NaviClient
import com.kakao.sdk.navi.model.CoordType
import com.kakao.sdk.navi.model.Location
import com.kakao.sdk.navi.model.NaviOption
import com.kakao.sdk.sample.common.cert.CertDemoClient
import com.kakao.sdk.sample.common.cert.RxCertDemoClient
import com.kakao.sdk.sample.common.cert.model.CertDemoError
import com.kakao.sdk.sample.common.internal.ApiAdapter
import com.kakao.sdk.sample.common.internal.FriendsActivity
import com.kakao.sdk.sample.common.internal.Log
import com.kakao.sdk.sample.common.internal.PickerItem
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.share.WebSharerClient
import com.kakao.sdk.share.rx
import com.kakao.sdk.talk.TalkApiClient
import com.kakao.sdk.talk.model.Order
import com.kakao.sdk.talk.rx
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
import com.kakao.sdk.user.model.AccountLoginParams
import com.kakao.sdk.user.model.LoginUiMode
import com.kakao.sdk.user.model.ScopeInfo
import com.kakao.sdk.user.rx
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream

class OpenFragmentRx : Fragment() {

    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var disposables: CompositeDisposable

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        Log.view = view
        disposables = CompositeDisposable()

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
                ApiAdapter.Item.ApiItem("loginWithKakaoTalk()") {

                    // 카카오톡으로 로그인
                    UserApiClient.rx.loginWithKakaoTalk(context)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ token ->
                            Log.i(TAG, "로그인 성공 ${token.accessToken}")
                        }, { error ->
                            Log.e(TAG, "로그인 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("loginWithKakaoAccount()") {

                    // 카카오계정으로 로그인
                    UserApiClient.rx.loginWithKakaoAccount(context)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ token ->
                            Log.i(TAG, "로그인 성공 ${token.accessToken}")
                        }, { error ->
                            Log.e(TAG, "로그인 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("loginWithKakao()") {
                    UserApiClient.rx.loginWithKakao(context, accountParams = AccountLoginParams(prompts = listOf(Prompt.LOGIN)))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ token ->
                            Log.i(TAG, "로그인 성공 ${token.accessToken}")
                        }, { error ->
                            Log.e(TAG, "로그인 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("loginWithKakaoAccount(prompts:login)") {

                    // 카카오계정으로 로그인 - 재인증
                    UserApiClient.rx.loginWithKakaoAccount(context, prompts = listOf(Prompt.LOGIN))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ token ->
                            Log.i(TAG, "로그인 성공 ${token.accessToken}")
                        }, { error ->
                            Log.e(TAG, "로그인 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("Combination Login") {

                    // 로그인 조합 예제

                    // 카카오톡이 설치되어 있으면 카카오톡으로 로그인, 아니면 카카오계정으로 로그인
                    if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                        UserApiClient.rx.loginWithKakaoTalk(context)
                            .observeOn(AndroidSchedulers.mainThread())
                            .onErrorResumeNext { error ->
                                // 유저에 의해서 카카오톡으로 로그인이 취소된 경우 카카오계정으로 로그인 생략 (ex 뒤로가기)
                                if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                                    Single.error(error)
                                } else {
                                    // 카카오톡에 로그인이 안되어있는 경우 카카오계정으로 로그인
                                    UserApiClient.rx.loginWithKakaoAccount(context)
                                }
                            }.observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ token ->
                                Log.i(TAG, "로그인 성공 ${token.accessToken}")
                            }, { error ->
                                Log.e(TAG, "로그인 실패", error)
                            }).addTo(disposables)
                    } else {
                        UserApiClient.rx.loginWithKakaoAccount(context)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ token ->
                                Log.i(TAG, "로그인 성공 ${token.accessToken}")
                            }, { error ->
                                Log.e(TAG, "로그인 실패", error)
                            }).addTo(disposables)
                    }
                },
                ApiAdapter.Item.ApiItem("me()") {

                    // 사용자 정보 요청 (기본)
                    UserApiClient.rx.me()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ user ->
                            Log.i(
                                TAG, "사용자 정보 요청 성공" +
                                        "\n회원번호: ${user.id}" +
                                        "\n이메일: ${user.kakaoAccount?.email}" +
                                        "\n닉네임: ${user.kakaoAccount?.profile?.nickname}" +
                                        "\n프로필사진: ${user.kakaoAccount?.profile?.thumbnailImageUrl}"
                            )
                        }, { error ->
                            Log.e(TAG, "사용자 정보 요청 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("me() - new scopes") {

                    // 사용자 정보 요청 (추가 동의)

                    // 사용자가 로그인 시 제3자 정보제공에 동의하지 않은 개인정보 항목 중 어떤 정보가 반드시 필요한 시나리오에 진입한다면
                    // 다음과 같이 추가 동의를 받고 해당 정보를 획득할 수 있습니다.

                    //  * 주의: 선택 동의항목은 사용자가 거부하더라도 서비스 이용에 지장이 없어야 합니다.

                    // 이메일 필수 시나리오 예제
                    UserApiClient.rx.me()
                        .flatMap { user ->
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
                                // InsufficientScope 에러 생성
                                Single.error(ApiError.fromScopes(scopes))
                            } else {
                                Single.just(user)
                            }
                        }
                        .retryWhen(
                            // InsufficientScope 에러에 대해 추가 동의 후 재요청
                            RxAuthOperations.instance.incrementalAuthorizationRequired(context)
                        )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ user ->
                            Log.i(TAG, "사용자 정보 요청 성공 $user")
                        }, { error ->
                            Log.e(TAG, "사용자 정보 요청 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("signup()") {
                    UserApiClient.rx.signup()
                        .subscribeOn(Schedulers.io())
                        .subscribe({ showSnackbar("signup() 성공") }, onError)
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("scopes()") {
                    UserApiClient.rx.scopes()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ scopeInfo: ScopeInfo? ->
                            Log.i(TAG, "동의 정보 조회 성공\n 현재 가지고 있는 동의 항목 $scopeInfo")
                        }, { error: Throwable? ->
                            Log.e(TAG, "동의 정보 조회 실패", error)
                        }).addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("scopes() - optional") {
                    val scopes = mutableListOf("account_email", "friends")
                    UserApiClient.rx.scopes(scopes)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ scopeInfo: ScopeInfo? ->
                            Log.i(TAG, "동의 정보 조회 성공\n 현재 가지고 있는 동의 항목 $scopeInfo")
                        }, { error: Throwable? ->
                            Log.e(TAG, "동의 정보 조회 실패", error)
                        }).addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("revokeScopes()") {
                    val scopes = mutableListOf("account_email", "friends")
                    UserApiClient.rx.revokeScopes(scopes)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ scopeInfo: ScopeInfo? ->
                            Log.i(TAG, "동의 철회 성공\n 현재 가지고 있는 동의 항목 $scopeInfo")
                        }, { error: Throwable? ->
                            Log.e(TAG, "동의 철회 실패", error)
                        }).addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("accessTokenInfo()") {

                    // 토큰 정보 조회
                    UserApiClient.rx.accessTokenInfo()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ tokenInfo ->
                            Log.i(
                                TAG, "토큰 정보 조회 성공" +
                                        "\n회원번호: ${tokenInfo.id}" +
                                        "\n만료시간: ${tokenInfo.expiresIn} 초"
                            )
                        }, { error ->
                            Log.e(TAG, "토큰 정보 조회 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("updateProfile()") {

                    // 사용자 정보 저장

                    // 변경할 내용
                    val properties = mapOf("custom_key" to "${System.currentTimeMillis()}")

                    UserApiClient.rx.updateProfile(properties)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            Log.i(TAG, "사용자 정보 저장 성공")
                        }, { error ->
                            Log.e(TAG, "사용자 정보 저장 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("selectShippingAddress() ") {
                    // 배송지 피커 호출
                    UserApiClient.rx.selectShippingAddress(requireContext())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ addressId ->
                            Log.i(OpenFragment.TAG, "배송지 선택 성공 $addressId")
                        }, { error ->
                            Log.e(OpenFragment.TAG, "배송지 선택 실패 $error")
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("shippingAddresses()") {

                    // 배송지 조회 (추가 동의)
                    UserApiClient.rx.shippingAddresses()
                        .flatMap { userShippingAddresses ->
                            if (userShippingAddresses.needsAgreement == true) {
                                Log.d(TAG, "사용자에게 배송지 제공 동의를 받아야 합니다.")

                                // InsufficientScope 에러 생성
                                Single.error(ApiError.fromScopes(listOf("shipping_address")))
                            } else {
                                Single.just(userShippingAddresses)
                            }
                        }
                        .retryWhen(
                            // InsufficientScope 에러에 대해 추가 동의 후 재요청
                            RxAuthOperations.instance.incrementalAuthorizationRequired(context)
                        )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ userShippingAddresses ->
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
                            } else {
                                Log.e(TAG, "사용자 계정에 배송지 없음. 꼭 필요하다면 동의항목 설정에서 수집 기능을 활성화 해보세요.")
                            }
                        }, { error ->
                            Log.e(TAG, "배송지 조회 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("serviceTerms()") {

                    // 동의한 약관 조회
                    UserApiClient.rx.serviceTerms()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ userServiceTerms ->
                            Log.i(
                                TAG, "동의한 약관 조회 성공" +
                                        "\n회원번호: ${userServiceTerms.id}" +
                                        "\n동의한 약관: \n${userServiceTerms.serviceTerms?.joinToString("\n")}"
                            )
                        }, { error ->
                            Log.e(TAG, "동의한 약관 조회 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("serviceTerms(app_service_terms)") {

                    // 전체 약관 조회
                    UserApiClient.rx.serviceTerms(result = "app_service_terms")
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ userServiceTerms ->
                            Log.i(
                                TAG, "전체 약관 조회 성공" +
                                        "\n회원번호: ${userServiceTerms.id}" +
                                        "\n약관: \n${userServiceTerms.serviceTerms?.joinToString("\n")}"
                            )
                        }, { error ->
                            Log.e(TAG, "전체 약관 조회 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("serviceTerms(tags)") {

                    // 특정 약관 조회
                    UserApiClient.rx.serviceTerms(tags = listOf("policy"))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ userServiceTerms ->
                            Log.i(
                                TAG, "약관 조회 성공" +
                                        "\n회원번호: ${userServiceTerms.id}" +
                                        "\n약관: \n${userServiceTerms.serviceTerms?.joinToString("\n")}"
                            )
                        }, { error ->
                            Log.e(TAG, "약관 조회 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("revokeServiceTerms()") {

                    // 약관 철회
                    UserApiClient.rx.revokeServiceTerms(listOf("revoke_test"))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ revokedServiceTerms ->
                            Log.i(
                                TAG, "약관 철회 성공" +
                                        "\n회원번호: ${revokedServiceTerms.id}" +
                                        "\n철회한 약관: \n${
                                            revokedServiceTerms.revokedServiceTerms?.joinToString(
                                                "\n"
                                            )
                                        }"
                            )
                        }, { error ->
                            Log.e(TAG, "약관 철회 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("logout()") {

                    // 로그아웃
                    UserApiClient.rx.logout()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            Log.i(TAG, "로그아웃 성공. SDK에서 토큰 폐기 됨")
                        }, { error ->
                            Log.e(TAG, "로그아웃 실패. SDK에서 토큰 폐기 됨", error)
                        }).addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("unlink()") {

                    // 연결 해제
                    UserApiClient.rx.unlink()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            Log.i(TAG, "연결 해제 성공. SDK에서 토큰 폐기 됨")
                        }, { error ->
                            Log.e(TAG, "연결 해제 실패", error)
                        }).addTo(disposables)
                },
                ApiAdapter.Item.Header("KakaoTalk API"),
                ApiAdapter.Item.ApiItem("profile()") {

                    // 카카오톡 프로필 조회
                    TalkApiClient.rx.profile()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ profile ->
                            Log.i(
                                TAG, "카카오톡 프로필 조회 성공" +
                                        "\n닉네임: ${profile.nickname}" +
                                        "\n프로필사진: ${profile.thumbnailUrl}" +
                                        "\n국가코드: ${profile.countryISO}"
                            )
                        }, { error ->
                            Log.e(TAG, "카카오톡 프로필 조회 실패", error)
                        }).addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("sendCustomMemo()") {

                    // 커스텀 템플릿으로 나에게 보내기

                    // 메시지 템플릿 아이디
                    //  * 만들기 가이드: https://developers.kakao.com/docs/latest/ko/message-template/common
                    val templateId = bundle.getLong("customMessage")

                    TalkApiClient.rx.sendCustomMemo(templateId)
                        .retryWhen(
                            // InsufficientScope 에러에 대해 추가 동의 후 재요청
                            RxAuthOperations.instance.incrementalAuthorizationRequired(context)
                        )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            Log.i(TAG, "나에게 보내기 성공")
                        }, { error ->
                            Log.e(TAG, "나에게 보내기 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("sendDefaultMemo()") {

                    // 디폴트 템플릿으로 나에게 보내기 - Feed
                    TalkApiClient.rx.sendDefaultMemo(defaultFeed)
                        .retryWhen(
                            // InsufficientScope 에러에 대해 추가 동의 후 재요청
                            RxAuthOperations.instance.incrementalAuthorizationRequired(context)
                        )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            Log.i(TAG, "나에게 보내기 성공")
                        }, { error ->
                            Log.e(TAG, "나에게 보내기 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("sendScrapMemo()") {

                    // 스크랩 템플릿으로 나에게 보내기

                    // 공유할 웹페이지 URL
                    //  * 주의: 개발자사이트 Web 플랫폼 설정에 공유할 URL의 도메인이 등록되어 있어야 합니다.
                    val url = "https://developers.kakao.com"

                    TalkApiClient.rx.sendScrapMemo(url)
                        .retryWhen(
                            // InsufficientScope 에러에 대해 추가 동의 후 재요청
                            RxAuthOperations.instance.incrementalAuthorizationRequired(context)
                        )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            Log.i(TAG, "나에게 보내기 성공")
                        }, { error ->
                            Log.e(TAG, "나에게 보내기 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("friends()") {

                    // 카카오톡 친구 목록 조회 (기본)
                    TalkApiClient.rx.friends()
                        .retryWhen(
                            // InsufficientScope 에러에 대해 추가 동의 후 재요청
                            RxAuthOperations.instance.incrementalAuthorizationRequired(context)
                        )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ friends ->
                            Log.i(TAG, "카카오톡 친구 목록 조회 성공 \n${friends.elements?.joinToString("\n")}")

                            // 친구의 UUID 로 메시지 보내기 가능
                        }, { error ->
                            Log.e(TAG, "카카오톡 친구 목록 조회 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("friends(order:) - desc") {

                    // 카카오톡 친구 목록 조회 (파라미터)

                    // 내림차순으로 받기
                    TalkApiClient.rx.friends(order = Order.DESC)
                        .retryWhen(
                            // InsufficientScope 에러에 대해 추가 동의 후 재요청
                            RxAuthOperations.instance.incrementalAuthorizationRequired(context)
                        )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ friends ->
                            Log.i(TAG, "카카오톡 친구 목록 조회 성공 \n${friends.elements?.joinToString("\n")}")

                            // 친구의 UUID 로 메시지 보내기 가능
                        }, { error ->
                            Log.e(TAG, "카카오톡 친구 목록 조회 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("sendCustomMessage()") {

                    // 커스텀 템플릿으로 친구에게 메시지 보내기

                    // 카카오톡 친구 목록 조회
                    TalkApiClient.rx.friends()

                        // 서비스에 상황에 맞게 메시지 보낼 친구의 UUID 를 가져오세요.
                        // 이 샘플에서는 친구 목록을 화면에 보여주고 체크박스로 선택된 친구들의 UUID 를 수집하도록 구현했습니다.
                        .flatMap { friends ->
                            friends.elements?.let {
                                FriendsActivity.startForResult(
                                    context,
                                    it.map { friend ->
                                        PickerItem(
                                            friend.uuid,
                                            friend.profileNickname ?: "",
                                            friend.profileThumbnailImage
                                        )
                                    }
                                )
                            } ?: Single.just(listOf())
                        }
                        .observeOn(Schedulers.io())
                        .flatMap { selectedItems ->

                            // 메시지 보낼 친구의 UUID 목록
                            val receiverUuids = selectedItems

                            // 메시지 템플릿 아이디
                            //  * 만들기 가이드: https://developers.kakao.com/docs/latest/ko/message-template/common
                            val templateId = bundle.getLong("customMessage")

                            TalkApiClient.rx.sendCustomMessage(receiverUuids, templateId)
                        }
                        .retryWhen(
                            // InsufficientScope 에러에 대해 추가 동의 후 재요청
                            RxAuthOperations.instance.incrementalAuthorizationRequired(context)
                        )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ result ->
                            Log.i(TAG, "메시지 보내기 성공 ${result.successfulReceiverUuids}")

                            if (result.failureInfos != null) {
                                Log.d(
                                    TAG,
                                    "메시지 보내기에 일부 성공했으나, 일부 대상에게는 실패 \n${result.failureInfos}"
                                )
                            }
                        }, { error ->
                            Log.e(TAG, "메시지 보내기 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("sendDefaultMessage()") {

                    // 디폴트 템플릿으로 친구에게 메시지 보내기 - Feed

                    // 카카오톡 친구 목록 조회
                    TalkApiClient.rx.friends()

                        // 서비스에 상황에 맞게 메시지 보낼 친구의 UUID 를 가져오세요.
                        // 이 샘플에서는 친구 목록을 화면에 보여주고 체크박스로 선택된 친구들의 UUID 를 수집하도록 구현했습니다.
                        .flatMap { friends ->
                            friends.elements?.let {
                                FriendsActivity.startForResult(
                                    context,
                                    it.map { friend ->
                                        PickerItem(
                                            friend.uuid,
                                            friend.profileNickname ?: "",
                                            friend.profileThumbnailImage
                                        )
                                    }
                                )
                            } ?: Single.just(listOf())
                        }
                        .observeOn(Schedulers.io())
                        .flatMap { selectedItems ->

                            // 메시지 보낼 친구의 UUID 목록
                            val receiverUuids = selectedItems

                            // Feed 메시지
                            val template = defaultFeed

                            TalkApiClient.rx.sendDefaultMessage(receiverUuids, template)
                        }
                        .retryWhen(
                            // InsufficientScope 에러에 대해 추가 동의 후 재요청
                            RxAuthOperations.instance.incrementalAuthorizationRequired(context)
                        )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ result ->
                            Log.i(TAG, "메시지 보내기 성공 ${result.successfulReceiverUuids}")

                            if (result.failureInfos != null) {
                                Log.d(
                                    TAG,
                                    "메시지 보내기에 일부 성공했으나, 일부 대상에게는 실패 \n${result.failureInfos}"
                                )
                            }
                        }, { error ->
                            Log.e(TAG, "메시지 보내기 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("sendScrapMessage()") {

                    // 스크랩 템플릿으로 친구에게 메시지 보내기

                    // 카카오톡 친구 목록 조회
                    TalkApiClient.rx.friends()

                        // 서비스에 상황에 맞게 메시지 보낼 친구의 UUID 를 가져오세요.
                        // 이 샘플에서는 친구 목록을 화면에 보여주고 체크박스로 선택된 친구들의 UUID 를 수집하도록 구현했습니다.
                        .flatMap { friends ->
                            friends.elements?.let {
                                FriendsActivity.startForResult(
                                    context,
                                    it.map { friend ->
                                        PickerItem(
                                            friend.uuid,
                                            friend.profileNickname ?: "",
                                            friend.profileThumbnailImage
                                        )
                                    }
                                )
                            } ?: Single.just(listOf())
                        }
                        .observeOn(Schedulers.io())
                        .flatMap { selectedItems ->

                            // 메시지 보낼 친구의 UUID 목록
                            val receiverUuids = selectedItems

                            // 공유할 웹페이지 URL
                            //  * 주의: 개발자사이트 Web 플랫폼 설정에 공유할 URL의 도메인이 등록되어 있어야 합니다.
                            val url = "https://developers.kakao.com"

                            TalkApiClient.rx.sendScrapMessage(receiverUuids, url)
                        }
                        .retryWhen(
                            // InsufficientScope 에러에 대해 추가 동의 후 재요청
                            RxAuthOperations.instance.incrementalAuthorizationRequired(context)
                        )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ result ->
                            Log.i(TAG, "메시지 보내기 성공 ${result.successfulReceiverUuids}")

                            if (result.failureInfos != null) {
                                Log.d(
                                    TAG,
                                    "메시지 보내기에 일부 성공했으나, 일부 대상에게는 실패 \n${result.failureInfos}"
                                )
                            }
                        }, { error ->
                            Log.e(TAG, "메시지 보내기 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("channels()") {

                    // 카카오톡 채널 관계 조회
                    TalkApiClient.rx.channels()
                        .retryWhen(
                            // InsufficientScope 에러에 대해 추가 동의 후 재요청
                            RxAuthOperations.instance.incrementalAuthorizationRequired(context)
                        )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ relations ->
                            Log.i(TAG, "채널 관계 조회 성공 \n${relations.channels}")
                        }, { error ->
                            Log.e(TAG, "채널 관계 조회 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("followChannel()") {
                    val channelPublicId = bundle.getString("channelId")!!

                    TalkApiClient.rx.followChannel(context, channelPublicId)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ result ->
                            Log.i(OpenFragment.TAG, "채널 추가 성공 $result")
                        }, { error ->
                            Log.i(OpenFragment.TAG, "채널 추가 실패 $error")
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("addChannel()") {
                    // 카카오톡 채널 추가
                    val channelPublicId = bundle.getString("channelId")

                    TalkApiClient.rx.addChannel(context, channelPublicId!!)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            Log.i(TAG, "성공")
                        }, { error ->
                            Log.e(TAG, "실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("chatChannel()") {
                    // 카카오톡 채널 채팅
                    val channelPublicId = bundle.getString("channelId")

                    TalkApiClient.rx.channelChat(context, channelPublicId!!)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            Log.i(TAG, "성공")
                        }, { error ->
                            Log.e(TAG, "실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("addChannelUrl()") {

                    // 카카오톡 채널 추가 URL
                    val channelPublicId = bundle.getString("channelId")
                    val url = TalkApiClient.instance.addChannelUrl(channelPublicId!!)

                    // CustomTabs 로 열기
                    KakaoCustomTabsClient.openWithDefault(context, url)
                },
                ApiAdapter.Item.ApiItem("chatChannelUrl()") {

                    // 카카오톡 채널 채팅 URL
                    val channelPublicId = bundle.getString("channelId")
                    val url = TalkApiClient.instance.chatChannelUrl(channelPublicId!!)

                    // CustomTabs 로 열기
                    KakaoCustomTabsClient.openWithDefault(context, url)
                },
                ApiAdapter.Item.Header("Friend API"),
                ApiAdapter.Item.ApiItem("멀티 친구 피커") {
                    val params = OpenPickerFriendRequestParams(title = "멀티 친구 피커")

                    PickerClient.rx.selectFriend(context, params, ViewType.FULL)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ selectedUsers ->
                            Log.i(TAG, "친구 선택 성공 $selectedUsers")
                        }, { error ->
                            Log.e(TAG, "친구 선택 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("싱글 친구 피커") {
                    val params = OpenPickerFriendRequestParams(title = "싱글 친구 피커")

                    PickerClient.rx.selectFriend(context, params, ViewType.FULL, false)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ selectedUsers ->
                            Log.i(TAG, "친구 선택 성공 $selectedUsers")
                        }, { error ->
                            Log.e(TAG, "친구 선택 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("멀티 친구 피커 팝업") {
                    val params = OpenPickerFriendRequestParams(title = "멀티 친구 피커")

                    PickerClient.rx.selectFriend(context, params, ViewType.POPUP)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ selectedUsers ->
                            Log.i(TAG, "친구 선택 성공 $selectedUsers")
                        }, { error ->
                            Log.e(TAG, "친구 선택 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("싱글 친구 피커 팝업") {
                    val params = OpenPickerFriendRequestParams(title = "싱글 친구 피커")

                    PickerClient.rx.selectFriend(context, params, ViewType.POPUP, false)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ selectedUsers ->
                            Log.i(TAG, "친구 선택 성공 $selectedUsers")
                        }, { error ->
                            Log.e(TAG, "친구 선택 실패", error)
                        })
                        .addTo(disposables)
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

                    // 메시지 템플릿 아이디
                    //  * 만들기 가이드: https://developers.kakao.com/docs/latest/ko/message-template/common
                    val templateId = bundle.getLong("customMemo")

                    ShareClient.rx.shareCustom(context, templateId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ sharingResult ->
                            Log.d(TAG, "카카오톡 공유 성공 ${sharingResult.intent}")
                            startActivity(sharingResult.intent)

                            // 카카오톡 공유에 성공했지만 아래 경고 메시지가 존재할 경우 일부 컨텐츠가 정상 동작하지 않을 수 있습니다.
                            Log.w(TAG, "Warning Msg: ${sharingResult.warningMsg}")
                            Log.w(TAG, "Argument Msg: ${sharingResult.argumentMsg}")
                        }, { error ->
                            Log.e(TAG, "카카오톡 공유 실패 ", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("scrapTemplate()") {

                    // 스크랩 템플릿으로 카카오톡 공유 메시지 발송

                    // 공유할 웹페이지 URL
                    //  * 주의: 개발자사이트 Web 플랫폼 설정에 공유할 URL의 도메인이 등록되어 있어야 합니다.
                    val url = "https://developers.kakao.com"

                    ShareClient.rx.shareScrap(context, url)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ sharingResult ->
                            Log.d(TAG, "카카오톡 공유 성공 ${sharingResult.intent}")
                            startActivity(sharingResult.intent)

                            // 카카오톡 공유에 성공했지만 아래 경고 메시지가 존재할 경우 일부 컨텐츠가 정상 동작하지 않을 수 있습니다.
                            Log.w(TAG, "Warning Msg: ${sharingResult.warningMsg}")
                            Log.w(TAG, "Argument Msg: ${sharingResult.argumentMsg}")
                        }, { error ->
                            Log.e(TAG, "카카오톡 공유 실패 ", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("defaultTemplate()") {

                    // 디폴트 템플릿으로 카카오톡 공유 메시지 발송 - Feed
                    ShareClient.rx.shareDefault(context, defaultFeed)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ sharingResult ->
                            Log.d(TAG, "카카오톡 공유 성공 ${sharingResult.intent}")
                            startActivity(sharingResult.intent)

                            // 카카오톡 공유에 성공했지만 아래 경고 메시지가 존재할 경우 일부 컨텐츠가 정상 동작하지 않을 수 있습니다.
                            Log.w(TAG, "Warning Msg: ${sharingResult.warningMsg}")
                            Log.w(TAG, "Argument Msg: ${sharingResult.argumentMsg}")
                        }, { error ->
                            Log.e(TAG, "카카오톡 공유 실패 ", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("defaultTemplate() - list") {

                    // 디폴트 템플릿으로 카카오톡 공유 메시지 발송 - List
                    ShareClient.rx.shareDefault(context, defaultList)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ sharingResult ->
                            Log.d(TAG, "카카오톡 공유 성공 ${sharingResult.intent}")
                            startActivity(sharingResult.intent)

                            // 카카오톡 공유에 성공했지만 아래 경고 메시지가 존재할 경우 일부 컨텐츠가 정상 동작하지 않을 수 있습니다.
                            Log.w(TAG, "Warning Msg: ${sharingResult.warningMsg}")
                            Log.w(TAG, "Argument Msg: ${sharingResult.argumentMsg}")
                        }, { error ->
                            Log.e(TAG, "카카오톡 공유 실패 ", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("defaultTemplate() - location") {

                    // 디폴트 템플릿으로 카카오톡 공유 메시지 발송 - Location
                    ShareClient.rx.shareDefault(context, defaultLocation)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ sharingResult ->
                            Log.d(TAG, "카카오톡 공유 성공 ${sharingResult.intent}")
                            startActivity(sharingResult.intent)

                            // 카카오톡 공유에 성공했지만 아래 경고 메시지가 존재할 경우 일부 컨텐츠가 정상 동작하지 않을 수 있습니다.
                            Log.w(TAG, "Warning Msg: ${sharingResult.warningMsg}")
                            Log.w(TAG, "Argument Msg: ${sharingResult.argumentMsg}")
                        }, { error ->
                            Log.e(TAG, "카카오톡 공유 실패 ", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("defaultTemplate() - commerce") {

                    // 디폴트 템플릿으로 카카오톡 공유 메시지 발송 - Commerce
                    ShareClient.rx.shareDefault(context, defaultCommerce)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ sharingResult ->
                            Log.d(TAG, "카카오톡 공유 성공 ${sharingResult.intent}")
                            startActivity(sharingResult.intent)

                            // 카카오톡 공유에 성공했지만 아래 경고 메시지가 존재할 경우 일부 컨텐츠가 정상 동작하지 않을 수 있습니다.
                            Log.w(TAG, "Warning Msg: ${sharingResult.warningMsg}")
                            Log.w(TAG, "Argument Msg: ${sharingResult.argumentMsg}")
                        }, { error ->
                            Log.e(TAG, "카카오톡 공유 실패 ", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("defaultTemplate() - text") {

                    // 디폴트 템플릿으로 카카오톡 공유 메시지 발송 - Text
                    ShareClient.rx.shareDefault(context, defaultText)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ sharingResult ->
                            Log.d(TAG, "카카오톡 공유 성공 ${sharingResult.intent}")
                            startActivity(sharingResult.intent)

                            // 카카오톡 공유에 성공했지만 아래 경고 메시지가 존재할 경우 일부 컨텐츠가 정상 동작하지 않을 수 있습니다.
                            Log.w(TAG, "Warning Msg: ${sharingResult.warningMsg}")
                            Log.w(TAG, "Argument Msg: ${sharingResult.argumentMsg}")
                        }, { error ->
                            Log.e(TAG, "카카오톡 공유 실패 ", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("defaultTemplate() - calendar") {

                    // 디폴트 템플릿으로 카카오톡 공유 메시지 발송 - Calendar
                    val defaultCalendar = createCalendarTemplate(bundle)
                    ShareClient.rx.shareDefault(context, defaultCalendar)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ sharingResult ->
                            Log.d(TAG, "카카오톡 공유 성공 ${sharingResult.intent}")
                            startActivity(sharingResult.intent)

                            // 카카오톡 공유에 성공했지만 아래 경고 메시지가 존재할 경우 일부 컨텐츠가 정상 동작하지 않을 수 있습니다.
                            Log.w(TAG, "Warning Msg: ${sharingResult.warningMsg}")
                            Log.w(TAG, "Argument Msg: ${sharingResult.argumentMsg}")
                        }, { error ->
                            Log.e(TAG, "카카오톡 공유 실패 ", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("customTemplateUri() - web sharer") {

                    // 커스텀 템플릿으로 웹에서 카카오톡 공유 메시지 발송
                    //  * 만들기 가이드: https://developers.kakao.com/docs/latest/ko/message-template/common
                    val templateId = bundle.getLong("customMemo")

                    val sharerUrl = WebSharerClient.instance.makeCustomUrl(templateId)

                    KakaoCustomTabsClient.openWithDefault(context, sharerUrl)
                },
                ApiAdapter.Item.ApiItem("scrapTemplateUri() - web sharer") {

                    // 스크랩 템플릿으로 웹에서 카카오톡 공유 메시지 발송

                    // 공유할 웹페이지 URL
                    //  * 주의: 개발자사이트 Web 플랫폼 설정에 공유할 URL의 도메인이 등록되어 있어야 합니다.
                    val url = "https://developers.kakao.com"

                    val sharerUrl = WebSharerClient.instance.makeScrapUrl(url)

                    KakaoCustomTabsClient.openWithDefault(context, sharerUrl)
                },
                ApiAdapter.Item.ApiItem("defaultTemplateUri() - web sharer - feed") {

                    // 디폴트 템플릿으로 웹에서 카카오톡 공유 메시지 발송 - Feed
                    val sharerUrl = WebSharerClient.instance.makeDefaultUrl(defaultFeed)

                    KakaoCustomTabsClient.openWithDefault(context, sharerUrl)
                },
                ApiAdapter.Item.ApiItem("defaultTemplateUri() - web sharer - location") {

                    // 디폴트 템플릿으로 웹에서 카카오톡 공유 메시지 발송 - Feed
                    val sharerUrl = WebSharerClient.instance.makeDefaultUrl(defaultLocation)

                    KakaoCustomTabsClient.openWithDefault(context, sharerUrl)
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
                    ShareClient.rx.uploadImage(file)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ imageUploadResult ->
                            Log.i(TAG, "이미지 업로드 성공 \n${imageUploadResult.infos.original}")
                        }, { error ->
                            Log.e(TAG, "이미지 업로드 실패", error)
                        }).addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("scrapImage()") {

                    // 이미지 스크랩

                    // 원본 원격 이미지 URL
                    val url =
                        "https://t1.kakaocdn.net/kakaocorp/Service/KakaoTalk/pc/slide/talkpc_theme_01.jpg"

                    // 카카오 이미지 서버로 업로드
                    ShareClient.rx.scrapImage(url)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ imageUploadResult ->
                            Log.i(TAG, "이미지 스크랩 성공 \n${imageUploadResult.infos.original}")
                        }, { error ->
                            Log.e(TAG, "이미지 스크랩 실패", error)
                        }).addTo(disposables)
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
                    if (NaviClient.instance.isKakaoNaviInstalled(requireContext())) {
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
                    if (NaviClient.instance.isKakaoNaviInstalled(requireContext())) {
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
                    if (NaviClient.instance.isKakaoNaviInstalled(requireContext())) {
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
                    if (NaviClient.instance.isKakaoNaviInstalled(requireContext())) {
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
                    if (NaviClient.instance.isKakaoNaviInstalled(requireContext())) {
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
                    if (NaviClient.instance.isKakaoNaviInstalled(requireContext())) {
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
                    UserApiClient.rx.loginWithKakaoTalk(
                        context = context,
                        serviceTerms = serviceTerms
                    )
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ token ->
                            Log.i(TAG, "로그인 성공 ${token.accessToken}")
                        }, { error ->
                            Log.e(TAG, "로그인 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("login(serviceTerms:) - empty") {

                    // 약관 동의 받지 않기

                    // serviceTerms 파라미터에 empty list 전달해서 카카오톡으로 로그인 요청 (카카오계정으로 로그인도 사용법 동일)
                    UserApiClient.rx.loginWithKakaoTalk(
                        context = context,
                        serviceTerms = listOf()
                    )
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ token ->
                            Log.i(TAG, "로그인 성공 ${token.accessToken}")
                        }, { error ->
                            Log.e(TAG, "로그인 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.Header("OIDC"),
                ApiAdapter.Item.ApiItem("loginWithKakaoTalk(nonce:openidtest)") {

                    // 카카오톡으로 로그인 - openId
                    UserApiClient.rx.loginWithKakaoTalk(context, nonce = "openidtest")
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ token ->
                            Log.i(TAG, "로그인 성공 id_token: ${token.idToken}")
                        }, { error ->
                            Log.e(TAG, "로그인 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("loginWithKakaoAccount(nonce:openidtest)") {

                    // 카카오계정으로 로그인 - openId
                    UserApiClient.rx.loginWithKakaoAccount(context, nonce = "openidtest")
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ token ->
                            Log.i(TAG, "로그인 성공 id_token: ${token.idToken}")
                        }, { error ->
                            Log.e(TAG, "로그인 실패", error)
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("me() - new scopes(nonce:openidtest)") {

                    // 사용자 정보 요청 (추가 동의)

                    // 사용자가 로그인 시 제3자 정보제공에 동의하지 않은 개인정보 항목 중 어떤 정보가 반드시 필요한 시나리오에 진입한다면
                    // 다음과 같이 추가 동의를 받고 해당 정보를 획득할 수 있습니다.

                    //  * 주의: 선택 동의항목은 사용자가 거부하더라도 서비스 이용에 지장이 없어야 합니다.

                    // 이메일 필수 시나리오 예제
                    UserApiClient.rx.me()
                        .flatMap { user ->
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

                            // OpenID 활성화 후
                            // - scope 파라메터에 openid 항목을 포함하여 요청할 경우 OIDC로 동작
                            // - scope 파라메터에 openid 항목을 미포함 시 OAuth2.0 으로 동작

                            // OIDC 요청이므로 "openid" 항목을 추가한다
                            scopes.add("openid")

                            // open id 외에 추가 동의가 필요한 항목이 있는 경우
                            if (scopes.size > 1) {
                                Log.d(TAG, "사용자에게 추가 동의를 받아야 합니다.")
                                // InsufficientScope 에러 생성
                                Single.error(ApiError.fromScopes(scopes))
                            } else {
                                Single.just(user)
                            }
                        }
                        .retryWhen(
                            // InsufficientScope 에러에 대해 추가 동의 후 재요청
                            RxAuthOperations.instance.incrementalAuthorizationRequired(context)
                        )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ user ->
                            Log.i(TAG, "사용자 정보 요청 성공 $user")
                        }, { error ->
                            Log.e(TAG, "사용자 정보 요청 실패", error)
                        })
                        .addTo(disposables)
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
                            context.getSharedPreferences("test_preferences", Context.MODE_PRIVATE)
                        val tokenKey = "test_token_key"

                        override fun getToken(): OAuthToken? =
                            preferences.getString(tokenKey, "")?.let {
                                KakaoJson.decodeFromString<OAuthToken>(it)
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
                    Log.i(TAG, "hasToken() usage")
                    if (AuthApiClient.instance.hasToken()) {
                        UserApiClient.rx.accessTokenInfo()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
                                //토큰 유효성 체크 성공(필요시 토큰갱신됨)
                            }, { err ->
                                if (err is KakaoSdkError && err.isInvalidTokenError()) {
                                    //login 필요
                                    UserApiClient.rx.loginWithKakaoAccount(context)
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe({ token ->
                                            Log.i(TAG, "로그인 성공 ${token.accessToken}")
                                        }, { error ->
                                            Log.e(TAG, "로그인 실패", error)
                                        })
                                        .addTo(disposables)
                                } else {
                                    //기타에러
                                }

                            })
                            .addTo(disposables)
                    } else {
                        //login 필요
                    }
                },
                ApiAdapter.Item.ApiItem("loginWithAccount()") {
                    UserApiClient.rx.loginWithKakaoAccount(context)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { token -> Log.i(TAG, token.accessToken) },
                            { error -> Log.e(TAG, "$error") })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("me()") {
                    UserApiClient.rx.me()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { user -> Log.i(TAG, "${user.id} ${user.kakaoAccount?.email}") },
                            { error -> Log.e(TAG, "$error") })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("logout()") {
                    UserApiClient.rx.logout()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ Log.i(TAG, "logout") }, { error -> Log.e(TAG, "$error") })
                        .addTo(disposables)
                },
                ApiAdapter.Item.Header("Cert - K2220"),
                ApiAdapter.Item.ApiItem("[SDK] publicKey()") {
                    val publicKey = CertApiClient.rx.publicKey(CertType.K2220)
                    ApiResultDialog.show(context, publicKey, "PublicKey")
                },
                ApiAdapter.Item.ApiItem("[SDK] sessionInfo request") {
                    val title = "SessionInfo"
                    val txId = RxCertDemoClient.instance(context).getTxId(CertType.K2220) ?: run {
                        val error = CertDemoError("txId is null. login first")
                        ApiResultDialog.show(context, error, title)
                        return@ApiItem
                    }

                    CertApiClient.rx.sessionInfo(CertType.K2220, txId)
                        .subscribe(
                            { sessionInfo ->
                                ApiResultDialog.show(context, sessionInfo, title)
                            },
                            { error ->
                                ApiResultDialog.show(context, error, title)
                            },
                        )
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("[SDK] sessionInfo()") {
                    val sessionInfo = CertApiClient.rx.sessionInfo(CertType.K2220)
                    ApiResultDialog.show(context, sessionInfo, "SessionInfo")
                },
                ApiAdapter.Item.ApiItem("[SDK] isValidSession()") {
                    val isValidSession = CertApiClient.rx.isValidSession(CertType.K2220)
                    ApiResultDialog.show(context, isValidSession, "IsValidSession")
                },
                ApiAdapter.Item.ApiItem("[SDK] deleteKeyPair()") {
                    AlertDialog.Builder(context)
                        .setTitle("deleteKeyPair()")
                        .setMessage("이전에 생성된 키쌍과 저장되어있는 세션 정보가 삭제됩니다")
                        .setPositiveButton("OK") { dialog, _ ->
                            CertApiClient.rx.deleteKeyPair(CertType.K2220)

                            // 이용기관 txId 삭제 필요
                            RxCertDemoClient.instance(context).deleteTxId(CertType.K2220)

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

                    val publicKey = CertApiClient.rx.publicKey(CertType.K2220) ?: run {
                        RxCertDemoClient.instance(context).deleteTxId(CertType.K2220)
                        val error = CertDemoError("publicKey is null")
                        ApiResultDialog.show(context, error)
                        return@ApiItem
                    }
                    val settleId = bundle.getString("settleId")

                    RxCertDemoClient.instance(context).demoSign(publicKey)
                        .flatMap { txId ->
                            CertApiClient.rx.certLoginWithKakaoTalk(
                                context,
                                CertType.K2220,
                                txId = txId,
                                settleId = settleId
                            )
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ certTokenInfo ->
                            RxCertDemoClient.instance(context)
                                .saveTxId(certTokenInfo.txId, CertType.K2220)
                            ApiResultDialog.show(context, certTokenInfo)
                        }, { error ->
                            ApiResultDialog.show(context, error)

                            if (error is KakaoSdkError) {
                                // 실패시 키생성, txId발급 로그인을 다시 해야함
                                CertApiClient.rx.deleteKeyPair(CertType.K2220)
                                RxCertDemoClient.instance(context).deleteTxId(CertType.K2220)
                            }
                        })
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("[이용기관] demoVerify(txId, appUserId)") {
                    val txId = RxCertDemoClient.instance(context).getTxId(CertType.K2220) ?: run {
                        val error = CertDemoError("txId is null. login first.")
                        ApiResultDialog.show(context, error, "Verify")
                        return@ApiItem
                    }

                    UserApiClient.rx.me()
                        .flatMap { user ->
                            val userId = user.id ?: throw IllegalStateException("user.id is null")
                            RxCertDemoClient.instance(context).demoVerify(txId, userId)
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { response -> ApiResultDialog.show(context, response, "Verify") },
                            { error -> ApiResultDialog.show(context, error, "Verify") }
                        )
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("[이용기관] demoReducedSign(txId, data, signature)") {
                    val txId = RxCertDemoClient.instance(context).getTxId(CertType.K2220) ?: run {
                        val error = CertDemoError("txId is null. login first.")
                        ApiResultDialog.show(context, error, "Sign")
                        return@ApiItem
                    }
                    val signData = "hello"

                    CertApiClient.rx.reducedSign(CertType.K2220, signData)
                        .flatMap { signature ->
                            RxCertDemoClient.instance(context)
                                .demoReducedSign(txId, signData, signature, CertType.K2220)
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { response -> ApiResultDialog.show(context, response, "Sign") },
                            { error -> ApiResultDialog.show(context, error, "Sign") }
                        )
                        .addTo(disposables)
                },
                ApiAdapter.Item.Header("Cert - K3220"),
                ApiAdapter.Item.ApiItem("[SDK] publicKey()") {
                    val publicKey = CertApiClient.rx.publicKey(certType = CertType.K3220)
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

                    CertApiClient.rx.sessionInfoByAppKey(
                        certType = CertType.K3220,
                        txId = txId,
                        targetAppKey = targetAppKey,
                    )
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { sessionInfo -> ApiResultDialog.show(context, sessionInfo, title) },
                            { error -> ApiResultDialog.show(context, error, title) },
                        )
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("[SDK] sessionInfo()") {
                    val sessionInfo = CertApiClient.rx.sessionInfo(certType = CertType.K3220)
                    ApiResultDialog.show(context, sessionInfo, "SessionInfo")
                },
                ApiAdapter.Item.ApiItem("[SDK] isValidSession()") {
                    val isValidSession =
                        CertApiClient.rx.isValidSession(certType = CertType.K3220)
                    ApiResultDialog.show(context, isValidSession, "IsValidSession")
                },
                ApiAdapter.Item.ApiItem("[SDK] deleteKeyPair()") {
                    AlertDialog.Builder(context)
                        .setTitle("deleteKeyPair()")
                        .setMessage("이전에 생성된 키쌍과 저장되어있는 세션 정보가 삭제됩니다")
                        .setPositiveButton("OK") { dialog, _ ->
                            CertApiClient.rx.deleteKeyPair(certType = CertType.K3220)

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

                    if (!CertApiClient.rx.isKakaoTalkSignAvailable(context)) {
                        val error = CertDemoError("KakaoTalk Sign is not available")
                        ApiResultDialog.show(context, error)

                        // 카카오톡 설치 호출 예시
                        // val uri = Uri.parse("market://launch?id=com.kakao.talk")
                        // startActivity(Intent(Intent.ACTION_VIEW, uri))
                        return@ApiItem
                    }

                    val publicKey = CertApiClient.rx.publicKey(certType = CertType.K3220) ?: run {
                        CertDemoClient.instance(context).deleteTxId(CertType.K3220)
                        val error = CertDemoError("publicKey is null")
                        ApiResultDialog.show(context, error)
                        return@ApiItem
                    }


                    RxCertDemoClient.instance(context).demoSign(
                        publicKey = publicKey,
                        returnUrl = "${KakaoSdk.applicationContextInfo.customScheme}://cert",
                        validateAppKey = KakaoSdk.appKey,
                        targetAppKey = targetAppKey
                    )
                        .flatMap { txId ->
                            CertApiClient.rx.signWithKakaoTalk(
                                context = context,
                                certType = CertType.K3220,
                                txId = txId,
                                targetAppKey = targetAppKey
                            )
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { signStatus ->
                                ApiResultDialog.show(context, signStatus)
                                RxCertDemoClient.instance(context)
                                    .saveTxId(signStatus.txId, CertType.K3220)
                            },
                            { error ->
                                ApiResultDialog.show(context, error)

                                if (error is KakaoSdkError) {
                                    CertApiClient.rx.deleteKeyPair(certType = CertType.K3220)
                                    CertDemoClient.instance(context).deleteTxId(CertType.K3220)
                                }
                            },
                        )
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("[이용기관] demoVerify(txId, appUserId)") {
                    val targetAppKey = bundle.getString("targetAppKey") ?: ""
                    val txId = CertDemoClient.instance(context).getTxId(CertType.K3220) ?: run {
                        val error = CertDemoError("txId is null. sign first.")
                        ApiResultDialog.show(context, error, "Verify")
                        return@ApiItem
                    }

                    RxCertDemoClient.instance(context)
                        .demoVerify(txId = txId, targetAppKey = targetAppKey)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { response -> ApiResultDialog.show(context, response, "Verify") },
                            { error -> ApiResultDialog.show(context, error, "Verify") },
                        )
                        .addTo(disposables)
                },
                ApiAdapter.Item.ApiItem("[이용기관] demoReducedSign(txId, data, signature)") {
                    val txId = CertDemoClient.instance(context).getTxId(CertType.K3220) ?: run {
                        val error = CertDemoError("txId is null. sign first.")
                        ApiResultDialog.show(context, error, "reducedSign")
                        return@ApiItem
                    }
                    val signData = "hello"

                    CertApiClient.rx.reducedSign(data = signData, certType = CertType.K3220)
                        .flatMap { signature ->
                            RxCertDemoClient.instance(context).demoReducedSign(
                                txId = txId,
                                data = signData,
                                signature = signature,
                                certType = CertType.K3220,
                            )
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { response -> ApiResultDialog.show(context, response, "reducedSign") },
                            { error -> ApiResultDialog.show(context, error, "reducedSign") },
                        )
                        .addTo(disposables)
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
        val view = inflater.inflate(R.layout.fragment_open, container, false)
        return view
    }

    override fun onDestroy() {
        disposables.dispose()
        super.onDestroy()
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

    private fun showSnackbar(any: Any) {
        val snackbar =
            Snackbar.make(requireView(), any.toString(), BaseTransientBottomBar.LENGTH_LONG)
        val layout = snackbar.view as Snackbar.SnackbarLayout
        layout.minimumHeight = 100
        snackbar.show()
    }

    private val onSuccess = Consumer<Any> {
        showSnackbar(it)
    }

    private val onError = Consumer<Throwable> {
        showSnackbar(it)
    }

    private lateinit var bundle: Bundle

    companion object {
        const val TAG = "KakaoSDKSample"

        fun newInstance(bundle: Bundle): OpenFragmentRx =
            OpenFragmentRx().apply {
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
