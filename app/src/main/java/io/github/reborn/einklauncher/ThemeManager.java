package io.github.reborn.einklauncher;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Calendar;

/**
 * 主题模式判定：纯逻辑、无 Android UI 依赖，供 {@link ThemeContext} 与图标联动调用。
 * 夜间判定结果 = 「是否黑夜」，资源层再按 values-night 取反。
 */
public final class ThemeManager {

  public static final String MODE_DAY = "day";
  public static final String MODE_NIGHT = "night";
  public static final String MODE_AUTO_SYSTEM = "auto_system";
  public static final String MODE_AUTO_TIME = "auto_time";

  private static final int DEFAULT_NIGHT_START = 18 * 60;
  private static final int DEFAULT_NIGHT_END = 7 * 60;

  private ThemeManager() {
  }

  /** 归一化模式串；未知值一律回退为跟随系统。 */
  public static String normalizeMode(String mode) {
    if (MODE_DAY.equals(mode) || MODE_NIGHT.equals(mode)
        || MODE_AUTO_SYSTEM.equals(mode) || MODE_AUTO_TIME.equals(mode)) {
      return mode;
    }
    return MODE_AUTO_SYSTEM;
  }

  /** 读取 uiMode 的夜间位（常量在编译期内联，单测可直接调用）。 */
  public static boolean systemIsNight(int uiMode) {
    return (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
  }

  /** 规格 §3.2 签名，默认时段 18:00→07:00。 */
  public static boolean isNight(String mode, boolean systemIsNight, int nowMinute) {
    return isNight(mode, systemIsNight, nowMinute, DEFAULT_NIGHT_START, DEFAULT_NIGHT_END);
  }

  /** 四模式判定；auto_time 支持跨零点，start==end 视为不启用。 */
  public static boolean isNight(String mode, boolean systemIsNight, int nowMinute,
      int startMinute, int endMinute) {
    switch (normalizeMode(mode)) {
      case MODE_DAY:
        return false;
      case MODE_NIGHT:
        return true;
      case MODE_AUTO_SYSTEM:
        return systemIsNight;
      default:
        return inNightWindow(nowMinute, startMinute, endMinute);
    }
  }

  static boolean inNightWindow(int now, int start, int end) {
    if (start == end) {
      return false;
    }
    if (start < end) {
      return now >= start && now < end;
    }
    return now >= start || now < end;
  }

  /** 本地时区「今天第几分钟」。 */
  public static int nowMinuteOfDay() {
    Calendar calendar = Calendar.getInstance();
    return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
  }

  /** 结合 Config、系统 uiMode 与当前时间的即时判定。 */
  public static boolean isNightNow(Context context) {
    Config config = new Config(context);
    int uiMode = context.getResources().getConfiguration().uiMode;
    return isNight(config.getThemeMode(), systemIsNight(uiMode), nowMinuteOfDay(),
        config.getThemeNightStart(), config.getThemeNightEnd());
  }
}
