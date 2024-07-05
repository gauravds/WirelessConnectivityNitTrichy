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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private TextView networkStatusTextView, networkDetailsTextView, signalStrengthTextView, otherInfoTextView;
    private ConnectivityManager connectivityManager;
    private WifiManager wifiManager;
    private BroadcastReceiver networkReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                Manifest.permission.INTERNET,
                Manifest.permission.READ_PHONE_STATE
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
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                networkStatusTextView.setText("Connected to: Mobile Network");
                if (isMobileDataEnabled()) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                        networkDetailsTextView.setText("Network Details: " + telephonyManager.getNetworkOperatorName());
                    } else {
                        networkDetailsTextView.setText("Network Details: Permission required");
                    }
                    signalStrengthTextView.setText("Signal Strength: Unknown");
                    otherInfoTextView.setText("Other Info: Mobile Network Type - " + getNetworkTypeString(activeNetwork.getSubtype()) + "\n"
                            + "IP Address - " + getMobileIpAddress() + "\n"
                            + "MAC Address - " + getMobileMacAddress());
                } else {
                    // Mobile data is off, but we are connected
                    networkDetailsTextView.setText("Network Details: Mobile data off");
                    signalStrengthTextView.setText("Signal Strength: N/A");
                    otherInfoTextView.setText("Other Info: N/A");
                }
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

        if (isMobileDataEnabled()) {
            otherInfoTextView.append("\nMobile Data is ON");
        } else {
            otherInfoTextView.append("\nMobile Data is OFF");
        }
    }

    private boolean isMobileDataEnabled() {
        try {
            NetworkInfo info = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            return info != null && info.isConnected();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getNetworkTypeString(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "LTE";
            case TelephonyManager.NETWORK_TYPE_NR:
                return "5G";
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

    private String intToIp(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                ((ip >> 24) & 0xFF);
    }

    private String getMobileIpAddress() {
        try {
            for (NetworkInterface intf : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress addr : Collections.list(intf.getInetAddresses())) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':') < 0;

                        if (isIPv4) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    private String getMobileMacAddress() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }

                return res1.toString();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }
}
