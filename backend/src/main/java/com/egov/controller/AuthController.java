package com.egov.controller;


import com.egov.util.CORSUtil;
import com.egov.util.DBConnection;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.sql.*;
import java.util.Map;

public class AuthController {

    // ── REGISTER ──────────────────────────────────────────────────────────
    public static void register(HttpExchange exchange) throws IOException {
        try {
            String body = CORSUtil.readBody(exchange);
            Map<String, String> data = CORSUtil.parseJson(body);

            String fullName = data.get("fullName");
            String email    = data.get("email");
            String phone    = data.get("phone");
            String aadhaar  = data.get("aadhaar");
            String dob      = data.get("dob");
            String ward     = data.get("ward");
            String address  = data.get("address");
            String password = data.get("password");

            if (fullName == null || email == null || password == null) {
                send(exchange, 400, CORSUtil.toJson("message", "Required fields missing"));
                return;
            }

            String hashedPw = hashPassword(password);

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO users (full_name, email, phone, aadhaar, dob, ward, address, password) VALUES (?,?,?,?,?,?,?,?)",
                     Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, fullName);
                ps.setString(2, email);
                ps.setString(3, phone);
                ps.setString(4, aadhaar);
                ps.setString(5, dob != null && !dob.isEmpty() ? dob : null);
                ps.setString(6, ward);
                ps.setString(7, address);
                ps.setString(8, hashedPw);
                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();
                long userId = rs.next() ? rs.getLong(1) : -1;

                send(exchange, 201, CORSUtil.toJson(
                    "message", "Registration successful",
                    "userId", userId
                ));
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            send(exchange, 409, CORSUtil.toJson("message", "Email or Aadhaar already registered"));
        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500, CORSUtil.toJson("message", "Internal server error"));
        }
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────
    public static void login(HttpExchange exchange) throws IOException {
        try {
            String body = CORSUtil.readBody(exchange);
            Map<String, String> data = CORSUtil.parseJson(body);

            String email    = data.get("email");
            String password = data.get("password");

            if (email == null || password == null) {
                send(exchange, 400, CORSUtil.toJson("message", "Email and password required"));
                return;
            }

            String hashedPw = hashPassword(password);

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, full_name, email, ward, phone FROM users WHERE email = ? AND password = ?")) {

                ps.setString(1, email);
                ps.setString(2, hashedPw);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    long id        = rs.getLong("id");
                    String name    = rs.getString("full_name");
                    String ward    = rs.getString("ward");
                    String phone   = rs.getString("phone");
                    // Simple token (use JWT in production)
                    String token   = "tok_" + id + "_" + System.currentTimeMillis();

                    send(exchange, 200, CORSUtil.toJson(
                        "id", id,
                        "fullName", name,
                        "email", email,
                        "ward", ward,
                        "phone", phone,
                        "token", token
                    ));
                } else {
                    send(exchange, 401, CORSUtil.toJson("message", "Invalid email or password"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500, CORSUtil.toJson("message", "Internal server error"));
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────
    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return password; // fallback (not for prod)
        }
    }

    private static void send(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }
}