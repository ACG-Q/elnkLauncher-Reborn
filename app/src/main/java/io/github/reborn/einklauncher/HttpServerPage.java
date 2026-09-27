package io.github.reborn.einklauncher;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import io.github.reborn.einklauncher.ftpservice.HttpService;

public class HttpServerPage extends Activity {

  private View stateDot;
  private TextView tvState;
  private TextView tvAddress;
  private EditText etPort;
  private Button btnToggle;
  private Button btnOpenBrowser;
  private Button btnCopy;
  private Button btnCopyMini;

  private final android.content.BroadcastReceiver receiver = new android.content.BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (HttpService.ACTION_STARTED.equals(action)) {
        updateStatus(true);
      } else if (HttpService.ACTION_STOPPED.equals(action)) {
        updateStatus(false);
      } else if (HttpService.ACTION_FAILEDTOSTART.equals(action)) {
        updateStatus(false);
        tvState.setText(R.string.http_server_failed_start);
      }
    }
  };

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_http_server);

    stateDot = findViewById(R.id.stateDot);
    tvState = findViewById(R.id.stateValue);
    tvAddress = findViewById(R.id.addressValue);
    etPort = findViewById(R.id.portInput);
    btnToggle = findViewById(R.id.btnToggle);
    btnOpenBrowser = findViewById(R.id.btnOpen);
    btnCopy = findViewById(R.id.btnCopy);
    btnCopyMini = findViewById(R.id.btnCopyMini);

    etPort.setText(String.valueOf(HttpService.getDefaultPortFromPreferences(
        PreferenceManager.getDefaultSharedPreferences(this))));

    findViewById(R.id.toBack).setOnClickListener(v -> finish());
    findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    btnToggle.setOnClickListener(v -> toggleServer());
    btnOpenBrowser.setOnClickListener(v -> openInBrowser());
    btnCopy.setOnClickListener(v -> copyAddress());
    btnCopyMini.setOnClickListener(v -> copyAddress());

    updateStatus(HttpService.isRunning());
  }

  private void toggleServer() {
    if (HttpService.isRunning()) {
      stopServer();
    } else {
      startServer();
    }
  }

  private void startServer() {
    if (!HttpService.isConnectedToWifi(this)) {
      Toast.makeText(this, R.string.http_server_wifi_first, Toast.LENGTH_SHORT).show();
      return;
    }

    int port;
    try {
      port = Integer.parseInt(etPort.getText().toString().trim());
      if (port <= 0 || port > 65535) {
        etPort.setError(getString(R.string.http_server_port_error));
        return;
      }
    } catch (NumberFormatException e) {
      etPort.setError(getString(R.string.http_server_port_invalid));
      return;
    }

    if (!HttpService.isPortAvailable(port)) {
      Toast.makeText(this, getString(R.string.server_port_busy, port), Toast.LENGTH_LONG).show();
      return;
    }

    HttpService.changePort(
        PreferenceManager.getDefaultSharedPreferences(this), port);
    Intent startIntent = new Intent(this, HttpService.class);
    startIntent.putExtra("port", port);
    startService(startIntent);
  }

  private void stopServer() {
    Intent stopIntent = new Intent(this, HttpService.class);
    stopService(stopIntent);
  }

  private void updateStatus(boolean running) {
    stateDot.setBackgroundResource(
        running ? R.drawable.state_dot_solid : R.drawable.state_dot);
    if (running) {
      tvState.setText(R.string.http_server_running);
      btnToggle.setText(R.string.http_server_stop);
      btnOpenBrowser.setEnabled(true);
      btnCopy.setEnabled(true);
      btnCopyMini.setEnabled(true);
      String addr = getAddressString();
      tvAddress.setText(addr != null ? addr : getString(R.string.http_server_none));
      tvAddress.setTextColor(addr != null ? Color.BLACK : 0xFF999999);
    } else {
      tvState.setText(R.string.http_server_stopped);
      btnToggle.setText(R.string.http_server_start);
      btnOpenBrowser.setEnabled(false);
      btnCopy.setEnabled(false);
      btnCopyMini.setEnabled(false);
      tvAddress.setText(R.string.http_server_none);
      tvAddress.setTextColor(0xFF999999);
    }
  }

  private String getAddressString() {
    java.net.InetAddress addr = HttpService.getLocalInetAddress(this);
    if (addr == null) return null;
    return "http://" + addr.getHostAddress() + ":" + HttpService.getPort();
  }

  private void openInBrowser() {
    String addr = getAddressString();
    if (addr == null) {
      Toast.makeText(this, R.string.http_server_no_address, Toast.LENGTH_SHORT).show();
      return;
    }
    Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(addr));
    startActivity(intent);
  }

  private void copyAddress() {
    String addr = getAddressString();
    if (addr == null) {
      Toast.makeText(this, R.string.http_server_no_address, Toast.LENGTH_SHORT).show();
      return;
    }
    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    cm.setPrimaryClip(ClipData.newPlainText("HTTP Server Address", addr));
    Toast.makeText(this, R.string.http_server_copied, Toast.LENGTH_SHORT).show();
  }

  @Override
  protected void onResume() {
    super.onResume();
    IntentFilter filter = new IntentFilter();
    filter.addAction(HttpService.ACTION_STARTED);
    filter.addAction(HttpService.ACTION_STOPPED);
    filter.addAction(HttpService.ACTION_FAILEDTOSTART);
    registerReceiver(receiver, filter);
    updateStatus(HttpService.isRunning());
  }

  @Override
  protected void onPause() {
    super.onPause();
    try {
      unregisterReceiver(receiver);
    } catch (IllegalArgumentException ignored) {
    }
  }
}
