package com.firemaples.googlewebtranslator.wigets

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.ViewGroup
import android.webkit.*
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import com.firemaples.googlewebtranslator.utils.Constants
import com.firemaples.googlewebtranslator.utils.Logger

@SuppressLint("SetJavaScriptEnabled")
internal class GTWebView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : WebView(context, attrs) {

    private val logger: Logger by lazy { Logger(this::class) }

    private val client: GTWebViewClient by lazy {
        GTWebViewClient { result, error ->

        }
    }
    private val chromeClient: GTWebChromeClient by lazy {
        GTWebChromeClient()
    }

    init {
        webChromeClient = chromeClient
        webViewClient = client

        with(settings) {
            javaScriptEnabled = true
//            builtInZoomControls = false
            blockNetworkImage = true
            databaseEnabled = false
            setGeolocationEnabled(false)
            javaScriptCanOpenWindowsAutomatically = false
            loadsImagesAutomatically = false
            setSupportMultipleWindows(false)
//            setSupportZoom(false)
        }
    }

    var callback: GTWebViewCallback? = null

    var startTime: Long = 0

    @MainThread
    fun setup(container: ViewGroup) {
        if (parent == container) return

        parent?.also { (it as ViewGroup).removeView(this) }
        container.addView(
            this,
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    @Throws(IllegalStateException::class)
    fun translate(text: String, targetLang: String) {
        if (parent == null) {
            throw IllegalStateException("Call setup() before translate()")
        }

        if (text.isBlank()) {
            logger.warn("The text to translate should not be blank")
            return
        }

        val textToTranslate = if (text.length > Constants.MAX_TEXT_LENGTH) {
            logger.warn("The length of the text is ${text.length}, it is larger than the max text length limit ${Constants.MAX_TEXT_LENGTH}")
            text.take(Constants.MAX_TEXT_LENGTH)
        } else text
        client.text = textToTranslate

        logger.debug("Translate with text(${text.length}): $text")
        startTime = System.currentTimeMillis()

        val url = Constants.getTranslationUrl(targetLang)

        logger.debug("Load default URL: $url")

        post { callback?.onTranslationStarted() }

        client.url = url
        loadUrl(url)
    }

    private class GTWebViewClient(
        private val onResult: (TranslationResult?, error: String?) -> Unit
    ) :
        WebViewClient() {
        private val logger: Logger by lazy { Logger(this::class) }

        private val variableTempText = "variableTempText"

        var url: String = ""
        var text: String = ""

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            handleError(error?.errorCode, error?.description?.toString(), request?.url?.toString())
        }

        override fun onReceivedError(
            view: WebView?,
            errorCode: Int,
            description: String?,
            failingUrl: String?
        ) {
            super.onReceivedError(view, errorCode, description, failingUrl)
            handleError(errorCode, description, failingUrl)
        }

        private fun handleError(
            errorCode: Int?,
            description: String?,
            failingUrl: String?
        ) {
            logger.error("handleError(), url: $failingUrl, error: ($errorCode)$description")
            if (failingUrl == url) {
                onResult.invoke(null, "($errorCode) $description")
            }
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            logger.debug("onPageFinished, url: $url")

            val text = text
            if (text.isBlank()) return

            text.chunked(Constants.TEXT_CHUNK_SIZE).forEachIndexed { index, s ->
                val js =
                    if (index == 0) "$variableTempText=\"$s\""
                    else "$variableTempText+=\"$s\""

                view.executeJS(js)
            }

            view.executeJS("${Constants.JS_SELECTOR_SOURCE_TEXT}.value=$variableTempText")
            view.executeJS(Constants.JS_FUNCTION_EVENT_FIRE)

            view.executeJS("${Constants.JS_SELECTOR_SOURCE_TEXT}.value") {
                val requestText = it.substring(1, it.length - 1)
                logger.debug("Got source value(${requestText.length})=$requestText")

                if (it.isNotBlank()) {
                    view.executeJS(
                        Constants.getEventFireJS(
                            Constants.JS_SELECTOR_TRANSLATE_BUTTON, Constants.JS_EVENT_MOUSE_DOWN
                        )
                    )
                }
            }
        }

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest
        ): WebResourceResponse? {
            logger.debug("shouldInterceptRequest(): ${request.url}")
            return super.shouldInterceptRequest(view, request)
        }

        private fun WebView.executeJS(js: String, callback: ((String) -> Unit)? = null) {
            postDelayed({
                val jsToLoad = Constants.getExecutableJS(js, callback != null)
                logger.debug("loadJS(): $jsToLoad")

                if (callback != null)
                    evaluateJavascript(jsToLoad) { callback.invoke(it) }
                else loadUrl(jsToLoad)
            }, 0)
        }
    }

    private class GTWebChromeClient : WebChromeClient() {
        private val logger: Logger by lazy { Logger(this::class) }

        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            logger.debug("onConsoleMessage(), level: ${consoleMessage?.messageLevel()}, message: ${consoleMessage?.message()}")
            return true
        }
    }

    interface GTWebViewCallback {
        fun onTranslationStarted()
        fun onTranslated()
        fun onTranslationFailed(error: String)
    }

    class TranslationResult {
        var text = ""

        var fromLanguage_didYouMean = false
        var fromLanguage_iso = ""

        var fromText_autoCorrected = false
        var fromText_value = ""
        var fromText_didYouMean = false

        var raw = ""
    }
}
