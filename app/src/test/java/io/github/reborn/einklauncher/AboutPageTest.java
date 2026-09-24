package io.github.reborn.einklauncher;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AboutPageTest {

  @Test
  public void versionLabel_includes_name_and_version_and_build() {
    assertEquals("E-Ink Launcher v0.2.0 (31)",
        AboutPage.versionLabel("E-Ink Launcher", "0.2.0", 31));
  }

  @Test
  public void versionLabel_blank_version_omits_suffix() {
    assertEquals("E-Ink Launcher", AboutPage.versionLabel("E-Ink Launcher", "", 0));
  }
}
