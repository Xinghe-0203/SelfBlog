package com.selfblog.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SiteAssetKind")
class SiteAssetKindTest {

    @ParameterizedTest
    @ValueSource(strings = {"logo", "favicon", "avatar", "music", "background"})
    @DisplayName("fromWireName_AcceptsEveryDocumentedWireName")
    void fromWireName_AcceptsEveryDocumentedWireName(String raw) {
        SiteAssetKind kind = SiteAssetKind.fromWireName(raw);
        assertNotNull(kind);
        assertEquals(raw, kind.getWireName());
    }

    @Test
    @DisplayName("fromWireName_IsCaseInsensitiveAndTrims")
    void fromWireName_IsCaseInsensitiveAndTrims() {
        assertEquals(SiteAssetKind.LOGO, SiteAssetKind.fromWireName("  LOGO "));
        assertEquals(SiteAssetKind.MUSIC, SiteAssetKind.fromWireName("Music"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "cover", "article", "video", "logo2", "会议"})
    @DisplayName("fromWireName_RejectsUnknownValues")
    void fromWireName_RejectsUnknownValues(String raw) {
        assertNull(SiteAssetKind.fromWireName(raw));
    }

    @Test
    @DisplayName("fromWireName_RejectsNull")
    void fromWireName_RejectsNull() {
        assertNull(SiteAssetKind.fromWireName(null));
    }

    @Test
    @DisplayName("onlyMusicIsAudio")
    void onlyMusicIsAudio() {
        assertTrue(SiteAssetKind.MUSIC.isAudio());
        assertEquals(SiteAssetKind.Category.AUDIO, SiteAssetKind.MUSIC.getCategory());
        assertFalse(SiteAssetKind.LOGO.isAudio());
        assertFalse(SiteAssetKind.FAVICON.isAudio());
        assertFalse(SiteAssetKind.AVATAR.isAudio());
        assertFalse(SiteAssetKind.BACKGROUND.isAudio());
    }

    @Test
    @DisplayName("backgroundAllowsRasterOnlyAndNoSvgOrIco")
    void backgroundAllowsRasterOnlyAndNoSvgOrIco() {
        assertTrue(SiteAssetKind.BACKGROUND.getExtensions().contains(".webp"));
        assertTrue(SiteAssetKind.BACKGROUND.getExtensions().contains(".jpg"));
        assertFalse(SiteAssetKind.BACKGROUND.getExtensions().contains(".svg"));
        assertFalse(SiteAssetKind.BACKGROUND.getExtensions().contains(".ico"));
    }

    @Test
    @DisplayName("backgroundGetsTheLargerCap")
    void backgroundGetsTheLargerCap() {
        assertEquals(8L, SiteAssetKind.BACKGROUND.getMaxMegabytes());
        assertTrue(SiteAssetKind.BACKGROUND.getMaxBytes() > SiteAssetKind.LOGO.getMaxBytes());
    }

    @Test
    @DisplayName("musicGetsTheLargerCap")
    void musicGetsTheLargerCap() {
        assertEquals(20L, SiteAssetKind.MUSIC.getMaxMegabytes());
        assertEquals(2L, SiteAssetKind.LOGO.getMaxMegabytes());
        assertEquals(2L, SiteAssetKind.FAVICON.getMaxMegabytes());
        assertEquals(2L, SiteAssetKind.AVATAR.getMaxMegabytes());
    }

    @Test
    @DisplayName("faviconAllowsIcoAndLogoAllowsSvg")
    void faviconAllowsIcoAndLogoAllowsSvg() {
        assertTrue(SiteAssetKind.FAVICON.getExtensions().contains(".ico"));
        assertTrue(SiteAssetKind.LOGO.getExtensions().contains(".svg"));
    }

    @Test
    @DisplayName("musicRejectsImageExtensions")
    void musicRejectsImageExtensions() {
        assertFalse(SiteAssetKind.MUSIC.getExtensions().contains(".png"));
    }

    @Test
    @DisplayName("describeExtensions_IsSortedAndReadable")
    void describeExtensions_IsSortedAndReadable() {
        assertEquals(".ico、.png、.svg", SiteAssetKind.FAVICON.describeExtensions());
    }

    @Test
    @DisplayName("extensionOf_LowercasesTheSuffix")
    void extensionOf_LowercasesTheSuffix() {
        assertEquals(".png", SiteAssetKind.extensionOf("Logo.PNG"));
        assertEquals(".jpeg", SiteAssetKind.extensionOf("my.photo.JPEG"));
        assertEquals(".svg", SiteAssetKind.extensionOf("a/b/my-logo.svg"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"noextension", "trailing.", ".hidden", "", "  "})
    @DisplayName("extensionOf_ReturnsEmptyWhenThereIsNoUsableSuffix")
    void extensionOf_ReturnsEmptyWhenThereIsNoUsableSuffix(String name) {
        assertEquals("", SiteAssetKind.extensionOf(name));
    }

    @Test
    @DisplayName("extensionOf_RejectsNull")
    void extensionOf_RejectsNull() {
        assertEquals("", SiteAssetKind.extensionOf(null));
    }
}
