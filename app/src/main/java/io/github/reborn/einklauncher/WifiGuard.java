package io.github.reborn.einklauncher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import io.github.reborn.einklauncher.model.WifiControl;

/**
 * 打开菜单/设置页期间监听网络：WiFi 断开时关闭 WiFi 名运行时显示。
 * register 幂等；unregister 吞复注册异常仅记日志。
 */
public final class WifiGuard {

  private static final String TAG = "WifiGuard";
  private static BroadcastReceiver receiver;

  private WifiGuard() {
  }

  public static void register(Activity activity) {
    if (receiver != null) {
      return;
    }
    receiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm == null ? null : cm.getActiveNetworkInfo();
        if (info == null || info.getType() != ConnectivityManager.TYPE_WIFI) {
          WifiControl.setShowWifiName(false);
        }
      }
    };
    IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    Utils.registerReceiverCompat(activity, receiver, filter);
  }

  public static void unregister(Activity activity) {
    if (receiver == null) {
      return;
    }
    try {
      activity.unregisterReceiver(receiver);
    } catch (IllegalArgumentException e) {
      Log.w(TAG, "receiver already unregistered", e);
    }
    receiver = null;
  }
}
