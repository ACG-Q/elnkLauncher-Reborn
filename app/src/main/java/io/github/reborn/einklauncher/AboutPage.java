package io.github.reborn.einklauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;

/**
 * 关于页面：由原关于弹窗换壳而成，内容（版本、更新检查、功能、自定义图标、开发者）不变。
 * 版本行经 {@link #versionLabel} 生成，含 versionCode。
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

    ScrollView scroll = findViewById(R.id.aboutScroll);
    scroll.addView(initLayout());
  }

  private View initLayout() {
    int pad = Utils.dp2Px(this, 14);

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(pad, pad, pad, pad);
    root.setBackgroundColor(0xffffffff);

    TextView title = new TextView(this);
    title.setText(R.string.app_name);
    title.setTextSize(26);
    title.setTypeface(null, android.graphics.Typeface.BOLD);
    title.setTextColor(0xff000000);
    root.addView(title);

    TextView version = new TextView(this);
    version.setText(versionLabel(getString(R.string.app_name),
        BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
    version.setTextSize(15);
    version.setTextColor(0xff666666);
    LinearLayout.LayoutParams versionLP = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    versionLP.topMargin = Utils.dp2Px(this, 4);
    versionLP.bottomMargin = Utils.dp2Px(this, 6);
    root.addView(version, versionLP);

    addDivider(root);

    final TextView updateStatus = new TextView(this);
    updateStatus.setTextSize(13);
    updateStatus.setTextColor(0xff666666);
    LinearLayout.LayoutParams statusLP = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    statusLP.topMargin = Utils.dp2Px(this, 4);
    root.addView(updateStatus, statusLP);

    final Button checkButton = new Button(this);
    checkButton.setText(R.string.update_check);
    LinearLayout.LayoutParams btnLP = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    btnLP.topMargin = Utils.dp2Px(this, 4);
    btnLP.bottomMargin = Utils.dp2Px(this, 4);
    root.addView(checkButton, btnLP);

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

    addDivider(root);

    addSectionHeader(root, R.string.about_features, 15);

    addSectionHeader(root, R.string.about_custom_icon_title, 16);
    addText(root, R.string.about_custom_icon_info, 14, 0xff333333, 8);
    addDivider(root);

    addText(root, R.string.about_custom_icon_filenames, 13, 0xff555555, 8);
    addDivider(root);

    addText(root, R.string.about_icon_path, 13, 0xff555555, 8);
    addDivider(root);

    addSectionHeader(root, R.string.about_developers, 16);

    TextView devInfo = new TextView(this);
    devInfo.setText(R.string.about_developer_info);
    devInfo.setTextSize(14);
    devInfo.setTextColor(0xff333333);
    devInfo.setLineSpacing(0, 1.4f);
    LinearLayout.LayoutParams devInfoLP = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    devInfoLP.topMargin = Utils.dp2Px(this, 4);
    devInfoLP.bottomMargin = Utils.dp2Px(this, 4);
    root.addView(devInfo, devInfoLP);

    SpannableString githubLink = new SpannableString(
        "https://github.com/ACG-Q/elnkLauncher-Reborn");
    githubLink.setSpan(new ClickableSpan() {
      @Override
      public void onClick(View widget) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
            Uri.parse("https://github.com/ACG-Q/elnkLauncher-Reborn"));
        startActivity(intent);
      }
    }, 0, githubLink.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    githubLink.setSpan(new AbsoluteSizeSpan(14, true), 0, githubLink.length(),
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    devInfo.append("\n");
    devInfo.append(githubLink);
    devInfo.setMovementMethod(LinkMovementMethod.getInstance());
    devInfo.setHighlightColor(0xFF4285F4);

    addDivider(root);

    TextView iconsCredit = new TextView(this);
    iconsCredit.setText("Icons: icons/ directory (e-ink style)");
    iconsCredit.setTextSize(12);
    iconsCredit.setTextColor(0xff999999);
    LinearLayout.LayoutParams creditLP = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    creditLP.topMargin = Utils.dp2Px(this, 4);
    creditLP.bottomMargin = Utils.dp2Px(this, 4);
    root.addView(iconsCredit, creditLP);

    return root;
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

  private void addSectionHeader(LinearLayout parent, int textRes, int textSizeSp) {
    TextView header = new TextView(this);
    header.setText(textRes);
    header.setTextSize(textSizeSp);
    header.setTypeface(null, android.graphics.Typeface.BOLD);
    header.setTextColor(0xff000000);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lp.topMargin = Utils.dp2Px(this, 6);
    lp.bottomMargin = Utils.dp2Px(this, 4);
    parent.addView(header, lp);
  }

  private void addText(LinearLayout parent, int textRes, int textSizeSp, int color,
                       int marginVerticalDp) {
    TextView tv = new TextView(this);
    tv.setText(textRes);
    tv.setTextSize(textSizeSp);
    tv.setTextColor(color);
    tv.setLineSpacing(0, 1.3f);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lp.topMargin = Utils.dp2Px(this, 2);
    lp.bottomMargin = Utils.dp2Px(this, marginVerticalDp);
    tv.setLayoutParams(lp);
    parent.addView(tv);
  }

  private void addDivider(LinearLayout parent) {
    View line = new View(this);
    line.setBackgroundColor(0xffcccccc);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, Utils.dp2Px(this, 1));
    lp.topMargin = Utils.dp2Px(this, 2);
    lp.bottomMargin = Utils.dp2Px(this, 2);
    line.setLayoutParams(lp);
    parent.addView(line);
  }
}
