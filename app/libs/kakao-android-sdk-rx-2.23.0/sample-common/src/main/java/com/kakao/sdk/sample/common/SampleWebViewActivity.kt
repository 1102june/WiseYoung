/*
  Copyright 2021 Kakao Corp.

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
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import java.net.URISyntaxException

class SampleWebViewActivity : AppCompatActivity() {
    private lateinit var closeButton: ImageButton
    private lateinit var urlBar: EditText
    private lateinit var refreshButton: ImageButton
    private lateinit var clearButton: ImageButton
    private lateinit var homeButton: ImageButton
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var webViewLayout: ViewGroup
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            when {
                webView.canGoBack() -> webView.goBack()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample_web_view)

        closeButton = findViewById(R.id.close_Btn)
        urlBar = findViewById(R.id.url_et)
        refreshButton = findViewById(R.id.refresh_btn)
        clearButton = findViewById(R.id.clear_btn)
        homeButton = findViewById(R.id.home_btn)
        webView = findViewById(R.id.web_view)
        progressBar = findViewById(R.id.progress_bar)
        webViewLayout = findViewById(R.id.web_view_layout)

        closeButton.setOnClickListener {
            finish()
        }

        clearButton.setOnClickListener { urlBar.setText("") }

        urlBar.apply {
            setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    clearButton.visibility = View.VISIBLE
                    refreshButton.visibility = View.INVISIBLE
                } else {
                    clearButton.visibility = View.INVISIBLE
                    refreshButton.visibility = View.VISIBLE
                    if ((v as EditText).text.isEmpty()) {
                        v.setText(webView.url.toString())
                    }
                }
            }
            setOnKeyListener { v, keyCode, event ->
                if ((event.action == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    if ((v as EditText).text.isEmpty()) {
                        v.setText(webView.url.toString())
                    }
                    webView.loadUrl(v.text.toString())
                    hideKeyboard()
                    true
                } else {
                    false
                }
            }
        }

        refreshButton.setOnClickListener {
            webView.reload()
        }

        homeButton.setOnClickListener {
            webView.loadUrl(DEFAULT_URL)
            urlBar.hideKeyboard()
        }

        findViewById<ImageButton>(R.id.reset_btn).setOnClickListener {
            val cookieManager = CookieManager.getInstance()
            webView.clearCache(true)
            cookieManager.removeAllCookies {
                Log.d(TAG, "WebView Cookies are cleared: $it")
            }
            cookieManager.flush()
        }

        webView.apply {
            loadUrl(DEFAULT_URL)
            settings.domStorageEnabled = true
            settings.javaScriptEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(true)

            webChromeClient = getCustomWebChromeClient()
            webViewClient = getCustomWebViewClient()
        }

        // https://developer.android.com/about/versions/13/features/predictive-back-gesture#update-custom
        if (Build.VERSION.SDK_INT >= 33) {
            onBackPressedDispatcher.addCallback(onBackPressedCallback)
        }
    }

    @Deprecated("This method was deprecated in API level 33.")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
            return
        }
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    private fun getCustomWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                urlBar.setText(url)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                Log.d(TAG, request.url.toString())

                if (request.url.scheme == "intent") {
                    try {
                        // Intent 생성
                        val intent =
                            Intent.parseUri(request.url.toString(), Intent.URI_INTENT_SCHEME)

                        // 실행 가능한 앱이 있으면 앱 실행
                        if (intent.resolveActivity(packageManager) != null) {
                            startActivity(intent)
                            Log.d(TAG, "ACTIVITY: ${intent.`package`}")
                            return true
                        }

                        // Fallback URL이 있으면 현재 웹뷰에 로딩
                        val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                        if (fallbackUrl != null) {
                            view.loadUrl(fallbackUrl)
                            Log.d(TAG, "FALLBACK: $fallbackUrl")
                            return true
                        }
                        Log.e(TAG, "Could not parse anythings")

                    } catch (e: URISyntaxException) {
                        Log.e(TAG, "Invalid intent request", e)
                    }
                }
                return false
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)

                if (Build.VERSION.SDK_INT >= 33) {
                    onBackPressedCallback.isEnabled = view.canGoBack()
                }
            }
        }
    }

    private fun getCustomWebChromeClient(): WebChromeClient {
        return object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean {
                val childWebView = WebView(view.context)
                childWebView.apply {
                    settings.domStorageEnabled = true
                    settings.javaScriptEnabled = true
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    settings.setSupportMultipleWindows(true)
                    layoutParams = view.layoutParams
                    webViewClient = getCustomWebViewClient()
                    webChromeClient = getCustomWebChromeClient()
                }

                webViewLayout.addView(childWebView)

                val transport = resultMsg.obj as WebView.WebViewTransport
                transport.webView = childWebView
                resultMsg.sendToTarget()
                return true
            }

            override fun onCloseWindow(window: WebView) {
                webViewLayout.removeView(window)
                window.destroy()
                super.onCloseWindow(window)
            }
        }
    }

    private fun EditText.hideKeyboard() {
        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    }

    companion object {
        const val TAG = "Kakao SDK"
        const val DEFAULT_URL = "https://developers.kakao.com/tool/demo/login/login"
    }
}