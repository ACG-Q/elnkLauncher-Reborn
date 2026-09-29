package io.github.reborn.einklauncher.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

/**
 * 圆形电量指示 View。
 * 外圈弧线表示当前电量百分比，中心显示数字。
 */
public class BatteryView extends View {

  private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

  private int maxProgress = 100;
  private int progress = 0;
  private BatteryIconState.State iconState = BatteryIconState.State.NONE;
  private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  public BatteryView(Context context) {
    super(context);
    init();
  }

  public BatteryView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public BatteryView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    circlePaint.setStyle(Paint.Style.STROKE);
    textPaint.setColor(0xff000000);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    int size = Math.min(getWidth(), getHeight());
    float strokeWidth = size / 10f;
    circlePaint.setStrokeWidth(strokeWidth);

    // 画灰色背景圆环
    circlePaint.setColor(0xffcccccc);
    canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, (size - strokeWidth) / 2f, circlePaint);

    // 画黑色电量弧线
    RectF arcRect = new RectF(
        (getWidth() - size + strokeWidth) / 2f,
        (getHeight() - size + strokeWidth) / 2f,
        (getWidth() - size - strokeWidth) / 2f + size,
        (getHeight() - size - strokeWidth) / 2f + size
    );
    circlePaint.setColor(0xff000000);
    float sweepAngle = progress * 1f / maxProgress * 360;
    canvas.drawArc(arcRect, -90, sweepAngle, false, circlePaint);

    // 画右下角状态图标（白底衬，保证弧上可见）
    if (iconState != BatteryIconState.State.NONE) {
      drawStateIcon(canvas, size);
    }

    // 画中心文字
    textPaint.setTextSize(size / 2.8f);
    drawText(canvas);
  }

  private void drawText(Canvas canvas) {
    String showText = String.format("%02d", Math.round(progress * 1f / maxProgress * 100));
    Rect rect = new Rect();
    textPaint.getTextBounds(showText, 0, showText.length(), rect);
    textPaint.setFakeBoldText(true);
    canvas.translate(getWidth() / 2f, getHeight() / 2f);
    canvas.drawText(showText,
        -(rect.right - rect.left) / 1.9f,
        (rect.bottom - rect.top) / 2f, textPaint);
  }

  private void drawStateIcon(Canvas canvas, float size) {
    float cx = getWidth() / 2f;
    float cy = getHeight() / 2f;
    float ringR = (size - size / 10f) / 2f;
    float ix = cx + ringR * (float) Math.cos(Math.PI / 4);
    float iy = cy + ringR * (float) Math.sin(Math.PI / 4);
    float maxR = Math.min(Math.min(ix, getWidth() - ix),
        Math.min(iy, getHeight() - iy)) - 1f;
    float backingR = Math.min(size * 0.26f, maxR);
    if (backingR <= 0) {
      return;
    }
    float r = backingR * 0.62f;

    iconPaint.setStyle(Paint.Style.FILL);
    iconPaint.setColor(0xffffffff);
    canvas.drawCircle(ix, iy, backingR, iconPaint);

    iconPaint.setColor(0xff000000);
    canvas.save();
    canvas.translate(ix, iy);
    switch (iconState) {
      case CHARGING:
        drawLightning(canvas, r);
        break;
      case FULL:
        drawCheck(canvas, r);
        break;
      case OVERHEAT:
        drawThermometer(canvas, r);
        break;
      case LOW:
        drawLowBattery(canvas, r);
        break;
      case UNKNOWN:
        drawQuestion(canvas, r);
        break;
      default:
        break;
    }
    canvas.restore();
  }

  private void drawLightning(Canvas canvas, float r) {
    Path bolt = new Path();
    bolt.moveTo(0.10f * r, -0.85f * r);
    bolt.lineTo(-0.55f * r, 0.10f * r);
    bolt.lineTo(-0.10f * r, 0.10f * r);
    bolt.lineTo(-0.30f * r, 0.85f * r);
    bolt.lineTo(0.55f * r, -0.15f * r);
    bolt.lineTo(0.10f * r, -0.15f * r);
    bolt.close();
    canvas.drawPath(bolt, iconPaint);
  }

  private void drawCheck(Canvas canvas, float r) {
    iconPaint.setStyle(Paint.Style.STROKE);
    iconPaint.setStrokeWidth(r * 0.35f);
    iconPaint.setStrokeCap(Paint.Cap.ROUND);
    iconPaint.setStrokeJoin(Paint.Join.ROUND);
    Path check = new Path();
    check.moveTo(-0.55f * r, 0.05f * r);
    check.lineTo(-0.15f * r, 0.45f * r);
    check.lineTo(0.60f * r, -0.45f * r);
    canvas.drawPath(check, iconPaint);
  }

  private void drawThermometer(Canvas canvas, float r) {
    iconPaint.setStyle(Paint.Style.STROKE);
    iconPaint.setStrokeWidth(r * 0.32f);
    iconPaint.setStrokeCap(Paint.Cap.ROUND);
    canvas.drawLine(0f, -0.65f * r, 0f, 0.35f * r, iconPaint);
    iconPaint.setStyle(Paint.Style.FILL);
    canvas.drawCircle(0f, 0.52f * r, 0.34f * r, iconPaint);
  }

  private void drawLowBattery(Canvas canvas, float r) {
    iconPaint.setStyle(Paint.Style.STROKE);
    iconPaint.setStrokeWidth(r * 0.16f);
    iconPaint.setStrokeCap(Paint.Cap.BUTT);
    RectF body = new RectF(-0.72f * r, -0.34f * r, 0.38f * r, 0.34f * r);
    canvas.drawRoundRect(body, r * 0.12f, r * 0.12f, iconPaint);
    iconPaint.setStyle(Paint.Style.FILL);
    canvas.drawRect(new RectF(0.44f * r, -0.12f * r, 0.62f * r, 0.12f * r),
        iconPaint);
    float bx = -0.17f * r;
    canvas.drawRect(new RectF(bx - 0.07f * r, -0.20f * r,
        bx + 0.07f * r, 0.06f * r), iconPaint);
    canvas.drawCircle(bx, 0.20f * r, 0.075f * r, iconPaint);
  }

  private void drawQuestion(Canvas canvas, float r) {
    iconPaint.setStyle(Paint.Style.FILL);
    iconPaint.setTextAlign(Paint.Align.CENTER);
    iconPaint.setTextSize(r * 1.5f);
    Paint.FontMetrics fm = iconPaint.getFontMetrics();
    canvas.drawText("?", 0f, -(fm.ascent + fm.descent) / 2f, iconPaint);
    iconPaint.setTextAlign(Paint.Align.LEFT);
  }

  public void setMaxProgress(int maxProgress) {
    this.maxProgress = maxProgress;
    invalidate();
  }

  public void setProgress(int progress) {
    this.progress = progress;
    invalidate();
  }

  /** 设置右下角状态图标；{@link BatteryIconState.State#NONE} 表示不显示。 */
  public void setIconState(BatteryIconState.State state) {
    BatteryIconState.State sanitized =
        state != null ? state : BatteryIconState.State.NONE;
    if (this.iconState != sanitized) {
      this.iconState = sanitized;
      invalidate();
    }
  }
}
