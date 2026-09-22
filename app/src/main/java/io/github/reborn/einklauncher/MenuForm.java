package io.github.reborn.einklauncher;

/**
 * 菜单形态常量与归一化。
 * "grouped" = B 分组菜单，"minimal" = C 极简面板；非法值一律回退 grouped。
 */
public final class MenuForm {

  public static final String GROUPED = "grouped";
  public static final String MINIMAL = "minimal";

  private MenuForm() {
  }

  public static String normalize(String value) {
    return MINIMAL.equals(value) ? MINIMAL : GROUPED;
  }
}
