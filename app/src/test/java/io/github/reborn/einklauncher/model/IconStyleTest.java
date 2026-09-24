package io.github.reborn.einklauncher.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IconStyleTest {

  @Test
  public void defaults_match_legacy_hardcoded_values() {
    IconStyle s = IconStyle.defaults();
    assertEquals(2.5f, s.getStroke(), 0.0001f);
    assertEquals(10f, s.getRadius(), 0.0001f);
    assertEquals(0.42f, s.getTextScale(), 0.0001f);
  }

  @Test
  public void clampedStroke_below_min_becomes_min() {
    assertEquals(1f, new IconStyle(0f, 10f, 0.42f).clamped().getStroke(), 0f);
  }

  @Test
  public void clampedStroke_above_max_becomes_max() {
    assertEquals(6f, new IconStyle(99f, 10f, 0.42f).clamped().getStroke(), 0f);
  }

  @Test
  public void clampedRadius_bounds() {
    assertEquals(0f, new IconStyle(2.5f, -5f, 0.42f).clamped().getRadius(), 0f);
    assertEquals(20f, new IconStyle(2.5f, 99f, 0.42f).clamped().getRadius(), 0f);
  }

  @Test
  public void clampedTextScale_below_min_becomes_min() {
    assertEquals(0.30f, new IconStyle(2.5f, 10f, 0.01f).clamped().getTextScale(), 0f);
  }

  @Test
  public void clampedTextScale_above_max_becomes_max() {
    assertEquals(0.55f, new IconStyle(2.5f, 10f, 0.99f).clamped().getTextScale(), 0f);
  }
}
