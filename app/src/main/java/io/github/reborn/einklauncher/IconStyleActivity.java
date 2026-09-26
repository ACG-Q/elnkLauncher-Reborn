package io.github.reborn.einklauncher;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import io.github.reborn.einklauncher.model.IconStyle;
import io.github.reborn.einklauncher.model.UnifiedIconRenderer;

/**
 * 图标样式子页：模式三态（含存储权限流）、统一图标全局样式滑条、实时预览、单字自定义入口。
 * 全局样式区仅在统一图标档可调，其余档整区禁用置灰。
 * 变更即时写入 {@link Config} 并置 RESULT_OK；桌面刷新由 SettingsFragment 返回链触发。
 */
public class IconStyleActivity extends Activity {

  private static final int REQ_STORAGE_STYLE = 10005;
  private static final String PREVIEW_TEXT = "WE";

  private Config config;
  private RadioGroup modeGroup;
  private TextView borderValue;
  private TextView radiusValue;
  private TextView textSizeValue;
  private ImageView preview;
  private SeekBar seekBorder;
  private SeekBar seekRadius;
  private SeekBar seekTextSize;
  private View styleSection;
  private boolean updating;
  private String pendingMode;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_icon_style);
    config = new Config(this);

    findViewById(R.id.toBack).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        finish();
      }
    });

    modeGroup = findViewById(R.id.modeGroup);
    borderValue = findViewById(R.id.borderValue);
    radiusValue = findViewById(R.id.radiusValue);
    textSizeValue = findViewById(R.id.textSizeValue);
    preview = findViewById(R.id.stylePreview);
    seekBorder = findViewById(R.id.seekBorder);
    seekRadius = findViewById(R.id.seekRadius);
    seekTextSize = findViewById(R.id.seekTextSize);
    styleSection = findViewById(R.id.styleSection);

    modeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (updating) {
          return;
        }
        String mode = checkedId == R.id.modeUnified ? IconMode.UNIFIED
            : checkedId == R.id.modeCustom ? IconMode.CUSTOM : IconMode.DEFAULT;
        selectIconMode(mode);
      }
    });

    SeekBar.OnSeekBarChangeListener styleListener = new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser || updating) {
          return;
        }
        writeStyleFromSeekBars();
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
      }
    };
    seekBorder.setOnSeekBarChangeListener(styleListener);
    seekRadius.setOnSeekBarChangeListener(styleListener);
    seekTextSize.setOnSeekBarChangeListener(styleListener);

    findViewById(R.id.styleReset).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        config.setUnifiedStyle(IconStyle.defaults());
        bindStyle(config.getUnifiedStyle());
        setResult(RESULT_OK);
      }
    });

    findViewById(R.id.charListRow).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startActivity(new Intent(IconStyleActivity.this, AppCharActivity.class));
      }
    });

    bindMode(config.getIconMode());
    bindStyle(config.getUnifiedStyle());
    updatePreview();
  }

  private void bindMode(String mode) {
    updating = true;
    modeGroup.check(IconMode.UNIFIED.equals(mode) ? R.id.modeUnified
        : IconMode.CUSTOM.equals(mode) ? R.id.modeCustom : R.id.modeDefault);
    updating = false;
    setStyleSectionEnabled(IconMode.UNIFIED.equals(mode));
  }

  private void setStyleSectionEnabled(boolean enabled) {
    setEnabledRecursive(styleSection, enabled);
    styleSection.setAlpha(enabled ? 1f : 0.4f);
  }

  private static void setEnabledRecursive(View view, boolean enabled) {
    view.setEnabled(enabled);
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        setEnabledRecursive(group.getChildAt(i), enabled);
      }
    }
  }

  private void bindStyle(IconStyle style) {
    updating = true;
    IconStyle s = style.clamped();
    seekBorder.setProgress(Math.round((s.getStroke() - 1f) / 0.5f));
    seekRadius.setProgress(Math.round(s.getRadius()));
    seekTextSize.setProgress(Math.round((s.getTextScale() - 0.30f) * 200f));
    borderValue.setText(String.format(java.util.Locale.US, "%.1f", s.getStroke()));
    radiusValue.setText(String.format(java.util.Locale.US, "%d", Math.round(s.getRadius())));
    textSizeValue.setText(String.format(java.util.Locale.US, "%.2f", s.getTextScale()));
    updating = false;
  }

  private void writeStyleFromSeekBars() {
    IconStyle style = new IconStyle(
        1f + seekBorder.getProgress() * 0.5f,
        seekRadius.getProgress(),
        0.30f + seekTextSize.getProgress() / 200f);
    config.setUnifiedStyle(style);
    bindStyle(style);
    updatePreview();
    setResult(RESULT_OK);
  }

  private void updatePreview() {
    int size = (int) (72 * getResources().getDisplayMetrics().density);
    preview.setImageDrawable(UnifiedIconRenderer.create(
        PREVIEW_TEXT, size, false, config.getUnifiedStyle()));
  }

  private void selectIconMode(String mode) {
    boolean needsStorage = !IconMode.DEFAULT.equals(mode);
    if (needsStorage
        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_DENIED) {
      pendingMode = mode;
      requestPermissions(new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE,
          Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQ_STORAGE_STYLE);
      return;
    }
    applyIconMode(mode);
  }

  private void applyIconMode(String mode) {
    config.setIconMode(mode);
    pendingMode = null;
    setStyleSectionEnabled(IconMode.UNIFIED.equals(mode));
    setResult(RESULT_OK);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode != REQ_STORAGE_STYLE || pendingMode == null) {
      return;
    }
    if (grantResults.length > 0
        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      applyIconMode(pendingMode);
    } else {
      Toast.makeText(this, R.string.icon_mode_permission_denied, Toast.LENGTH_SHORT).show();
      pendingMode = null;
      bindMode(config.getIconMode());
    }
  }
}
