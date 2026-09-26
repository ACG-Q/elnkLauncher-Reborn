package io.github.reborn.einklauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;

/**
 * 关于页面：Hero 头部 + 描边卡片分区（更新 / 功能 / 自定义图标 / 开发者）。
 * 版本行经 {@link #versionLabel} 生成，含 versionCode；检查更新走 {@link UpdateChecker}（GitHub Releases）。
 */
public class AboutPage extends Activity {

  static String versionLabel(String appName, String versionName, int versionCode) {
    if (versionName == null || versionName.isEmpty()) {
      return appName;
    }
    return appName + " v" + versionName + " (" + versionCode + ")";
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_about);

    findViewById(R.id.toBack).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        finish();
      }
    });

    ((TextView) findViewById(R.id.heroVersion)).setText(
        versionLabel(getString(R.string.app_name),
            BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));

    final TextView updateStatus = (TextView) findViewById(R.id.updateStatus);
    final Button checkButton = (Button) findViewById(R.id.updateButton);
    checkButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        checkButton.setEnabled(false);
        updateStatus.setText(R.string.update_checking);
        UpdateChecker.check(new UpdateChecker.Callback() {
          @Override
          public void onUpdateAvailable(final UpdateChecker.UpdateInfo info) {
            checkButton.setEnabled(true);
            updateStatus.setText(getString(
                R.string.update_found, info.versionName, BuildConfig.VERSION_NAME));
            new AlertDialog.Builder(AboutPage.this)
                .setTitle(R.string.update_found_title)
                .setMessage(info.notes == null || info.notes.isEmpty()
                    ? getString(R.string.update_found, info.versionName, BuildConfig.VERSION_NAME)
                    : info.notes)
                .setPositiveButton(R.string.update_download, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    startDownload(info, updateStatus, checkButton);
                  }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
          }

          @Override
          public void onNoUpdate() {
            checkButton.setEnabled(true);
            updateStatus.setText(R.string.update_no_update);
          }

          @Override
          public void onError(String message) {
            checkButton.setEnabled(true);
            updateStatus.setText(getString(R.string.update_check_failed, message));
          }
        });
      }
    });
  }

  private void startDownload(final UpdateChecker.UpdateInfo info,
                             final TextView status, final Button button) {
    button.setEnabled(false);
    UpdateChecker.download(this, info, new UpdateChecker.ProgressCallback() {
      @Override
      public void onProgress(int percent) {
        status.setText(getString(R.string.update_downloading,
            percent >= 0 ? percent + "%" : "…"));
      }

      @Override
      public void onComplete(File apkFile) {
        button.setEnabled(true);
        status.setText(getString(R.string.update_downloading, "100%"));
        UpdateChecker.install(AboutPage.this, apkFile);
      }

      @Override
      public void onError(String message) {
        button.setEnabled(true);
        status.setText(getString(R.string.update_download_failed,
            "busy".equals(message) ? getString(R.string.update_busy) : message));
      }
    });
  }
}
