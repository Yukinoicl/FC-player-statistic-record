package com.fcdata.fcdataserver.config;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashSet;

public final class LocalAppSupport {

    public static final int DEFAULT_PORT = 11899;
    public static final int PACKAGED_PREFERRED_PORT = 80;
    public static final String PACKAGED_HOST = "fc-data-record.localhost";
    public static final String APP_ID = "fc-data-record";

    private LocalAppSupport() {}

    public static boolean packaged() {
        return Boolean.parseBoolean(System.getProperty("fcdata.packaged", "false"));
    }

    public static int port() {
        return port(new String[0]);
    }

    public static boolean hasExplicitPort(String[] args) {
        if (args != null) {
            for (String arg : args) {
                if (arg != null && arg.startsWith("--server.port=")) {
                    return true;
                }
            }
        }
        String value = System.getProperty("server.port");
        return value != null && !value.isBlank();
    }

    public static int port(String[] args) {
        if (args != null) {
            for (String arg : args) {
                if (arg != null && arg.startsWith("--server.port=")) {
                    return Integer.parseInt(arg.substring("--server.port=".length()).trim());
                }
            }
        }
        String value = System.getProperty("server.port");
        if (value != null && !value.isBlank()) {
            return Integer.parseInt(value);
        }
        return DEFAULT_PORT;
    }

    public static int resolveListenPort(String[] args) {
        if (hasExplicitPort(args)) {
            return port(args);
        }
        if (packaged() && !portInUse(PACKAGED_PREFERRED_PORT)) {
            return PACKAGED_PREFERRED_PORT;
        }
        return port(args);
    }

    public static Integer findRunningInstance(String[] args) {
        LinkedHashSet<Integer> candidates = new LinkedHashSet<>();
        if (packaged()) {
            candidates.add(PACKAGED_PREFERRED_PORT);
            candidates.add(DEFAULT_PORT);
        }
        candidates.add(port(args));
        for (int candidate : candidates) {
            if (isOurApp(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public static boolean isOurApp(int port) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create("http://127.0.0.1:" + port + "/api/health").toURL().openConnection();
            conn.setConnectTimeout(400);
            conn.setReadTimeout(400);
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(false);
            if (conn.getResponseCode() != 200) {
                return false;
            }
            try (InputStream in = conn.getInputStream()) {
                String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                if (!body.contains(APP_ID) && !(body.contains("\"status\"") && body.contains("ok"))) {
                    return false;
                }
                if (packaged()) {
                    return body.contains("\"mode\":\"packaged\"") || body.contains("\"mode\": \"packaged\"");
                }
                return true;
            }
        } catch (Exception ignored) {
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public static String publicHost() {
        return packaged() ? PACKAGED_HOST : "127.0.0.1";
    }

    public static String publicUrl(int port) {
        String host = publicHost();
        if (port == 80) {
            return "http://" + host;
        }
        return "http://" + host + ":" + port;
    }

    public static Path dataDir() {
        String override = System.getProperty("fcdata.home");
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        if (packaged()) {
            String local = System.getenv("LOCALAPPDATA");
            if (local == null || local.isBlank()) {
                local = System.getProperty("user.home");
            }
            return Path.of(local, "FC26Career", "data");
        }
        return Path.of("data");
    }

    public static Path dbFile() {
        return dataDir().resolve("fcdata.db");
    }

    public static String jdbcUrl() {
        return "jdbc:sqlite:" + dbFile().toAbsolutePath().normalize().toString().replace('\\', '/');
    }

    public static boolean portInUse(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 250);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    public static void openBrowser(int port) {
        String url = publicUrl(port);
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (Exception ignored) {
        }
        try {
            new ProcessBuilder("cmd", "/c", "start", "", url).start();
        } catch (IOException ignored) {
        }
    }
}
