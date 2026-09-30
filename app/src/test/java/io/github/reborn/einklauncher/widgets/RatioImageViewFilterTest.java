package io.github.reborn.einklauncher.widgets;

import android.graphics.ColorMatrixColorFilter;

import org.junit.Test;

import io.github.reborn.einklauncher.Utils;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class RatioImageViewFilterTest {

  @Test
  public void filterFor_notPressedWithNullBase_returnsNull() {
    assertNull(RatioImageView.filterFor(false, null));
  }

  @Test
  public void filterFor_pressedWithNullBase_returnsInvert() {
    assertNotNull(RatioImageView.filterFor(true, null));
  }

  @Test
  public void filterFor_notPressedWithBase_returnsSameBaseReference() {
    ColorMatrixColorFilter base = Utils.invertColorFilter();
    assertSame(base, RatioImageView.filterFor(false, base));
  }

  @Test
  public void filterFor_pressedWithNonNullBase_returnsNull() {
    ColorMatrixColorFilter base = Utils.invertColorFilter();
    assertNull(RatioImageView.filterFor(true, base));
  }
}
