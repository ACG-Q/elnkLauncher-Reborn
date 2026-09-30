package io.github.reborn.einklauncher;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UpdateNotesTest {

  @Test
  public void sanitize_null_returns_empty() {
    assertEquals("", UpdateNotes.sanitize(null));
  }

  @Test
  public void sanitize_empty_returns_empty() {
    assertEquals("", UpdateNotes.sanitize("   "));
  }

  @Test
  public void sanitize_strips_bold_and_heading_markers() {
    assertEquals("新功能一览", UpdateNotes.sanitize("**新功能一览**"));
    assertEquals("标题", UpdateNotes.sanitize("## 标题"));
  }

  @Test
  public void sanitize_removes_full_changelog_line_with_url() {
    String raw = "修复若干问题\n\n**Full Changelog**: https://github.com/a/b/compare/v1...v2";
    assertEquals("修复若干问题", UpdateNotes.sanitize(raw));
  }

  @Test
  public void sanitize_removes_changelog_line_with_chinese_label() {
    String raw = "新增托盘\n\n**完整变更日志**: https://github.com/a/b/compare/v1...v2";
    assertEquals("新增托盘", UpdateNotes.sanitize(raw));
  }

  @Test
  public void sanitize_keeps_sentence_containing_url() {
    String raw = "详情见 https://github.com/a/b/releases 了解更多信息";
    assertEquals("详情见 了解更多信息", UpdateNotes.sanitize(raw));
  }

  @Test
  public void sanitize_collapses_blank_lines() {
    assertEquals("第一段\n\n第二段", UpdateNotes.sanitize("第一段\n\n\n\n第二段"));
  }

  @Test
  public void sanitize_normalizes_crlf() {
    assertEquals("a\nb", UpdateNotes.sanitize("a\r\nb"));
  }

  @Test
  public void extract_url_returns_first_link() {
    assertEquals("https://github.com/a/b/compare/v1...v2",
        UpdateNotes.extractChangelogUrl("见 https://github.com/a/b/compare/v1...v2"));
  }

  @Test
  public void extract_url_strips_trailing_punctuation() {
    assertEquals("https://example.com/x",
        UpdateNotes.extractChangelogUrl("(https://example.com/x)."));
  }

  @Test
  public void extract_url_missing_returns_empty() {
    assertEquals("", UpdateNotes.extractChangelogUrl("没有链接"));
    assertEquals("", UpdateNotes.extractChangelogUrl(null));
  }
}
