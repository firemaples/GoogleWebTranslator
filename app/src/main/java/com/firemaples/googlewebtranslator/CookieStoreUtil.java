package com.firemaples.googlewebtranslator;

import android.webkit.CookieManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.impl.client.BasicCookieStore;
import cz.msebera.android.httpclient.impl.cookie.BasicClientCookie;

public class CookieStoreUtil {
    private static BasicCookieStore cookieStore = new BasicCookieStore();

    public static BasicCookieStore getCookieStore() {
        return cookieStore;
    }

    //https://stackoverflow.com/questions/26798500/android-sync-cookies-webview-and-httpclient
    public static void syncFromWebView(String url) {
        String cookie = CookieManager.getInstance().getCookie(url);
        try {
            String domain = getDomainName(url);
            cookieStore.addCookies(getCookieStore(cookie, domain));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private static BasicClientCookie[] getCookieStore(String cookies, String domain) {
        String[] cookieValues = cookies.split(";");
        List<BasicClientCookie> cookieList = new ArrayList<>();

        BasicClientCookie cookie;
        for (String cookieValue : cookieValues) {
            String[] split = cookieValue.split("=");
            if (split.length == 2) {
                cookie = new BasicClientCookie(split[0], split[1]);
            } else {
                cookie = new BasicClientCookie(split[0], null);
            }

            cookie.setDomain(domain);
            cookieList.add(cookie);
        }
        return cookieList.toArray(new BasicClientCookie[cookieList.size()]);

    }

    private static String getDomainName(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String domain = uri.getHost();
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }
}
