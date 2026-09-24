package io.github.reborn.einklauncher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class IconCharRulesTest {

  @Test
  public void normalize_single_ideographic_char_returns_it() {
    assertEquals("游", IconCharRules.normalize("游"));
  }

  @Test
  public void normalize_two_ideographic_chars_returns_null() {
    assertNull(IconCharRules.normalize("游戏"));
  }

  @Test
  public void normalize_two_letters_uppercases() {
    assertEquals("PL", IconCharRules.normalize("pl"));
    assertEquals("CH", IconCharRules.normalize("Ch"));
  }

  @Test
  public void normalize_three_letters_returns_null() {
    assertNull(IconCharRules.normalize("abc"));
  }

  @Test
  public void normalize_single_digit_returns_it() {
    assertEquals("5", IconCharRules.normalize("5"));
  }

  @Test
  public void normalize_blank_returns_null() {
    assertNull(IconCharRules.normalize(""));
    assertNull(IconCharRules.normalize("   "));
    assertNull(IconCharRules.normalize(null));
  }

  @Test
  public void normalize_trims_whitespace() {
    assertEquals("微", IconCharRules.normalize("  微  "));
  }

  @Test
  public void normalize_mixed_letter_and_ideographic_returns_null() {
    assertNull(IconCharRules.normalize("a游"));
  }
}
