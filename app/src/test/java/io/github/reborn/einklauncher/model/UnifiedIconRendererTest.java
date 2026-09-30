package io.github.reborn.einklauncher.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;

import org.junit.Test;

/**
 * {@link UnifiedIconRenderer} 纯逻辑助手测试：前景色、描边宽度、内容区尺寸。
 * 位图像素断言不可在纯 JVM 单测中进行，改由模拟器验收覆盖。
 */
public class UnifiedIconRendererTest {

  @Test
  public void resolveFgReturnsBlackInDayMode() {
    assertEquals(Color.BLACK, UnifiedIconRenderer.resolveFg(false));
  }

  @Test
  public void resolveFgReturnsWhiteInNightMode() {
    assertEquals(Color.WHITE, UnifiedIconRenderer.resolveFg(true));
  }

  @Test
  public void strokeWidthPxFollowsStyleRatio() {
    assertEquals(2.5f, UnifiedIconRenderer.strokeWidthPx(48, IconStyle.defaults()), 0.001f);
    assertEquals(5f, UnifiedIconRenderer.strokeWidthPx(96, IconStyle.defaults()), 0.001f);
  }

  @Test
  public void strokeWidthPxHasLowerBound() {
    assertTrue(UnifiedIconRenderer.strokeWidthPx(8, IconStyle.defaults()) >= 1f);
  }

  @Test
  public void contentBoxPxScalesWithGlyphScale() {
    assertEquals(48f * UnifiedIconRenderer.GLYPH_SCALE,
        UnifiedIconRenderer.contentBoxPx(48), 0.001f);
    assertTrue(UnifiedIconRenderer.contentBoxPx(100) < 100f);
  }
}
