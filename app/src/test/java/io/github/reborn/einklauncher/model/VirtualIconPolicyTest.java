package io.github.reborn.einklauncher.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.github.reborn.einklauncher.IconMode;

/**
 * {@link VirtualIconPolicy} 裁定规则：自定义 PNG > 统一矢量 > 传统 PNG；
 * 以及虚拟入口布尔态到 {@link UnifiedIconRenderer.GlyphType} 的映射。
 */
public class VirtualIconPolicyTest {

  @Test
  public void customIconWinsOverUnifiedMode() {
    assertEquals(VirtualIconPolicy.Path.CUSTOM_PNG,
        VirtualIconPolicy.resolve(true, IconMode.UNIFIED));
  }

  @Test
  public void unifiedModeWithoutCustomUsesGlyph() {
    assertEquals(VirtualIconPolicy.Path.GLYPH,
        VirtualIconPolicy.resolve(false, IconMode.UNIFIED));
  }

  @Test
  public void defaultModeWithoutCustomUsesLegacyPng() {
    assertEquals(VirtualIconPolicy.Path.LEGACY_PNG,
        VirtualIconPolicy.resolve(false, IconMode.DEFAULT));
  }

  @Test
  public void customModeWithoutCustomUsesLegacyPng() {
    assertEquals(VirtualIconPolicy.Path.LEGACY_PNG,
        VirtualIconPolicy.resolve(false, IconMode.CUSTOM));
  }

  @Test
  public void nullIconModeTreatedAsLegacy() {
    assertEquals(VirtualIconPolicy.Path.LEGACY_PNG,
        VirtualIconPolicy.resolve(false, null));
  }

  @Test
  public void wifiGlyphMapsOnOffStates() {
    assertEquals(UnifiedIconRenderer.GlyphType.WIFI_ON, VirtualIconPolicy.wifiGlyph(true));
    assertEquals(UnifiedIconRenderer.GlyphType.WIFI_OFF, VirtualIconPolicy.wifiGlyph(false));
  }

  @Test
  public void serverGlyphMapsRunningStoppedStates() {
    assertEquals(UnifiedIconRenderer.GlyphType.SERVER_ON, VirtualIconPolicy.serverGlyph(true));
    assertEquals(UnifiedIconRenderer.GlyphType.SERVER_OFF, VirtualIconPolicy.serverGlyph(false));
  }
}
