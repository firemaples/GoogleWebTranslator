package com.example.testapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.webkit.WebView
import android.widget.ArrayAdapter
import com.firemaples.googlewebtranslator.GoogleWebTranslator
import com.firemaples.googlewebtranslator.Language
import com.firemaples.googlewebtranslator.Tools
import com.firemaples.googlewebtranslator.TranslatedResult
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val tag = MainActivity::class.java.simpleName
    private val translator by lazy { GoogleWebTranslator(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Tools.DEBUG = true

        WebView.setWebContentsDebuggingEnabled(true)

        sp_lang.adapter = ArrayAdapter(this,
                android.R.layout.simple_spinner_dropdown_item, Language.values()
                .map { it.name })

        translator.setup(wrap_webView)

        bt_submit.setOnClickListener {
            val text = et_text.text
            if (!text.isNullOrBlank()) {
                translator.translate(_text = text.toString(),
                        targetLang = Language.values()[sp_lang.selectedItemPosition].langCode,
                        callback = object : GoogleWebTranslator.OnTranslationCallback {
                            override fun onStart() {
                                Log.d(tag, "onStart()")
                            }

                            override fun onTranslated(result: TranslatedResult) {
                                Log.d(tag, "onTranslated(): ${result.text}")

                                tv_result.text = result.text
                            }

                            override fun onTranslationFailed(errorMsg: String) {
                                Log.e(tag, "onTranslationFailed: $errorMsg")

                                tv_result.text = errorMsg
                            }
                        })
            }
        }
    }
}
