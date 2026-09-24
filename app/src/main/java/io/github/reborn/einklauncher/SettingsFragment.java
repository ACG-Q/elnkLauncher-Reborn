package io.github.reborn.einklauncher;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import io.github.reborn.einklauncher.model.AppSortComparator;
import io.github.reborn.einklauncher.model.WifiControl;
import io.github.reborn.einklauncher.widgets.ToggleView;

/**
 * 全屏平铺设置页：三灰底分区（桌面外观/应用/系统）。
 * 开关与 Spinner 就地生效不返回；管理应用弹出整个返回栈回桌面。
 */
public class SettingsFragment extends Fragment implements View.OnClickListener {

  private static final String TAG = "SettingsFragment";
  private static final int REQ_WIFI_NAME = 10002;
  private static final int REQ_ICON_STYLE = 10004;

  private QuickMenuFragment.OnSettingChangeListener listener;
  private Config config;
  private View rootView;
  private TextView fontSizeValue;
  private TextView menuFormValue;
  private TextView iconModeValue;
  private TextView iconModeSub;

  @SuppressWarnings("deprecation")
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (activity instanceof QuickMenuFragment.OnSettingChangeListener) {
      listener = (QuickMenuFragment.OnSettingChangeListener) activity;
    } else {
      throw new ClassCastException(
          activity.toString() + " must implement OnSettingChangeListener");
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return inflater.inflate(R.layout.activity_settings, null);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    rootView = getView();
    config = new Config(getActivity());

    rootView.findViewById(R.id.toBack).setOnClickListener(this);
    rootView.findViewById(R.id.spFontSize).setOnClickListener(this);
    rootView.findViewById(R.id.spMenuForm).setOnClickListener(this);
    rootView.findViewById(R.id.spManage).setOnClickListener(this);
    rootView.findViewById(R.id.spDeviceAdmin).setOnClickListener(this);
    rootView.findViewById(R.id.spAbout).setOnClickListener(this);
    rootView.findViewById(R.id.spDividerRow).setOnClickListener(this);
    rootView.findViewById(R.id.spStatusRow).setOnClickListener(this);
    rootView.findViewById(R.id.spWifiRow).setOnClickListener(this);
    rootView.findViewById(R.id.spCustomIconRow).setOnClickListener(this);

    fontSizeValue = rootView.findViewById(R.id.spFontSizeValue);
    fontSizeValue.setText(FontSizeOverlay.formatSp(config.getFontSize()));
    menuFormValue = rootView.findViewById(R.id.spMenuFormValue);
    refreshMenuFormValue();
    iconModeValue = rootView.findViewById(R.id.spCustomIconValue);
    iconModeSub = rootView.findViewById(R.id.spCustomIconSub);
    refreshIconModeValue();

    ToggleView divider = rootView.findViewById(R.id.spDividerToggle);
    divider.setChecked(!config.isHideDivider());
    ToggleView status = rootView.findViewById(R.id.spStatusToggle);
    status.setChecked(config.isShowStatusBar());
    ToggleView wifi = rootView.findViewById(R.id.spWifiToggle);
    wifi.setChecked(config.isShowWifiName());

    initSpinners();
    WifiGuard.register(getActivity());
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    WifiGuard.unregister(getActivity());
  }

  private void refreshMenuFormValue() {
    menuFormValue.setText(MenuForm.MINIMAL.equals(config.getMenuForm())
        ? R.string.menu_form_minimal
        : R.string.menu_form_grouped);
  }

  private void refreshIconModeValue() {
    String mode = config.getIconMode();
    int res;
    if (IconMode.UNIFIED.equals(mode)) {
      res = R.string.icon_mode_unified;
    } else if (IconMode.DEFAULT.equals(mode)) {
      res = R.string.icon_mode_default;
    } else {
      res = R.string.icon_mode_custom;
    }
    iconModeValue.setText(res);
    if (iconModeSub != null) {
      int subRes;
      if (IconMode.UNIFIED.equals(mode)) {
        subRes = R.string.icon_mode_subtitle_unified;
      } else if (IconMode.DEFAULT.equals(mode)) {
        subRes = R.string.icon_mode_subtitle_default;
      } else {
        subRes = R.string.icon_mode_subtitle_custom;
      }
      iconModeSub.setText(subRes);
    }
  }

  private void initSpinners() {
    Spinner rowNumSpinner = rootView.findViewById(R.id.spRowNum);
    rowNumSpinner.setSelection(config.getRowNum() - 2, false);
    rowNumSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int rowNum = position + 2;
        config.setRowNum(rowNum);
        listener.onRowNumChanged(rowNum);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });

    Spinner colNumSpinner = rootView.findViewById(R.id.spColNum);
    colNumSpinner.setSelection(config.getColNum() - 2, false);
    colNumSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int colNum = position + 2;
        config.setColNum(colNum);
        listener.onColNumChanged(colNum);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });

    Spinner nameLinesSpinner = rootView.findViewById(R.id.spNameLines);
    nameLinesSpinner.setSelection(getAppLineSpinnerSelectPosition(), false);
    nameLinesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int lines = (position == 3) ? Integer.MAX_VALUE : position;
        config.setAppNameLines(lines);
        listener.onAppNameLinesChanged(lines);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });

    Spinner sortSpinner = rootView.findViewById(R.id.spSort);
    sortSpinner.setSelection(config.getSortMode(), false);
    sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (AppSortComparator.modeNeedsUsageStats(position)
            && !AppSortComparator.hasUsageStatsPermission(getActivity())) {
          Toast.makeText(getActivity(), R.string.sort_need_usage_permission,
              Toast.LENGTH_LONG).show();
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
          }
          ((Spinner) parent).setSelection(config.getSortMode(), false);
          return;
        }
        config.setSortMode(position);
        listener.onSortModeChanged(position);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
  }

  private int getAppLineSpinnerSelectPosition() {
    int lines = config.getAppNameLines();
    return (lines <= 2) ? lines : 3;
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.toBack) {
      getActivity().onBackPressed();
    } else if (id == R.id.spFontSize) {
      handleFontSize();
    } else if (id == R.id.spMenuForm) {
      showMenuFormDialog();
    } else if (id == R.id.spDividerRow) {
      ToggleView t = rootView.findViewById(R.id.spDividerToggle);
      t.setChecked(!t.isChecked());
      config.setHideDivider(!t.isChecked());
      listener.onHideDividerChanged(!t.isChecked());
    } else if (id == R.id.spStatusRow) {
      ToggleView t = rootView.findViewById(R.id.spStatusToggle);
      t.setChecked(!t.isChecked());
      config.setShowStatusBar(t.isChecked());
      listener.onShowStatusBarChanged(t.isChecked());
    } else if (id == R.id.spWifiRow) {
      handleToggleWifiName();
    } else if (id == R.id.spCustomIconRow) {
      startActivityForResult(
          new Intent(getActivity(), IconStyleActivity.class), REQ_ICON_STYLE);
    } else if (id == R.id.spManage) {
      listener.onEnterManageMode();
      getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    } else if (id == R.id.spDeviceAdmin) {
      openDeviceAdmin();
    } else if (id == R.id.spAbout) {
      AboutDialog.getInstance(getActivity()).show();
    }
  }

  private void handleFontSize() {
    FontSizeOverlay.show(getActivity(), config.getFontSize(),
        new FontSizeOverlay.OnSizeChangedListener() {
          @Override
          public void onSizeChanged(float newSize) {
            config.setFontSize(newSize);
            listener.onFontSizeChanged(newSize);
            fontSizeValue.setText(FontSizeOverlay.formatSp(newSize));
          }
        });
  }

  private void showMenuFormDialog() {
    final String[] forms = { MenuForm.GROUPED, MenuForm.MINIMAL };
    final String[] labels = {
        getString(R.string.menu_form_grouped),
        getString(R.string.menu_form_minimal)
    };
    int checked = MenuForm.MINIMAL.equals(config.getMenuForm()) ? 1 : 0;
    new AlertDialog.Builder(getActivity())
        .setTitle(R.string.menu_form_label)
        .setSingleChoiceItems(labels, checked, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            config.setMenuForm(forms[which]);
            refreshMenuFormValue();
            dialog.dismiss();
          }
        })
        .setNegativeButton(R.string.dialog_cancel, null)
        .show();
  }

  private void handleToggleWifiName() {
    ToggleView toggle = rootView.findViewById(R.id.spWifiToggle);
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
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQ_WIFI_NAME) {
      if (grantResults.length > 0
          && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        WifiControl.reloadWifiName();
      }
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQ_ICON_STYLE && resultCode == Activity.RESULT_OK) {
      refreshIconModeValue();
      listener.onIconModeChanged(config.getIconMode());
    }
  }
}
