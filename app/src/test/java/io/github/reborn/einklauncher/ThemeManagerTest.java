package io.github.reborn.einklauncher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.res.Configuration;

import org.junit.Test;

public class ThemeManagerTest {

  @Test
  public void day_always_light() {
    assertFalse(ThemeManager.isNight(ThemeManager.MODE_DAY, true, 1200));
    assertFalse(ThemeManager.isNight(ThemeManager.MODE_DAY, false, 0));
  }

  @Test
  public void night_always_dark() {
    assertTrue(ThemeManager.isNight(ThemeManager.MODE_NIGHT, false, 600));
    assertTrue(ThemeManager.isNight(ThemeManager.MODE_NIGHT, true, 600));
  }

  @Test
  public void auto_system_follows_system() {
    assertTrue(ThemeManager.isNight(ThemeManager.MODE_AUTO_SYSTEM, true, 600));
    assertFalse(ThemeManager.isNight(ThemeManager.MODE_AUTO_SYSTEM, false, 600));
  }

  @Test
  public void auto_time_default_range_boundaries() {
    String mode = ThemeManager.MODE_AUTO_TIME;
    assertFalse(ThemeManager.isNight(mode, false, 17 * 60 + 59));
    assertTrue(ThemeManager.isNight(mode, false, 18 * 60));
    assertTrue(ThemeManager.isNight(mode, false, 6 * 60 + 59));
    assertFalse(ThemeManager.isNight(mode, false, 7 * 60));
  }

  @Test
  public void auto_time_cross_midnight() {
    assertTrue(ThemeManager.isNight(ThemeManager.MODE_AUTO_TIME, false, 0));
    assertTrue(ThemeManager.isNight(ThemeManager.MODE_AUTO_TIME, false, 23 * 60 + 59));
    assertFalse(ThemeManager.isNight(ThemeManager.MODE_AUTO_TIME, false, 12 * 60));
  }

  @Test
  public void auto_time_same_start_end_never_night() {
    assertFalse(ThemeManager.isNight(ThemeManager.MODE_AUTO_TIME, false, 720, 720, 720));
    assertFalse(ThemeManager.isNight(ThemeManager.MODE_AUTO_TIME, false, 721, 720, 720));
  }

  @Test
  public void auto_time_range_within_single_day() {
    String mode = ThemeManager.MODE_AUTO_TIME;
    assertFalse(ThemeManager.isNight(mode, false, 479, 480, 1020));
    assertTrue(ThemeManager.isNight(mode, false, 480, 480, 1020));
    assertTrue(ThemeManager.isNight(mode, false, 1019, 480, 1020));
    assertFalse(ThemeManager.isNight(mode, false, 1020, 480, 1020));
  }

  @Test
  public void normalize_mode_defaults_to_auto_system() {
    assertEquals(ThemeManager.MODE_AUTO_SYSTEM, ThemeManager.normalizeMode(null));
    assertEquals(ThemeManager.MODE_AUTO_SYSTEM, ThemeManager.normalizeMode("weird"));
    assertEquals(ThemeManager.MODE_DAY, ThemeManager.normalizeMode("day"));
    assertEquals(ThemeManager.MODE_NIGHT, ThemeManager.normalizeMode("night"));
    assertEquals(ThemeManager.MODE_AUTO_TIME, ThemeManager.normalizeMode("auto_time"));
  }

  @Test
  public void system_is_night_reads_ui_mode_mask() {
    assertTrue(ThemeManager.systemIsNight(Configuration.UI_MODE_NIGHT_YES));
    assertFalse(ThemeManager.systemIsNight(Configuration.UI_MODE_NIGHT_NO));
  }
}
