package io.github.reborn.einklauncher;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import io.github.reborn.einklauncher.widgets.ToggleView;

/**
 * 右下角快捷菜单：按 Config 菜单形态渲染 B 分组菜单或 C 极简面板。
 * 「全部设置」压入 SettingsFragment；字号打开 FontSizeOverlay。
 * 开关点击生效后关闭菜单回桌面（沿用原版行为）。
 */
public class QuickMenuFragment extends Fragment implements View.OnClickListener {

  private static final String TAG = "QuickMenuFragment";
  private static final int REQ_WIFI_NAME = 10002;

  /** 与 Launcher 的回调契约（原 SettingFragment.OnSettingChangeListener 原样迁移）。 */
  public interface OnSettingChangeListener {
    void onRowNumChanged(int rowNum);

    void onColNumChanged(int colNum);

    void onFontSizeChanged(float size);

    void onAppNameLinesChanged(int lines);

    void onHideDividerChanged(boolean hide);

    void onShowStatusBarChanged(boolean show);

    void onShowCustomIconChanged(boolean show);

    void onSortModeChanged(int mode);

    void onEnterManageMode();

    void onShowWifiNameChanged(boolean show);
  }

  private OnSettingChangeListener listener;
  private Config config;
  private View rootView;

  @SuppressWarnings("deprecation")
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (activity instanceof OnSettingChangeListener) {
      listener = (OnSettingChangeListener) activity;
    } else {
      throw new ClassCastException(
          activity.toString() + " must implement OnSettingChangeListener");
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return inflater.inflate(R.layout.activity_quick_menu, null);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    rootView = getView();
    config = new Config(getActivity());

    applyMenuForm();
    bindGrouped();
    bindMinimal();

    rootView.findViewById(R.id.rootView).setOnClickListener(this);
    rootView.findViewById(R.id.toBack).setOnClickListener(this);
    WifiGuard.register(getActivity());
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    WifiGuard.unregister(getActivity());
  }

  private void applyMenuForm() {
    boolean grouped = MenuForm.GROUPED.equals(config.getMenuForm());
    rootView.findViewById(R.id.menuGrouped)
        .setVisibility(grouped ? View.VISIBLE : View.GONE);
    rootView.findViewById(R.id.menuMinimal)
        .setVisibility(grouped ? View.GONE : View.VISIBLE);
  }

  private void bindGrouped() {
    rootView.findViewById(R.id.miFontSize).setOnClickListener(this);
    rootView.findViewById(R.id.miManage).setOnClickListener(this);
    rootView.findViewById(R.id.miDeviceAdmin).setOnClickListener(this);
    rootView.findViewById(R.id.miAbout).setOnClickListener(this);
    rootView.findViewById(R.id.miSettings).setOnClickListener(this);
    ImageView gear = rootView.findViewById(R.id.miSettingsGear);
    gear.setImageDrawable(Utils.tintDrawable(
        getResources().getDrawable(R.drawable.navibar_icon_settings_highlight),
        ColorStateList.valueOf(0xff000000)));
    rootView.findViewById(R.id.miDividerRow).setOnClickListener(this);
    rootView.findViewById(R.id.miStatusRow).setOnClickListener(this);
    rootView.findViewById(R.id.miWifiRow).setOnClickListener(this);

    ToggleView divider = rootView.findViewById(R.id.miDividerToggle);
    divider.setChecked(!config.isHideDivider());
    ToggleView status = rootView.findViewById(R.id.miStatusToggle);
    status.setChecked(config.isShowStatusBar());
    ToggleView wifi = rootView.findViewById(R.id.miWifiToggle);
    wifi.setChecked(config.isShowWifiName());
  }

  private void bindMinimal() {
    rootView.findViewById(R.id.cFontSize).setOnClickListener(this);
    rootView.findViewById(R.id.cManage).setOnClickListener(this);
    rootView.findViewById(R.id.cDivider).setOnClickListener(this);
    rootView.findViewById(R.id.cStatus).setOnClickListener(this);
    rootView.findViewById(R.id.cSettings).setOnClickListener(this);
    refreshMinimalLabels();
  }

  private void refreshMinimalLabels() {
    if (rootView == null || config == null) {
      return;
    }
    TextView dividerLabel = rootView.findViewById(R.id.cDividerLabel);
    dividerLabel.setText(getString(R.string.menu_divider) + " "
        + getString(config.isHideDivider() ? R.string.cell_off : R.string.cell_on));
    TextView statusLabel = rootView.findViewById(R.id.cStatusLabel);
    statusLabel.setText(getString(R.string.menu_statusbar) + " "
        + getString(config.isShowStatusBar() ? R.string.cell_on : R.string.cell_off));
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.rootView || id == R.id.toBack) {
      getActivity().onBackPressed();
    } else if (id == R.id.miFontSize || id == R.id.cFontSize) {
      openFontOverlay();
    } else if (id == R.id.miManage || id == R.id.cManage) {
      listener.onEnterManageMode();
      getActivity().onBackPressed();
    } else if (id == R.id.miSettings || id == R.id.cSettings) {
      getFragmentManager().beginTransaction()
          .replace(android.R.id.content, new SettingsFragment())
          .addToBackStack(null)
          .commit();
    } else if (id == R.id.miDividerRow || id == R.id.cDivider) {
      handleToggleDivider();
    } else if (id == R.id.miStatusRow || id == R.id.cStatus) {
      handleToggleStatusBar();
    } else if (id == R.id.miWifiRow) {
      handleToggleWifiName();
    } else if (id == R.id.miDeviceAdmin) {
      openDeviceAdmin();
    } else if (id == R.id.miAbout) {
      AboutDialog.getInstance(getActivity()).show();
    }
  }

  private void openFontOverlay() {
    FontSizeOverlay.show(getActivity(), config.getFontSize(),
        new FontSizeOverlay.OnSizeChangedListener() {
          @Override
          public void onSizeChanged(float newSize) {
            config.setFontSize(newSize);
            listener.onFontSizeChanged(newSize);
          }
        });
  }

  private void handleToggleDivider() {
    boolean hide = !config.isHideDivider();
    config.setHideDivider(hide);
    listener.onHideDividerChanged(hide);
    ToggleView toggle = rootView.findViewById(R.id.miDividerToggle);
    toggle.setChecked(!hide);
    refreshMinimalLabels();
  }

  private void handleToggleStatusBar() {
    boolean show = !config.isShowStatusBar();
    config.setShowStatusBar(show);
    listener.onShowStatusBarChanged(show);
    ToggleView toggle = rootView.findViewById(R.id.miStatusToggle);
    toggle.setChecked(show);
    refreshMinimalLabels();
  }

  private void handleToggleWifiName() {
    ToggleView toggle = rootView.findViewById(R.id.miWifiToggle);
    boolean show = !config.isShowWifiName();
    config.setShowWifiName(show);
    toggle.setChecked(show);
    listener.onShowWifiNameChanged(show);
    if (show && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        && getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_DENIED) {
      requestPermissions(new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },
          REQ_WIFI_NAME);
    }
  }

  private void openDeviceAdmin() {
    try {
      startActivity(new Intent().setComponent(new ComponentName(
          "com.android.settings", "com.android.settings.DeviceAdminSettings")));
    } catch (Exception e) {
      Log.w(TAG, "open DeviceAdminSettings failed", e);
      Toast.makeText(getActivity(), R.string.open_failed, Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                         int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQ_WIFI_NAME) {
      if (grantResults.length > 0
          && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        io.github.reborn.einklauncher.model.WifiControl.reloadWifiName();
      }
    }
  }
}
