package io.github.reborn.einklauncher;

/**
 * 字号步进纯逻辑：每次 ±1sp，范围 [10, 30] 钳制。
 * 保留旧 SeekBar 可能存下的小数位（如 14.3 → +1 = 15.3）。
 */
public final class FontSizeStepper {

  public static final float MIN = 10f;
  public static final float MAX = 30f;
  public static final float STEP = 1f;

  private FontSizeStepper() {
  }

  public static float step(float current, int direction) {
    float next = current + Math.signum(direction) * STEP;
    if (next < MIN) {
      return MIN;
    }
    if (next > MAX) {
      return MAX;
    }
    return next;
  }

  public static boolean canStep(float current, int direction) {
    float next = current + Math.signum(direction) * STEP;
    return next >= MIN && next <= MAX;
  }
}
