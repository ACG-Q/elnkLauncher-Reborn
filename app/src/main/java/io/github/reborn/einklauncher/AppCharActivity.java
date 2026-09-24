package io.github.reborn.einklauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.reborn.einklauncher.model.UnifiedIconRenderer;

/**
 * 统一图标单字自定义列表页：列出可启动应用，点击行弹窗编辑覆写字。
 * 覆写经 {@link IconCharRules#normalize} 校验后写入 {@link Config}；
 * 桌面刷新由返回链（IconStyleActivity → SettingsFragment → onIconModeChanged）触发。
 */
public class AppCharActivity extends Activity {

  private static final int MAX_APPS = 200;

  private Config config;
  private PackageManager pm;
  private final List<ResolveInfo> apps = new ArrayList<>();
  private CharAdapter adapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_app_char);
    config = new Config(this);
    pm = getPackageManager();

    findViewById(R.id.toBack).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        finish();
      }
    });

    loadApps();
    adapter = new CharAdapter();
    ListView list = findViewById(R.id.charList);
    list.setAdapter(adapter);
    list.setOnItemClickListener((parent, view, position, id) -> showEditDialog(position));
  }

  private void loadApps() {
    Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
    List<ResolveInfo> resolved = pm.queryIntentActivities(mainIntent, 0);
    java.util.Set<String> hidden = config.getHideApps();
    for (ResolveInfo info : resolved) {
      String pkg = info.activityInfo != null ? info.activityInfo.packageName : null;
      if (pkg == null || hidden.contains(pkg) || getPackageName().equals(pkg)) {
        continue;
      }
      apps.add(info);
      if (apps.size() >= MAX_APPS) {
        break;
      }
    }
  }

  private CharSequence labelOf(ResolveInfo info) {
    return info.loadLabel(pm);
  }

  private void showEditDialog(final int position) {
    final ResolveInfo info = apps.get(position);
    final String pkg = info.activityInfo.packageName;
    final String label = labelOf(info).toString();
    Map<String, String> overrides = config.getUnifiedCharOverrides();
    String current = overrides.get(pkg);

    final EditText input = new EditText(this);
    input.setHint(R.string.char_edit_hint);
    if (current != null) {
      input.setText(current);
    }

    AlertDialog dialog = new AlertDialog.Builder(this)
        .setTitle(label)
        .setView(input)
        .setPositiveButton(android.R.string.ok, null)
        .setNegativeButton(android.R.string.cancel, null)
        .setNeutralButton(R.string.char_restore, null)
        .show();

    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        String text = IconCharRules.normalize(input.getText().toString());
        if (text == null) {
          Toast.makeText(AppCharActivity.this, R.string.char_invalid_toast,
              Toast.LENGTH_SHORT).show();
          return;
        }
        config.setUnifiedChar(pkg, text);
        dialog.dismiss();
        adapter.notifyDataSetChanged();
      }
    });
    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        config.clearUnifiedChar(pkg);
        dialog.dismiss();
        adapter.notifyDataSetChanged();
      }
    });
  }

  private class CharAdapter extends BaseAdapter {

    private final LayoutInflater inflater = LayoutInflater.from(AppCharActivity.this);

    @Override
    public int getCount() {
      return apps.size();
    }

    @Override
    public Object getItem(int position) {
      return apps.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View row = convertView != null ? convertView
          : inflater.inflate(R.layout.item_app_char, parent, false);
      ResolveInfo info = apps.get(position);
      String pkg = info.activityInfo.packageName;
      String label = labelOf(info).toString();

      String override = config.getUnifiedCharOverrides().get(pkg);
      String effective = IconText.effective(label, override);

      ImageView preview = row.findViewById(R.id.charPreview);
      int size = (int) (34 * getResources().getDisplayMetrics().density);
      preview.setImageDrawable(UnifiedIconRenderer.create(
          effective, size, false, config.getUnifiedStyle()));

      TextView labelView = row.findViewById(R.id.charLabel);
      labelView.setText(label);

      TextView valueView = row.findViewById(R.id.charValue);
      valueView.setText(effective);

      return row;
    }
  }
}
