package com.example.testapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.*
import com.firemaples.googlewebtranslator.GoogleWebTranslator
import com.firemaples.googlewebtranslator.Language
import com.firemaples.googlewebtranslator.Tools
import com.firemaples.googlewebtranslator.TranslatedResult

class MainActivity : AppCompatActivity() {
    private val tag = MainActivity::class.java.simpleName
    private val translator by lazy { GoogleWebTranslator(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Tools.DEBUG = true

        WebView.setWebContentsDebuggingEnabled(true)

        val spLang = findViewById<Spinner>(R.id.sp_lang)
        val btSubmit = findViewById<Button>(R.id.bt_submit)
        val wrapWebView = findViewById<ViewGroup>(R.id.wrap_webView)
        val etText = findViewById<EditText>(R.id.et_text)
        val tvResult = findViewById<TextView>(R.id.tv_result)

        spLang.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item, Language.values()
                .map { it.name })
//
        translator.setup(wrapWebView)

        btSubmit.setOnClickListener {
            val text = etText.text
            if (!text.isNullOrBlank()) {
                translator.translate(_text = text.toString(),
                    targetLang = Language.values()[spLang.selectedItemPosition].langCode,
                    callback = object : GoogleWebTranslator.OnTranslationCallback {
                        override fun onStart() {
                            Log.d(tag, "onStart()")
                        }

                        override fun onTranslated(result: TranslatedResult) {
                            Log.d(tag, "onTranslated(): ${result.text}")

                            tvResult.text = result.text
                        }

                        override fun onTranslationFailed(errorMsg: String) {
                            Log.e(tag, "onTranslationFailed: $errorMsg")

                            tvResult.text = errorMsg
                        }
                    })
            }
        }
    }
}
