package io.github.reborn.einklauncher;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IconTextTest {

  @Test
  public void effective_nonempty_override_wins() {
    assertEquals("游", IconText.effective("Google Play 游戏", "游"));
    assertEquals("XY", IconText.effective("Chrome", "XY"));
  }

  @Test
  public void effective_null_override_falls_back_to_of() {
    assertEquals("CH", IconText.effective("Chrome", null));
    assertEquals("微", IconText.effective("微信", null));
  }

  @Test
  public void effective_empty_override_falls_back_to_of() {
    assertEquals("CH", IconText.effective("Chrome", ""));
  }
}
