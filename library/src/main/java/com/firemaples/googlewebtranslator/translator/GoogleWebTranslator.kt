package com.firemaples.googlewebtranslator.translator

import android.content.Context
import android.view.ViewGroup
import com.firemaples.googlewebtranslator.utils.Logger
import com.firemaples.googlewebtranslator.wigets.GTWebView

class GoogleWebTranslator(private val context: Context) {
    private val logger: Logger by lazy { Logger(this::class) }

    private val webView: GTWebView by lazy {
        GTWebView(context).apply {
            onResult = { text, error ->
                this@GoogleWebTranslator.onResult?.invoke(text, error)
            }
        }
    }

    var onResult: ((text: String?, error: String?) -> Unit)? = null

    fun setup(container: ViewGroup) {
        webView.setup(container)
    }

    fun translate(text: String, targetLang: String) {
        webView.translate(text, targetLang)
    }
}