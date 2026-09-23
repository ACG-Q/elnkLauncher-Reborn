package io.github.reborn.einklauncher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.net.URLDecoder;

public class CrashReportTest {

  private static final String NORMAL_LOG =
      "versionName=0.2.0\r\n"
          + "versionCode=31\r\n"
          + "osVersion=14\r\n"
          + "\r\n"
          + "java.lang.NullPointerException: Attempt to invoke virtual method 'void foo()'\r\n"
          + "\tat io.github.reborn.einklauncher.Launcher.onCreate(Launcher.java:100)\r\n"
          + "\tat android.app.Activity.performCreate(Activity.java:1)\r\n"
          + "Caused by: java.lang.IllegalStateException: boom\r\n"
          + "\tat io.github.reborn.einklauncher.CrashReport.extractSummary(CrashReport.java:1)\r\n";

  @Test
  public void extractSummary_normal_log_returns_exception_line() {
    assertEquals(
        "java.lang.NullPointerException: Attempt to invoke virtual method 'void foo()'",
        CrashReport.extractSummary(NORMAL_LOG));
  }

  @Test
  public void extractSummary_device_info_only_returns_unknown() {
    assertEquals(CrashReport.UNKNOWN_ERROR,
        CrashReport.extractSummary("versionName=0.2.0\r\nversionCode=31\r\n"));
  }

  @Test
  public void extractSummary_null_returns_unknown() {
    assertEquals(CrashReport.UNKNOWN_ERROR, CrashReport.extractSummary(null));
  }

  @Test
  public void extractSummary_blank_returns_unknown() {
    assertEquals(CrashReport.UNKNOWN_ERROR, CrashReport.extractSummary("  \r\n "));
  }

  @Test
  public void extractSummary_exception_without_frames_returns_unknown() {
    assertEquals(CrashReport.UNKNOWN_ERROR,
        CrashReport.extractSummary("versionName=1.0\r\njava.lang.RuntimeException: lost frames\r\n"));
  }

  @Test
  public void buildIssueTitle_long_summary_truncated_to_120() {
    StringBuilder longLine = new StringBuilder();
    for (int i = 0; i < 30; i++) {
      longLine.append("0123456789");
    }
    String title = CrashReport.buildIssueTitle(longLine.toString());
    assertTrue(title.startsWith("[Crash] "));
    assertEquals("[Crash] " + longLine.substring(0, 120), title);
  }

  @Test
  public void buildIssueBody_contains_steps_guide_and_report() {
    String body = CrashReport.buildIssueBody("THE-REPORT");
    assertTrue(body.startsWith("Steps to reproduce:"));
    assertTrue(body.contains("THE-REPORT"));
  }

  @Test
  public void buildIssueUrl_short_report_fits_without_truncation() {
    String url = CrashReport.buildIssueUrl(NORMAL_LOG);
    assertTrue(url.startsWith(
        "https://github.com/ACG-Q/elnkLauncher-Reborn/issues/new?title="));
    assertTrue(url.length() <= CrashReport.URL_MAX);
    assertFalse(url.contains("truncated"));
  }

  @Test
  public void buildIssueUrl_long_report_truncates_middle_keeps_head_and_tail()
      throws Exception {
    StringBuilder sb = new StringBuilder("versionName=9.9.9\r\nHEAD-MARKER\r\n");
    while (sb.length() < 9000) {
      sb.append("frame line with some stack content\r\n");
    }
    sb.append("Caused by: java.lang.IllegalStateException: TAIL-MARKER\r\n");
    String url = CrashReport.buildIssueUrl(sb.toString());
    assertTrue("url too long: " + url.length(), url.length() <= CrashReport.URL_MAX);
    String decoded = URLDecoder.decode(url, "UTF-8");
    assertTrue(decoded.contains("versionName=9.9.9"));
    assertTrue(decoded.contains("TAIL-MARKER"));
    assertTrue(decoded.contains("[truncated]"));
  }

  @Test
  public void buildClipboardText_blank_report_returns_degraded_text() {
    String text = CrashReport.buildClipboardText(null, "file not found");
    assertTrue(text.contains(CrashReport.UNKNOWN_ERROR));
    assertTrue(text.contains("file not found"));
  }

  @Test
  public void buildClipboardText_valid_report_returns_report_verbatim() {
    assertEquals(NORMAL_LOG, CrashReport.buildClipboardText(NORMAL_LOG, null));
  }
}
