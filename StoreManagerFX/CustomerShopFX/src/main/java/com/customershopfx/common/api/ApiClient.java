package com.customershopfx.common.api;

import com.customershopfx.app.AppConfig;
import com.customershopfx.app.SessionManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;

public class ApiClient {
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public <T> T get(String path, TypeReference<T> type) {
        return send(request(path).GET(), type);
    }

    public <T> T post(String path, Object body, TypeReference<T> type) {
        return send(jsonRequest(path, body).POST(json(body)), type);
    }

    public <T> T put(String path, Object body, TypeReference<T> type) {
        return send(jsonRequest(path, body).PUT(json(body)), type);
    }

    public <T> T delete(String path, TypeReference<T> type) {
        return send(request(path).DELETE(), type);
    }

    public String query(Map<String, String> params) {
        StringJoiner joiner = new StringJoiner("&");
        params.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                joiner.add(encode(key) + "=" + encode(value));
            }
        });
        String query = joiner.toString();
        return query.isBlank() ? "" : "?" + query;
    }

    private HttpRequest.BodyPublisher json(Object body) {
        try {
            return HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body));
        } catch (IOException e) {
            throw new ApiException("Cannot serialize request");
        }
    }

    private HttpRequest.Builder jsonRequest(String path, Object ignored) {
        return request(path).header("Content-Type", "application/json");
    }

    private HttpRequest.Builder request(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(AppConfig.API_BASE_URL + path))
                .header("Accept", "application/json");
        if (SessionManager.authenticated()) {
            builder.header("Authorization", "Bearer " + SessionManager.token());
        }
        return builder;
    }

    private <T> T send(HttpRequest.Builder builder, TypeReference<T> type) {
        try {
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ApiException(message(response.body(), response.statusCode()));
            }
            if (type.getType().getTypeName().equals("java.lang.Void")) {
                return null;
            }
            if (response.body() == null || response.body().isBlank()) {
                return null;
            }
            return mapper.readValue(response.body(), type);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Cannot reach store API");
        }
    }

    private String message(String body, int status) {
        try {
            Map<String, String> error = mapper.readValue(body, new TypeReference<>() {});
            return error.getOrDefault("message", "Request failed");
        } catch (Exception ignored) {
            return "Request failed with HTTP " + status;
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
