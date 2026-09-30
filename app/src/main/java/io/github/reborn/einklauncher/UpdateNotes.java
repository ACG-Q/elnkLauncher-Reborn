package io.github.reborn.einklauncher;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Release 说明文本处理：Markdown 清洗与变更日志链接提取。
 * 纯字符串逻辑，不依赖 Android 运行时，便于单元测试。
 */
final class UpdateNotes {

  private static final Pattern URL_RE = Pattern.compile("https?://\\S+");

  /** 「标签 + 链接」型行的标签词（大小写不敏感），此类行整体剔除，链接另行呈现。 */
  private static final Pattern CHANGELOG_LABEL_RE =
      Pattern.compile("full\\s*changelog|changelog|变更日志|更新日志",
          Pattern.CASE_INSENSITIVE);

  private UpdateNotes() {
  }

  /**
   * 清洗 release 说明：去掉 Markdown 标记与标题井号，剔除「标签 + 链接」型行
   * （如 Full Changelog 行），压缩连续空行。
   */
  static String sanitize(String raw) {
    if (raw == null || raw.isEmpty()) {
      return "";
    }
    String[] lines = raw.replace("\r\n", "\n").split("\n", -1);
    StringBuilder out = new StringBuilder();
    boolean pendingBlank = false;
    boolean lastBlank = true;
    for (String line : lines) {
      boolean hasUrl = line.contains("http://") || line.contains("https://");
      String t = line.replace("**", "").replace("`", "");
      if (t.startsWith("#")) {
        t = t.replaceFirst("^#+\\s*", "");
      }
      t = URL_RE.matcher(t).replaceAll(" ");
      t = t.replaceAll("\\s{2,}", " ")
          .replaceFirst("[:：]\\s*$", "")
          .trim();
      if (t.isEmpty()) {
        if (!lastBlank) {
          pendingBlank = true;
          lastBlank = true;
        }
        continue;
      }
      if (hasUrl && CHANGELOG_LABEL_RE.matcher(t).find()) {
        continue;
      }
      if (out.length() > 0) {
        out.append(pendingBlank ? "\n\n" : "\n");
      }
      out.append(t);
      pendingBlank = false;
      lastBlank = false;
    }
    return out.toString().trim();
  }

  /** 提取 release 说明中的变更日志对比链接，无则返回空串。 */
  static String extractChangelogUrl(String raw) {
    if (raw == null) {
      return "";
    }
    Matcher m = URL_RE.matcher(raw);
    if (!m.find()) {
      return "";
    }
    String url = m.group();
    while (!url.isEmpty() && ".,);]".indexOf(url.charAt(url.length() - 1)) >= 0) {
      url = url.substring(0, url.length() - 1);
    }
    return url;
  }
}
