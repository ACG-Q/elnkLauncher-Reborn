package io.github.reborn.einklauncher.model;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * 统一文字图标渲染器：白底（dark 时黑底）圆角方框 + 应用名缩写字。
 * 视觉参数按 48px 基准换算（参考图风格）：圆角 10/48、描边 2.5/48、字号 42%。
 * dark 参数供未来夜间模式使用，当前调用方固定传 false。
 */
public final class UnifiedIconRenderer {

  private static final float BASE = 48f;

  private UnifiedIconRenderer() {
  }

  public static Drawable create(String text, int sizePx, boolean dark) {
    return create(text, sizePx, dark, IconStyle.defaults());
  }

  public static Drawable create(String text, int sizePx, boolean dark, IconStyle style) {
    IconStyle s = (style == null ? IconStyle.defaults() : style).clamped();
    int size = Math.max(sizePx, 1);
    Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);

    int bg = dark ? Color.BLACK : Color.WHITE;
    int fg = dark ? Color.WHITE : Color.BLACK;
    canvas.drawColor(bg);

    Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    stroke.setStyle(Paint.Style.STROKE);
    stroke.setColor(fg);
    float strokeWidth = Math.max(1f, size * s.getStroke() / BASE);
    stroke.setStrokeWidth(strokeWidth);
    float inset = strokeWidth / 2f + 1f;
    canvas.drawRoundRect(new RectF(inset, inset, size - inset, size - inset),
        size * s.getRadius() / BASE, size * s.getRadius() / BASE, stroke);

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
}
