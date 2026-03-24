package com.egov;



import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.egov.controller.AuthController;
import com.egov.controller.RequestController;
import com.egov.util.DBConnection;
import com.egov.util.CORSUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * NagarSeva E-Governance Portal – Backend Entry Point
 * Java RESTful API using built-in HttpServer + JDBC + MySQL
 * 
 * Run: javac -cp .:mysql-connector-j.jar src/**\/*.java -d out
 *       java -cp out:mysql-connector-j.jar com.egov.App
 */
public class App {

    public static void main(String[] args) throws IOException {
        // Initialize DB
        DBConnection.init();
        System.out.println("[NagarSeva] Database connected.");

        // Start HTTP server on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Register routes
        server.createContext("/api/auth/register", exchange -> {
            CORSUtil.setCORSHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(204, -1); return; }
            AuthController.register(exchange);
        });

        server.createContext("/api/auth/login", exchange -> {
            CORSUtil.setCORSHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(204, -1); return; }
            AuthController.login(exchange);
        });

        server.createContext("/api/requests", exchange -> {
            CORSUtil.setCORSHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(204, -1); return; }
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            // Route: POST /api/requests, GET /api/requests/{id}, GET /api/requests/user/{userId}
            if (path.matches("/api/requests/user/\\d+") && "GET".equals(method)) {
                RequestController.getByUser(exchange);
            } else if (path.matches("/api/requests/[A-Z0-9]+") && "GET".equals(method)) {
                RequestController.getById(exchange);
            } else if ("POST".equals(method)) {
                RequestController.create(exchange);
            } else {
                sendError(exchange, 405, "Method Not Allowed");
            }
        });

        server.createContext("/api/health", exchange -> {
            CORSUtil.setCORSHeaders(exchange);
            sendResponse(exchange, 200, "{\"status\":\"OK\",\"service\":\"NagarSeva API\"}");
        });

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("[NagarSeva] Server started on http://localhost:8080");
        System.out.println("[NagarSeva] API Base: http://localhost:8080/api");
    }

    public static void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    public static void sendError(HttpExchange exchange, int code, String message) throws IOException {
        sendResponse(exchange, code, String.format("{\"error\":\"%s\",\"message\":\"%s\"}", code, message));
    }
}