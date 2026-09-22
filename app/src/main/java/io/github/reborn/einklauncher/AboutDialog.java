package io.github.reborn.einklauncher;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;

public class AboutDialog {

  private final Context context;

  private AboutDialog(Context context) {
    this.context = context;
  }

  public static AboutDialog getInstance(Context context) {
    return new AboutDialog(context);
  }

  private View initLayout() {
    int pad = Utils.dp2Px(context, 14);

    ScrollView scrollView = new ScrollView(context);
    LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(pad, pad, pad, pad);
    root.setBackgroundColor(0xffffffff);

    // App name
    TextView title = new TextView(context);
    title.setText(R.string.app_name);
    title.setTextSize(26);
    title.setTypeface(null, android.graphics.Typeface.BOLD);
    title.setTextColor(0xff000000);
    root.addView(title);

    // Version
    TextView version = new TextView(context);
    version.setText("v" + BuildConfig.VERSION_NAME);
    version.setTextSize(15);
    version.setTextColor(0xff666666);
    LinearLayout.LayoutParams versionLP = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    versionLP.topMargin = Utils.dp2Px(context, 4);
    versionLP.bottomMargin = Utils.dp2Px(context, 6);
    root.addView(version, versionLP);

    addDivider(root);

    // Update check
    final TextView updateStatus = new TextView(context);
    updateStatus.setTextSize(13);
    updateStatus.setTextColor(0xff666666);
    LinearLayout.LayoutParams statusLP = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    statusLP.topMargin = Utils.dp2Px(context, 4);
    root.addView(updateStatus, statusLP);

    final android.widget.Button checkButton = new android.widget.Button(context);
    checkButton.setText(R.string.update_check);
    LinearLayout.LayoutParams btnLP = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    btnLP.topMargin = Utils.dp2Px(context, 4);
    btnLP.bottomMargin = Utils.dp2Px(context, 4);
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
            updateStatus.setText(context.getString(
                R.string.update_found, info.versionName, BuildConfig.VERSION_NAME));
            new AlertDialog.Builder(context)
                .setTitle(R.string.update_found_title)
                .setMessage(info.notes == null || info.notes.isEmpty()
                    ? context.getString(R.string.update_found, info.versionName, BuildConfig.VERSION_NAME)
                    : info.notes)
                .setPositiveButton(R.string.update_download, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    startDownload(context, info, updateStatus, checkButton);
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
            updateStatus.setText(context.getString(R.string.update_check_failed, message));
          }
        });
      }
    });

    addDivider(root);

    // Features
    addSectionHeader(root, R.string.about_features, 15);

    // Custom Icons
    addSectionHeader(root, R.string.about_custom_icon_title, 16);
    TextView customIconInfo = addText(root, R.string.about_custom_icon_info, 14, 0xff333333, 8);
    addDivider(root);

    TextView iconNames = addText(root, R.string.about_custom_icon_filenames, 13, 0xff555555, 8);
    addDivider(root);

    TextView pathInfo = addText(root, R.string.about_icon_path, 13, 0xff555555, 8);
    addDivider(root);

    // Developers
    addSectionHeader(root, R.string.about_developers, 16);

    TextView devInfo = new TextView(context);
    devInfo.setText(R.string.about_developer_info);
    devInfo.setTextSize(14);
    devInfo.setTextColor(0xff333333);
    devInfo.setLineSpacing(0, 1.4f);
    LinearLayout.LayoutParams devInfoLP = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    devInfoLP.topMargin = Utils.dp2Px(context, 4);
    devInfoLP.bottomMargin = Utils.dp2Px(context, 4);
    root.addView(devInfo, devInfoLP);

    // Clickable GitHub link
    SpannableString githubLink = new SpannableString(
        "https://github.com/ACG-Q/elnkLauncher-Reborn");
    githubLink.setSpan(new ClickableSpan() {
      @Override
      public void onClick(android.view.View widget) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
            Uri.parse("https://github.com/ACG-Q/elnkLauncher-Reborn"));
        context.startActivity(intent);
      }
    }, 0, githubLink.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    githubLink.setSpan(new AbsoluteSizeSpan(14, true), 0, githubLink.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    devInfo.append("\n");
    devInfo.append(githubLink);
    devInfo.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
    devInfo.setHighlightColor(0xFF4285F4);

    addDivider(root);

    // Icons credit
    TextView iconsCredit = new TextView(context);
    iconsCredit.setText("Icons: icons/ directory (e-ink style)");
    iconsCredit.setTextSize(12);
    iconsCredit.setTextColor(0xff999999);
    LinearLayout.LayoutParams creditLP = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    creditLP.topMargin = Utils.dp2Px(context, 4);
    creditLP.bottomMargin = Utils.dp2Px(context, 4);
    root.addView(iconsCredit, creditLP);

    scrollView.addView(root);
    return scrollView;
  }

  private void startDownload(final Context context, final UpdateChecker.UpdateInfo info,
                              final TextView status, final android.widget.Button button) {
    button.setEnabled(false);
    UpdateChecker.download(context, info, new UpdateChecker.ProgressCallback() {
      @Override
      public void onProgress(int percent) {
        status.setText(context.getString(R.string.update_downloading,
            percent >= 0 ? percent + "%" : "…"));
      }

      @Override
      public void onComplete(File apkFile) {
        button.setEnabled(true);
        status.setText(context.getString(R.string.update_downloading, "100%"));
        UpdateChecker.install(context, apkFile);
      }

      @Override
      public void onError(String message) {
        button.setEnabled(true);
        status.setText(context.getString(R.string.update_download_failed,
            "busy".equals(message) ? context.getString(R.string.update_busy) : message));
      }
    });
  }

  private void addSectionHeader(LinearLayout parent, int textRes, int textSizeSp) {
    TextView header = new TextView(context);
    header.setText(textRes);
    header.setTextSize(textSizeSp);
    header.setTypeface(null, android.graphics.Typeface.BOLD);
    header.setTextColor(0xff000000);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lp.topMargin = Utils.dp2Px(context, 6);
    lp.bottomMargin = Utils.dp2Px(context, 4);
    parent.addView(header, lp);
  }

  private TextView addText(int textRes, int textSizeSp, int color, int marginVerticalDp) {
    TextView tv = new TextView(context);
    tv.setText(textRes);
    tv.setTextSize(textSizeSp);
    tv.setTextColor(color);
    tv.setLineSpacing(0, 1.3f);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lp.topMargin = Utils.dp2Px(context, 2);
    lp.bottomMargin = Utils.dp2Px(context, marginVerticalDp);
    tv.setLayoutParams(lp);
    return tv;
  }

  private TextView addText(LinearLayout parent, int textRes, int textSizeSp, int color, int marginVerticalDp) {
    TextView tv = addText(textRes, textSizeSp, color, marginVerticalDp);
    parent.addView(tv);
    return tv;
  }

  private void addDivider(LinearLayout parent) {
    View line = new View(context);
    line.setBackgroundColor(0xffcccccc);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, Utils.dp2Px(context, 1));
    lp.topMargin = Utils.dp2Px(context, 2);
    lp.bottomMargin = Utils.dp2Px(context, 2);
    line.setLayoutParams(lp);
    parent.addView(line);
  }

  public void show() {
    new AlertDialog.Builder(context)
        .setView(initLayout())
        .setPositiveButton(R.string.dialog_close, null)
        .show();
  }
}
