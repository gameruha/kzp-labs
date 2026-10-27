package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ApplicationInfoTest {

    @Test
    void versionIsFilledInByMavenFromPom() {
        assertTrue(ApplicationInfo.version().matches("\\d+\\.\\d+\\.\\d+(-.+)?"));
    }

    @Test
    void jarNameContainsVersion() {
        assertEquals("lab01-%s".formatted(ApplicationInfo.version()), ApplicationInfo.jarName());
    }
}
