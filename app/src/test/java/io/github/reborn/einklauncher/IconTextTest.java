package io.github.reborn.einklauncher;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IconTextTest {

  @Test
  public void chinese_label_uses_first_char() {
    assertEquals("相", IconText.of("相机"));
  }

  @Test
  public void chinese_mixed_with_latin_prefix_uses_two_uppercase_letters() {
    assertEquals("PL", IconText.of("Play 商店"));
  }

  @Test
  public void english_label_uses_first_two_uppercase_letters() {
    assertEquals("CH", IconText.of("Chrome"));
  }

  @Test
  public void digit_prefix_uses_first_char() {
    assertEquals("9", IconText.of("91助手"));
  }

  @Test
  public void single_letter_label() {
    assertEquals("A", IconText.of("A"));
  }

  @Test
  public void empty_label_is_placeholder() {
    assertEquals("?", IconText.of(""));
  }

  @Test
  public void null_label_is_placeholder() {
    assertEquals("?", IconText.of(null));
  }

  @Test
  public void symbol_prefix_uses_first_char() {
    assertEquals("#", IconText.of("#频道"));
  }
}
