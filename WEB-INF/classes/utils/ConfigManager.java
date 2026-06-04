package utils;

import jakarta.servlet.ServletContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {
    private static Properties properties = new Properties();

    public static void init(ServletContext context) {
        try (InputStream is = context.getResourceAsStream("/WEB-INF/config.properties")) {
            if (is != null) {
                properties.load(is);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    public static boolean isDeepSeekConfigured() {
        String apiKey = getProperty("deepseek.api.key");
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("YOUR_DEEPSEEK_API_KEY_HERE");
    }
}
