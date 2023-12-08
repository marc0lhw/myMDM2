package com.example.mymdm2;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.lang.reflect.Method;

public class BlockingService extends Service {

    private static final String TAG = "BlockingService";
    private static final long SERVICE_INTERVAL = 2000; // 2 seconds
    private Handler handler;

    // 브로드캐스트 액션
    public static final String ACTION_BLOCKING_STATUS = "com.example.mymdm2.BLOCKING_STATUS";
    public static final String EXTRA_USB_BLOCKED = "extraUsbBlocked";
    public static final String EXTRA_TETHERING_BLOCKED = "extraTetheringBlocked";
    public static final String EXTRA_WIFI_BLOCKED = "extraWifiBlocked";
    public static final String EXTRA_BLUETOOTH_BLOCKED = "extraBluetoothBlocked";

    // 초기 상태는 차단되지 않음
    private boolean usbBlocked = false;
    private boolean tetheringBlocked = false;
    private boolean wifiBlocked = false;
    private boolean bluetoothBlocked = false;
    private static String POLICY_STATUS = "GREEN";
    private boolean IS_DEBUGGING = true;

    private BroadcastReceiver tetheringReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                // Get ConnectivityManager
                Class<?> connectivityManagerClass = Class.forName("android.net.ConnectivityManager");
                Object connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE);

                // Get the stopTethering method
                Method stopTetheringMethod = connectivityManagerClass.getDeclaredMethod("stopTethering", int.class);
                stopTetheringMethod.setAccessible(true);

                // Use the constant for TYPE_MOBILE_HIPRI instead of TYPE_MOBILE
                int typeMobileHipri = connectivityManagerClass.getField("TYPE_MOBILE_HIPRI").getInt(null);

                // Invoke the stopTethering method
                stopTetheringMethod.invoke(connectivityManager, typeMobileHipri);
                Log.e(TAG, "Tethering blocked4");
                tetheringBlocked = true;
                Toast.makeText(getApplicationContext(), "Tethering 기능 차단", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Wifi 연결 이벤트 처리
            if (intent.getAction() != null && intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);

                switch (wifiState) {
                    case WifiManager.WIFI_STATE_ENABLED:
                        // Wi-Fi가 활성화된 경우
                        boolean wifiConnected = isWifiConnected(context);
                        wifiBlocked = isWifiBlocked(wifiConnected);
                        updateBlockingStatus();
                        Log.d(TAG, "Wifi connected");
                        break;
                    case WifiManager.WIFI_STATE_DISABLED:
                        // Wi-Fi가 비활성화된 경우
                        Log.d("WifiReceiver", "Wi-Fi disabled");
                        break;
                    default:
                        // 다른 상태
                }
            }


        }
    };

    private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 블루투스 이벤트 처리
            if (intent.getAction() != null && intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

                switch (bluetoothState) {
                    case BluetoothAdapter.STATE_ON:
                        // Bluetooth가 켜진 경우
                        mBluetoothAdapter.disable();
                        bluetoothBlocked = true;
                        updateBlockingStatus();
                        Toast.makeText(getApplicationContext(), "Bluetooth 기능 차단", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Bluetooth blocked");
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        // Bluetooth가 꺼진 경우
                        Log.d("BluetoothReceiver", "Bluetooth turned off");
                        break;
                    default:
                        // 다른 상태
                }
            }
        }
    };

    private Runnable serviceRunnable = new Runnable() {
        @Override
        public void run() {
            // 2초에 한 번씩 실행되는 서비스 로직
            Log.d(TAG, "Service is running~^___^ / POLICY_STATUS is " + POLICY_STATUS);
            updateBlockingStatus();
            handler.postDelayed(this, SERVICE_INTERVAL);
            switch (POLICY_STATUS) {
                case "GREEN":
                    break;
                case "YELLOW":
                    break;
                case "ORANGE":
                    break;
                case "RED":
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("BlockingService", "Service created");
        handler = new Handler(Looper.getMainLooper());
        registerReceivers();
        startForegroundService();
        // 설치 차단 기능 활성화
        Log.d(TAG, "Service created 2");
        handler.postDelayed(serviceRunnable, SERVICE_INTERVAL);
    }

    private void registerReceivers() {
        // 필요한 브로드캐스트 리시버 등록
//        registerReceiver(usbReceiver, new IntentFilter("android.hardware.usb.action.USB_STATE"));
//        registerReceiver(tetheringReceiver, new IntentFilter("android.net.conn.TETHER_STATE_CHANGED"));
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
        registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    private void startForegroundService() {
        // 포어그라운드 서비스 시작

        String channelId = "BlockingServiceChannel";
        NotificationChannel channel = new NotificationChannel(channelId,
                "Blocking Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Blocking Service")
                .setContentText("Service is running in the background")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();

        startForeground(1, notification);

    }

    private void updateBlockingStatus() {
        // 현재 차단 상태를 MainActivity로 로컬 브로드캐스트
        Intent intent = new Intent(ACTION_BLOCKING_STATUS);
        intent.putExtra(EXTRA_USB_BLOCKED, usbBlocked);
        intent.putExtra(EXTRA_TETHERING_BLOCKED, tetheringBlocked);
        intent.putExtra(EXTRA_WIFI_BLOCKED, wifiBlocked);
        intent.putExtra(EXTRA_BLUETOOTH_BLOCKED, bluetoothBlocked);
        sendBroadcast(intent);
    }

    private boolean isTetheringActive() {
        // 테더링이 활성화되어 있을 때 차단
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            Class<?> managerClass = Class.forName(cm.getClass().getName());
            Method method = managerClass.getDeclaredMethod("getTetheredIfaces");
            method.setAccessible(true);
            String[] tetheredIfaceArray = (String[]) method.invoke(cm);
            return tetheredIfaceArray != null && tetheredIfaceArray.length > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isTetheringBlocked(boolean tetheringActive) {
        // 비공식적인 방법으로 테더링 차단 여부를 판단하는 로직 추가
        // 이 코드는 안정성이 보장되지 않을 수 있습니다.
        if (tetheringActive) {
            blockTethering();
            return true;
        }
        return false;
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        }
        return false;
    }

    private boolean isWifiBlocked(boolean wifiConnected) {
        // 실제로 Wifi 차단 여부를 판단하는 로직 추가
        // 예제: Wifi가 연결되었을 때 차단
        WifiManager wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(false);
        Log.d(TAG, "WifiBlock on ");
        Toast.makeText(getApplicationContext(), "Wi-FI 기능 차단", Toast.LENGTH_SHORT).show();
        return true;
    }

    private void blockTethering() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            Class<?> cmClass = cm.getClass();
            Method getTetheredIfaces = cmClass.getDeclaredMethod("getTetheredIfaces");
            getTetheredIfaces.setAccessible(true);
            String[] tetheredIfaces = (String[]) getTetheredIfaces.invoke(cm);

            Log.e(TAG, "blockTethering1 " + (tetheredIfaces != null) + tetheredIfaces.length);
            if (tetheredIfaces != null && tetheredIfaces.length > 0) {
                Class<?> tetheringClass = cmClass.getClassLoader().loadClass("android.os.INetworkManagementService");
                Method getService = ConnectivityManager.class.getDeclaredMethod("getNetworkManagementService", (Class<?>[]) null);
                getService.setAccessible(true);
                Object networkManagementService = getService.invoke(null);

                for (String iface : tetheredIfaces) {
                    Method untetherMethod = tetheringClass.getDeclaredMethod("untether", String.class);
                    untetherMethod.setAccessible(true);
                    untetherMethod.invoke(networkManagementService, iface);
                }
                Log.e(TAG, "blockTethering2");
            }
            else {
                Log.e(TAG, "blockTethering3");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        // 리시버 등록 해제 및 서비스 종료 로직 추가
//        unregisterReceiver(usbReceiver);
//        unregisterReceiver(tetheringReceiver);
        unregisterReceiver(wifiReceiver);
        unregisterReceiver(bluetoothReceiver);
        handler.removeCallbacks(serviceRunnable);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
