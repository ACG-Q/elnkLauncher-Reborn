package io.github.reborn.einklauncher.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * 墨水屏风格拨杆：2dp 黑描边胶囊轨道，开=黑底白点在右，关=白底黑点在左。
 * 默认尺寸 40x22dp，onMeasure 可被布局覆盖。
 */
public class ToggleView extends View {

  private boolean checked;
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF track = new RectF();
  private final float density;

  public ToggleView(Context context) {
    this(context, null);
  }

  public ToggleView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ToggleView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    density = context.getResources().getDisplayMetrics().density;
  }

  public void setChecked(boolean checked) {
    if (this.checked == checked) {
      return;
    }
    this.checked = checked;
    invalidate();
  }

  public boolean isChecked() {
    return checked;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int w = (int) (40 * density + 0.5f);
    int h = (int) (22 * density + 0.5f);
    setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec));
  }

  @Override
  protected void onDraw(Canvas canvas) {
    float stroke = 2 * density;
    float radius = getHeight() / 2f;
    track.set(stroke / 2f, stroke / 2f, getWidth() - stroke / 2f, getHeight() - stroke / 2f);

    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(stroke);
    paint.setColor(0xff000000);
    canvas.drawRoundRect(track, radius, radius, paint);

    if (checked) {
      paint.setStyle(Paint.Style.FILL);
      canvas.drawRoundRect(track, radius, radius, paint);
    }

    float knobR = (getHeight() - 6 * density) / 2f;
    float cx = checked
        ? getWidth() - knobR - 3 * density
        : knobR + 3 * density;
    paint.setColor(checked ? 0xffffffff : 0xff000000);
    canvas.drawCircle(cx, getHeight() / 2f, knobR, paint);

    if (checked) {
      paint.setColor(0xff000000);
      paint.setStyle(Paint.Style.STROKE);
      canvas.drawCircle(cx, getHeight() / 2f, knobR, paint);
    }
  }
}
