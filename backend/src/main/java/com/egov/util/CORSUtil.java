package com.egov.util;

import com.sun.net.httpserver.HttpExchange;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CORSUtil {

    public static void setCORSHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    public static String readBody(HttpExchange exchange) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /** Minimal JSON key-value extractor (avoids external dependencies) */
    public static Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.isBlank()) return map;
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

        // Simple regex-free parsing for flat JSON objects
        String[] parts = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String part : parts) {
            String[] kv = part.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replaceAll("\"", "");
                String val = kv[1].trim().replaceAll("^\"|\"$", "");
                map.put(key, val);
            }
        }
        return map;
    }

    /** Build a JSON response string from key-value pairs */
    public static String toJson(Object... pairs) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < pairs.length - 1; i += 2) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(pairs[i]).append("\":");
            Object val = pairs[i + 1];
            if (val instanceof Number || val instanceof Boolean) {
                sb.append(val);
            } else if (val == null) {
                sb.append("null");
            } else {
                sb.append("\"").append(String.valueOf(val).replace("\"", "\\\"")).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}