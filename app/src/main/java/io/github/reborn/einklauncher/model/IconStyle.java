package io.github.reborn.einklauncher.model;

/**
 * 统一文字图标全局样式：边框厚度、圆角、字号比例（均以 48px 基准换算）。
 * 构造接受任意值，{@link #clamped()} 收敛到合法区间；{@link #defaults()} 与旧硬编码一致。
 */
public final class IconStyle {

  public static final float MIN_STROKE = 1f;
  public static final float MAX_STROKE = 6f;
  public static final float DEFAULT_STROKE = 2.5f;

  public static final float MIN_RADIUS = 0f;
  public static final float MAX_RADIUS = 20f;
  public static final float DEFAULT_RADIUS = 10f;

  public static final float MIN_TEXT_SCALE = 0.30f;
  public static final float MAX_TEXT_SCALE = 0.55f;
  public static final float DEFAULT_TEXT_SCALE = 0.42f;

  private final float stroke;
  private final float radius;
  private final float textScale;

  public IconStyle(float stroke, float radius, float textScale) {
    this.stroke = stroke;
    this.radius = radius;
    this.textScale = textScale;
  }

  public static IconStyle defaults() {
    return new IconStyle(DEFAULT_STROKE, DEFAULT_RADIUS, DEFAULT_TEXT_SCALE);
  }

  public IconStyle clamped() {
    return new IconStyle(
        clamp(stroke, MIN_STROKE, MAX_STROKE),
        clamp(radius, MIN_RADIUS, MAX_RADIUS),
        clamp(textScale, MIN_TEXT_SCALE, MAX_TEXT_SCALE));
  }

  private static float clamp(float value, float min, float max) {
    return Math.max(min, Math.min(max, value));
  }

  public float getStroke() {
    return stroke;
  }

  public float getRadius() {
    return radius;
  }

  public float getTextScale() {
    return textScale;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof IconStyle)) {
      return false;
    }
    IconStyle other = (IconStyle) o;
    return Float.compare(stroke, other.stroke) == 0
        && Float.compare(radius, other.radius) == 0
        && Float.compare(textScale, other.textScale) == 0;
  }

  @Override
  public int hashCode() {
    int result = Float.floatToIntBits(stroke);
    result = 31 * result + Float.floatToIntBits(radius);
    result = 31 * result + Float.floatToIntBits(textScale);
    return result;
  }
}
