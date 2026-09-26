package io.github.reborn.einklauncher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  @Test
  public void testResolveServerCustomIcon_runningPrefersOnStateFile() {
    Map<String, File> icons = new HashMap<>();
    File onState = new File("E-ink_Launcher.HttpServer.On.png");
    File both = new File("E-ink_Launcher.HttpServer.png");
    icons.put(VirtualEntry.ID_SERVER_ON, onState);
    icons.put("E-ink_Launcher.HttpServer", both);
    assertSame(onState, VirtualEntry.resolveServerCustomIcon(icons, true));
  }

  @Test
  public void testResolveServerCustomIcon_stoppedPrefersOffStateFile() {
    Map<String, File> icons = new HashMap<>();
    File offState = new File("E-ink_Launcher.HttpServer.Off.png");
    File both = new File("E-ink_Launcher.HttpServer.png");
    icons.put(VirtualEntry.ID_SERVER_OFF, offState);
    icons.put("E-ink_Launcher.HttpServer", both);
    assertSame(offState, VirtualEntry.resolveServerCustomIcon(icons, false));
  }

  @Test
  public void testResolveServerCustomIcon_fallsBackToSingleFile() {
    Map<String, File> icons = new HashMap<>();
    File both = new File("E-ink_Launcher.HttpServer.png");
    icons.put("E-ink_Launcher.HttpServer", both);
    assertSame(both, VirtualEntry.resolveServerCustomIcon(icons, true));
    assertSame(both, VirtualEntry.resolveServerCustomIcon(icons, false));
  }

  @Test
  public void testResolveServerCustomIcon_missingReturnsNull() {
    assertNull(VirtualEntry.resolveServerCustomIcon(new HashMap<String, File>(), true));
    assertNull(VirtualEntry.resolveServerCustomIcon(null, false));
  }
}
