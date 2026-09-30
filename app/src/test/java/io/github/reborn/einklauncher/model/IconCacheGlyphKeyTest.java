package io.github.reborn.einklauncher.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

/**
 * {@link IconCache#glyphKey} 测试：key 对包名、图形类型、尺寸、昼夜、样式的区分性。
 */
public class IconCacheGlyphKeyTest {

  private static String key(String pkg, UnifiedIconRenderer.GlyphType type,
                            int size, boolean dark, int styleFp) {
    return IconCache.glyphKey(pkg, type, size, dark, styleFp);
  }

  @Test
  public void keyIsDeterministic() {
    assertEquals(
        key("p", UnifiedIconRenderer.GlyphType.LOCK, 100, true, 7),
        key("p", UnifiedIconRenderer.GlyphType.LOCK, 100, true, 7));
  }

  @Test
  public void keysDifferByGlyphType() {
    assertNotEquals(
        key("E-ink_Launcher.WiFi", UnifiedIconRenderer.GlyphType.WIFI_ON, 100, true, 7),
        key("E-ink_Launcher.WiFi", UnifiedIconRenderer.GlyphType.WIFI_OFF, 100, true, 7));
  }

  @Test
  public void keysDifferByDarkFlag() {
    assertNotEquals(
        key("p", UnifiedIconRenderer.GlyphType.LOCK, 100, true, 7),
        key("p", UnifiedIconRenderer.GlyphType.LOCK, 100, false, 7));
  }

  @Test
  public void keysDifferBySize() {
    assertNotEquals(
        key("p", UnifiedIconRenderer.GlyphType.LOCK, 100, true, 7),
        key("p", UnifiedIconRenderer.GlyphType.LOCK, 144, true, 7));
  }

  @Test
  public void keysDifferByStyleFingerprint() {
    assertNotEquals(
        key("p", UnifiedIconRenderer.GlyphType.LOCK, 100, true, 7),
        key("p", UnifiedIconRenderer.GlyphType.LOCK, 100, true, 8));
  }

  @Test
  public void keysDifferByPackage() {
    assertNotEquals(
        key("a", UnifiedIconRenderer.GlyphType.LOCK, 100, true, 7),
        key("b", UnifiedIconRenderer.GlyphType.LOCK, 100, true, 7));
  }
}
