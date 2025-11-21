package org.openhab.binding.rachio.internal.api;

import static java.net.HttpURLConnection.*;
import static org.openhab.binding.rachio.RachioBindingConstants.*;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;

import org.openhab.binding.rachio.internal.api.RachioApi.RachioApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioHttp} class handles HTTP GET, POST, PUT, DELETE requests to the Rachio cloud API.
 *
 * @author Chris Graham - Initial contribution
 * @author Markus Michels - adapted for Rachio binding
 */
public class RachioHttp {
    private final Logger logger = LoggerFactory.getLogger(RachioHttp.class);

    private int apiCalls = 0;
    private String apikey = "";
        public RachioHttp(final String key) throws RachioApiException {
        this.apikey = key;
    }

    public RachioApiResult httpGet(String url, String urlParameters) throws RachioApiException {
        return httpRequest(HTTP_METHOD_GET, url, urlParameters, null);
    }

    public RachioApiResult httpPut(String url, String putData) throws RachioApiException {
        return httpRequest(HTTP_METHOD_PUT, url, null, putData);
    }

    public RachioApiResult httpPost(String url, String postData) throws RachioApiException {
        return httpRequest(HTTP_METHOD_POST, url, null, postData);
    }

    public RachioApiResult httpDelete(String url, String urlParameters) throws RachioApiException {
        return httpRequest(HTTP_METHOD_DELETE, url, urlParameters, null);
    }
        protected RachioApiResult httpRequest(String method, String url, String urlParameters, String reqData)
            throws RachioApiException {

        RachioApiResult result = new RachioApiResult();
        try {
            apiCalls++;
            URL location = (urlParameters != null) ? new URL(url + "?" + urlParameters) : new URL(url);

            result.requestMethod = method;
            result.url = location.toString();
            result.apiCalls = apiCalls;

            HttpURLConnection request = (HttpURLConnection) location.openConnection();
            request.setRequestMethod(method);
            request.setConnectTimeout(15000);
            request.setRequestProperty("User-Agent", SERVLET_WEBHOOK_USER_AGENT);
            request.setRequestProperty("Content-Type", SERVLET_WEBHOOK_APPLICATION_JSON);
            if (apikey != null) {
                request.setRequestProperty("Authorization", "Bearer " + apikey);
                result.apikey = apikey;
            }

            logger.trace("RachioHttp[Call #{}]: {} '{}'", apiCalls, method, result.url);

            if (method.equals(HTTP_METHOD_PUT) || method.equals(HTTP_METHOD_POST)) {
                request.setDoOutput(true);
                try (DataOutputStream wr = new DataOutputStream(request.getOutputStream())) {
                    wr.writeBytes(reqData);
                    wr.flush();
                }
            }

            result.responseCode = request.getResponseCode();
            if (request.getHeaderField(RACHIO_JSON_RATE_LIMIT) != null) {
                result.setRateLimit(
                Integer.parseInt(request.getHeaderField(RACHIO_JSON_RATE_LIMIT)),
                Integer.parseInt(request.getHeaderField(RACHIO_JSON_RATE_REMAINING)),
                request.getHeaderField(RACHIO_JSON_RATE_RESET));

                if (result.isRateLimitBlocked()) {
                    String message = MessageFormat.format(
                            "RachioHttp: Critical API rate limit: {0}/{1}, reset at {2}",
                            result.rateRemaining, result.rateLimit, result.rateReset);
                    throw new RachioApiException(message, result);
                }
            }

            boolean isSuccess = result.responseCode == HTTP_OK
                    || (result.responseCode == HTTP_NO_CONTENT
                        && (method.equals(HTTP_METHOD_PUT) || method.equals(HTTP_METHOD_DELETE)));

            if (!isSuccess) {
                String message = MessageFormat.format(
                        "RachioHttp: HTTP {0} to {1} failed with response code {2}",
                        method, url, result.responseCode);
                throw new RachioApiException(message, result);
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }

            result.resultString = response.toString();
            logger.trace("RachioHttp: {} {} - Response='{}'", method, url, result.resultString);
            return result;

        } catch (Throwable e) {
            throw new RachioApiException(e.toString(), result, e);
        }
    }
}