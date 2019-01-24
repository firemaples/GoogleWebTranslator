package com.example.testapp

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.firemaples.googlewebtranslator.GoogleTranslator
import com.firemaples.googlewebtranslator.Language
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val translator by lazy { GoogleTranslator(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        translator.setup(wrap_webView)

        bt_submit.setOnClickListener {
            val text = et_text.text
            if (!text.isNullOrBlank()) {
                translator.translate(text = text.toString(),
                        targetLang = Language.Chinese_Traditional.langCode)
            }
        }
    }
}