package ua.lpnu.kzp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Build information filled in by Maven from {@code pom.xml}, the single source of the version.
 */
final class ApplicationInfo {

    private static final String RESOURCE = "/version.properties";
    private static final Properties PROPERTIES = load();

    private ApplicationInfo() {
    }

    /**
     * Returns the project version defined in {@code pom.xml}.
     *
     * @return project version
     */
    static String version() {
        return PROPERTIES.getProperty("version");
    }

    /**
     * Returns the executable JAR name without extension.
     *
     * @return JAR base name defined by Maven
     */
    static String jarName() {
        return PROPERTIES.getProperty("jarName");
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream stream = ApplicationInfo.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Ресурс %s не знайдено".formatted(RESOURCE));
            }
            properties.load(stream);
        } catch (IOException exception) {
            throw new IllegalStateException("Не вдалося прочитати %s".formatted(RESOURCE), exception);
        }
        return properties;
    }
}
