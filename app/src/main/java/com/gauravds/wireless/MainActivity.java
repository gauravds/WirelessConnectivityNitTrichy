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
import android.telephony.TelephonyManager;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 1;
    private static final int PERMISSIONS_REQUEST_CODE_READ_PHONE_STATE = 2;
    private TextView networkStatusTextView, networkDetailsTextView, signalStrengthTextView, otherInfoTextView;
    private ConnectivityManager connectivityManager;
    private WifiManager wifiManager;
    private BroadcastReceiver networkReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Reference to the layout file

        networkStatusTextView = findViewById(R.id.networkStatusTextView);
        networkDetailsTextView = findViewById(R.id.networkDetailsTextView);
        signalStrengthTextView = findViewById(R.id.signalStrengthTextView);
        otherInfoTextView = findViewById(R.id.otherInfoTextView);

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Check for location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION);
        } else {
            registerNetworkReceiver();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(networkReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                registerNetworkReceiver();
            } else {
                networkStatusTextView.setText("Permission denied");
            }
        } else if (requestCode == PERMISSIONS_REQUEST_CODE_READ_PHONE_STATE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateNetworkInfo();
            } else {
                networkDetailsTextView.setText("Network Details: Permission required");
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
                otherInfoTextView.setText("Other Info: Link Speed - " + wifiInfo.getLinkSpeed() + "Mbps");
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                networkStatusTextView.setText("Connected to: Mobile Network");
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                    networkDetailsTextView.setText("Network Details: " + telephonyManager.getNetworkOperatorName());
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSIONS_REQUEST_CODE_READ_PHONE_STATE);
                    networkDetailsTextView.setText("Network Details: Permission required");
                }
                // Signal strength for mobile networks can be more complex to obtain; here we simplify
                signalStrengthTextView.setText("Signal Strength: Unknown");
                otherInfoTextView.setText("Other Info: Mobile Network Type - " + getNetworkTypeString(activeNetwork.getSubtype()));
            }
        } else {
            networkStatusTextView.setText("Not connected to the internet");
            networkDetailsTextView.setText("Network Details: N/A");
            signalStrengthTextView.setText("Signal Strength: N/A");
            otherInfoTextView.setText("Other Info: N/A");
        }

        if (wifiManager.isWifiEnabled()) {
            otherInfoTextView.append("\nWi-Fi is ON");
        } else {
            otherInfoTextView.append("\nWi-Fi is OFF");
        }
    }

    private String getNetworkTypeString(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "LTE";
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return "HSPA";
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return "EDGE";
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return "GPRS";
            default:
                return "Unknown";
        }
    }
}
