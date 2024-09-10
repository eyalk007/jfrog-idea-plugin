package com.jfrog.ide.idea.utils;

import com.jfrog.ide.idea.ui.configuration.JFrogGlobalConfiguration;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class HttpClientWrapper {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final int DEFAULT_RETRIES = 5;
    private static final long DEFAULT_RETRY_DELAY_MS = 1000;
    private static final int DEFAULT_TIMEOUT_MS = 60000;

    private final String accessToken;
    private final BasicAuth basicAuth;
    private final HttpClient httpClient;
    private final Logger logger;

    public HttpClientWrapper(HttpConfig config, Logger logger) {
        this.accessToken = config.getAccessToken();
        this.basicAuth = new BasicAuth(config.getUsername(), config.getPassword());
        this.logger = logger;

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(java.time.Duration.ofMillis(config.getTimeout() != null ? config.getTimeout() : DEFAULT_TIMEOUT_MS))
                .build();
    }

    public HttpResponse<String> doGetRequest(String url, Map<String, String> queryParams) throws IOException, InterruptedException {
        String query = (queryParams != null) ? queryParams.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&")) : "";

        if (!query.isEmpty()) {
            url = url + "?" + query;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header(AUTHORIZATION_HEADER, getAuthHeader())
                .header(USER_AGENT_HEADER, "jfrog-idea-plugin/" + JFrogGlobalConfiguration.class.getPackage().getImplementationVersion())
                .build();

        return executeWithRetry(request, DEFAULT_RETRIES, DEFAULT_RETRY_DELAY_MS);
    }

    public HttpResponse<String> doRequest(RequestParams requestParams) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(requestParams.getUrl()))
                .method(requestParams.getMethod(), HttpRequest.BodyPublishers.ofString(requestParams.getData()) )
                .header(AUTHORIZATION_HEADER,getAuthHeader())
                .header(USER_AGENT_HEADER, "jfrog-idea-plugin/" + JFrogGlobalConfiguration.class.getPackage().getImplementationVersion());


         HttpRequest request = requestBuilder.build();

        return executeWithRetry(request, DEFAULT_RETRIES, DEFAULT_RETRY_DELAY_MS);
    }

    private String getAuthHeader() {
        if (accessToken != null && !accessToken.isEmpty()) {
            return  "Bearer " + accessToken;
        } else if (basicAuth != null) {
           return  "Basic " + encodeBasicAuth(basicAuth.username, basicAuth.password);
        }
        return null;
    }

    private String encodeBasicAuth(String username, String password) {
        String auth = username + ":" + password;
        return Base64.getEncoder().encodeToString(auth.getBytes());
    }

    private HttpResponse<String> executeWithRetry(HttpRequest request, int retries, long delayMs) throws IOException, InterruptedException {
        for (int attempt = 1; ; attempt++) {
            try {
                return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                if (attempt >= retries) {
                    throw e;
                }
                logger.log(Level.WARNING, "Request failed, retrying in " + delayMs + " ms, attempt #" + attempt, e);
                Thread.sleep(delayMs);
            }
        }
    }

    public static class BasicAuth {
        private final String username;
        private final String password;

        public BasicAuth(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    public static class HttpConfig {
        private String serverUrl;
        private String username;
        private String password;
        private String accessToken;
        private Integer timeout;
        private Map<String, String> headers;
        private Integer retries;
        private Integer retryDelay;

       public HttpConfig(String serverUrl, String username, String password, String accessToken) {
            this.serverUrl = serverUrl;
            this.username = username;
            this.password = password;
            this.accessToken = accessToken;
        }

        public String getServerUrl() { return serverUrl; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getAccessToken() { return accessToken; }
        public Integer getTimeout() { return timeout; }
    }

    public static class RequestParams {
        private String url;
        private String method;
        private String data;
        private Map<String, String> headers;
        private Integer timeout = DEFAULT_TIMEOUT_MS;
        private Function<Integer, Boolean> validateStatus;

        public RequestParams(String url, String method, String data) {
            this.url = url;
            this.method = method;
            this.data = data;
            this.headers = new HashMap<>();
        }
        // Getters and Setters

        public String getUrl() { return url; }
        public String getMethod() { return method; }
        public String getData() { return data; }
        public Map<String, String> getHeaders() { return headers; }
        public Integer getTimeout() { return timeout; }
        public Function<Integer, Boolean> getValidateStatus() { return validateStatus; }
    }
}
