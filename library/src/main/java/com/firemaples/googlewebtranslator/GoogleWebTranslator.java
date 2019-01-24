package com.firemaples.googlewebtranslator;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.net.http.SslError;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.loopj.android.http.SyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cz.msebera.android.httpclient.Header;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class GoogleWebTranslator {
    private static final String TAG = GoogleWebTranslator.class.getSimpleName();

    private final static String FORMAT_TEXT = "{TEXT}";
    //    private final static String FORMAT_SOURCE_LANG = "{SL}";
    private final static String FORMAT_TARGET_LANG = "{TL}";

    private static String URL_GOOGLE_TRANSLATE = "https://translate.google.com/m/translate?sl=auto&tl={TL}&ie=UTF-8";
    private static String URL_LOAD_TRANSLATION_RESULT = "https://translate.google.com/translate_a/single?";

    private static String JS_FORMAT = "javascript:%s;void 0";
    private static String JS_INIT = "document.getElementsByClassName('translation')[0].addEventListener('DOMNodeInserted', function(e){window.HtmlViewer.onTranslationSuccess(e.target.textContent);})";
    private static String JS_TRANSLATE = "document.getElementById('source').value = '{TEXT}'";
//    private static String JS_TRANSLATION_FAILED_CHECK = "if(document.getElementsByClassName('translation-error')[0].style.display === ''){onTranslationFailed();}";
//    private static String JS_FORCE_POST_TRANSLATE = "var text='%s';for(var i=0;i<700;i++){text+=i};document.getElementById('source').value = text";

//    private static String POST_FORM_Q = "q";

    private static String HTTP_GET = "GET";
    private static String UTF_8 = "UTF-8";


    @SuppressLint("StaticFieldLeak")
    private static GoogleWebTranslator _instance = null;

    private WeakReference<Context> weakContext;

    private WebView webView;
    private String currentTranslatorUrl;
    private boolean receivedErrorBeforeInitialized = false;
    private boolean initialized = false;
    private String textToTranslate = null;
    private long prepareStartTime = 0, translationStartTime = 0;

    private boolean callbackOnMainThread = true;
    @VisibleForTesting
    protected List<OnTranslationCallback> callbackList = new ArrayList<>();
    private Handler backgroundHandler = new Handler();

    public static synchronized GoogleWebTranslator init(Context context) {
        if (_instance == null) {
            _instance = new GoogleWebTranslator(context);
        }
        return _instance;
    }

    public static synchronized GoogleWebTranslator getInstance() {
        if (_instance == null) {
            throw new IllegalStateException("Please initialize GoogleWebTranslator be for using.");
        }
        return _instance;
    }

    private GoogleWebTranslator(Context context) {
        this.weakContext = new WeakReference<>(context);
        _init();
    }

    private Context getContext() {
        if (weakContext == null) {
            throw new IllegalStateException("Please initialize GoogleWebTranslator be for using.");
        } else {
            Context context = weakContext.get();
            if (context == null) {
                throw new NullPointerException("Context is null");
            } else {
                return context;
            }
        }
    }

    public WebView getNonParentWebView() {
        ViewGroup viewGroup = (ViewGroup) webView.getParent();
        if (viewGroup != null) {
            viewGroup.removeView(webView);
        }

        return webView;
    }

    @SuppressLint({"ClickableViewAccessibility", "SetJavaScriptEnabled"})
    private void _init() {
        webView = new WebView(getContext());
        webView.addJavascriptInterface(new MyJavaScriptInterface(), "HtmlViewer");
        webView.setWebViewClient(new MyWebViewClient());
        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return false;
            }
        });
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);

//        settings.setAppCacheEnabled(false);
        settings.setBuiltInZoomControls(false);
        settings.setBlockNetworkImage(true);
        settings.setDatabaseEnabled(false);
        settings.setGeolocationEnabled(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setLoadsImagesAutomatically(false);
        settings.setSaveFormData(false);
        //noinspection deprecation
        settings.setSavePassword(false);
        settings.setSupportMultipleWindows(false);
    }

    public void addOnTranslationCallback(OnTranslationCallback onTranslationCallback) {
        this.callbackList.add(onTranslationCallback);
    }

    public void removeOnTranslationCallback(OnTranslationCallback onTranslationCallback) {
        this.callbackList.remove(onTranslationCallback);
    }

    public void clearAllCallbacks() {
        this.callbackList.clear();
    }

    public void setCallbackOnMainThread(boolean callbackOnMainThread) {
        this.callbackOnMainThread = callbackOnMainThread;
    }

    public void setTargetLanguage(Language targetLanguage) {
        setTargetLanguage(targetLanguage.getLangCode());
    }

    @SuppressWarnings("WeakerAccess")
    public void setTargetLanguage(String targetLanguage) {
        String url = URL_GOOGLE_TRANSLATE.replace(FORMAT_TARGET_LANG, targetLanguage);

        LogTool.i(TAG, "setTargetLanguage: " + url);

        currentTranslatorUrl = url;

        _loadTranslatorUrl();
    }

    private void _loadTranslatorUrl() {
        prepareStartTime = System.currentTimeMillis();
        receivedErrorBeforeInitialized = false;
        initialized = false;

        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl(currentTranslatorUrl);
            }
        });
    }

    private void runOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            webView.post(runnable);
        }
    }

    public void translate(String text) {
        textToTranslate = text;

        if (!initialized) {
            _loadTranslatorUrl();
        } else {
//        text = text.replaceAll("\\R", "\\n");
//        String textToSend = String.format(JS_FORCE_POST_TRANSLATE, String.valueOf(translationCounter = !translationCounter));
//        _doJavascript(textToSend);

            translationStartTime = System.currentTimeMillis();

            _doJavascript(JS_TRANSLATE.replace(FORMAT_TEXT, text));
        }
    }

    private void _doJavascript(String javascript) {
        final String url = String.format(Locale.US, JS_FORMAT, javascript);

        LogTool.i(TAG, "_doJavascript: " + url);

        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl(url);
            }
        });
    }

    private void _postCallbackToThread(Runnable runnable) {
        if (callbackOnMainThread) {
            webView.post(runnable);
        } else {
            backgroundHandler.post(runnable);
        }
    }

    private void postInitialized() {
        for (final OnTranslationCallback callback : callbackList) {
            _postCallbackToThread(new Runnable() {
                @Override
                public void run() {
                    callback.onInitialized(GoogleWebTranslator.this);
                }
            });
        }
    }

    private void postInitializationFailed(final int errorCode, final String desc) {
        for (final OnTranslationCallback callback : callbackList) {
            _postCallbackToThread(new Runnable() {
                @Override
                public void run() {
                    callback.onInitializationFailed(GoogleWebTranslator.this, errorCode, desc);
                }
            });
        }
    }

    private void postTranslationFinish(final TranslatedResult result) {
        for (final OnTranslationCallback callback : callbackList) {
            _postCallbackToThread(new Runnable() {
                @Override
                public void run() {
                    callback.onTranslationSuccess(GoogleWebTranslator.this, result);
                }
            });
        }
    }

    private void postTranslationFailed(final Throwable throwable) {
        for (final OnTranslationCallback callback : callbackList) {
            _postCallbackToThread(new Runnable() {
                @Override
                public void run() {
                    callback.onTranslationFailed(GoogleWebTranslator.this, throwable);
                }
            });
        }
    }

    private class MyWebViewClient extends WebViewClient {
        private SyncHttpClient httpClient = new SyncHttpClient();

        private MyWebViewClient() {
            httpClient.setCookieStore(CookieStoreUtil.getCookieStore());
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            if (!receivedErrorBeforeInitialized) {
                _doJavascript(JS_INIT);
                initialized = true;
                long preparedSpentTime = System.currentTimeMillis() - prepareStartTime;
                LogTool.i(TAG, "Page initialized, spent: " + preparedSpentTime + " ms");

                postInitialized();

                if (textToTranslate != null) {
                    translate(textToTranslate);
                }
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return;
            }
            LogTool.e(TAG, "onReceivedError(), errorCode: " + errorCode + ", desc: " + description);

            _onReceivedError(failingUrl, errorCode, description);
        }

        @TargetApi(android.os.Build.VERSION_CODES.M)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            LogTool.e(TAG, "onReceivedError()");

            _onReceivedError(request.getUrl().toString(), error.getErrorCode(), error.getDescription().toString());
        }

        private void _onReceivedError(String url, int errorCode, String desc) {
            if (!initialized && url.equalsIgnoreCase(currentTranslatorUrl)) {
                receivedErrorBeforeInitialized = true;
                initialized = false;
                postInitializationFailed(errorCode, desc);
            }
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            super.onReceivedSslError(view, handler, error);
        }

        // shouldInterceptRequest:
        // https://gist.github.com/kibotu/32313b957cd01258cf67

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            boolean methodGet = HTTP_GET.equalsIgnoreCase(request.getMethod());
            Map<String, String> requestHeaders = request.getRequestHeaders();

            WebResourceResponse response = _interceptRequest(url, methodGet, requestHeaders);
            if (response != null) {
                return response;
            } else {
                return super.shouldInterceptRequest(view, request);
            }
        }

//        @Nullable
//        @Override
//        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
//
////            WebResourceResponse response = _interceptRequest(url, true, null);
////
////            if (response != null) {
////                return response;
////            } else {
//            return super.shouldInterceptRequest(view, url);
////            }
//        }

        private long getTranslationSpentTime() {
            return System.currentTimeMillis() - translationStartTime;
        }

        private WebResourceResponse _interceptRequest(String url, boolean methodGet, Map<String, String> requestHeaders) {
            if (!url.startsWith(URL_LOAD_TRANSLATION_RESULT)) {
                return null;
            }

            LogTool.i(TAG, "_interceptRequest(), url: " + url
                    + ", methodGet: " + methodGet
                    + ", requestHeaders!=null: " + String.valueOf(requestHeaders != null));

            if (!methodGet) {
                LogTool.e(TAG, "POST not working now, spent: " + getTranslationSpentTime() + " ms");

                postTranslationFailed(new IllegalArgumentException("Http POST is not supported now"));

                return null;
            }

            final boolean[] success = {false};
            final Header[][] responseHeaders = new Header[1][1];
            final String[] responseText = new String[1];
            final Throwable[] httpError = new Throwable[1];

//            if (methodGet) {
//                try {
//                    url = removeQueryParameter(url, POST_FORM_Q);
//                } catch (URISyntaxException e) {
//                    e.printStackTrace();
//                }
//            }

            //Convert headers
            Header[] headersToSend = HttpUtil.getHeaders(requestHeaders);

            //Convert cookie
            CookieStoreUtil.syncFromWebView(url);

//            if (methodGet) {
            httpClient.get(getContext(), url, headersToSend, null, new TextHttpResponseHandler(UTF_8) {
                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    success[0] = false;
                    httpError[0] = throwable;
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, String responseString) {
                    success[0] = true;
                    responseHeaders[0] = headers;
                    responseText[0] = responseString;
                }
            });
//            } else {
//                //TODO not working now
//
//                Map<String, String> formData = new HashMap<>();
//                formData.put(POST_FORM_Q, textToTranslate);
////            formData.putAll(requestHeaders);
//
//                StringEntity stringEntity = null;
//                try {
//                    stringEntity = new StringEntity(POST_FORM_Q + "=" + textToTranslate);
//                } catch (UnsupportedEncodingException e) {
//                    e.printStackTrace();
//                }
//
//                httpClient.post(getContext(), url, headersToSend, stringEntity, "application/json; charset=UTF-8", new TextHttpResponseHandler(UTF_8) {
//                    @Override
//                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
//                        success[0] = false;
//                        httpError[0] = throwable;
//                    }
//
//                    @Override
//                    public void onSuccess(int statusCode, Header[] headers, String responseString) {
//                        success[0] = true;
//                        responseHeaders[0] = headers;
//                        responseText[0] = responseString;
//                    }
//                });
//            }

            if (success[0]) {
                String raw = responseText[0];

                TranslatedResult result = ResultParser.parse(raw);

                LogTool.i(TAG, "On result success, spent: " + getTranslationSpentTime() + " ms, result: " + raw);

                postTranslationFinish(result);

                try {
                    return new WebResourceResponse(
                            null,
                            UTF_8,
                            new ByteArrayInputStream(raw.getBytes(UTF_8))
                    ) {
                        @Override
                        public Map<String, String> getResponseHeaders() {
                            Map<String, String> headers = new HashMap<>();
                            for (Header header : responseHeaders[0]) {
                                headers.put(header.getName(), header.getValue());
                            }
                            return headers;
                        }
                    };
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                LogTool.e(TAG, "Result failed: " + httpError[0].getMessage());

                postTranslationFailed(httpError[0]);

                return null;
            }
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);

            LogTool.d(TAG, "onLoadResource: " + url);
        }
    }

    @SuppressWarnings("unused")
    private class MyJavaScriptInterface {
        @JavascriptInterface
        public void onTranslationSuccess(String text) {
            LogTool.d(TAG, "onTranslationSuccess: " + text);
        }

        @JavascriptInterface
        public void onTranslationFailed() {
            LogTool.d(TAG, "onTranslationFailed");
        }
    }

    public interface OnTranslationCallback {
        void onInitialized(GoogleWebTranslator translator);

        void onInitializationFailed(GoogleWebTranslator translator, int errorCode, String desc);

        void onTranslationSuccess(GoogleWebTranslator translator, TranslatedResult result);

        void onTranslationFailed(GoogleWebTranslator translator, Throwable throwable);
    }
}