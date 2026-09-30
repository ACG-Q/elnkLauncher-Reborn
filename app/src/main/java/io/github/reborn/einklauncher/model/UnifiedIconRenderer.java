package io.github.reborn.einklauncher.model;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * 统一图标渲染器：透明底圆角描边 + 内容（应用名缩写字或矢量图形）。
 * 视觉参数按 48px 基准换算（参考图风格）：圆角 10/48、描边 2.5/48、字号 42%。
 * 位图不填充背景色（四角透明）；dark 只决定前景色（夜白昼黑）。
 */
public final class UnifiedIconRenderer {

  private static final float BASE = 48f;

  /** 矢量图形在图标内的内容区边长占比（相对图标边长） */
  static final float GLYPH_SCALE = 0.5f;

  /** 虚拟入口矢量图形类型 */
  public enum GlyphType {
    LOCK, WIFI_ON, WIFI_OFF, SERVER_ON, SERVER_OFF
  }

  private UnifiedIconRenderer() {
  }

  /** 前景色：夜（dark=true）为白，昼为黑（与 text_primary 语义一致） */
  static int resolveFg(boolean dark) {
    return dark ? Color.WHITE : Color.BLACK;
  }

  /** 描边宽度（像素）：按 48px 基准换算，下限 1px */
  static float strokeWidthPx(int sizePx, IconStyle style) {
    return Math.max(1f, sizePx * style.getStroke() / BASE);
  }

  /** 矢量图形内容区边长（像素） */
  static float contentBoxPx(int sizePx) {
    return sizePx * GLYPH_SCALE;
  }

  public static Drawable create(String text, int sizePx, boolean dark) {
    return create(text, sizePx, dark, IconStyle.defaults());
  }

  public static Drawable create(String text, int sizePx, boolean dark, IconStyle style) {
    IconStyle s = (style == null ? IconStyle.defaults() : style).clamped();
    int size = Math.max(sizePx, 1);
    Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);

    int fg = resolveFg(dark);
    drawBorder(canvas, size, s, fg);

    Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    textPaint.setColor(fg);
    textPaint.setTextAlign(Paint.Align.CENTER);
    textPaint.setTextSize(size * s.getTextScale());
    textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
    Paint.FontMetrics fm = textPaint.getFontMetrics();
    float baseline = size / 2f - (fm.ascent + fm.descent) / 2f;
    canvas.drawText(text, size / 2f, baseline, textPaint);

    return new BitmapDrawable(android.content.res.Resources.getSystem(), bitmap);
  }

  /** 生成虚拟入口矢量图标：透明底 + 圆角描边 + 图形内容 */
  public static Drawable createGlyph(GlyphType type, int sizePx, boolean dark, IconStyle style) {
    IconStyle s = (style == null ? IconStyle.defaults() : style).clamped();
    int size = Math.max(sizePx, 1);
    Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);

    int fg = resolveFg(dark);
    drawBorder(canvas, size, s, fg);

    float box = contentBoxPx(size);
    float origin = (size - box) / 2f;
    drawGlyph(canvas, type, origin, origin, box, strokeWidthPx(size, s), fg);

    return new BitmapDrawable(android.content.res.Resources.getSystem(), bitmap);
  }

  private static void drawBorder(Canvas canvas, int size, IconStyle s, int fg) {
    Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    stroke.setStyle(Paint.Style.STROKE);
    stroke.setColor(fg);
    float strokeWidth = strokeWidthPx(size, s);
    stroke.setStrokeWidth(strokeWidth);
    float inset = strokeWidth / 2f + 1f;
    canvas.drawRoundRect(new RectF(inset, inset, size - inset, size - inset),
        size * s.getRadius() / BASE, size * s.getRadius() / BASE, stroke);
  }

  // =========================================================================
  // 矢量图形（内容区局部坐标，单位 0~1，左上为原点）
  // =========================================================================

  private static void drawGlyph(Canvas canvas, GlyphType type, float x, float y,
                                float w, float strokeWidth, int fg) {
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setColor(fg);
    paint.setStrokeWidth(strokeWidth);
    paint.setStrokeCap(Paint.Cap.ROUND);
    paint.setStrokeJoin(Paint.Join.ROUND);
    switch (type) {
      case LOCK:
        drawLockGlyph(canvas, x, y, w, paint);
        break;
      case WIFI_ON:
        drawWifiOnGlyph(canvas, x, y, w, paint);
        break;
      case WIFI_OFF:
        drawWifiOffGlyph(canvas, x, y, w, paint);
        break;
      case SERVER_ON:
        drawServerOnGlyph(canvas, x, y, w, paint);
        break;
      case SERVER_OFF:
        drawServerOffGlyph(canvas, x, y, w, paint, fg);
        break;
      default:
        break;
    }
  }

  /** 锁：填充圆角矩形身体 + 圆弧锁梁（描边） */
  private static void drawLockGlyph(Canvas canvas, float x, float y, float w, Paint paint) {
    paint.setStyle(Paint.Style.FILL);
    canvas.drawRoundRect(
        new RectF(x + 0.30f * w, y + 0.50f * w, x + 0.70f * w, y + 0.82f * w),
        0.06f * w, 0.06f * w, paint);
    paint.setStyle(Paint.Style.STROKE);
    canvas.drawArc(new RectF(x + 0.36f * w, y + 0.36f * w,
        x + 0.64f * w, y + 0.64f * w), 180, 180, false, paint);
  }

  /** WiFi 开启（伞形）：大弧盖 + 小弧层 + 短柄 + 底部小圆环 */
  private static void drawWifiOnGlyph(Canvas canvas, float x, float y, float w, Paint paint) {
    paint.setStyle(Paint.Style.FILL);
    Path dome = new Path();
    RectF domeRect = new RectF(x + 0.14f * w, y + 0.30f * w, x + 0.86f * w, y + 0.86f * w);
    dome.moveTo(domeRect.left, domeRect.centerY());
    dome.arcTo(domeRect, 180, 180);
    dome.close();
    canvas.drawPath(dome, paint);

    Path tier = new Path();
    RectF tierRect = new RectF(x + 0.30f * w, y + 0.54f * w, x + 0.70f * w, y + 0.90f * w);
    tier.moveTo(tierRect.left, tierRect.centerY());
    tier.arcTo(tierRect, 180, 180);
    tier.close();
    canvas.drawPath(tier, paint);

    paint.setStyle(Paint.Style.STROKE);
    canvas.drawLine(x + 0.50f * w, y + 0.72f * w, x + 0.50f * w, y + 0.82f * w, paint);
    canvas.drawCircle(x + 0.50f * w, y + 0.875f * w, 0.045f * w, paint);
  }

  /** WiFi 关闭：X 两条对角线 */
  private static void drawWifiOffGlyph(Canvas canvas, float x, float y, float w, Paint paint) {
    paint.setStyle(Paint.Style.STROKE);
    canvas.drawLine(x + 0.26f * w, y + 0.26f * w, x + 0.74f * w, y + 0.74f * w, paint);
    canvas.drawLine(x + 0.74f * w, y + 0.26f * w, x + 0.26f * w, y + 0.74f * w, paint);
  }

  /** 服务器运行（线框地球）：圆 + 竖椭圆 + 水平中线，全描边 */
  private static void drawServerOnGlyph(Canvas canvas, float x, float y, float w, Paint paint) {
    paint.setStyle(Paint.Style.STROKE);
    canvas.drawCircle(x + 0.50f * w, y + 0.50f * w, 0.30f * w, paint);
    canvas.drawOval(new RectF(x + 0.37f * w, y + 0.20f * w,
        x + 0.63f * w, y + 0.80f * w), paint);
    canvas.drawLine(x + 0.20f * w, y + 0.50f * w, x + 0.80f * w, y + 0.50f * w, paint);
  }

  /** 服务器停止（实心地球）：填充圆 + CLEAR 模式擦出透明经纬线 */
  private static void drawServerOffGlyph(Canvas canvas, float x, float y, float w,
                                         Paint paint, int fg) {
    Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    fill.setColor(fg);
    fill.setStyle(Paint.Style.FILL);
    canvas.drawCircle(x + 0.50f * w, y + 0.50f * w, 0.30f * w, fill);

    paint.setStyle(Paint.Style.STROKE);
    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    canvas.drawOval(new RectF(x + 0.38f * w, y + 0.24f * w,
        x + 0.62f * w, y + 0.76f * w), paint);
    canvas.drawLine(x + 0.24f * w, y + 0.50f * w, x + 0.76f * w, y + 0.50f * w, paint);
    paint.setXfermode(null);
  }
}
