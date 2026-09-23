package io.github.reborn.einklauncher;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileReader;

public class CrashDetailPage extends Activity {

  private static final String TAG = "CrashDetailPage";

  private TextView crashSummary;
  private TextView crashLog;
  private String report;
  private String readError;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_crash_detail);
    crashSummary = findViewById(R.id.crashSummary);
    crashLog = findViewById(R.id.crashLog);
    loadReport();
    fillContent();
    findViewById(R.id.crashCopy).setOnClickListener(v -> copyReport());
    findViewById(R.id.crashIssue).setOnClickListener(v -> openIssue());
    findViewById(R.id.crashRestart).setOnClickListener(v -> restartLauncher());
  }

  private void loadReport() {
    String fileName = getIntent().getStringExtra("crashFile");
    if (TextUtils.isEmpty(fileName)) {
      readError = "crashFile extra missing";
      return;
    }
    File crashFile = new File(getExternalFilesDir("crash"), fileName);
    try {
      char[] data = new char[(int) crashFile.length()];
      try (FileReader reader = new FileReader(crashFile)) {
        int read = reader.read(data);
        report = read > 0 ? new String(data, 0, read) : "";
      }
      if (report.isEmpty()) {
        report = null;
        readError = "crash file is empty";
      }
    } catch (Throwable e) {
      Log.w(TAG, "failed to read crash file: " + crashFile, e);
      report = null;
      readError = String.valueOf(e.getMessage());
    }
  }

  private void fillContent() {
    crashSummary.setText(CrashReport.extractSummary(report));
    if (report != null) {
      crashLog.setText(report);
    } else {
      crashLog.setText(getString(R.string.crash_log_read_failed,
          readError != null ? readError : "unknown"));
    }
  }

  private void copyReport() {
    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    if (cm == null) {
      Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show();
      return;
    }
    cm.setPrimaryClip(ClipData.newPlainText(
        getString(R.string.crash_report_clip_label),
        CrashReport.buildClipboardText(report, readError)));
    Toast.makeText(this, R.string.crash_copied, Toast.LENGTH_SHORT).show();
  }

  private void openIssue() {
    String url = CrashReport.buildIssueUrl(report);
    try {
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    } catch (Exception e) {
      Log.w(TAG, "failed to open issue url: " + url, e);
      Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show();
    }
  }

  private void restartLauncher() {
    Intent intent = new Intent(CrashDetailPage.this, Launcher.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
  }
}
