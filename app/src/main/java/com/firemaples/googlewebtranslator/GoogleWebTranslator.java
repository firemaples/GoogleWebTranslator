package com.firemaples.googlewebtranslator;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.utils.URIBuilder;
import cz.msebera.android.httpclient.message.BasicHeader;

public class GoogleWebTranslator {
    private final static Logger logger = LoggerFactory.getLogger(GoogleWebTranslator.class);

    //    private static String URL_GOOGLE_TRANSLATE = "https://translate.google.com/m/translate?sl=auto&tl={TL}&ie=UTF-8&q={TEXT}";
    private final static String FORMAT_KEY = "{KEY}";
    private final static String FORMAT_TEXT = "{TEXT}";
    private final static String FORMAT_SOURCE_LANG = "{SL}";
    private final static String FORMAT_TARGET_LANG = "{TL}";

    private static String URL_GOOGLE_TRANSLATE = "https://translate.google.com/m/translate?sl=auto&tl={TL}&ie=UTF-8";
    private static String URL_LOAD_TRANSLATION_RESULT = "https://translate.google.com/translate_a/single?";

    private static String JS_FORMAT = "javascript:%s;void 0";
    private static String JS_INIT = "document.getElementsByClassName('translation')[0].addEventListener('DOMNodeInserted', function(e){window.HtmlViewer.onTranslationSuccess(e.target.textContent);})";
    private static String JS_TRANSLATE = "document.getElementById('source').value = '{TEXT}'";
    private static String JS_TRANSLATION_FAILED_CHECK = "if(document.getElementsByClassName('translation-error')[0].style.display === ''){onTranslationFailed();}";
    private static String JS_FORCE_POST_TRANSLATE = "var text='%s';for(var i=0;i<700;i++){text+=i};document.getElementById('source').value = text";

    private static String POST_FORM_Q = "q";

    private static String HTTP_GET = "GET";
    private static String UTF_8 = "UTF-8";


    private static GoogleWebTranslator _instance = null;

    private WeakReference<Context> weakContext;

    private WebView webView;
    private boolean initialized = false;

    private boolean translationCounter = true;
    private String textToTranslate = null;

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

    public WebView getWebView() {
        return webView;
    }

    public WebView getNonParentWebView() {
        WebView webView = getWebView();
        ViewGroup viewGroup = (ViewGroup) webView.getParent();
        if (viewGroup != null) {
            viewGroup.removeView(webView);
        }

        return webView;
    }

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
        settings.setSavePassword(false);
        settings.setSupportMultipleWindows(false);
    }

    public void setTargetLanguage(String targetLanguage) {
        String url = URL_GOOGLE_TRANSLATE.replace(FORMAT_TARGET_LANG, targetLanguage);

        logger.info("setTargetLanguage: " + url);

        webView.loadUrl(url);
    }

    public void translate(String text) {
//        text = text.replaceAll("\\R", "\\n");
        textToTranslate = text;
        String textToSend = String.format(JS_FORCE_POST_TRANSLATE, String.valueOf(translationCounter = !translationCounter));
//        _doJavascript(JS_TRANSLATE.replace(FORMAT_TEXT, text));
        _doJavascript(textToSend);
    }

    private void _doJavascript(String javascript) {
        String url = String.format(Locale.US, JS_FORMAT, javascript);

        logger.info("_doJavascript: " + url);

        webView.loadUrl(url);
    }

    private class MyWebViewClient extends WebViewClient {
        private SyncHttpClient httpClient = new SyncHttpClient();

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            _doJavascript(JS_INIT);
            initialized = true;
            logger.info("Page initialized");
        }

        // shouldInterceptRequest:
        // https://gist.github.com/kibotu/32313b957cd01258cf67

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
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

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {

//            WebResourceResponse response = _interceptRequest(url, false, null);
//
//            if (response != null) {
//                return response;
//            } else {
            return super.shouldInterceptRequest(view, url);
//            }
        }

        private WebResourceResponse _interceptRequest(String url, boolean methodGet, Map<String, String> requestHeaders) {
            if (!url.startsWith(URL_LOAD_TRANSLATION_RESULT)) {
                return null;
            }

            logger.info("_interceptRequest(), url: " + url
                    + ", methodGet: " + methodGet
                    + ", requestHeaders!=null: " + String.valueOf(requestHeaders != null));

            if (requestHeaders != null) {
                logger.debug("====== Cookie Start ======");

                Set<String> keySet = requestHeaders.keySet();
                for (String key : keySet) {
                    String value = requestHeaders.get(key);

                    logger.debug(key + "=" + value);
                }

                logger.debug("====== Cookie End ======");

                //Overwrite JSON response:
                //https://stackoverflow.com/questions/33370123/how-to-get-webviewclient-shouldinterceptrequest-invoked-asynchronously
            }

            final boolean[] success = {false};
            final Header[][] responseHeaders = new Header[1][1];
            final String[] responseText = new String[1];
            final Throwable[] httpError = new Throwable[1];

            if (methodGet) {
                try {
                    url = removeQueryParameter(url, POST_FORM_Q);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }

            //Convert cookie
            //https://stackoverflow.com/questions/26798500/android-sync-cookies-webview-and-httpclient
            //TODO Convert cookie

            //Convert headers
            Header[] headersToSend = null;
            if (requestHeaders != null) {
                headersToSend = new Header[requestHeaders.size()];
                Set<String> keySet = requestHeaders.keySet();
                int i = 0;
                for (String key : keySet) {
                    headersToSend[i] = new BasicHeader(key, requestHeaders.get(key));
                }
            }

            Map<String, String> formData = new HashMap<>();
            formData.put(POST_FORM_Q, textToTranslate);
            httpClient.post(getContext(), url, headersToSend, new RequestParams(formData), null, new TextHttpResponseHandler(UTF_8) {

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

            if (success[0]) {
                logger.info("On result success: " + responseText[0]);
                try {
                    return new WebResourceResponse(
                            null,
                            UTF_8,
                            new ByteArrayInputStream(responseText[0].getBytes(UTF_8))
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
                logger.warn("Result failed: " + httpError[0].getMessage());
                return null;
            }
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);

            logger.debug("onLoadResource: " + url);
        }

        public String removeQueryParameter(String url, String parameterName) throws URISyntaxException {
            URIBuilder uriBuilder = new URIBuilder(url);
            List<NameValuePair> queryParameters = uriBuilder.getQueryParams();
            for (Iterator<NameValuePair> queryParameterItr = queryParameters.iterator(); queryParameterItr.hasNext(); ) {
                NameValuePair queryParameter = queryParameterItr.next();
                if (queryParameter.getName().equals(parameterName)) {
                    queryParameterItr.remove();
                }
            }
            uriBuilder.setParameters(queryParameters);
            return uriBuilder.build().toString();
        }
    }

    @SuppressWarnings("unused")
    private class MyJavaScriptInterface {
        @JavascriptInterface
        public void onTranslationSuccess(String text) {
            logger.info("onTranslationSuccess: " + text);
        }

        @JavascriptInterface
        public void onTranslationFailed() {
            logger.info("onTranslationFailed");
        }
    }
}
