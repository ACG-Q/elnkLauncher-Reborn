package io.github.reborn.einklauncher;

import java.util.Locale;

/**
 * 统一图标单字覆写校验，口径与 {@link IconText#of} 一致：
 * 表意文字限 1 字；字母限 1-2 位并转大写（Locale.US）；其他首字符限 1 字符。
 * 非法输入返回 null。
 */
public final class IconCharRules {

  private IconCharRules() {
  }

  public static String normalize(String input) {
    if (input == null) {
      return null;
    }
    String text = input.trim();
    if (text.isEmpty()) {
      return null;
    }
    char first = text.charAt(0);
    if (Character.isIdeographic(first)) {
      return text.length() == 1 ? text : null;
    }
    if (Character.isLetter(first)) {
      if (text.length() > 2) {
        return null;
      }
      for (int i = 0; i < text.length(); i++) {
        char c = text.charAt(i);
        if (!Character.isLetter(c) || Character.isIdeographic(c)) {
          return null;
        }
      }
      return text.toUpperCase(Locale.US);
    }
    return text.length() == 1 ? text : null;
  }
}
