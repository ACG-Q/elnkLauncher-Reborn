package io.github.reborn.einklauncher.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;

import io.github.reborn.einklauncher.R;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;

/**
 * E-Ink 桌面网格布局 ViewGroup。
 * <p>
 * 纯布局容器，职责仅限于：
 * <ul>
 *   <li>网格布局（行列排列、尺寸测量）</li>
 *   <li>手势检测（滑动翻页）</li>
 *   <li>向 {@link LauncherAdapter} 请求 ViewHolder 的创建与绑定</li>
 * </ul>
 * 所有数据管理和交互逻辑由 {@link LauncherAdapter} 处理。
 */
public class EInkLauncherView extends ViewGroup {

  // =========================================================================
  // 翻页监听
  // =========================================================================

  /** 翻页手势回调 */
  public interface OnPageChangeListener {
    void onPageNext();
    void onPagePrev();
  }

  // =========================================================================
  // 字段
  // =========================================================================

  // 网格参数
  private int rowNum = 5;
  private int colNum = 5;
  private boolean hideDivider = false;

  // 自绘边框与分隔线
  private final Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  // 外部依赖
  private LauncherAdapter adapter;
  private OnPageChangeListener pageChangeListener;

  // 滑动检测
  private float touchDownX;
  private float touchDownY;
  private float swipeThreshold;

  // =========================================================================
  // 构造器
  // =========================================================================

  public EInkLauncherView(Context context) {
    this(context, null);
  }

  public EInkLauncherView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public EInkLauncherView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    setWillNotDraw(false);
    float density = context.getResources().getDisplayMetrics().density;
    framePaint.setColor(ContextCompat.getColor(context, R.color.text_primary));
    framePaint.setStyle(Paint.Style.STROKE);
    framePaint.setStrokeWidth(2 * density);
    dividerPaint.setColor(ContextCompat.getColor(context, R.color.dash_line));
    dividerPaint.setStyle(Paint.Style.STROKE);
    dividerPaint.setStrokeWidth(Math.max(1f, density));
    dividerPaint.setPathEffect(
        new DashPathEffect(new float[] {4 * density, 4 * density}, 0));
  }

  // =========================================================================
  // Adapter & Listener
  // =========================================================================

  /** 设置数据适配器，触发网格重建 */
  public void setAdapter(LauncherAdapter adapter) {
    if (this.adapter != null) {
      this.adapter.detachView();
    }
    this.adapter = adapter;
    if (adapter != null) {
      adapter.attachView(this);
    }
    resetGrid();
  }

  /** 设置翻页手势监听 */
  public void setOnPageChangeListener(OnPageChangeListener listener) {
    this.pageChangeListener = listener;
  }

  // =========================================================================
  // 网格配置
  // =========================================================================

  /**
   * 一次性配置所有网格参数，仅触发一次 {@code resetGrid()}。
   */
  public void configure(int colNum, int rowNum, boolean hideDivider) {
    this.colNum = colNum;
    this.rowNum = rowNum;
    this.hideDivider = hideDivider;
    resetGrid();
  }

  public void setGridSize(int colNum, int rowNum) {
    this.colNum = colNum;
    this.rowNum = rowNum;
    resetGrid();
  }

  public void setColNum(int colNum) {
    this.colNum = colNum;
    resetGrid();
  }

  public void setRowNum(int rowNum) {
    this.rowNum = rowNum;
    resetGrid();
  }

  public void setHideDivider(boolean hideDivider) {
    this.hideDivider = hideDivider;
    resetGrid();
  }

  // =========================================================================
  // onLayout / onMeasure
  // =========================================================================

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    int w = getAdjustedWidth();
    int h = getAdjustedHeight();
    if (w <= 0 || h <= 0) return;

    swipeThreshold = Math.min(w, h) / 6f;
    int cellW = w / colNum;
    int cellH = h / rowNum;

    for (int row = 0; row < rowNum; row++) {
      for (int col = 0; col < colNum; col++) {
        int index = row * colNum + col;
        if (index >= getChildCount()) return;
        getChildAt(index).layout(
            col * cellW, row * cellH,
            (col + 1) * cellW, (row + 1) * cellH);
      }
    }

  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    int w = getAdjustedWidth();
    int h = getAdjustedHeight();
    if (w <= 0 || h <= 0) return;

    int cellWSpec = makeMeasureSpec(w / colNum, EXACTLY);
    int cellHSpec = makeMeasureSpec(h / rowNum, EXACTLY);
    for (int i = 0; i < getChildCount(); i++) {
      getChildAt(i).measure(cellWSpec, cellHSpec);
    }
  }

  private int getAdjustedWidth() {
    return getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
  }

  private int getAdjustedHeight() {
    return getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    int w = getWidth();
    int h = getHeight();
    if (w <= 0 || h <= 0) {
      return;
    }
    canvas.drawColor(ContextCompat.getColor(getContext(), R.color.window_bg));
    float inset = framePaint.getStrokeWidth() / 2f;
    canvas.drawRect(inset, inset, w - inset, h - inset, framePaint);
    if (hideDivider) {
      return;
    }
    float start = framePaint.getStrokeWidth();
    for (int x : interiorDividers(w, colNum)) {
      canvas.drawLine(x, start, x, h - start, dividerPaint);
    }
    for (int y : interiorDividers(h, rowNum)) {
      canvas.drawLine(start, y, w - start, y, dividerPaint);
    }
  }

  /**
   * 内部格线位置（不含外圈，外圈由 2dp 黑框承担）。
   * 与 {@link #onLayout} 的 cellW = total / count 整数除法严格对齐。
   */
  static int[] interiorDividers(int total, int count) {
    if (count <= 1) {
      return new int[0];
    }
    int[] result = new int[count - 1];
    int step = total / count;
    for (int i = 1; i < count; i++) {
      result[i - 1] = i * step;
    }
    return result;
  }

  // =========================================================================
  // 网格构建
  // =========================================================================

  private void resetGrid() {
    if (adapter == null) return;
    int targetCount = rowNum * colNum;

    if (adapter.getHolderCount() == targetCount) {
      // 数量不变，仅刷新背景
      for (int i = 0; i < targetCount; i++) {
        getChildAt(i).setBackgroundResource(R.drawable.app_item_cell);
      }
      rebind();
      return;
    }

    // 数量变化，完整重建
    removeAllViews();
    adapter.clearHolders();

    for (int i = 0; i < targetCount; i++) {
      LauncherAdapter.ItemViewHolder holder = adapter.createViewHolder(this);
      holder.itemView.setBackgroundResource(R.drawable.app_item_cell);
      addView(holder.itemView);
    }
    rebind();
  }

  /** 请求 adapter 重新绑定所有数据，完成后强制重绘以触发 E-ink 刷新 */
  void rebind() {
    if (adapter != null) {
      adapter.bindAll();
      invalidate();
    }
  }

  // =========================================================================
  // 手势检测
  // =========================================================================

  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        touchDownX = event.getX();
        touchDownY = event.getY();
        break;
      case MotionEvent.ACTION_UP:
        int dir = detectSwipe(event.getX(), event.getY());
        if (dir != 0 && pageChangeListener != null) {
          if (dir > 0) pageChangeListener.onPagePrev();
          else pageChangeListener.onPageNext();
          return true;
        }
        break;
    }
    return super.dispatchTouchEvent(event);
  }

  /**
   * 检测滑动方向。
   *
   * @return 1 = 上一页, -1 = 下一页, 0 = 无有效滑动
   */
  private int detectSwipe(float upX, float upY) {
    if (swipeThreshold <= 0) return 0;
    float dx = upX - touchDownX;
    float dy = upY - touchDownY;
    if (dx > swipeThreshold || dy > swipeThreshold) return 1;
    if (dx < -swipeThreshold || dy < -swipeThreshold) return -1;
    return 0;
  }
}
