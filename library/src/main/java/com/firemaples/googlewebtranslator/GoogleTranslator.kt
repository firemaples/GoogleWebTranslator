package com.firemaples.googlewebtranslator

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream

const val chunkSize = 50000

@SuppressLint("SetJavaScriptEnabled")
class GoogleTranslator(private val context: Context) {
    private val tag = GoogleTranslator::class.java.name

    private val webView: WebView by lazy {
        WebView(context).apply {
            webViewClient = MyWebViewClient()
//            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            settings.apply {
                //                userAgentString = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36"
                javaScriptEnabled = true
//                builtInZoomControls = false
                blockNetworkImage = true
                databaseEnabled = false
                setGeolocationEnabled(false)
                javaScriptCanOpenWindowsAutomatically = false
                loadsImagesAutomatically = false
                setSupportMultipleWindows(false)
//                setSupportZoom(false)
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

    public fun translate(_text: String, targetLang: String) {
        threadUI.launch {
            if (webView.parent == null) {
                LogTool.e(tag, "Please call setup() before translation")
                return@launch
            }

            val text = _text//.substring(0, 2000)
            LogTool.d(tag, "substring: $text")

            LogTool.d(tag, "text size: ${text.length}")
            val url: String
//            if (text.length <= chunkSize) {
//                url = "https://translate.google.com/m/translate?sl=auto&tl=$targetLang&ie=UTF-8&text=$text"
//            } else {
            url = "https://translate.google.com/m/translate?sl=auto&tl=$targetLang&ie=UTF-8"
            webView.setTag(R.id.text, text)
//            }

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

        private val okHttpClient by lazy {
            OkHttpClient.Builder().apply {
                cookieJar(WebviewCookieHandler())
            }.build()
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            LogTool.i(tag, "onPageFinished, url: $url")

            val tagText = view.getTag(R.id.text)
            if (tagText != null) {
                view.setTag(R.id.text, null)

                val text = tagText.toString()

                text.chunked(100).forEachIndexed { index, s ->
                    val js = if (index == 0) {
                        "$tempABCD=\"$s\""
                    } else {
                        "$tempABCD+=\"$s\""
                    }
                    loadJS(view, js)
                }

                loadJS(view, "document.getElementById('source').value=$tempABCD")
                loadJS(view, "function eventFire(el, etype){\n" +
                        "  if (el.fireEvent) {\n" +
                        "    el.fireEvent('on' + etype);\n" +
                        "  } else {\n" +
                        "    var evObj = document.createEvent('Events');\n" +
                        "    evObj.initEvent(etype, true, false);\n" +
                        "    el.dispatchEvent(evObj);\n" +
                        "  }\n" +
                        "}")
                loadJS(view, "document.getElementById('source').value") {
                    LogTool.i(tag, "Got 'source' value: $it")
                    if (!it.isBlank()) {
                        view.setTag(R.id.textPost, it)
                        loadJS(view, "eventFire(document.getElementsByClassName('go-button')[0],'mousedown')")
                    }
                }


                //Base64
//                val base64Text = Base64.encodeToString(text.toByteArray(), Base64.DEFAULT)
//                val chunkedText = base64Text.chunked(1000)
//
//                chunkedText.forEachIndexed { index, s ->
//                    val js: String = if (index == 0) {
//                        "$tempABCD=\"$s\""
//                    } else {
//                        "$tempABCD+=\"$s\""
//                    }
//
//                    loadJS(view, js)
//                }
//

//                val js = "document.getElementById('source').value=window.atob($tempABCD)"
//                loadJS(view, js, 1000L)
            }
        }

        private fun loadJS(view: WebView, js: String, callback: ((String) -> Unit)? = null) {
            view.postDelayed({
                val fullJS = "javascript:$js" //;void 0
                LogTool.i(tag, "load JS: $fullJS")
                view.evaluateJavascript(fullJS) {
                    callback?.invoke(it)
                }
            }, 0)
        }

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            LogTool.d(tag, "shouldInterceptRequest: ${request.url}")
            if (request.url.toString().startsWith(urlLoadTranslationResult, true)) {
                val response = doInterceptRequest(view, request)
                if (response != null) {
                    return response
                }
            }
            return super.shouldInterceptRequest(view, request)
        }

        private fun doInterceptRequest(view: WebView, originReq: WebResourceRequest): WebResourceResponse? {
            LogTool.i(tag, "doInterceptRequest: ${originReq.method} ${originReq.url}")

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
                    } else {
                        LogTool.w(tag, "Response body is null")
                    }
                } else {
                    LogTool.w(tag, "Response failed: ${response.code()}, ${response.body()?.string()}")
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }

            return null
        }
    }
}