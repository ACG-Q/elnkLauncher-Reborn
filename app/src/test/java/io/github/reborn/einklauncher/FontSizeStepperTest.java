package io.github.reborn.einklauncher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FontSizeStepperTest {

  @Test
  public void step_up_increases_one() {
    assertEquals(15f, FontSizeStepper.step(14f, +1), 0.001f);
  }

  @Test
  public void step_down_decreases_one() {
    assertEquals(13f, FontSizeStepper.step(14f, -1), 0.001f);
  }

  @Test
  public void clamps_at_max() {
    assertEquals(30f, FontSizeStepper.step(30f, +1), 0.001f);
    assertFalse(FontSizeStepper.canStep(30f, +1));
  }

  @Test
  public void clamps_at_min() {
    assertEquals(10f, FontSizeStepper.step(10f, -1), 0.001f);
    assertFalse(FontSizeStepper.canStep(10f, -1));
  }

  @Test
  public void can_step_inside_range() {
    assertTrue(FontSizeStepper.canStep(14f, +1));
    assertTrue(FontSizeStepper.canStep(14f, -1));
  }

  @Test
  public void legacy_fractional_size_steps_preserve_fraction() {
    assertEquals(15.3f, FontSizeStepper.step(14.3f, +1), 0.001f);
  }
}
