package io.github.reborn.einklauncher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * 更新提示托盘：底部滑入的半屏卡片，替代模态弹窗；
 * 点击背板任意位置关闭。正文为清洗后的 release 说明
 * （含可点击的完整日志链接），中性按钮与下载按钮回调见 {@link Listener}。
 */
public final class UpdateTray {

  public interface Listener {
    /** 用户点击「下载安装」。 */
    void onDownload();

    /** 用户点击中性按钮（稍后 / 忽略 / 关闭）。 */
    void onNeutral();
  }

  private UpdateTray() {
  }

  /**
   * 在 activity 内容层展示半屏托盘：透明背板铺满全屏，点击背板关闭；
   * 托盘贴底、高度为屏幕一半，正文超长时在内部滚动。
   */
  public static void show(final Activity activity, final UpdateChecker.UpdateInfo info,
                          int neutralText, final Listener listener) {
    final FrameLayout overlay = new FrameLayout(activity);
    final View tray = LayoutInflater.from(activity)
        .inflate(R.layout.view_update_tray, overlay, false);
    final TextView title = tray.findViewById(R.id.trayTitle);
    final TextView notes = tray.findViewById(R.id.trayNotes);
    final TextView neutral = tray.findViewById(R.id.trayNeutral);
    final TextView download = tray.findViewById(R.id.trayDownload);

    title.setText(activity.getString(
        R.string.update_found, info.versionName, BuildConfig.VERSION_NAME));
    CharSequence body = UpdateChecker.buildNotesText(activity, info);
    if (body.length() == 0) {
      notes.setVisibility(View.GONE);
    } else {
      notes.setText(body);
      notes.setMovementMethod(LinkMovementMethod.getInstance());
      guardDraggedLink(activity, notes);
    }

    final int screenH = activity.getResources().getDisplayMetrics().heightPixels;

    neutral.setText(neutralText);
    neutral.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        dismiss(overlay, tray, listener, false);
      }
    });
    download.setText(R.string.update_download);
    download.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        dismiss(overlay, tray, listener, true);
      }
    });

    View backdrop = new View(activity);
    backdrop.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        dismiss(overlay, tray, null, false);
      }
    });
    overlay.addView(backdrop, new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    FrameLayout.LayoutParams trayLp = new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, screenH / 2, Gravity.BOTTOM);
    int margin = Math.round(dp(activity, 12));
    trayLp.leftMargin = margin;
    trayLp.rightMargin = margin;
    trayLp.bottomMargin = margin;
    overlay.addView(tray, trayLp);
    activity.addContentView(overlay, new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    tray.setTranslationY(screenH / 2f);
    tray.animate().translationY(0f).setDuration(250).start();
  }

  /**
   * LinkMovementMethod 在 ACTION_UP 时只要松手坐标落在链接上就触发 URLSpan，
   * 不判断是否发生过拖动；notes 无滚动容器时滑动松手会误开浏览器。
   * 记录按下点，位移超过 touch slop 后吞掉 UP 并清除选区，仅保留原生点击。
   * 标准 TextView 无法 override performClick，UP 分支已调用其上报无障碍，故抑制告警。
   */
  @SuppressLint("ClickableViewAccessibility")
  private static void guardDraggedLink(Activity activity, final TextView notes) {
    final int slop = ViewConfiguration.get(activity).getScaledTouchSlop();
    final float[] downY = new float[1];
    final boolean[] dragged = new boolean[1];
    notes.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent ev) {
        switch (ev.getActionMasked()) {
          case MotionEvent.ACTION_DOWN:
            downY[0] = ev.getY();
            dragged[0] = false;
            return false;
          case MotionEvent.ACTION_MOVE:
            if (Math.abs(ev.getY() - downY[0]) > slop) {
              dragged[0] = true;
            }
            return false;
          case MotionEvent.ACTION_UP:
            if (dragged[0]) {
              clearSelection(notes);
              return true;
            }
            // 无 OnClickListener，仅向无障碍服务上报点击；链接仍由 LinkMovementMethod 打开
            v.performClick();
            return false;
          default:
            return false;
        }
      }
    });
  }

  private static void clearSelection(TextView notes) {
    if (notes.getText() instanceof Spannable) {
      Selection.removeSelection((Spannable) notes.getText());
    }
  }

  /** 滑出托盘并移除 overlay；listener 为 null 时静默关闭（点击背板）。 */
  private static void dismiss(final FrameLayout overlay, final View tray,
                              final Listener listener, final boolean download) {
    tray.animate().cancel();
    tray.animate()
        .translationY(tray.getHeight() > 0 ? tray.getHeight() : dp(tray.getContext(), 96))
        .setDuration(200)
        .setListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            removeFromParent(overlay);
            if (listener == null) {
              return;
            }
            if (download) {
              listener.onDownload();
            } else {
              listener.onNeutral();
            }
          }
        })
        .start();
  }

  private static void removeFromParent(View tray) {
    if (tray.getParent() instanceof ViewGroup) {
      ((ViewGroup) tray.getParent()).removeView(tray);
    }
  }

  private static float dp(android.content.Context context, float value) {
    return value * context.getResources().getDisplayMetrics().density;
  }
}
