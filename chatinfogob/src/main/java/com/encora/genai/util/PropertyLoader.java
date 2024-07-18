package com.encora.genai.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyLoader.class);
    private static final String CONFIG_FILE = "config.properties";

    private static final Properties properties = new Properties();

    static {
        try (InputStream input = PropertyLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            properties.load(input);
            LOGGER.debug("{} was loaded.", CONFIG_FILE);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to find " + CONFIG_FILE);
        }
    }

    private PropertyLoader() {
    }

    public static String getString(String key) {
        return properties.getProperty(key);
    }

    public static Integer getInteger(String key) {
        return Integer.valueOf(properties.getProperty(key));
    }

    public static Double getDouble(String key) {
        return Double.valueOf(properties.getProperty(key));
    }

    public static String getString(String key, Map<String, String> replacement) {
        String value = properties.getProperty(key);
        for (Entry<String, String> entry : replacement.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            value = value.replace(placeholder , entry.getValue());
        }
        return value;
    }

    public static String[] getArray(String key) {
        String separator = getString("common.array.separator");
        return properties.getProperty(key).split(separator, 0);
    }

    public static String addSpacer(String text) {
        String spacer = getString("common.text.spacer");
        return text.replaceAll("(?m)^", spacer);
    }

}
