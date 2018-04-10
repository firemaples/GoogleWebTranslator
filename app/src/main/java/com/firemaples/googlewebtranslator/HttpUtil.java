package com.firemaples.googlewebtranslator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.utils.URIBuilder;
import cz.msebera.android.httpclient.message.BasicHeader;

public class HttpUtil {
    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);

    public static Header[] getHeaders(Map<String, String> requestHeaders){
        Header[] headersToSend = null;
        if (requestHeaders != null) {
            logger.debug("====== Header Start ======");
            headersToSend = new Header[requestHeaders.size()];
            Set<String> keySet = requestHeaders.keySet();
            int i = 0;
            for (String key : keySet) {
                String value = requestHeaders.get(key);
                logger.debug(key + "=" + value);
                headersToSend[i++] = new BasicHeader(key, value);
            }
            logger.debug("====== Header End ======");
        }
        return headersToSend;
    }

    public static String removeQueryParameter(String url, String parameterName) throws URISyntaxException {
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
