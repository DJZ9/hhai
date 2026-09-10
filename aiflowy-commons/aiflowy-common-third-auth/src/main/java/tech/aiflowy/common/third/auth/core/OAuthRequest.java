package tech.aiflowy.common.third.auth.core;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.*;
import tech.aiflowy.common.third.auth.entity.PlatformUser;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 配置格式：
 * {
 *         "platform": "wxOpen",
 *         "displayName": "微信登录",
 *         "icon": "https://sxxxx",
 *         "clientId": "xxx",
 *         "clientSecret": "xxx",
 *         "redirectUrl": "https://xxx.com/thirdAuth/callback/wxOpen",
 *         "authUrl": "https://open.weixin.qq.com/connect/qrconnect",
 *         "authHttpConfig": {
 *             "params": {
 *                 "appid": "${config.clientId}",
 *                 "redirect_uri": "${config.redirectUrl}",
 *                 "response_type": "code",
 *                 "scope": "snsapi_login"
 *             }
 *         },
 *         "tokenUrl": "https://api.weixin.qq.com/sns/oauth2/access_token",
 *         "tokenHttpConfig": {
 *             "method": "get",
 *             "headers": {
 *             },
 *             "params": {
 *                 "appid": "${config.clientId}",
 *                 "secret": "${config.clientSecret}",
 *                 "code": "${callback.code}",
 *                 "grant_type": "authorization_code"
 *             }
 *         },
 *         "userInfoUrl": "https://api.weixin.qq.com/sns/userinfo",
 *         "userInfoHttpConfig": {
 *             "method": "get",
 *             "params": {
 *                 "access_token": "${result.access_token}",
 *                 "openid": "${result.openid}"
 *             },
 *             "fieldMapping": {
 *                 "userKey": "unionid",
 *                 "username": "unionid",
 *                 "nickname": "nickname",
 *                 "avatar": "headimgurl"
 *             }
 *         }
 *     }
 */
public class OAuthRequest {

    private final JSONObject config;
    private final OkHttpClient httpClient;

    public static Pattern pattern = Pattern.compile("\\$\\{([^}]+)}");

    public OAuthRequest(JSONObject config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder().build();
    }

    public OAuthRequest(JSONObject config, OkHttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    public String getAuthUrl(String state) {
        String base = config.getString("authUrl");
        JSONObject authHttpConfig = config.getJSONObject("authHttpConfig");
        JSONObject params = authHttpConfig.getJSONObject("params");

        HttpUrl.Builder urlBuilder = HttpUrl.parse(base).newBuilder();

        // 处理参数
        processParams(params, null, null, urlBuilder, null);

        urlBuilder.addQueryParameter("state", state);
        return urlBuilder.build().toString();
    }

    public JSONObject getAccessToken(JSONObject callback) throws IOException {
        String base = config.getString("tokenUrl");
        JSONObject tokenHttpConfig = config.getJSONObject("tokenHttpConfig");
        JSONObject headers = tokenHttpConfig.getJSONObject("headers");
        JSONObject params = tokenHttpConfig.getJSONObject("params");

        // 构建请求
        Request request = buildRequest(base, tokenHttpConfig.getString("method"),
                headers, params, callback, null);

        // 执行请求
        return executeRequest(request);
    }

    public PlatformUser getUserInfo(JSONObject result) throws IOException {
        String base = config.getString("userInfoUrl");
        JSONObject userInfoHttpConfig = config.getJSONObject("userInfoHttpConfig");
        JSONObject headers = userInfoHttpConfig.getJSONObject("headers");
        JSONObject params = userInfoHttpConfig.getJSONObject("params");
        JSONObject fieldMapping = userInfoHttpConfig.getJSONObject("fieldMapping");

        // 构建请求
        Request request = buildRequest(base, userInfoHttpConfig.getString("method"),
                headers, params, null, result);

        // 执行请求
        JSONObject jsonResponse = executeRequest(request);

        // 解析用户信息
        return parseUserInfo(jsonResponse, fieldMapping);
    }

    /**
     * 构建HTTP请求
     */
    private Request buildRequest(String url, String method, JSONObject headers,
                                 JSONObject params, JSONObject callback, JSONObject result) {
        Request.Builder requestBuilder = new Request.Builder();

        // 构建URL
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) {
            throw new IllegalArgumentException("Invalid URL: " + url);
        }

        // 处理请求头
        processHeaders(headers, requestBuilder, callback, result);

        // 构建请求体和URL参数
        return buildRequestBody(requestBuilder, httpUrl, method, headers, params, callback, result);
    }

    /**
     * 处理请求头
     */
    private void processHeaders(JSONObject headers, Request.Builder requestBuilder,
                                JSONObject callback, JSONObject result) {
        if (headers == null) return;

        for (String key : headers.keySet()) {
            String value = headers.getString(key);
            String processedValue = resolvePlaceholder(value, callback, result);
            // 跳过null值，防止添加null header
            if (processedValue != null) {
                requestBuilder.addHeader(key, processedValue);
            }
        }
    }

    /**
     * 构建请求体或URL参数
     */
    private Request buildRequestBody(Request.Builder requestBuilder, HttpUrl httpUrl,
                                     String method, JSONObject headers, JSONObject params,
                                     JSONObject callback, JSONObject result) {
        String contentType = headers != null ? headers.getString("Content-Type") : null;
        boolean isPost = "post".equalsIgnoreCase(method);

        if (isPost) {
            // POST请求
            RequestBody requestBody = createRequestBody(params, contentType, callback, result);
            requestBuilder.url(httpUrl).post(requestBody);
        } else {
            // GET请求，参数添加到URL
            HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
            processParams(params, callback, result, urlBuilder, null);
            requestBuilder.url(urlBuilder.build());
        }

        return requestBuilder.build();
    }

    /**
     * 创建请求体
     */
    private RequestBody createRequestBody(JSONObject params, String contentType,
                                          JSONObject callback, JSONObject result) {
        if (params == null) {
            return RequestBody.create(null, new byte[0]);
        }

        if ("application/json".equals(contentType)) {
            // JSON格式
            JSONObject jsonBody = new JSONObject();
            processParams(params, callback, result, null, jsonBody);
            return RequestBody.create(
                    MediaType.parse("application/json"),
                    jsonBody.toJSONString()
            );
        } else {
            // Form表单格式
            FormBody.Builder formBuilder = new FormBody.Builder();
            processParams(params, callback, result, null, formBuilder);
            return formBuilder.build();
        }
    }

    /**
     * 处理参数（通用方法）
     */
    private void processParams(JSONObject params, JSONObject callback, JSONObject result,
                               HttpUrl.Builder urlBuilder, Object bodyBuilder) {
        if (params == null) return;

        for (String key : params.keySet()) {
            String value = params.getString(key);
            String processedValue = resolvePlaceholder(value, callback, result);
            
            // 跳过null值
            if (processedValue == null) {
                continue;
            }

            if (urlBuilder != null) {
                // 添加到URL参数
                urlBuilder.addQueryParameter(key, processedValue);
            } else if (bodyBuilder instanceof JSONObject) {
                // 添加到JSON对象
                ((JSONObject) bodyBuilder).put(key, processedValue);
            } else if (bodyBuilder instanceof FormBody.Builder) {
                // 添加到Form表单
                ((FormBody.Builder) bodyBuilder).add(key, processedValue);
            }
        }
    }

    /**
     * 解析占位符变量
     */
    private String resolvePlaceholder(String value, JSONObject callback, JSONObject result) {
        if (value == null || !value.startsWith("${")) {
            return value;
        }

        String[] arr = getKey(value);
        String resolvedValue = null;
        
        if ("config".equals(arr[0])) {
            resolvedValue = config.getString(arr[1]);
        } else if ("callback".equals(arr[0]) && callback != null) {
            resolvedValue = callback.getString(arr[1]);
        } else if ("result".equals(arr[0]) && result != null) {
            resolvedValue = result.getString(arr[1]);
        }

        // 如果解析结果为null，返回空字符串而不是null，防止传递null值到OkHttp
        return resolvedValue != null ? resolvedValue : "";
    }

    /**
     * 执行HTTP请求
     */
    private JSONObject executeRequest(Request request) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP request failed with code: " + response.code() +
                        ", message: " + response.message());
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("Response body is null");
            }

            String body = responseBody.string();
            return JSON.parseObject(body);
        }
    }

    /**
     * 解析用户信息
     */
    private PlatformUser parseUserInfo(JSONObject jsonResponse, JSONObject fieldMapping) {
        PlatformUser user = new PlatformUser();
        JSONObject mappedData = new JSONObject();

        if (fieldMapping != null) {
            for (String key : fieldMapping.keySet()) {
                String field = fieldMapping.getString(key);
                mappedData.put(key, jsonResponse.getString(field));
            }
        }

        user.setUserKey(mappedData.getString("userKey"));
        user.setUsername(mappedData.getString("username"));
        user.setNickname(mappedData.getString("nickname"));
        user.setAvatar(mappedData.getString("avatar"));
        user.setOrigData(jsonResponse);

        return user;
    }

    private String[] getKey(String str) {
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return matcher.group(1).split("\\.");
        } else {
            throw new RuntimeException("Invalid placeholder string: " + str);
        }
    }
}