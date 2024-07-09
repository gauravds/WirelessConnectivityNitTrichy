package com.gauravds.wireless;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private TextView networkStatusTextView, networkDetailsTextView, signalStrengthTextView, otherInfoTextView;
    private ConnectivityManager connectivityManager;
    private WifiManager wifiManager;
    private BroadcastReceiver networkReceiver;
    private View bannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bannerView = findViewById(R.id.bannerView);
        networkStatusTextView = findViewById(R.id.networkStatusTextView);
        networkDetailsTextView = findViewById(R.id.networkDetailsTextView);
        signalStrengthTextView = findViewById(R.id.signalStrengthTextView);
        otherInfoTextView = findViewById(R.id.otherInfoTextView);

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        checkPermissions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(networkReceiver);
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.INTERNET
        };

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE);
        } else {
            registerNetworkReceiver();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                registerNetworkReceiver();
            } else {
                networkStatusTextView.setText("Permission denied");
            }
        }
    }

    private void registerNetworkReceiver() {
        networkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateNetworkInfo();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(networkReceiver, filter);
        updateNetworkInfo();
    }

    private void updateNetworkInfo() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                networkStatusTextView.setText("Connected to: Wi-Fi");
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                networkDetailsTextView.setText("Network Details: " + wifiInfo.getSSID());
                int signalStrength = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 5);
                signalStrengthTextView.setText("Signal Strength: " + signalStrength + "/5");
                otherInfoTextView.setText("Other Info: Link Speed - " + wifiInfo.getLinkSpeed() + "Mbps\n"
                        + "IP Address - " + intToIp(wifiInfo.getIpAddress()) + "\n"
                        + "MAC Address - " + wifiInfo.getMacAddress());
                showBanner("Connected to Wi-Fi", android.R.color.holo_green_dark);
            }
        } else {
            networkStatusTextView.setText("Not connected to the internet");
            networkDetailsTextView.setText("Network Details: N/A");
            signalStrengthTextView.setText("Signal Strength: N/A");
            otherInfoTextView.setText("Other Info: N/A");
            showBanner("Not connected to the internet", android.R.color.holo_red_dark);
        }

        if (wifiManager.isWifiEnabled()) {
            otherInfoTextView.append("\nWi-Fi is ON");
        } else {
            otherInfoTextView.append("\nWi-Fi is OFF");
        }
    }

    private void showBanner(String message, int colorResId) {
        bannerView.setBackgroundColor(ContextCompat.getColor(this, colorResId));
        bannerView.setVisibility(View.VISIBLE);
        networkStatusTextView.setText(message);
    }

    private String intToIp(int ipAddress) {
        return ((ipAddress >> 24) & 0xFF) + "." +
                ((ipAddress >> 16) & 0xFF) + "." +
                ((ipAddress >> 8) & 0xFF) + "." +
                (ipAddress & 0xFF);
    }
}
