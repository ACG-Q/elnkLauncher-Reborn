package io.github.reborn.einklauncher.model;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.github.reborn.einklauncher.IconMode;

/**
 * 应用图标、标签的内存缓存，以及自定义图标替换映射管理。
 * <ul>
 *   <li>{@link #getIcon} / {@link #getLabel} —— 带缓存的图标 / 标签加载</li>
 *   <li>{@link #refreshCustomIcons} —— 扫描外部存储中的自定义图标文件</li>
 *   <li>{@link #markDirty()} —— 标记需要重新扫描文件系统</li>
 *   <li>{@link #clearAppCache()} —— 应用安装/卸载后清除缓存</li>
 *   <li>{@link #getUnifiedIcon} —— 生成并缓存统一文字图标</li>
 * </ul>
 */
public class IconCache {

  private static final String TAG = "IconCache";
  private static final String ICON_DIR = "E-Ink Launcher" + File.separator + "icon";

  private final Map<String, Drawable> drawableCache = new HashMap<>();
  private final Map<String, CharSequence> labelCache = new HashMap<>();
  private final Map<String, File> customIconMap = new HashMap<>();
  private String iconMode = IconMode.DEFAULT;
  /** 统一文字图标 LRU 缓存，上限 128 条 */
  private final Map<String, Drawable> unifiedCache =
      new java.util.LinkedHashMap<String, Drawable>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Drawable> eldest) {
          return size() > 128;
        }
      };
  private boolean dirty = true;
  private IconStyle unifiedStyle = IconStyle.defaults();
  private Map<String, String> charOverrides = Collections.emptyMap();

  /** 推送统一图标全局样式与单字覆写；样式变更清空 LRU 以失效旧位图。 */
  public void setUnifiedConfig(IconStyle style, Map<String, String> overrides) {
    IconStyle newStyle = (style == null ? IconStyle.defaults() : style).clamped();
    if (!newStyle.equals(unifiedStyle)) {
      unifiedCache.clear();
    }
    this.unifiedStyle = newStyle;
    this.charOverrides = (overrides == null)
        ? Collections.<String, String>emptyMap() : new HashMap<>(overrides);
  }

  /** 返回指定包名的单字覆写，无则 null。 */
  public String getCharOverride(String packageName) {
    return charOverrides.get(packageName);
  }

  // =========================================================================
  // 自定义图标
  // =========================================================================

  /** 标记自定义图标映射为脏，下次 {@link #refreshCustomIcons} 时重新扫描 */
  public void markDirty() {
    dirty = true;
    unifiedCache.clear();
  }

  /** 当前图标模式，由 {@link #refreshCustomIcons} 在刷新时写入 */
  public String getIconMode() {
    return iconMode;
  }

  /**
   * 如有必要，重新扫描外部存储中的自定义图标目录。
   *
   * @param hasExternalStorage 外部存储是否可用
   * @param iconMode           图标模式；DEFAULT 不扫描并清空映射，UNIFIED/CUSTOM 均扫描
   * @return true 表示执行了实际扫描
   */
  public boolean refreshCustomIcons(boolean hasExternalStorage, String iconMode) {
    Log.d(TAG, "refreshCustomIcons: hasExternalStorage=" + hasExternalStorage
        + ", iconMode=" + iconMode + ", dirty=" + dirty);
    this.iconMode = IconMode.normalize(iconMode);
    if (!dirty) return false;
    customIconMap.clear();

    if (hasExternalStorage && !IconMode.DEFAULT.equals(this.iconMode)) {
      File root = getIconDirectory();
      Log.d(TAG, "Icon directory: " + root.getAbsolutePath() + ", exists=" + root.exists());
      if (!root.exists()) {
        try {
          boolean created = root.mkdirs();
          Log.d(TAG, "mkdirs result: " + created);
        } catch (Exception e) {
          Log.e(TAG, "mkdirs failed", e);
        }
      }
      File[] files = root.listFiles();
      Log.d(TAG, "Found files: " + (files != null ? files.length : 0));
      if (files != null) {
        for (File file : files) {
          String name = file.getName();
          int dot = name.lastIndexOf('.');
          String pkg = dot > 0 ? name.substring(0, dot) : name;
          Log.d(TAG, "Custom icon: pkg=" + pkg + ", file=" + file.getAbsolutePath());
          customIconMap.put(pkg, file);
        }
      }
    } else {
      Log.d(TAG, "Skipped scanning: hasExternalStorage=" + hasExternalStorage
          + ", iconMode=" + this.iconMode);
    }
    dirty = false;
    return true;
  }

  private static File getIconDirectory() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
      return new File(
          Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), ICON_DIR);
    }
    return new File(Environment.getExternalStorageDirectory(), ICON_DIR);
  }

  /** 获取指定包名的自定义图标文件，不存在时返回 null */
  public File getCustomIcon(String packageName) {
    return customIconMap.get(packageName);
  }

  /** 获取完整的自定义图标映射（包名 → 文件），供 WifiControl 使用 */
  public Map<String, File> getCustomIconMap() {
    return Collections.unmodifiableMap(customIconMap);
  }

  /** 获取（并缓存）指定应用的统一文字图标；key 含包名、文字、像素尺寸、反色标志与样式指纹 */
  public Drawable getUnifiedIcon(String packageName, String text, int sizePx, boolean dark) {
    String key = packageName + "|" + text + "|" + sizePx + "|" + dark + "|" + styleFingerprint();
    Drawable cached = unifiedCache.get(key);
    if (cached == null) {
      cached = UnifiedIconRenderer.create(text, sizePx, dark, unifiedStyle);
      unifiedCache.put(key, cached);
    }
    return cached;
  }

  private int styleFingerprint() {
    return Float.floatToIntBits(unifiedStyle.getStroke())
        * 31 + Float.floatToIntBits(unifiedStyle.getRadius()) * 31
        + Float.floatToIntBits(unifiedStyle.getTextScale());
  }

  // =========================================================================
  // 应用图标 & 标签缓存
  // =========================================================================

  /** 带缓存的图标加载；加载异常时按虚拟项自带资源/系统默认图标顺序兜底 */
  public Drawable getIcon(String packageName, ResolveInfo info, PackageManager pm) {
    Drawable cached = drawableCache.get(packageName);
    if (cached == null) {
      Drawable fallback = createIconFallback(info, pm);
      cached = safeLoad(packageName, () -> info.loadIcon(pm), fallback);
      drawableCache.put(packageName, cached);
    }
    return cached;
  }

  /** 带缓存的标签加载；加载异常或结果为空时兜底为包名 */
  public CharSequence getLabel(String packageName, ResolveInfo info, PackageManager pm) {
    CharSequence cached = labelCache.get(packageName);
    if (cached == null) {
      cached = safeLoad(packageName, () -> info.loadLabel(pm), packageName);
      labelCache.put(packageName, cached);
    }
    return cached;
  }

  /** 可抛出异常的加载器，由 {@link #safeLoad} 统一兜底 */
  interface Loader<T> {
    T load() throws Exception;
  }

  /**
   * 安全加载：加载器抛出异常或返回 null 时记录 warning 并返回兜底值，不向外抛出。
   *
   * @param packageName 用于日志定位的包名
   * @param loader      实际加载逻辑
   * @param fallback    兜底值
   * @return 加载结果；失败时为 {@code fallback}
   */
  static <T> T safeLoad(String packageName, Loader<T> loader, T fallback) {
    try {
      T value = loader.load();
      if (value != null) {
        return value;
      }
      Log.w(TAG, "load returned null: pkg=" + packageName);
    } catch (Exception e) {
      Log.w(TAG, "load failed: pkg=" + packageName, e);
    }
    return fallback;
  }

  /** 图标加载失败时的兜底：优先虚拟项自带资源，其次系统默认应用图标 */
  private static Drawable createIconFallback(ResolveInfo info, PackageManager pm) {
    try {
      if (info.icon != 0 && info.resolvePackageName != null) {
        Drawable d = pm.getResourcesForApplication(info.resolvePackageName)
            .getDrawable(info.icon);
        if (d != null) {
          return d;
        }
      }
    } catch (Exception e) {
      Log.w(TAG, "fallback resource load failed: pkg=" + info.resolvePackageName, e);
    }
    return pm.getDefaultActivityIcon();
  }

  /** 清除图标和标签缓存（应用安装/卸载时调用） */
  public void clearAppCache() {
    drawableCache.clear();
    labelCache.clear();
    unifiedCache.clear();
  }
}
