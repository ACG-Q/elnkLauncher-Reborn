package io.github.reborn.einklauncher.widgets;

import static org.junit.Assert.assertEquals;

import android.os.BatteryManager;

import org.junit.Test;

public class BatteryIconStateTest {

  @Test
  public void overheat_health_wins_over_any_status() {
    assertEquals(BatteryIconState.State.OVERHEAT,
        BatteryIconState.derive(BatteryManager.BATTERY_STATUS_CHARGING,
            BatteryManager.BATTERY_HEALTH_OVERHEAT, 80));
  }

  @Test
  public void charging_wins_over_low_level() {
    assertEquals(BatteryIconState.State.CHARGING,
        BatteryIconState.derive(BatteryManager.BATTERY_STATUS_CHARGING,
            BatteryManager.BATTERY_HEALTH_GOOD, 5));
  }

  @Test
  public void full_status_shows_full() {
    assertEquals(BatteryIconState.State.FULL,
        BatteryIconState.derive(BatteryManager.BATTERY_STATUS_FULL,
            BatteryManager.BATTERY_HEALTH_GOOD, 100));
  }

  @Test
  public void charging_status_shows_lightning() {
    assertEquals(BatteryIconState.State.CHARGING,
        BatteryIconState.derive(BatteryManager.BATTERY_STATUS_CHARGING,
            BatteryManager.BATTERY_HEALTH_GOOD, 50));
  }

  @Test
  public void discharging_below_fifteen_shows_low() {
    assertEquals(BatteryIconState.State.LOW,
        BatteryIconState.derive(BatteryManager.BATTERY_STATUS_DISCHARGING,
            BatteryManager.BATTERY_HEALTH_GOOD, 14));
  }

  @Test
  public void discharging_at_fifteen_shows_nothing() {
    assertEquals(BatteryIconState.State.NONE,
        BatteryIconState.derive(BatteryManager.BATTERY_STATUS_DISCHARGING,
            BatteryManager.BATTERY_HEALTH_GOOD, 15));
  }

  @Test
  public void not_charging_below_fifteen_shows_low() {
    assertEquals(BatteryIconState.State.LOW,
        BatteryIconState.derive(BatteryManager.BATTERY_STATUS_NOT_CHARGING,
            BatteryManager.BATTERY_HEALTH_GOOD, 10));
  }

  @Test
  public void unknown_status_shows_question_mark() {
    assertEquals(BatteryIconState.State.UNKNOWN,
        BatteryIconState.derive(BatteryManager.BATTERY_STATUS_UNKNOWN,
            BatteryManager.BATTERY_HEALTH_GOOD, 50));
  }

  @Test
  public void invalid_status_defaults_to_unknown() {
    assertEquals(BatteryIconState.State.UNKNOWN,
        BatteryIconState.derive(-1, -1, 50));
  }

  @Test
  public void discharging_with_negative_level_shows_nothing() {
    assertEquals(BatteryIconState.State.NONE,
        BatteryIconState.derive(BatteryManager.BATTERY_STATUS_DISCHARGING,
            BatteryManager.BATTERY_HEALTH_GOOD, -1));
  }
}
