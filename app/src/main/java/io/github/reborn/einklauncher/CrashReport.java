package io.github.reborn.einklauncher;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * 崩溃报告的纯文本处理：摘要提取、GitHub Issue URL 拼装（含超长截断）、剪贴板文本降级。
 * 无 Android 依赖，供 JUnit 直测。
 */
public final class CrashReport {

  static final String UNKNOWN_ERROR = "Unknown error";
  static final int URL_MAX = 6000;
  static final String ISSUE_NEW_URL =
      "https://github.com/ACG-Q/elnkLauncher-Reborn/issues/new";

  private static final int TITLE_MAX = 120;
  private static final String TITLE_PREFIX = "[Crash] ";
  private static final String TRUNCATION_MARK = "\n... [truncated] ...\n";
  private static final String STEPS_GUIDE =
      "Steps to reproduce:\n\n1. \n2. \n3. \n\n---\n\n";

  private CrashReport() {
  }

  /**
   * 提取异常首行：第一个堆栈帧（"\tat " 行）之前的最后一个非空、非 key=value 设备信息行。
   * 找不到堆栈帧或无有效行时返回 {@link #UNKNOWN_ERROR}。
   */
  public static String extractSummary(String log) {
    if (log == null) {
      return UNKNOWN_ERROR;
    }
    String[] lines = log.split("\r?\n", -1);
    String candidate = null;
    boolean foundFrame = false;
    for (String line : lines) {
      if (line.startsWith("\tat ")) {
        foundFrame = true;
        break;
      }
      String trimmed = line.trim();
      if (trimmed.isEmpty() || isDeviceInfoLine(trimmed)) {
        continue;
      }
      candidate = trimmed;
    }
    return foundFrame && candidate != null ? candidate : UNKNOWN_ERROR;
  }

  /** Issue 标题："[Crash] " + 摘要截断至 120 字符。 */
  public static String buildIssueTitle(String summary) {
    String safe = summary == null || summary.isEmpty() ? UNKNOWN_ERROR : summary;
    String truncated = safe.length() > TITLE_MAX ? safe.substring(0, TITLE_MAX) : safe;
    return TITLE_PREFIX + truncated;
  }

  /** Issue 正文：复现步骤引导 + 完整报告。 */
  public static String buildIssueBody(String report) {
    return STEPS_GUIDE + (report == null ? "" : report);
  }

  /**
   * 拼装预填 Issue URL。编码后总长超过 {@link #URL_MAX} 时，
   * 反复从中部收缩报告（保留头尾，插入截断标记）直至达标。
   */
  public static String buildIssueUrl(String report) {
    if (report == null) {
      report = "";
    }
    String title = buildIssueTitle(extractSummary(report));
    String body = buildIssueBody(report);
    String url = compose(title, body);
    int budget = body.length();
    while (url.length() > URL_MAX && budget > 200) {
      budget = Math.max(200,
          budget - Math.max(400, (url.length() - URL_MAX) * 2));
      body = buildIssueBody(shrink(report, budget));
      url = compose(title, body);
    }
    return url;
  }

  /** 剪贴板文本：报告可用则原文返回，否则返回含读取错误的降级文本。 */
  public static String buildClipboardText(String report, String readError) {
    if (report != null && !report.trim().isEmpty()) {
      return report;
    }
    StringBuilder sb = new StringBuilder();
    sb.append("Error summary: ").append(UNKNOWN_ERROR).append('\n');
    if (readError != null) {
      sb.append("Failed to read crash file: ").append(readError);
    }
    return sb.toString();
  }

  /** 中部截断：保留头部与尾部各 budget 的一半，中间插截断标记。 */
  static String shrink(String report, int budget) {
    if (report.length() <= budget) {
      return report;
    }
    int keep = Math.max(50, (budget - TRUNCATION_MARK.length()) / 2);
    return report.substring(0, keep)
        + TRUNCATION_MARK
        + report.substring(report.length() - keep);
  }

  private static String compose(String title, String body) {
    return ISSUE_NEW_URL + "?title=" + encode(title) + "&body=" + encode(body);
  }

  private static String encode(String value) {
    try {
      return URLEncoder.encode(value, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("UTF-8 not supported", e);
    }
  }

  /** 设备信息行形如 versionName=0.2.0、FINGERPRINT=... ：字母开头后直接跟 =。 */
  private static boolean isDeviceInfoLine(String line) {
    return line.matches("^[A-Za-z][A-Za-z0-9_]*=.*");
  }
}
