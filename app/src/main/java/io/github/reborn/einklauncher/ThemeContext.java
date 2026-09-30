package io.github.reborn.einklauncher;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Log;

/**
 * 按主题配置为 Context 注入强制 uiMode（values-night 解析开关），
 * 并在夜间状态跨边界时触发 Activity 重建。
 */
public final class ThemeContext {

  private static final String TAG = "ThemeContext";
  private static volatile Boolean lastAppliedNight;

  private ThemeContext() {
  }

  /** 计算目标夜间态并返回可解析到对应 values-night 的 Context。 */
  public static Context wrap(Context base) {
    boolean night = ThemeManager.isNightNow(base);
    int oldUiMode = base.getResources().getConfiguration().uiMode;
    int newUiMode = (oldUiMode & ~Configuration.UI_MODE_NIGHT_MASK)
        | (night ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
    lastAppliedNight = night;
    if (oldUiMode == newUiMode) {
      return base;
    }
    Configuration override = new Configuration(base.getResources().getConfiguration());
    override.uiMode = newUiMode;
    if (Build.VERSION.SDK_INT >= 17) {
      return base.createConfigurationContext(override);
    }
    return updateLegacy(base, override);
  }

  /** 系统态或时段跨边界时重建；未跨边界返回 false。 */
  public static boolean refreshIfChanged(Activity activity) {
    Boolean applied = lastAppliedNight;
    if (applied == null || activity == null || activity.isFinishing()) {
      return false;
    }
    boolean now = ThemeManager.isNightNow(activity);
    if (now == applied) {
      return false;
    }
    lastAppliedNight = now;
    Log.d(TAG, "recreate: night " + applied + " -> " + now);
    activity.recreate();
    return true;
  }

  @SuppressWarnings("deprecation")
  private static Context updateLegacy(Context base, Configuration override) {
    base.getResources().updateConfiguration(override, base.getResources().getDisplayMetrics());
    return base;
  }
}
