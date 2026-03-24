package com.egov.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database Connection Utility – JDBC + MySQL
 */
public class DBConnection {

    private static final String DB_URL  = "jdbc:mysql://localhost:3306/nagarseva?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "12345678babu"; // Change this

    public static void init() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                createTables(stmt);
                System.out.println("[DB] Tables verified/created.");
            }
        } catch (Exception e) {
            System.err.println("[DB] Init error: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    private static void createTables(Statement stmt) throws SQLException {
        // Users table
        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS users (
                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                full_name   VARCHAR(100) NOT NULL,
                email       VARCHAR(150) UNIQUE NOT NULL,
                phone       VARCHAR(15)  NOT NULL,
                aadhaar     VARCHAR(12)  UNIQUE,
                dob         DATE,
                ward        VARCHAR(80),
                address     TEXT,
                password    VARCHAR(255) NOT NULL,
                created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);

        // Service requests table
        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS service_requests (
                id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                request_id      VARCHAR(20) UNIQUE NOT NULL,
                user_id         BIGINT,
                applicant_name  VARCHAR(100) NOT NULL,
                mobile          VARCHAR(15)  NOT NULL,
                email           VARCHAR(150),
                ward            VARCHAR(80)  NOT NULL,
                location        VARCHAR(255) NOT NULL,
                category        VARCHAR(50)  NOT NULL,
                sub_category    VARCHAR(80),
                priority        VARCHAR(20)  DEFAULT 'medium',
                description     TEXT NOT NULL,
                status          VARCHAR(20)  DEFAULT 'PENDING',
                assigned_to     VARCHAR(100),
                remarks         TEXT,
                created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                resolved_at     TIMESTAMP NULL,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
            )
        """);

        // Request status history
        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS request_history (
                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                request_id  VARCHAR(20) NOT NULL,
                old_status  VARCHAR(20),
                new_status  VARCHAR(20) NOT NULL,
                remarks     TEXT,
                changed_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);
    }
}