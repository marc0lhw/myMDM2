package com.example.mymdm2;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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

    private BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // USB 연결 이벤트 처리
            boolean usbConnected = intent.getBooleanExtra("connected", false);
            usbBlocked = isUsbBlocked(context, usbConnected);
            updateBlockingStatus();
            Log.d(TAG, "USB connected");
        }
    };

    private BroadcastReceiver tetheringReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 테더링 이벤트 처리
            boolean tetheringActive = isTetheringActive();
            tetheringBlocked = isTetheringBlocked(tetheringActive);
            updateBlockingStatus();
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
            Log.d(TAG, "Wifi connected");
        }
    };

    private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 블루투스 이벤트 처리
            boolean bluetoothConnected = intent.getIntExtra("android.bluetooth.adapter.extra.CONNECTION_STATE", -1)
                    == 2; // Bluetooth 연결 상태 확인 (2는 연결 상태)
            bluetoothBlocked = isBluetoothBlocked(bluetoothConnected);
            updateBlockingStatus();
            Log.d(TAG, "Bluetooth connected");
        }
    };

    private Runnable serviceRunnable = new Runnable() {
        @Override
        public void run() {
            // 2초에 한 번씩 실행되는 서비스 로직
            Log.d(TAG, "Service is running~^___^");
            updateBlockingStatus();
            handler.postDelayed(this, SERVICE_INTERVAL);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        ComponentName adminComponent = new ComponentName(this, MyDeviceAdminReceiver.class);
        registerReceivers();
        startForegroundService();
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
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private boolean isUsbBlocked(Context context, boolean usbConnected) {
        // 실제로 USB 차단 여부를 판단하는 로직 추가
        // 예제: USB가 연결되었을 때 차단
        if (usbConnected) {
            // 여기에서 USB 차단 로직을 추가
            boolean success = UsbControlHelper.disableUsbDataSignaling(context);
            return success; // USB 차단 여부 반환
        } else {
            return false; // USB 차단되지 않음
        }
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

        return wifiBlocked;
    }

    private boolean isBluetoothBlocked(boolean bluetoothConnected) {
        // 실제로 블루투스 차단 여부를 판단하는 로직 추가
        // 예제: 블루투스가 연결되었을 때 차단하지 않음
        return bluetoothConnected;
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
