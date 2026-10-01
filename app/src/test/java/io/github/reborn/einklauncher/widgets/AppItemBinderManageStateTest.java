package io.github.reborn.einklauncher.widgets;

import android.view.View;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppItemBinderManageStateTest {

  @Test
  public void menuContainerVisible_inManageModeWithinData_isVisible() {
    assertTrue(AppItemBinder.menuContainerVisible(true, 0, 25));
    assertTrue(AppItemBinder.menuContainerVisible(true, 24, 25));
  }

  @Test
  public void menuContainerVisible_beyondDataSize_isHidden_evenInManageMode() {
    assertFalse(AppItemBinder.menuContainerVisible(true, 25, 25));
    assertFalse(AppItemBinder.menuContainerVisible(true, 5, 5));
  }

  @Test
  public void menuContainerVisible_outsideManageMode_isHidden() {
    assertFalse(AppItemBinder.menuContainerVisible(false, 0, 25));
    assertFalse(AppItemBinder.menuContainerVisible(false, 0, 0));
  }

  @Test
  public void deleteButtonVisibility_deletable_showsButton() {
    assertEquals(View.VISIBLE, AppItemBinder.deleteButtonVisibility(true));
  }

  @Test
  public void deleteButtonVisibility_notDeletable_keepsPlaceholderForUniformSize() {
    assertEquals(View.INVISIBLE, AppItemBinder.deleteButtonVisibility(false));
  }
}
