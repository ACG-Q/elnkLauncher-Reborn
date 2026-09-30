package io.github.reborn.einklauncher.model;

import io.github.reborn.einklauncher.IconMode;

/**
 * 虚拟入口（一键锁屏 / WiFi / 服务器）图标路径裁定。
 * 优先级：自定义 PNG > 统一模式矢量（glyph）> 传统 PNG（夜间 INVERT）。
 */
public final class VirtualIconPolicy {

  /** 裁定结果：三路图标来源 */
  public enum Path {
    CUSTOM_PNG, GLYPH, LEGACY_PNG
  }

  private VirtualIconPolicy() {
  }

  /**
   * 裁定图标路径。
   *
   * @param hasCustomIcon 是否存在用户自定义图标文件
   * @param iconMode      当前图标模式；null 或未知值走 LEGACY_PNG
   * @return 三路之一
   */
  public static Path resolve(boolean hasCustomIcon, String iconMode) {
    if (hasCustomIcon) {
      return Path.CUSTOM_PNG;
    }
    if (IconMode.UNIFIED.equals(iconMode)) {
      return Path.GLYPH;
    }
    return Path.LEGACY_PNG;
  }

  /**
   * WiFi 状态到图形类型的映射。
   *
   * @param on true=已开启/连接中（伞形），false=关闭/开启中（X）
   */
  public static UnifiedIconRenderer.GlyphType wifiGlyph(boolean on) {
    return on ? UnifiedIconRenderer.GlyphType.WIFI_ON
        : UnifiedIconRenderer.GlyphType.WIFI_OFF;
  }

  /**
   * 服务器状态到图形类型的映射。
   *
   * @param running true=运行中（线框地球），false=已停止（实心地球）
   */
  public static UnifiedIconRenderer.GlyphType serverGlyph(boolean running) {
    return running ? UnifiedIconRenderer.GlyphType.SERVER_ON
        : UnifiedIconRenderer.GlyphType.SERVER_OFF;
  }
}
