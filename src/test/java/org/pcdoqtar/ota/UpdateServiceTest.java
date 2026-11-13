package org.pcdoqtar.ota;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UpdateService Release Parsing Tests")
class UpdateServiceTest {

    private UpdateService updateService;

    @BeforeEach
    void setUp() {
        updateService = new UpdateService("test-owner/test-repo", HttpClient.newHttpClient(), new ObjectMapper());
    }

    @Test
    @DisplayName("Should parse GitHub Release JSON and extract PCDOQTAR-Setup.exe asset")
    void testParseReleasePayloadWithSetupExe() {
        String json = """
        {
          "tag_name": "v1.2.0",
          "name": "PC DOQTAR v1.2.0",
          "body": "## What's Changed\\n* Added OTA update feature",
          "html_url": "https://github.com/KishoreNMproject/pc-doqtar/releases/tag/v1.2.0",
          "published_at": "2026-11-13T12:00:00Z",
          "assets": [
            {
              "name": "other-file.zip",
              "size": 5000,
              "browser_download_url": "https://github.com/downloads/other-file.zip"
            },
            {
              "name": "PCDOQTAR-Setup.exe",
              "size": 135000000,
              "browser_download_url": "https://github.com/downloads/PCDOQTAR-Setup.exe"
            }
          ]
        }
        """;

        Optional<UpdateInfo> opt = updateService.parseReleasePayload(json, "1.1.4");
        assertTrue(opt.isPresent(), "Expected UpdateInfo to be parsed");

        UpdateInfo info = opt.get();
        assertEquals("v1.2.0", info.tagName());
        assertEquals("1.2.0", info.version());
        assertEquals("PC DOQTAR v1.2.0", info.name());
        assertTrue(info.updateAvailable(), "v1.2.0 should be marked as updateAvailable compared to 1.1.4");
        assertEquals("https://github.com/downloads/PCDOQTAR-Setup.exe", info.downloadUrl());
        assertEquals(135000000L, info.assetSizeBytes());
        assertTrue(info.getFormattedSize().contains("MB"));
    }

    @Test
    @DisplayName("Should detect when installed version is already up to date")
    void testParseReleasePayloadAlreadyUpToDate() {
        String json = """
        {
          "tag_name": "v1.2.0",
          "name": "PC DOQTAR v1.2.0",
          "body": "No changes",
          "html_url": "https://github.com/KishoreNMproject/pc-doqtar/releases/tag/v1.2.0",
          "published_at": "2026-11-13T12:00:00Z",
          "assets": []
        }
        """;

        Optional<UpdateInfo> opt = updateService.parseReleasePayload(json, "1.2.0");
        assertTrue(opt.isPresent());
        assertFalse(opt.get().updateAvailable(), "Should not report update when local version is 1.2.0");
    }

    @Test
    @DisplayName("Should fallback to generic .exe asset if PCDOQTAR-Setup.exe is not explicitly named")
    void testFallbackExeAsset() {
        String json = """
        {
          "tag_name": "v1.3.0",
          "name": "PC DOQTAR v1.3.0",
          "assets": [
            {
              "name": "pcdoqtar-installer.exe",
              "size": 120000000,
              "browser_download_url": "https://github.com/downloads/pcdoqtar-installer.exe"
            }
          ]
        }
        """;

        Optional<UpdateInfo> opt = updateService.parseReleasePayload(json, "1.2.0");
        assertTrue(opt.isPresent());
        assertEquals("https://github.com/downloads/pcdoqtar-installer.exe", opt.get().downloadUrl());
    }

    @Test
    @DisplayName("Should return empty optional for invalid JSON")
    void testInvalidJson() {
        Optional<UpdateInfo> opt = updateService.parseReleasePayload("not a valid json", "1.2.0");
        assertTrue(opt.isEmpty());
    }
}
