package io.github.reborn.einklauncher;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 字号步进浮层：A− / 当前值 / A+，每次 ±1sp（FontSizeStepper 钳制 10–30），
 * 每步回调 onSizeChanged 供调用方写 Config 并刷新桌面。
 * 全局唯一实现，三处入口（B 菜单、C 面板、设置页）共用。
 */
public final class FontSizeOverlay {

  /** 字号变化回调，主线程。 */
  public interface OnSizeChangedListener {
    void onSizeChanged(float newSize);
  }

  private FontSizeOverlay() {
  }

  /** 浮动格式化：整数省略小数（14 → "14sp"），否则保留（14.3 → "14.3sp"）。 */
  public static String formatSp(float size) {
    if (size == (int) size) {
      return ((int) size) + "sp";
    }
    return size + "sp";
  }

  public static void show(Context context, float currentSize,
                          final OnSizeChangedListener listener) {
    final float[] size = { currentSize };

    LinearLayout body = new LinearLayout(context);
    body.setOrientation(LinearLayout.VERTICAL);
    int pad = Utils.dp2Px(context, 16);
    body.setPadding(pad, Utils.dp2Px(context, 8), pad, pad);

    LinearLayout row = new LinearLayout(context);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);

    final Button minus = new Button(context);
    minus.setText("A−");
    minus.setMinWidth(Utils.dp2Px(context, 72));
    minus.setMinHeight(Utils.dp2Px(context, 48));

    final TextView value = new TextView(context);
    value.setText(formatSp(currentSize));
    value.setTextSize(22);
    value.setTypeface(null, Typeface.BOLD);
    value.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams valueLP = new LinearLayout.LayoutParams(
        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    value.setLayoutParams(valueLP);

    final Button plus = new Button(context);
    plus.setText("A+");
    plus.setMinWidth(Utils.dp2Px(context, 72));
    plus.setMinHeight(Utils.dp2Px(context, 48));

    row.addView(minus);
    row.addView(value);
    row.addView(plus);
    body.addView(row);

    TextView range = new TextView(context);
    range.setText(R.string.font_range);
    range.setTextSize(12);
    range.setTextColor(0xff999999);
    range.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams rangeLP = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    rangeLP.topMargin = Utils.dp2Px(context, 6);
    range.setLayoutParams(rangeLP);
    body.addView(range);

    final AlertDialog dialog = new AlertDialog.Builder(context)
        .setTitle(R.string.font_title)
        .setView(body)
        .setPositiveButton(R.string.font_done, null)
        .create();

    View.OnClickListener stepListener = new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        int direction = (v == plus) ? +1 : -1;
        size[0] = FontSizeStepper.step(size[0], direction);
        value.setText(formatSp(size[0]));
        minus.setEnabled(FontSizeStepper.canStep(size[0], -1));
        plus.setEnabled(FontSizeStepper.canStep(size[0], +1));
        listener.onSizeChanged(size[0]);
      }
    };
    minus.setOnClickListener(stepListener);
    plus.setOnClickListener(stepListener);
    minus.setEnabled(FontSizeStepper.canStep(currentSize, -1));
    plus.setEnabled(FontSizeStepper.canStep(currentSize, +1));
    dialog.show();
  }
}
