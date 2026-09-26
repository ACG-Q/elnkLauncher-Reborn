package io.github.reborn.einklauncher.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.github.reborn.einklauncher.R;

/**
 * 虚拟入口的纯逻辑映射：虚拟包名 → 入口类型、弹窗标题与展示 id 行。
 * 展示 id 仅用于长按弹窗正文（标识入口的状态语义），不参与隐藏/分发等内部逻辑；
 * 内部逻辑始终使用 {@link AppDataCenter} 中稳定的虚拟包名。
 */
public final class VirtualEntry {

  /** 虚拟入口类型 */
  public enum Type {
    /** 一键锁屏 */
    LOCK,
    /** WiFi 开关 */
    WIFI,
    /** HTTP 文件服务器 */
    SERVER
  }

  /** 一键锁屏展示 id（与虚拟包名同值） */
  public static final String ID_LOCK = AppDataCenter.LOCK_PACKAGE_NAME;
  /** WiFi 入口开态展示 id */
  public static final String ID_WIFI_ON = "E-ink_Launcher.WiFi.On";
  /** WiFi 入口关态展示 id */
  public static final String ID_WIFI_OFF = "E-ink_Launcher.WiFi.Off";
  /** 服务器入口开态展示 id */
  public static final String ID_SERVER_ON = "E-ink_Launcher.HttpServer.On";
  /** 服务器入口关态展示 id */
  public static final String ID_SERVER_OFF = "E-ink_Launcher.HttpServer.Off";

  private final Type type;
  private final List<String> displayIds;

  private VirtualEntry(Type type, List<String> displayIds) {
    this.type = type;
    this.displayIds = Collections.unmodifiableList(displayIds);
  }

  /**
   * 按虚拟包名解析入口。
   *
   * @param packageName 虚拟入口包名或普通应用包名
   * @return 对应的虚拟入口；普通应用包名返回 {@code null}
   */
  public static VirtualEntry fromPackage(String packageName) {
    if (AppDataCenter.LOCK_PACKAGE_NAME.equals(packageName)) {
      return new VirtualEntry(Type.LOCK, Collections.singletonList(ID_LOCK));
    }
    if (AppDataCenter.WIFI_PACKAGE_NAME.equals(packageName)) {
      List<String> ids = new ArrayList<>(2);
      ids.add(ID_WIFI_ON);
      ids.add(ID_WIFI_OFF);
      return new VirtualEntry(Type.WIFI, ids);
    }
    if (AppDataCenter.HTTP_SERVER_PACKAGE_NAME.equals(packageName)) {
      List<String> ids = new ArrayList<>(2);
      ids.add(ID_SERVER_ON);
      ids.add(ID_SERVER_OFF);
      return new VirtualEntry(Type.SERVER, ids);
    }
    return null;
  }

  /**
   * 解析 HTTP 服务器入口的自定义图标文件。
   * 优先取当前运行状态专属键（开态 {@link #ID_SERVER_ON} / 关态 {@link #ID_SERVER_OFF}），
   * 不存在时回退包名单文件键，兼容仅提供单文件的旧用法；均缺失返回 {@code null} 以走默认图标。
   *
   * @param icons   自定义图标映射（文件名去扩展名 → 文件），可为 {@code null}
   * @param running 服务器当前是否运行
   * @return 命中的自定义图标文件；无命中返回 {@code null}
   */
  public static File resolveServerCustomIcon(Map<String, File> icons, boolean running) {
    if (icons == null) {
      return null;
    }
    String stateKey = running ? ID_SERVER_ON : ID_SERVER_OFF;
    File stateFile = icons.get(stateKey);
    if (stateFile != null) {
      return stateFile;
    }
    return icons.get(AppDataCenter.HTTP_SERVER_PACKAGE_NAME);
  }

  /** 入口类型 */
  public Type getType() {
    return type;
  }

  /** 弹窗正文展示 id 行：锁屏 1 行，WiFi/服务器 2 行（开态在前、关态在后） */
  public List<String> getDisplayIds() {
    return displayIds;
  }

  /** 弹窗标题资源 */
  public int getTitleRes() {
    switch (type) {
      case LOCK:
        return R.string.item_lockscreen;
      case WIFI:
        return R.string.entry_title_wifi;
      case SERVER:
        return R.string.item_server;
      default:
        throw new IllegalStateException("unknown virtual entry type: " + type);
    }
  }
}
