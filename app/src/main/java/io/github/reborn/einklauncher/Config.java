package io.github.reborn.einklauncher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.github.reborn.einklauncher.model.IconStyle;

/**
 * 应用配置管理类。
 * 统一管理 SharedPreferences 的读写，缓存常用配置值。
 * 偏好键（KEY_*）集中定义于此类。
 */
public class Config {

  // ---- 偏好键常量 ----
  public static final String KEY_COL_NUM = "colNumKey";
  public static final String KEY_ROW_NUM = "rowNumKey";
  public static final String KEY_APP_NAME_LINES = "appNameShowLines";
  public static final String KEY_HIDE_APPS = "hideAppsKey";
  public static final String KEY_FONT_SIZE = "launcherFontSize";
  public static final String KEY_HIDE_DIVIDER = "launcherHideDivider";
  public static final String KEY_SHOW_STATUS_BAR = "launcherShowStatusBar";
  public static final String KEY_SHOW_CUSTOM_ICON = "launcherShowCustomIcon";
  public static final String KEY_ICON_MODE = "launcherIconMode";
  public static final String KEY_SORT_MODE = "launcherSortMode";
  public static final String KEY_SHOW_WIFI_NAME = "launcherShowWifiName";
  public static final String KEY_LAST_UPDATE_CHECK = "launcherLastUpdateCheck";
  public static final String KEY_IGNORED_UPDATE_VERSION = "launcherIgnoredUpdateVersion";
  public static final String KEY_MENU_FORM = "launcherMenuForm";
  public static final String KEY_UNIFIED_STROKE = "launcherUnifiedStroke";
  public static final String KEY_UNIFIED_RADIUS = "launcherUnifiedRadius";
  public static final String KEY_UNIFIED_TEXT_SCALE = "launcherUnifiedTextScale";
  public static final String KEY_UNIFIED_CHARS = "launcherUnifiedChars";

  // ---- 默认值 ----
  private static final int DEFAULT_COL_NUM = 5;
  private static final int DEFAULT_ROW_NUM = 5;
  private static final float DEFAULT_FONT_SIZE = 14f;
  private static final int DEFAULT_APP_NAME_LINES = Integer.MAX_VALUE;
  private static final boolean DEFAULT_HIDE_DIVIDER = true;
  private static final boolean DEFAULT_SHOW_STATUS_BAR = true;
  private static final boolean DEFAULT_SHOW_CUSTOM_ICON = true;
  private static final boolean DEFAULT_SHOW_WIFI_NAME = true;
  private static final int DEFAULT_SORT_MODE = 0;

  private static final String PREFS_FILE = "launcherPropertyFile";

  private final SharedPreferences prefs;

  // ---- 缓存字段 ----
  private int colNum = -1;
  private int rowNum = -1;
  private float fontSize = -1;
  private int appNameLines = -1;
  private boolean hideDivider;
  private boolean showStatusBar;
  private boolean showWifiName;
  private int sortMode = -1;
  private final Set<String> hideApps = new HashSet<>();
  private boolean hideAppsLoaded = false;
  private final Map<String, String> charOverrides = new HashMap<>();
  private boolean charOverridesLoaded = false;

  public Config(Context context) {
    this.prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
    // 预加载布尔配置
    this.hideDivider = prefs.getBoolean(KEY_HIDE_DIVIDER, DEFAULT_HIDE_DIVIDER);
    this.showStatusBar = prefs.getBoolean(KEY_SHOW_STATUS_BAR, DEFAULT_SHOW_STATUS_BAR);
    this.showWifiName = prefs.getBoolean(KEY_SHOW_WIFI_NAME, DEFAULT_SHOW_WIFI_NAME);
    this.appNameLines = prefs.getInt(KEY_APP_NAME_LINES, DEFAULT_APP_NAME_LINES);
  }

  // ---- 列数 ----

  public int getColNum() {
    if (colNum == -1) {
      colNum = prefs.getInt(KEY_COL_NUM, DEFAULT_COL_NUM);
    }
    return colNum;
  }

  public void setColNum(int colNum) {
    if (this.colNum == colNum) return;
    this.colNum = colNum;
    prefs.edit().putInt(KEY_COL_NUM, colNum).apply();
  }

  // ---- 行数 ----

  public int getRowNum() {
    if (rowNum == -1) {
      rowNum = prefs.getInt(KEY_ROW_NUM, DEFAULT_ROW_NUM);
    }
    return rowNum;
  }

  public void setRowNum(int rowNum) {
    if (this.rowNum == rowNum) return;
    this.rowNum = rowNum;
    prefs.edit().putInt(KEY_ROW_NUM, rowNum).apply();
  }

  // ---- 隐藏应用 ----

  public void addHideApp(String packageName) {
    ensureHideAppsLoaded();
    hideApps.add(packageName);
    prefs.edit().putStringSet(KEY_HIDE_APPS, hideApps).apply();
  }

  public void removeHideApp(String packageName) {
    ensureHideAppsLoaded();
    hideApps.remove(packageName);
    prefs.edit().putStringSet(KEY_HIDE_APPS, hideApps).apply();
  }

  public void setHideApps(Set<String> hideApps) {
    this.hideApps.clear();
    this.hideApps.addAll(hideApps);
    this.hideAppsLoaded = true;
    prefs.edit().putStringSet(KEY_HIDE_APPS, this.hideApps).apply();
  }

  public Set<String> getHideApps() {
    ensureHideAppsLoaded();
    return hideApps;
  }

  private void ensureHideAppsLoaded() {
    if (!hideAppsLoaded) {
      hideApps.addAll(prefs.getStringSet(KEY_HIDE_APPS, new HashSet<String>()));
      hideAppsLoaded = true;
    }
  }

  // ---- 字体大小 ----

  public float getFontSize() {
    if (fontSize < 0) {
      fontSize = prefs.getFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
    }
    return fontSize;
  }

  public void setFontSize(float fontSize) {
    this.fontSize = fontSize;
    prefs.edit().putFloat(KEY_FONT_SIZE, fontSize).apply();
  }

  // ---- 分隔线 ----

  public boolean isHideDivider() {
    return hideDivider;
  }

  public void setHideDivider(boolean hide) {
    this.hideDivider = hide;
    prefs.edit().putBoolean(KEY_HIDE_DIVIDER, hide).apply();
  }

  // ---- 状态栏 ----

  public boolean isShowStatusBar() {
    return showStatusBar;
  }

  public void setShowStatusBar(boolean show) {
    this.showStatusBar = show;
    prefs.edit().putBoolean(KEY_SHOW_STATUS_BAR, show).apply();
  }

  // ---- 图标模式 ----

  /**
   * 返回归一化后的图标模式：IconMode.DEFAULT / UNIFIED / CUSTOM。
   * 新键缺失时按旧布尔 showCustomIcon 一次性迁移（true→custom，false→default）。
   */
  public String getIconMode() {
    String stored = prefs.getString(KEY_ICON_MODE, null);
    if (stored == null) {
      String legacy = IconMode.fromLegacyBoolean(
          prefs.getBoolean(KEY_SHOW_CUSTOM_ICON, DEFAULT_SHOW_CUSTOM_ICON));
      prefs.edit().putString(KEY_ICON_MODE, legacy).apply();
      return legacy;
    }
    return IconMode.normalize(stored);
  }

  public void setIconMode(String mode) {
    prefs.edit().putString(KEY_ICON_MODE, IconMode.normalize(mode)).apply();
  }

  // ---- 统一图标全局样式 ----

  public IconStyle getUnifiedStyle() {
    IconStyle style = new IconStyle(
        prefs.getFloat(KEY_UNIFIED_STROKE, IconStyle.DEFAULT_STROKE),
        prefs.getFloat(KEY_UNIFIED_RADIUS, IconStyle.DEFAULT_RADIUS),
        prefs.getFloat(KEY_UNIFIED_TEXT_SCALE, IconStyle.DEFAULT_TEXT_SCALE));
    return style.clamped();
  }

  public void setUnifiedStyle(IconStyle style) {
    IconStyle c = style.clamped();
    prefs.edit()
        .putFloat(KEY_UNIFIED_STROKE, c.getStroke())
        .putFloat(KEY_UNIFIED_RADIUS, c.getRadius())
        .putFloat(KEY_UNIFIED_TEXT_SCALE, c.getTextScale())
        .apply();
  }

  // ---- 统一图标单字覆写 ----

  /** 返回包名 → 覆写字 的只读快照。 */
  public Map<String, String> getUnifiedCharOverrides() {
    ensureCharOverridesLoaded();
    return new HashMap<>(charOverrides);
  }

  public void setUnifiedChar(String packageName, String text) {
    ensureCharOverridesLoaded();
    charOverrides.put(packageName, text);
    prefs.edit().putStringSet(KEY_UNIFIED_CHARS, charEntrySet()).apply();
  }

  public void clearUnifiedChar(String packageName) {
    ensureCharOverridesLoaded();
    charOverrides.remove(packageName);
    prefs.edit().putStringSet(KEY_UNIFIED_CHARS, charEntrySet()).apply();
  }

  private Set<String> charEntrySet() {
    Set<String> entries = new HashSet<>();
    for (Map.Entry<String, String> e : charOverrides.entrySet()) {
      entries.add(e.getKey() + "=" + e.getValue());
    }
    return entries;
  }

  private void ensureCharOverridesLoaded() {
    if (charOverridesLoaded) {
      return;
    }
    Set<String> stored = prefs.getStringSet(KEY_UNIFIED_CHARS, new HashSet<String>());
    for (String entry : stored) {
      int eq = entry.indexOf('=');
      if (eq > 0 && eq < entry.length() - 1) {
        charOverrides.put(entry.substring(0, eq), entry.substring(eq + 1));
      }
    }
    charOverridesLoaded = true;
  }

  // ---- 显示WiFi名字 ----

  public boolean isShowWifiName() {
    return showWifiName;
  }

  public void setShowWifiName(boolean show) {
    this.showWifiName = show;
    prefs.edit().putBoolean(KEY_SHOW_WIFI_NAME, show).apply();
  }

  // ---- 应用名行数 ----

  public int getAppNameLines() {
    return appNameLines;
  }

  public void setAppNameLines(int lines) {
    this.appNameLines = lines;
    prefs.edit().putInt(KEY_APP_NAME_LINES, lines).apply();
  }

  // ---- 排序方式 ----

  public int getSortMode() {
    if (sortMode == -1) {
      sortMode = prefs.getInt(KEY_SORT_MODE, DEFAULT_SORT_MODE);
    }
    return sortMode;
  }

  public void setSortMode(int mode) {
    if (this.sortMode == mode) return;
    this.sortMode = mode;
    prefs.edit().putInt(KEY_SORT_MODE, mode).apply();
  }

  // ---- 自动更新 ----

  public long getLastUpdateCheck() {
    return prefs.getLong(KEY_LAST_UPDATE_CHECK, 0);
  }

  public void setLastUpdateCheck(long timeMillis) {
    prefs.edit().putLong(KEY_LAST_UPDATE_CHECK, timeMillis).apply();
  }

  public String getIgnoredUpdateVersion() {
    return prefs.getString(KEY_IGNORED_UPDATE_VERSION, "");
  }

  public void setIgnoredUpdateVersion(String version) {
    prefs.edit().putString(KEY_IGNORED_UPDATE_VERSION, version).apply();
  }

  // ---- 菜单形态 ----

  /** 返回归一化后的菜单形态：MenuForm.GROUPED 或 MenuForm.MINIMAL。 */
  public String getMenuForm() {
    return MenuForm.normalize(prefs.getString(KEY_MENU_FORM, MenuForm.GROUPED));
  }

  public void setMenuForm(String form) {
    prefs.edit().putString(KEY_MENU_FORM, MenuForm.normalize(form)).apply();
  }
}
