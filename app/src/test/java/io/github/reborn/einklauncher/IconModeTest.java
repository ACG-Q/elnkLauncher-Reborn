package io.github.reborn.einklauncher;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IconModeTest {

  @Test
  public void null_defaults_to_custom() {
    assertEquals(IconMode.CUSTOM, IconMode.normalize(null));
  }

  @Test
  public void unknown_value_defaults_to_custom() {
    assertEquals(IconMode.CUSTOM, IconMode.normalize("weird"));
  }

  @Test
  public void default_kept() {
    assertEquals(IconMode.DEFAULT, IconMode.normalize("default"));
  }

  @Test
  public void unified_kept() {
    assertEquals(IconMode.UNIFIED, IconMode.normalize("unified"));
  }

  @Test
  public void custom_kept() {
    assertEquals(IconMode.CUSTOM, IconMode.normalize("custom"));
  }

  @Test
  public void legacy_true_maps_to_custom() {
    assertEquals(IconMode.CUSTOM, IconMode.fromLegacyBoolean(true));
  }

  @Test
  public void legacy_false_maps_to_default() {
    assertEquals(IconMode.DEFAULT, IconMode.fromLegacyBoolean(false));
  }
}
