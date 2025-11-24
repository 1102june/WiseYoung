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

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.kakao.sdk.auth.model.IdentifyItem
import com.kakao.sdk.auth.model.Prompt
import com.kakao.sdk.sample.common.R

class ParameterChoiceDialogFragment : DialogFragment() {
    private lateinit var callback: (DialogResult?) -> Unit
    private lateinit var method: String
    private var settleId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        retainInstance = true
        return inflater.inflate(R.layout.parameter_choice_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.let {
            method = it.getString(KEY_METHOD_NAME) ?: throw IllegalStateException()
            settleId = it.getString(KEY_SETTLE_ID)
        }

        initDialog()
    }

    private fun initDialog() {
        setTitle()
        setPrompts()
        setIdentifyItems()
        setLoginHints()
        setScopes()
        setState()
        setSettleId()
        setChannelPublicIds()
        setServiceTerms()

        initButtons()
    }

    private fun setTitle() {
        requireView().findViewById<TextView>(R.id.title_tv).text = method
    }

    private fun setPrompts() {
        val layout = requireView().findViewById<LinearLayout>(R.id.prompts_linear_layout)
        layout.visibility = when (method) {
            LOGIN_WITH_KAKAO_TALK, LOGIN_WITH_NEW_SCOPES, CERT_LOGIN_WITH_KAKAO_TALK -> View.GONE
            else -> View.VISIBLE
        }
    }

    private fun setIdentifyItems() {
        val layout =
            requireView().findViewById<LinearLayout>(R.id.sign_identify_items_linear_layout)
        layout.visibility = when (method) {
            LOGIN_WITH_KAKAO_TALK, LOGIN_WITH_KAKAO_ACCOUNT, LOGIN_WITH_NEW_SCOPES -> View.GONE
            else -> View.VISIBLE
        }
    }

    private fun setLoginHints() {
        val layout = requireView().findViewById<LinearLayout>(R.id.login_hints_linear_layout)
        layout.visibility = when (method) {
            LOGIN_WITH_KAKAO_TALK, LOGIN_WITH_NEW_SCOPES, CERT_LOGIN_WITH_KAKAO_TALK -> View.GONE
            else -> View.VISIBLE
        }
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private fun setScopes() {
        val scopesSwitch = requireView().findViewById<Switch>(R.id.scopes_switch)
        scopesSwitch.isChecked = true
        scopesSwitch.isClickable = false
        val layout = requireView().findViewById<LinearLayout>(R.id.scopes_linear_layout)
        layout.visibility = when (method) {
            LOGIN_WITH_KAKAO_TALK, LOGIN_WITH_KAKAO_ACCOUNT, CERT_LOGIN_WITH_KAKAO_TALK, CERT_LOGIN_WITH_KAKAO_ACCOUNT -> View.GONE
            else -> View.VISIBLE
        }
    }

    private fun setState() {
        val layout = requireView().findViewById<LinearLayout>(R.id.state_linear_layout)
        layout.visibility = when (method) {
            LOGIN_WITH_KAKAO_TALK, LOGIN_WITH_KAKAO_ACCOUNT, LOGIN_WITH_NEW_SCOPES -> View.GONE
            else -> View.VISIBLE
        }
    }

    private fun setSettleId() {
        requireView().findViewById<Switch>(R.id.settle_id_switch).apply {
            isChecked = true
            isClickable = false
        }
        val settleIdLayout = requireView().findViewById<LinearLayout>(R.id.settle_id_linear_layout)
        settleId?.let {
            requireView().findViewById<EditText>(R.id.settle_id_et).setText(settleId)
        }
        requireView().findViewById<EditText>(R.id.settle_id_et).isFocusable = false
        settleIdLayout.visibility = when (method) {
            LOGIN_WITH_KAKAO_TALK, LOGIN_WITH_KAKAO_ACCOUNT, LOGIN_WITH_NEW_SCOPES -> View.GONE
            else -> View.VISIBLE
        }
    }

    private fun setChannelPublicIds() {
        val layout = requireView().findViewById<LinearLayout>(R.id.channel_public_ids_linear_layout)
        layout.visibility = when (method) {
            LOGIN_WITH_NEW_SCOPES -> View.GONE
            else -> View.VISIBLE
        }
    }

    private fun setServiceTerms() {
        val layout = requireView().findViewById<LinearLayout>(R.id.service_terms_linear_layout)
        layout.visibility = when (method) {
            LOGIN_WITH_NEW_SCOPES -> View.GONE
            else -> View.VISIBLE
        }
    }

    private fun initButtons() {
        requireView().findViewById<Button>(R.id.cancel_btn).setOnClickListener {
            dismiss()
            callback(null)
        }
        requireView().findViewById<Button>(R.id.request_btn).setOnClickListener {
            dismiss()
            callback(getDialogResult())
        }
    }

    private fun getDialogResult(): DialogResult {
        val prompts = mutableListOf<Prompt>()
        val identifyItems = mutableListOf<IdentifyItem>()
        var loginHints: String? = null
        val scopes = mutableListOf<String>()
        var state: String? = null
        var nonce: String? = null
        var settleId: String? = null
        val channelPublicIds = mutableListOf<String>()
        val serviceTerms = mutableListOf<String>()

        if (requireView().findViewById<Switch>(R.id.prompts_switch).isChecked) {
            if (requireView().findViewById<CheckBox>(R.id.prompts_login_checkbox).isChecked) {
                prompts.add(Prompt.LOGIN)
            }
            if (requireView().findViewById<CheckBox>(R.id.prompts_create_checkbox).isChecked) {
                prompts.add(Prompt.CREATE)
            }
            if (requireView().findViewById<CheckBox>(R.id.prompts_select_account_checkbox).isChecked) {
                prompts.add(Prompt.SELECT_ACCOUNT)
            }
        }

        if (requireView().findViewById<Switch>(R.id.login_hint_switch).isChecked) {
            loginHints = requireView().findViewById<EditText>(R.id.login_hint_et).text.toString()
        }
        if (requireView().findViewById<Switch>(R.id.scopes_switch).isChecked) {
            scopes.addAll(
                requireView().findViewById<EditText>(R.id.scopes_et).text.toString().split(",")
            )
        }
        if (requireView().findViewById<Switch>(R.id.state_switch).isChecked) {
            state = requireView().findViewById<EditText>(R.id.state_et).text.toString()
        }
        if (requireView().findViewById<Switch>(R.id.nonce_switch).isChecked) {
            nonce = requireView().findViewById<EditText>(R.id.nonce_et).text.toString()
        }
        if (requireView().findViewById<Switch>(R.id.settle_id_switch).isChecked) {
            settleId = requireView().findViewById<EditText>(R.id.settle_id_et).text.toString()
        }

        if (requireView().findViewById<Switch>(R.id.identify_items_switch).isChecked) {
            if (requireView().findViewById<CheckBox>(R.id.identify_phone_number_checkbox).isChecked) {
                identifyItems.add(IdentifyItem.PHONE_NUMBER)
            }
            if (requireView().findViewById<CheckBox>(R.id.identify_ci_checkbox).isChecked) {
                identifyItems.add(IdentifyItem.CI)
            }
            if (requireView().findViewById<CheckBox>(R.id.identify_name_checkbox).isChecked) {
                identifyItems.add(IdentifyItem.NAME)
            }
            if (requireView().findViewById<CheckBox>(R.id.identify_birthday_checkbox).isChecked) {
                identifyItems.add(IdentifyItem.BIRTHDAY)
            }
        }

        if (requireView().findViewById<Switch>(R.id.channel_public_ids_switch).isChecked) {
            channelPublicIds.addAll(
                requireView().findViewById<EditText>(R.id.channel_public_ids_et).text.toString()
                    .split(",")
            )
        }
        if (requireView().findViewById<Switch>(R.id.service_terms_switch).isChecked) {
            serviceTerms.addAll(
                requireView().findViewById<EditText>(R.id.service_terms_et).text.toString()
                    .split(",")
            )
        }
        return DialogResult(
            prompts = prompts,
            loginHints = loginHints,
            scopes = scopes,
            state = state,
            nonce = nonce,
            settleId = settleId,
            identifyItems = identifyItems,
            channelPublicIds = channelPublicIds,
            serviceTerms = serviceTerms,
        )
    }

    companion object {
        private const val KEY_METHOD_NAME = "key.method.name"
        private const val KEY_SETTLE_ID = "key.settleId"

        const val LOGIN_WITH_KAKAO_TALK = "loginWithKakaoTalk"
        const val LOGIN_WITH_KAKAO_ACCOUNT = "loginWithKakaoAccount"
        const val LOGIN_WITH_NEW_SCOPES = "loginWithNewScopes"
        const val CERT_LOGIN_WITH_KAKAO_TALK = "certLoginWithKakaoTalk"
        const val CERT_LOGIN_WITH_KAKAO_ACCOUNT = "certLoginWithKakaoAccount"

        fun newInstance(
            method: String,
            settleId: String? = null,
            callback: (result: DialogResult?) -> Unit,
        ): ParameterChoiceDialogFragment {
            val arguments = Bundle().apply {
                putString(KEY_METHOD_NAME, method)
                settleId?.let { putString(KEY_SETTLE_ID, it) }
            }
            return ParameterChoiceDialogFragment().apply {
                this.arguments = arguments
                this.callback = callback
            }
        }
    }
}
