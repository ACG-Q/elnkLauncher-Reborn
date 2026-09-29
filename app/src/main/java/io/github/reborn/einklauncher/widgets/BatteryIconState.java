package io.github.reborn.einklauncher.widgets;

import android.os.BatteryManager;

/**
 * 由 BatteryManager 原始状态推导电池图标的单槽位状态。
 * 优先级：过热 &gt; 已满 &gt; 充电 &gt; 低电 &gt; 未知；正常放电不显示图标。
 */
public final class BatteryIconState {

  public enum State { NONE, OVERHEAT, FULL, CHARGING, LOW, UNKNOWN }

  private BatteryIconState() {
  }

  public static State derive(int status, int health, int level) {
    if (health == BatteryManager.BATTERY_HEALTH_OVERHEAT) {
      return State.OVERHEAT;
    }
    switch (status) {
      case BatteryManager.BATTERY_STATUS_FULL:
        return State.FULL;
      case BatteryManager.BATTERY_STATUS_CHARGING:
        return State.CHARGING;
      case BatteryManager.BATTERY_STATUS_DISCHARGING:
      case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
        return (level >= 0 && level < 15) ? State.LOW : State.NONE;
      case BatteryManager.BATTERY_STATUS_UNKNOWN:
      default:
        return State.UNKNOWN;
    }
  }
}
