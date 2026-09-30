package io.github.reborn.einklauncher.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.ColorMatrixColorFilter;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import io.github.reborn.einklauncher.R;
import io.github.reborn.einklauncher.Utils;

/**
 * 按照宽高比例自适应尺寸的 ImageView。
 * 以 {@link ReferenceType} 指定的边为基准，另一边按比例计算。
 */
public class RatioImageView extends ImageView {

  private ReferenceType reference = ReferenceType.WIDTH;
  private double ratioWidth = 1;
  private double ratioHeight = 1;

  /** 常态滤镜：非按下时应用的滤镜；null 表示无滤镜。 */
  private ColorMatrixColorFilter baseFilter;

  public enum ReferenceType {
    WIDTH,
    HEIGHT
  }

  public RatioImageView(Context context) {
    super(context);
  }

  public RatioImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initAttrs(context, attrs, 0);
  }

  public RatioImageView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initAttrs(context, attrs, defStyleAttr);
  }

  private void initAttrs(Context context, AttributeSet attrs, int defStyleAttr) {
    TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RatioLayout, defStyleAttr, 0);
    reference = ta.getInt(R.styleable.RatioLayout_reference, 0) == 0
        ? ReferenceType.WIDTH : ReferenceType.HEIGHT;
    ratioHeight = ta.getFloat(R.styleable.RatioLayout_ratioHeight, 1);
    ratioWidth = ta.getFloat(R.styleable.RatioLayout_ratioWidth, 1);
    ta.recycle();
  }

  /**
   * 声明常态滤镜并按当前按下状态应用。
   * 按下期间在常态基础上叠加一次取反（项目常态滤镜只有 null 与 INVERT 两种，
   * INVERT 自逆 → 叠加取反即二者互换）。
   */
  public void setBaseFilter(ColorMatrixColorFilter filter) {
    this.baseFilter = filter;
    applyFilter();
  }

  @Override
  protected void drawableStateChanged() {
    super.drawableStateChanged();
    applyFilter();
  }

  private void applyFilter() {
    setColorFilter(filterFor(isPressed(), baseFilter));
  }

  /** 按下滤镜决策纯函数：pressed = 对常态叠加一次取反。包私有供单测。 */
  static ColorMatrixColorFilter filterFor(boolean pressed, ColorMatrixColorFilter base) {
    if (!pressed) {
      return base;
    }
    return base == null ? Utils.invertColorFilter() : null;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    boolean widthBased = (reference == ReferenceType.WIDTH);

    setMeasuredDimension(
        View.getDefaultSize(0, widthBased ? widthMeasureSpec
            : (int) (heightMeasureSpec / ratioHeight * ratioWidth)),
        View.getDefaultSize(0, !widthBased ? heightMeasureSpec
            : (int) (widthMeasureSpec / ratioWidth * ratioHeight))
    );

    int baseSize = widthBased ? getMeasuredWidth() : getMeasuredHeight();
    int otherSpec = widthBased
        ? View.MeasureSpec.makeMeasureSpec((int) (baseSize / ratioWidth * ratioHeight), View.MeasureSpec.EXACTLY)
        : View.MeasureSpec.makeMeasureSpec((int) (baseSize / ratioHeight * ratioWidth), View.MeasureSpec.EXACTLY);

    super.onMeasure(
        widthBased ? widthMeasureSpec : otherSpec,
        widthBased ? otherSpec : heightMeasureSpec
    );
  }
}
