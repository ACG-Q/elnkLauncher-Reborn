package io.github.reborn.einklauncher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import io.github.reborn.einklauncher.model.VirtualEntry;
import org.junit.Test;

/**
 * {@link VirtualEntry} 的包名映射与展示 id 行测试。
 */
public class VirtualEntryTest {

  @Test
  public void testFromPackageLockMapsToSingleId() {
    VirtualEntry entry = VirtualEntry.fromPackage("E-ink_Launcher.Lock");
    assertNotNull(entry);
    assertEquals(VirtualEntry.Type.LOCK, entry.getType());
    List<String> ids = entry.getDisplayIds();
    assertEquals(1, ids.size());
    assertEquals("E-ink_Launcher.Lock", ids.get(0));
  }

  @Test
  public void testFromPackageWifiMapsToOnBeforeOffIds() {
    VirtualEntry entry = VirtualEntry.fromPackage("E-ink_Launcher.WiFi");
    assertNotNull(entry);
    assertEquals(VirtualEntry.Type.WIFI, entry.getType());
    List<String> ids = entry.getDisplayIds();
    assertEquals(2, ids.size());
    assertEquals("E-ink_Launcher.WiFi.On", ids.get(0));
    assertEquals("E-ink_Launcher.WiFi.Off", ids.get(1));
  }

  @Test
  public void testFromPackageServerMapsToOnBeforeOffIds() {
    VirtualEntry entry = VirtualEntry.fromPackage("E-ink_Launcher.HttpServer");
    assertNotNull(entry);
    assertEquals(VirtualEntry.Type.SERVER, entry.getType());
    List<String> ids = entry.getDisplayIds();
    assertEquals(2, ids.size());
    assertEquals("E-ink_Launcher.HttpServer.On", ids.get(0));
    assertEquals("E-ink_Launcher.HttpServer.Off", ids.get(1));
  }

  @Test
  public void testFromPackageNormalAppReturnsNull() {
    assertNull(VirtualEntry.fromPackage("com.example.someapp"));
    assertNull(VirtualEntry.fromPackage(""));
  }
}
