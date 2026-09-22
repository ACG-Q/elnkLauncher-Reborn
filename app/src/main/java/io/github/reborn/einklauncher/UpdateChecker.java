package io.github.reborn.einklauncher;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 本地自动更新：从 GitHub Releases 检查最新版本、下载 APK 并调起系统安装器。
 * 所有回调均回主线程。
 */
public final class UpdateChecker {

  private static final String TAG = "UpdateChecker";
  private static final String API_URL =
      "https://api.github.com/repos/ACG-Q/elnkLauncher-Reborn/releases/latest";
  private static final String USER_AGENT = "E-Ink-Launcher-Reborn-Updater";
  private static final int CONNECT_TIMEOUT_MS = 15000;
  private static final int READ_TIMEOUT_MS = 30000;
  private static final int NOTES_MAX_LEN = 500;

  private static final Handler MAIN = new Handler(Looper.getMainLooper());
  private static volatile boolean downloading;

  private UpdateChecker() {
  }

  /** 检查结果。 */
  public interface Callback {
    void onUpdateAvailable(UpdateInfo info);

    void onNoUpdate();

    void onError(String message);
  }

  /** 下载进度与结果。 */
  public interface ProgressCallback {
    /** total 未知时 percent 为 -1。 */
    void onProgress(int percent);

    void onComplete(File apkFile);

    void onError(String message);
  }

  /** 最新版本信息。 */
  public static final class UpdateInfo {
    public final String versionName;
    public final String notes;
    public final String apkUrl;

    UpdateInfo(String versionName, String notes, String apkUrl) {
      this.versionName = versionName;
      this.notes = notes;
      this.apkUrl = apkUrl;
    }
  }

  // =========================================================================
  // 检查
  // =========================================================================

  /** 后台检查是否有新版本，回调在主线程。 */
  public static void check(final Callback callback) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          final UpdateInfo info = fetchLatest();
          if (info == null) {
            postError(callback, "release 中未找到 APK 资源");
          } else if (!isNewerVersion(info.versionName, BuildConfig.VERSION_NAME)) {
            MAIN.post(new Runnable() {
              @Override
              public void run() {
                callback.onNoUpdate();
              }
            });
          } else {
            MAIN.post(new Runnable() {
              @Override
              public void run() {
                callback.onUpdateAvailable(info);
              }
            });
          }
        } catch (final Exception e) {
          Log.w(TAG, "check failed", e);
          postError(callback, errorMessage(e));
        }
      }
    }, "update-check").start();
  }

  private static UpdateInfo fetchLatest() throws Exception {
    String body = httpGet(API_URL);
    JSONObject json = new JSONObject(body);
    String tag = json.optString("tag_name", "");
    String version = stripPrefixV(tag);
    if (version.isEmpty()) {
      throw new IOException("无效的 tag_name: " + tag);
    }
    JSONArray assets = json.optJSONArray("assets");
    String apkUrl = null;
    if (assets != null) {
      for (int i = 0; i < assets.length(); i++) {
        JSONObject asset = assets.getJSONObject(i);
        String name = asset.optString("name", "");
        if (name.toLowerCase().endsWith(".apk")) {
          apkUrl = asset.optString("browser_download_url", null);
          if (apkUrl != null && !apkUrl.isEmpty()) {
            break;
          }
        }
      }
    }
    if (apkUrl == null || apkUrl.isEmpty()) {
      return null;
    }
    String notes = truncate(json.optString("body", ""), NOTES_MAX_LEN);
    return new UpdateInfo(version, notes, apkUrl);
  }

  // =========================================================================
  // 下载与安装
  // =========================================================================

  /** 下载 APK 到应用缓存目录，完成后回调在主线程。同一时间仅允许一个下载任务。 */
  public static void download(final Context context, final UpdateInfo info,
                              final ProgressCallback callback) {
    if (downloading) {
      MAIN.post(new Runnable() {
        @Override
        public void run() {
          callback.onError("busy");
        }
      });
      return;
    }
    downloading = true;
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          File apk = downloadApk(context, info, callback);
          MAIN.post(new Runnable() {
            @Override
            public void run() {
              downloading = false;
              callback.onComplete(apk);
            }
          });
        } catch (final Exception e) {
          Log.w(TAG, "download failed", e);
          MAIN.post(new Runnable() {
            @Override
            public void run() {
              downloading = false;
              callback.onError(errorMessage(e));
            }
          });
        }
      }
    }, "update-download").start();
  }

  private static File downloadApk(Context context, UpdateInfo info,
                                  final ProgressCallback callback) throws IOException {
    File dir = new File(context.getCacheDir(), "update");
    if (!dir.exists() && !dir.mkdirs()) {
      throw new IOException("无法创建下载目录");
    }
    String safeVersion = info.versionName.replaceAll("[^a-zA-Z0-9._-]", "_");
    File apk = new File(dir, "elnk-launcher-" + safeVersion + ".apk");
    File partial = new File(apk.getPath() + ".part");
    if (partial.exists() && !partial.delete()) {
      throw new IOException("无法清理旧的下载临时文件");
    }

    HttpURLConnection conn = null;
    try {
      conn = openConnection(info.apkUrl);
      int code = conn.getResponseCode();
      if (code / 100 != 2) {
        throw new IOException("HTTP " + code);
      }
      int total = conn.getContentLength();
      InputStream in = conn.getInputStream();
      FileOutputStream out = new FileOutputStream(partial);
      try {
        byte[] buffer = new byte[64 * 1024];
        long downloaded = 0;
        int lastPercent = -1;
        int n;
        while ((n = in.read(buffer)) != -1) {
          out.write(buffer, 0, n);
          downloaded += n;
          if (total > 0) {
            int percent = (int) (downloaded * 100 / total);
            if (percent != lastPercent) {
              lastPercent = percent;
              final int p = percent;
              MAIN.post(new Runnable() {
                @Override
                public void run() {
                  callback.onProgress(p);
                }
              });
            }
          }
        }
        out.flush();
      } finally {
        out.close();
        in.close();
      }
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }

    if (apk.exists() && !apk.delete()) {
      partial.delete();
      throw new IOException("无法覆盖旧的安装包");
    }
    if (!partial.renameTo(apk)) {
      partial.delete();
      throw new IOException("保存安装包失败");
    }
    return apk;
  }

  /** 调起系统安装器，由用户确认安装。 */
  public static void install(Context context, File apk) {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      Uri uri = FileProvider.getUriForFile(
          context, context.getPackageName() + ".fileProvider", apk);
      intent.setDataAndType(uri, "application/vnd.android.package-archive");
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    } else {
      intent.setDataAndType(Uri.fromFile(apk), "application/vnd.android.package-archive");
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(intent);
  }

  // =========================================================================
  // 工具
  // =========================================================================

  /** 逐段数值比较版本号，remote 更新返回 true。 */
  static boolean isNewerVersion(String remote, String local) {
    String[] r = remote.split("\\.");
    String[] l = local.split("\\.");
    int max = Math.max(r.length, l.length);
    for (int i = 0; i < max; i++) {
      int rv = i < r.length ? parseSegment(r[i]) : 0;
      int lv = i < l.length ? parseSegment(l[i]) : 0;
      if (rv != lv) {
        return rv > lv;
      }
    }
    return false;
  }

  private static int parseSegment(String segment) {
    try {
      return Integer.parseInt(segment.trim());
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private static String stripPrefixV(String tag) {
    if (tag.startsWith("v") || tag.startsWith("V")) {
      return tag.substring(1);
    }
    return tag;
  }

  private static String truncate(String s, int max) {
    if (s == null) {
      return "";
    }
    s = s.trim();
    return s.length() <= max ? s : s.substring(0, max) + "…";
  }

  private static String errorMessage(Exception e) {
    String msg = e.getMessage();
    return (msg == null || msg.isEmpty()) ? e.getClass().getSimpleName() : msg;
  }

  private static HttpURLConnection openConnection(String url) throws IOException {
    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
    conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
    conn.setReadTimeout(READ_TIMEOUT_MS);
    conn.setInstanceFollowRedirects(true);
    conn.setRequestProperty("User-Agent", USER_AGENT);
    conn.setRequestProperty("Accept", "*/*");
    return conn;
  }

  private static String httpGet(String url) throws IOException {
    HttpURLConnection conn = null;
    try {
      conn = openConnection(url);
      conn.setRequestProperty("Accept", "application/vnd.github+json");
      int code = conn.getResponseCode();
      if (code / 100 != 2) {
        throw new IOException("HTTP " + code);
      }
      InputStream in = conn.getInputStream();
      try {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] buffer = new byte[8 * 1024];
        int n;
        while ((n = in.read(buffer)) != -1) {
          buf.write(buffer, 0, n);
        }
        return buf.toString("UTF-8");
      } finally {
        in.close();
      }
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
  }

  private static void postError(final Callback callback, final String message) {
    MAIN.post(new Runnable() {
      @Override
      public void run() {
        callback.onError(message);
      }
    });
  }
}
