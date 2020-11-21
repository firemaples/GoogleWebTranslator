package com.firemaples.googlewebtranslator

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import android.view.ViewGroup
import android.webkit.*
import kotlinx.coroutines.Job
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream

@SuppressLint("SetJavaScriptEnabled")
class GoogleWebTranslator(private val context: Context) {
    companion object {
        internal val tag: String = GoogleWebTranslator::class.java.simpleName
        private var stethoInitialized = false
    }

    private val maxTextSize = 5000

    private val webView: WebView by lazy {
        WebView(context).also {
            it.webViewClient = MyWebViewClient(context)
//            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            it.addJavascriptInterface(MyJSInterface(it), "MyJS")

            it.settings.apply {
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
//        if (DEBUG && !stethoInitialized) {
//            Stetho.initializeWithDefaults(context)
//        }
        threadUI.launch {
            webView.parent?.also { (it as ViewGroup).removeView(webView) }
            parentView.addView(webView,
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    public fun translate(_text: String, targetLang: String, callback: OnTranslationCallback) {
        threadUI.launch {
            if (webView.parent == null) {
                LogTool.e(tag, "Please call setup() before translation", true)
                return@launch
            }

            val text = _text.let {
                if (it.length > maxTextSize) {
                    LogTool.w(tag, "The maximum request text is limited to $maxTextSize, current is ${it.length}", true)
                    it.substring(0, maxTextSize)
                } else it
            }

            LogTool.d(tag, "Text to request[${text.length}]: $text")
            val url = "https://translate.google.com/?sl=auto&tl=$targetLang&ie=UTF-8&op=translate"
            webView.setTag(R.id.text, text)

            LogTool.i(tag, "load url: $url")
            webView.setTag(R.id.url, url)
            webView.setTag(R.id.startTime, System.currentTimeMillis())
            webView.setTag(R.id.callback, callback)

            threadUI.launch {
                callback.onStart()
            }

            webView.loadUrl(url)
        }
    }

    private class MyWebViewClient(val context: Context) : WebViewClient() {
        private val httpGet = "GET"
        private val utf8 = "UTF-8"
        private val urlLoadTranslationResult = "https://translate.google.com/translate_a/single?"
        private val tempABCD = "tempABCD"

        private val htmlTitleParser by lazy { Regex("<title>([\\s\\S]*?)<\\/title>") }


        val interceptorJS: String by lazy {
            context.assets.open("intercept-network-requests.js").bufferedReader().use { it.readText() }
        }

        private val okHttpClient by lazy {
            OkHttpClient.Builder().apply {
                cookieJar(WebViewCookieHandler())
//                if (DEBUG) {
//                    addNetworkInterceptor(StethoInterceptor())
//                }
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

                val sourceTextArea = "document.getElementsByTagName('textarea')[0]"
                loadJS(view, "$sourceTextArea.value=$tempABCD")
                loadJS(view, "function eventFire(el, etype){\n" +
                        "  if (el.fireEvent) {\n" +
                        "    el.fireEvent('on' + etype);\n" +
                        "  } else {\n" +
                        "    var evObj = document.createEvent('Events');\n" +
                        "    evObj.initEvent(etype, true, false);\n" +
                        "    el.dispatchEvent(evObj);\n" +
                        "  }\n" +
                        "}")
                loadJS(view, interceptorJS)
                loadJS(view, "$sourceTextArea.dispatchEvent(new Event('input', { bubbles: true}))")

                loadJSAsync(view, "$sourceTextArea.value") {
                    val requestText = it.substring(1, it.length - 1)
                    LogTool.i(tag, "Got 'source' value[${requestText.length}]: $requestText")
                    if (!it.isBlank()) {
                        view.setTag(R.id.textPost, requestText)
//                        val translateButton = "document.querySelectorAll(\"[aria-label='Translate']\")[0]"
//                        loadJS(view, "eventFire($translateButton,'mousedown')")
                        loadJS(view, "$sourceTextArea.dispatchEvent(new Event('input', { bubbles: true}))")
                    }
                }
            }
        }

        override fun onReceivedError(view: WebView, errorCode: Int, description: String?, failingUrl: String?) {
            super.onReceivedError(view, errorCode, description, failingUrl)
            handleError(view, errorCode, description, failingUrl)
        }

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onReceivedError(view: WebView, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            handleError(view, error?.errorCode, error?.description?.toString(), request?.url.toString())
        }

        private fun handleError(view: WebView, errorCode: Int?, description: String?, failingUrl: String?) {
            LogTool.e(tag, "handleError(), url: $failingUrl, error: ($errorCode)$description")
            val originUrl = view.getTag(R.id.url) as String?
            if (originUrl != null && failingUrl == originUrl) {
                view.postCallback {
                    it.onTranslationFailed("($errorCode) $description")
                }
            }
        }

        private fun loadJS(view: WebView, js: String) {
            view.postDelayed({
                val fullJS = "javascript:$js;void 0"
                LogTool.i(tag, "load JS: $fullJS")
                view.loadUrl(fullJS)
            }, 0)
        }

        private fun loadJSAsync(view: WebView, js: String, callback: ((String) -> Unit)? = null) {
            view.postDelayed({
                val fullJS = "javascript:$js"
                LogTool.i(tag, "load JSAsync: $fullJS")
                view.evaluateJavascript(fullJS) {
                    callback?.invoke(it)
                }
            }, 0)
        }

//        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
//            LogTool.d(tag, "shouldInterceptRequest: ${request.url}")
//            if (request.url.toString().startsWith(urlLoadTranslationResult, true)) {
//                val response = doInterceptRequest(view, request)
//                if (response != null) {
//                    return response
//                }
//            }
//            return super.shouldInterceptRequest(view, request)
//        }
//
//        private fun doInterceptRequest(view: WebView, originReq: WebResourceRequest): WebResourceResponse? {
//            LogTool.i(tag, "doInterceptRequest: ${originReq.method} ${originReq.url}")
//
//            val request = Request.Builder().apply {
//                url(originReq.url.toString())
//                when {
//                    httpGet.equals(originReq.method, true) ->
//                        method(httpGet, null)
//                    else ->
//                        method(originReq.method,
//                                FormBody.Builder().add("q",
//                                        view.getTag(R.id.textPost).toString()).build())
//                }
//                originReq.requestHeaders.forEach {
//                    header(it.key, it.value)
//                }
//            }.build()
//
//            LogTool.i(tag, "intercept: $request")
//
//            var errorMsg: String? = null
//
//            try {
//                val response = okHttpClient.newCall(request).execute()
//                val headers = response.headers().toMultimap()
//                        .map { it.key to it.value.firstOrNull() }.toMap()
//                val status = response.code()
//                val body = response.body()?.string()
//                val inputStream = body?.let {
//                    ByteArrayInputStream(it.toByteArray())
//                }
//                val timeSpent = (view.getTag(R.id.startTime) as Long?)?.let {
//                    "${System.currentTimeMillis() - it}ms"
//                }
//
//                if (response.isSuccessful) {
//                    if (body != null) {
//                        LogTool.i(tag, body)
//                        val result = ResultParser.parse(body)
//                        LogTool.i(tag, "result($timeSpent): ${result.text}")
//                        view.postCallback {
//                            it.onTranslated(result)
//                        }
//                        return WebResourceResponse(null, utf8,
//                                status, "OK",
//                                headers,
//                                inputStream)
//                    } else {
//                        LogTool.w(tag, "Response body is null($timeSpent)")
////                        return WebResourceResponse(null, utf8,
////                                status, "OK",
////                                headers,
////                                inputStream)
//                    }
//                } else {
//                    errorMsg = if (body != null) {
//                        htmlTitleParser.find(body)?.groupValues?.let {
//                            if (it.size >= 2) {
//                                it[1]
//                            } else null
//                        }
//                    } else null
//                    LogTool.w(tag, "Response failed($timeSpent): $status, $errorMsg, $body")
////                    return WebResourceResponse(null, utf8, status, "OK", headers, inputStream)
//                }
//            } catch (t: Throwable) {
//                LogTool.e(tag, "Error shown: ${Log.getStackTraceString(t)}")
//                errorMsg = t.localizedMessage
//            }
//
//            view.postCallback {
//                it.onTranslationFailed(errorMsg ?: "Unexpected result")
//            }
//
//            return null
//        }
    }

    @Suppress("unused")
    private class MyJSInterface(val webView: WebView) {
        @JavascriptInterface
        fun onTranslatedResult(text: String) {
            LogTool.d(tag, "On translated, $text")

            webView.postCallback {
                it.onTranslated(TranslatedResult().apply { this.text = text })
            }
        }
    }

    interface OnTranslationCallback {
        fun onStart()
        fun onTranslated(result: TranslatedResult)
        fun onTranslationFailed(errorMsg: String)
    }
}

private fun WebView.postCallback(block: (GoogleWebTranslator.OnTranslationCallback) -> Unit): Job = threadUI.launch {
    this@postCallback.getTag(R.id.callback)?.also {
        val callback = it as GoogleWebTranslator.OnTranslationCallback
        this@postCallback.setTag(R.id.callback, null)

        block(callback)
    }
}