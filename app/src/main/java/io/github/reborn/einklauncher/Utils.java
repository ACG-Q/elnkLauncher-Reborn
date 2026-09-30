package io.github.reborn.einklauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.core.graphics.drawable.DrawableCompat;

import java.util.Calendar;

/**
 * 通用工具类。
 */
public class Utils {

  private static final String[] CN_AM_PM = {
      "凌晨", "黎明", "早晨", "上午", "中午", "下午", "晚上", "深夜"
  };

  private Utils() {
    // 工具类不可实例化
  }

  /**
   * 给 Drawable 着色。
   */
  public static Drawable tintDrawable(Drawable drawable, ColorStateList colors) {
    final Drawable wrappedDrawable = DrawableCompat.wrap(drawable);
    DrawableCompat.setTintList(wrappedDrawable, colors);
    return wrappedDrawable;
  }

  /**
   * dp 转 px。
   */
  public static int dp2Px(Context context, float dp) {
    final float scale = context.getResources().getDisplayMetrics().density;
    return (int) (dp * scale + 0.5f);
  }

  /**
   * 获取中文时段描述（凌晨/黎明/早晨/上午/中午/下午/晚上/深夜）。
   */
  public static String getAMPMCNString(int hours, int ampm) {
    if (ampm == Calendar.AM) {
      if (hours < 5) return CN_AM_PM[0];       // 凌晨
      if (hours < 7) return CN_AM_PM[1];        // 黎明
      if (hours < 9) return CN_AM_PM[2];        // 早晨
      if (hours < 12) return CN_AM_PM[3];       // 上午
      return CN_AM_PM[0];
    } else {
      if (hours == 0 || hours == 12) return CN_AM_PM[4];  // 中午
      if (hours < 6) return CN_AM_PM[5];        // 下午
      if (hours <= 9) return CN_AM_PM[6];       // 晚上
      return CN_AM_PM[7];                        // 深夜
    }
  }

  /**
   * 兼容 Android 13+ 的广播注册。
   * API 33 起需要指定 RECEIVER_EXPORTED / RECEIVER_NOT_EXPORTED。
   */
  public static void registerReceiverCompat(Context context, BroadcastReceiver receiver,
                                             IntentFilter filter) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
    } else {
      context.registerReceiver(receiver, filter);
    }
  }
}
