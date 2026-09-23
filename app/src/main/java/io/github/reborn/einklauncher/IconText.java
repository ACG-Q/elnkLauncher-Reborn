package io.github.reborn.einklauncher;

/**
 * 统一文字图标的取字规则。
 * 中文（CJK 表意文字）取首字；字母取前两个转大写；数字/其他取首字符；空值返回占位符 "?"。
 */
public final class IconText {

  private IconText() {
  }

  public static String of(CharSequence label) {
    if (label == null || label.length() == 0) {
      return "?";
    }
    char first = label.charAt(0);
    if (Character.isIdeographic(first)) {
      return String.valueOf(first);
    }
    if (Character.isLetter(first)) {
      StringBuilder sb = new StringBuilder();
      sb.append(Character.toUpperCase(first));
      if (label.length() > 1 && Character.isLetter(label.charAt(1))) {
        sb.append(Character.toUpperCase(label.charAt(1)));
      }
      return sb.toString();
    }
    return String.valueOf(first);
  }
}
