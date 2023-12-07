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
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.UserManager;
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

    private BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                        // 여기에 USB 연결을 차단하는 코드를 추가
                        usbBlocked = blockUsbConnection(context);
                        updateBlockingStatus();
                        Toast.makeText(getApplicationContext(), "USB connection disallow", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "USB Connected");
                        break;
//                    case UsbManager.ACTION_USB_DEVICE_DETACHED:
//                        // USB가 연결이 해제되었을 때의 동작
//                        Toast.makeText(context, "USB Disconnected", Toast.LENGTH_SHORT).show();
//                        // 여기에 USB 연결 차단을 해제하는 코드를 추가
//                        unblockUsbConnection(context);
//                        break;
                }
            }
        }

        private boolean blockUsbConnection(Context context) {
            DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName componentName = new ComponentName(context, MyDeviceAdminReceiver.class);

            if (devicePolicyManager.isAdminActive(componentName)) {
                // 디바이스 관리자 권한이 활성화된 경우에만 USB 차단 시도
                devicePolicyManager.addUserRestriction(componentName, UserManager.DISALLOW_USB_FILE_TRANSFER);
                Toast.makeText(context, "USB Connection Blocked", Toast.LENGTH_SHORT).show();
                return true;
            } else {
                Toast.makeText(context, "Device Admin permission required", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        private void unblockUsbConnection(Context context) {
            DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName componentName = new ComponentName(context, MyDeviceAdminReceiver.class);

            if (devicePolicyManager.isAdminActive(componentName)) {
                // 디바이스 관리자 권한이 활성화된 경우에만 USB 차단 해제 시도
                devicePolicyManager.clearUserRestriction(componentName, UserManager.DISALLOW_USB_FILE_TRANSFER);
                Toast.makeText(context, "USB Connection Unblocked", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Device Admin permission required", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private BroadcastReceiver tetheringReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 테더링 이벤트 처리
            boolean tetheringActive = isTetheringActive();
            tetheringBlocked = isTetheringBlocked(tetheringActive);
            updateBlockingStatus();
            Toast.makeText(getApplicationContext(), "Tethering disallow", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Tethering detected");
        }
    };

    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Wifi 연결 이벤트 처리
            boolean wifiConnected = isWifiConnected(context);
            wifiBlocked = isWifiBlocked(wifiConnected);
            updateBlockingStatus();
            Toast.makeText(getApplicationContext(), "Wi-Fi disallow", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Wifi connected");
        }
    };

    private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 블루투스 이벤트 처리
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                // Device does not support Bluetooth
            } else if (!mBluetoothAdapter.isEnabled()) {
                // Bluetooth is not enabled :)
            } else {
                // Bluetooth is enabled
                mBluetoothAdapter.disable();
                bluetoothBlocked = true;
                updateBlockingStatus();
                Toast.makeText(getApplicationContext(), "Bluetooth disallow", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Bluetooth connected");
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
        registerReceiver(usbReceiver, new IntentFilter("android.hardware.usb.action.USB_STATE"));
        registerReceiver(tetheringReceiver, new IntentFilter("android.net.conn.TETHER_STATE_CHANGED"));
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
        registerReceiver(bluetoothReceiver, new IntentFilter("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED"));
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
        wifiBlocked = !wifiManager.isWifiEnabled();
        return wifiBlocked;
    }

    private void blockTethering() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            Class<?> cmClass = cm.getClass();
            Method getTetheredIfaces = cmClass.getDeclaredMethod("getTetheredIfaces");
            getTetheredIfaces.setAccessible(true);
            String[] tetheredIfaces = (String[]) getTetheredIfaces.invoke(cm);

            if (tetheredIfaces != null && tetheredIfaces.length > 0) {
                Method setTethering = cmClass.getDeclaredMethod("setTethering", String.class, boolean.class);
                setTethering.setAccessible(true);

                for (String iface : tetheredIfaces) {
                    setTethering.invoke(cm, iface, false);
                }

                Log.d(TAG, "Tethering is blocked.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        // 리시버 등록 해제 및 서비스 종료 로직 추가
        unregisterReceiver(usbReceiver);
        unregisterReceiver(tetheringReceiver);
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
