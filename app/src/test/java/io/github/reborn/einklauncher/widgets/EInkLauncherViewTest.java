package io.github.reborn.einklauncher.widgets;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class EInkLauncherViewTest {

  @Test
  public void interior_dividers_match_cell_layout_boundaries() {
    assertArrayEquals(new int[] {215, 430, 645, 860},
        EInkLauncherView.interiorDividers(1076, 5));
  }

  @Test
  public void single_column_has_no_interior_divider() {
    assertArrayEquals(new int[0], EInkLauncherView.interiorDividers(1076, 1));
  }

  @Test
  public void two_columns_split_at_integer_half() {
    assertArrayEquals(new int[] {538}, EInkLauncherView.interiorDividers(1077, 2));
  }
}
