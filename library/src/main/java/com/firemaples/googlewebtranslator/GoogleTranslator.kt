package com.firemaples.googlewebtranslator

import android.annotation.SuppressLint
import android.content.Context
import android.text.Html
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.net.URLEncoder

const val chunkSize = 500

@SuppressLint("SetJavaScriptEnabled")
class GoogleTranslator(private val context: Context) {
    private val tag = GoogleTranslator::class.java.name

    private val webView: WebView by lazy {
        WebView(context).apply {
            webViewClient = MyWebViewClient()
            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            settings.apply {
                javaScriptEnabled = true
                builtInZoomControls = false
                blockNetworkImage = true
                databaseEnabled = false
                setGeolocationEnabled(false)
                javaScriptCanOpenWindowsAutomatically = false
                loadsImagesAutomatically = false
                setSupportMultipleWindows(false)
                setSupportZoom(false)
            }
        }
    }

    public fun setup(parentView: ViewGroup) {
        threadUI.launch {
            webView.parent?.also { (it as ViewGroup).removeView(webView) }
            parentView.addView(webView,
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    public fun translate(text: String, targetLang: String) {
        threadUI.launch {
            if (webView.parent == null) {
                LogTool.e(tag, "Please call setup() before translation")
                return@launch
            }

            val url: String
            if (text.length <= chunkSize) {
                url = "https://translate.google.com/m/translate?sl=auto&tl=$targetLang&ie=UTF-8&text=$text"
            } else {
                url = "https://translate.google.com/m/translate?sl=auto&tl=$targetLang&ie=UTF-8"
                webView.setTag(R.id.text, text)
            }

            LogTool.i(tag, "load url: $url")
            webView.loadUrl(url)
        }
    }

    private class MyWebViewClient : WebViewClient() {
        private val tag = MyWebViewClient::class.java.name

        private val httpGet = "GET"
        private val utf8 = "UTF-8"
        private val urlLoadTranslationResult = "https://translate.google.com/translate_a/single?"
        private val tempABCD = "tempABCD"

        private val okHttpClient by lazy { OkHttpClient() }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            LogTool.i(tag, "onPageFinished, url: $url")

            val tagText = view.getTag(R.id.text)
            if (tagText != null) {
                view.setTag(R.id.text, null)

                val text = tagText.toString()
                val chunkedText = text.chunked(chunkSize)

                chunkedText.forEachIndexed { index, s ->
                    val js: String
                    val encodedText = URLEncoder.encode(s, utf8)
                    if (index == 0) {
                        js = "javascript:$tempABCD=\"$encodedText\";void 0"
                    } else {
                        js = "javascript:$tempABCD+=\"$encodedText\";void 0"
                    }
                    LogTool.i(tag, "load JS: $js")
                    view.loadUrl(url)
                }

                view.setTag(R.id.textPost, text)
                view.loadUrl("javascript:document.getElementById('source').value=$tempABCD;void 0")
            }
        }

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            if (request.url.toString().startsWith(urlLoadTranslationResult, true)) {
                val response = doInterceptRequest(view, request)
                if (response != null) {
                    return response
                }
            }
            return super.shouldInterceptRequest(view, request)
        }

        private fun doInterceptRequest(view: WebView, originReq: WebResourceRequest): WebResourceResponse? {
            val request = Request.Builder().apply {
                url(originReq.url.toString())
                when {
                    httpGet.equals(originReq.method, true) ->
                        method(httpGet, null)
                    else ->
                        method(originReq.method,
                                FormBody.Builder().add("q",
                                        view.getTag(R.id.textPost).toString()).build())
                }
                originReq.requestHeaders.forEach {
                    header(it.key, it.value)
                }
            }.build()

            LogTool.i(tag, "intercept: $request")

            try {
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val raw = response.body()?.string()
                    if (raw != null) {
                        LogTool.i(tag, raw)
                        val result = ResultParser.parse(raw)
                        LogTool.i(tag, "result: ${result.text}")
                        val headers = response.headers().toMultimap()
                                .map { it.key to it.value.firstOrNull() }.toMap()
                        return WebResourceResponse(null, utf8,
                                response.code(), "OK",
                                headers,
                                ByteArrayInputStream(raw.toByteArray()))
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }

            return null
        }
    }
}