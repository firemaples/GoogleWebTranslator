package com.firemaples.googlewebtranslator.utils

internal object Constants {
    const val MAX_TEXT_LENGTH = 5000
    const val TEXT_CHUNK_SIZE = 100

    const val JS_SELECTOR_SOURCE_TEXT = "document.querySelector('textarea[jsname=\"BJE2fc\"]')"
    const val JS_SELECTOR_TRANSLATE_BUTTON = "document.querySelector('div[jsname=\"vSSGHe\"]')"
    const val JS_SELECTOR_TRANSLATED_TEXT = "document.querySelector('span[jsname=\"W297wb\"]')"
    const val JS_FUNCTION_EVENT_FIRE = "function eventFire(el, etype){\n" +
            "  if (el.fireEvent) {\n" +
            "    el.fireEvent('on' + etype);\n" +
            "  } else {\n" +
            "    var evObj = document.createEvent('Events');\n" +
            "    evObj.initEvent(etype, true, false);\n" +
            "    el.dispatchEvent(evObj);\n" +
            "  }\n" +
            "}"
    const val JS_EVENT_MOUSE_DOWN = "mousedown"

    fun getTranslationUrl(targetLang: String): String =
        "https://translate.google.com/?sl=auto&tl=$targetLang&ie=UTF-8"

    fun getExecutableJS(js: String, evaluate: Boolean): String =
        if (evaluate) "javascript:$js" else "javascript:$js;void 0"

    fun getEventFireJS(jsSelector: String, event: String): String =
        "eventFire($jsSelector,'$event')"

    fun isTranslationResultUrl(url: String): Boolean =
        url.startsWith("https://translate.google.com/_/TranslateWebserverUi/data/batchexecute?")
}