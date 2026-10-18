package com.sunrisedental.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DBConnection {
    private static volatile DBConnection instance;

    private final String url;
    private final String user;
    private final String password;

    private DBConnection() {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read db.properties", e);
        }
        this.url = setting("DB_URL", props.getProperty("db.url"));
        this.user = setting("DB_USER", props.getProperty("db.user"));
        this.password = setting("DB_PASSWORD", props.getProperty("db.password"));
        if (url == null || user == null) {
            throw new IllegalStateException("Database settings are missing. Check db.properties.");
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC driver not found", e);
        }
    }

    private static String setting(String envName, String fileValue) {
        String env = System.getenv(envName);
        return (env != null && !env.isBlank()) ? env : fileValue;
    }

    public static DBConnection getInstance() {
        if (instance == null) {
            synchronized (DBConnection.class) {
                if (instance == null) {
                    instance = new DBConnection();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password == null ? "" : password);
    }
}
