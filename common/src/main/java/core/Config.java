package core;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public final class Config {

    private static final String LOCAL_FILE = "config.local.properties";
    private static final Properties LOCAL = load(LOCAL_FILE);

    private static final Map<String, String> DEFAULTS = Map.of(
            "BASE_URL", "https://eventhub.rahulshettyacademy.com",
            "API_URL", "https://api.eventhub.rahulshettyacademy.com/api",
            "BROWSER", "chrome",
            "HEADLESS", "true",
            "UI_TIMEOUT_MS", "30000");

    private Config() {
    }

    public static String baseUrl() {
        return stripTrailingSlash(required("BASE_URL"));
    }

    public static String apiUrl() {
        return stripTrailingSlash(required("API_URL"));
    }

    public static String browser() {
        return required("BROWSER").toLowerCase();
    }

    public static boolean headless() {
        return Boolean.parseBoolean(required("HEADLESS"));
    }

    public static double uiTimeoutMillis() {
        return Double.parseDouble(required("UI_TIMEOUT_MS"));
    }

    public static String required(String key) {
        return get(key).orElseThrow(() -> new IllegalStateException(
                "Missing required configuration '" + key + "'. Set it as an environment variable, "
                        + "a -D" + key + "=... system property, or in " + LOCAL_FILE
                        + " (see README > Settings)."));
    }

    public static Optional<String> get(String key) {
        return firstNonBlank(
                System.getProperty(key),
                System.getenv(key),
                LOCAL.getProperty(key),
                DEFAULTS.get(key));
    }

    private static Optional<String> firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return Optional.of(value.trim());
            }
        }
        return Optional.empty();
    }

    private static Properties load(String fileName) {
        Properties props = new Properties();
        Path dir = Paths.get("").toAbsolutePath();
        for (int depth = 0; dir != null && depth < 4; depth++, dir = dir.getParent()) {
            Path candidate = dir.resolve(fileName);
            if (Files.isRegularFile(candidate)) {
                try (Reader reader = Files.newBufferedReader(candidate)) {
                    props.load(reader);
                } catch (IOException e) {
                    throw new IllegalStateException("Cannot read " + candidate, e);
                }
                break;
            }
        }
        return props;
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
