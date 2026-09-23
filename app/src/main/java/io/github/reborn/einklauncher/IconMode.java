package io.github.reborn.einklauncher;

/**
 * 图标模式常量与归一化。
 * "default" = 全系统图标，"unified" = 统一文字图标（有自定义 png 仍优先），
 * "custom" = 仅自定义 png 替换；非法值一律回退 custom（与旧版默认行为一致）。
 */
public final class IconMode {

  public static final String DEFAULT = "default";
  public static final String UNIFIED = "unified";
  public static final String CUSTOM = "custom";

  private IconMode() {
  }

  public static String normalize(String value) {
    if (DEFAULT.equals(value) || UNIFIED.equals(value)) {
      return value;
    }
    return CUSTOM;
  }

  /** 旧布尔 showCustomIcon 的一次性迁移：true→custom，false→default。 */
  public static String fromLegacyBoolean(boolean showCustomIcon) {
    return showCustomIcon ? CUSTOM : DEFAULT;
  }
}
