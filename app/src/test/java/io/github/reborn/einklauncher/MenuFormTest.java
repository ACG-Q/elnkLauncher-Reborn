package io.github.reborn.einklauncher;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MenuFormTest {

  @Test
  public void null_defaults_to_grouped() {
    assertEquals(MenuForm.GROUPED, MenuForm.normalize(null));
  }

  @Test
  public void unknown_value_defaults_to_grouped() {
    assertEquals(MenuForm.GROUPED, MenuForm.normalize("weird"));
  }

  @Test
  public void grouped_kept() {
    assertEquals(MenuForm.GROUPED, MenuForm.normalize("grouped"));
  }

  @Test
  public void minimal_kept() {
    assertEquals(MenuForm.MINIMAL, MenuForm.normalize("minimal"));
  }
}
