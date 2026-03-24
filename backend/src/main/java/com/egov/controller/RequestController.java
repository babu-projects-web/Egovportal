package com.egov.controller;

import com.egov.util.CORSUtil;
import com.egov.util.DBConnection;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.*;
import java.util.Map;

public class RequestController {

    // ── CREATE REQUEST ─────────────────────────────────────────────────────
    public static void create(HttpExchange exchange) throws IOException {
        try {
            String body = CORSUtil.readBody(exchange);
            Map<String, String> data = CORSUtil.parseJson(body);

            String applicantName = data.getOrDefault("applicantName", "");
            String mobile        = data.getOrDefault("mobile", "");
            String email         = data.getOrDefault("email", "");
            String ward          = data.getOrDefault("ward", "");
            String location      = data.getOrDefault("location", "");
            String category      = data.getOrDefault("category", "other");
            String subCategory   = data.getOrDefault("subCategory", "");
            String priority      = data.getOrDefault("priority", "medium");
            String description   = data.getOrDefault("description", "");
            String userIdStr     = data.getOrDefault("userId", null);

            if (applicantName.isBlank() || mobile.isBlank() || ward.isBlank()
                    || location.isBlank() || description.isBlank()) {
                send(exchange, 400, CORSUtil.toJson("message", "Required fields missing"));
                return;
            }

            String requestId = generateRequestId();

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO service_requests (request_id, user_id, applicant_name, mobile, email, ward, location, category, sub_category, priority, description) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?,?)")) {

                ps.setString(1, requestId);
                ps.setObject(2, userIdStr != null ? Long.parseLong(userIdStr) : null, Types.BIGINT);
                ps.setString(3, applicantName);
                ps.setString(4, mobile);
                ps.setString(5, email);
                ps.setString(6, ward);
                ps.setString(7, location);
                ps.setString(8, category);
                ps.setString(9, subCategory);
                ps.setString(10, priority);
                ps.setString(11, description);
                ps.executeUpdate();

                // Log initial status
                try (PreparedStatement logPs = conn.prepareStatement(
                        "INSERT INTO request_history (request_id, new_status, remarks) VALUES (?, 'PENDING', 'Request submitted')")) {
                    logPs.setString(1, requestId);
                    logPs.executeUpdate();
                }

                send(exchange, 201, CORSUtil.toJson(
                    "requestId", requestId,
                    "status", "PENDING",
                    "message", "Service request submitted successfully"
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500, CORSUtil.toJson("message", "Failed to submit request"));
        }
    }

    // ── GET BY ID ──────────────────────────────────────────────────────────
    public static void getById(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String requestId = path.substring(path.lastIndexOf('/') + 1);

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM service_requests WHERE request_id = ?")) {

                ps.setString(1, requestId);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    String json = buildRequestJson(rs);
                    send(exchange, 200, json);
                } else {
                    send(exchange, 404, CORSUtil.toJson("message", "Request not found"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500, CORSUtil.toJson("message", "Server error"));
        }
    }

    // ── GET BY USER ────────────────────────────────────────────────────────
    public static void getByUser(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String userId = path.substring(path.lastIndexOf('/') + 1);

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM service_requests WHERE user_id = ? ORDER BY created_at DESC")) {

                ps.setLong(1, Long.parseLong(userId));
                ResultSet rs = ps.executeQuery();

                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    sb.append(buildRequestJson(rs));
                    first = false;
                }
                sb.append("]");
                send(exchange, 200, sb.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500, CORSUtil.toJson("message", "Server error"));
        }
    }

    // ── HELPERS ────────────────────────────────────────────────────────────
    private static String generateRequestId() {
        return "NS" + System.currentTimeMillis() % 10000000;
    }

    private static String buildRequestJson(ResultSet rs) throws SQLException {
        return String.format(
            "{\"requestId\":\"%s\",\"applicantName\":\"%s\",\"mobile\":\"%s\",\"email\":\"%s\"," +
            "\"ward\":\"%s\",\"location\":\"%s\",\"category\":\"%s\",\"subCategory\":\"%s\"," +
            "\"priority\":\"%s\",\"description\":\"%s\",\"status\":\"%s\"," +
            "\"createdAt\":\"%s\",\"updatedAt\":\"%s\"}",
            rs.getString("request_id"),
            esc(rs.getString("applicant_name")),
            esc(rs.getString("mobile")),
            esc(rs.getString("email")),
            esc(rs.getString("ward")),
            esc(rs.getString("location")),
            esc(rs.getString("category")),
            esc(rs.getString("sub_category")),
            esc(rs.getString("priority")),
            esc(rs.getString("description")),
            esc(rs.getString("status")),
            rs.getTimestamp("created_at"),
            rs.getTimestamp("updated_at")
        );
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static void send(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }
}