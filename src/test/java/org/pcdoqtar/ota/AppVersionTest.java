package org.pcdoqtar.ota;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AppVersion & Semantic Versioning Tests")
class AppVersionTest {

    @Test
    @DisplayName("Should load application version from version.properties")
    void testGetVersion() {
        String version = AppVersion.get();
        assertNotNull(version);
        assertFalse(version.isBlank());
        assertEquals("1.2.0", version);
    }

    @ParameterizedTest
    @CsvSource({
            "v1.2.0, 1.2.0",
            "V2.0.1, 2.0.1",
            "1.0.0, 1.0.0",
            "'  v1.3.4  ', 1.3.4",
            "'', ''"
    })
    @DisplayName("Should clean version strings properly")
    void testCleanVersion(String input, String expected) {
        assertEquals(expected, AppVersion.cleanVersion(input));
    }

    @ParameterizedTest
    @CsvSource({
            "1.2.0, 1.1.4, true",
            "v1.2.0, 1.1.4, true",
            "1.2.0, v1.2.0, false",
            "1.1.4, 1.2.0, false",
            "1.10.0, 1.9.0, true",
            "2.0.0, 1.99.99, true",
            "1.2.1, 1.2.0, true",
            "1.2.0-rc1, 1.2.0, false",
            "1.2.0, 1.2.0-beta, true"
    })
    @DisplayName("Should accurately determine if remote version is newer")
    void testIsNewer(String remote, String local, boolean expected) {
        assertEquals(expected, AppVersion.isNewer(remote, local));
    }

    @Test
    @DisplayName("Compare should return 0 for identical versions")
    void testCompareIdentical() {
        assertEquals(0, AppVersion.compare("1.2.0", "1.2.0"));
        assertEquals(0, AppVersion.compare("v1.2.0", "1.2.0"));
        assertEquals(0, AppVersion.compare("1.2", "1.2.0"));
    }
}
