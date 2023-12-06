package com.example.mymdm2;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private TextView statusTextView;
    private BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = findViewById(R.id.statusTextView);

        // 서비스가 실행 중인지 확인하고 실행 중이 아니면 시작
        if (!isServiceRunning(BlockingService.class)) {
            startService(new Intent(this, BlockingService.class));
        }
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean usbBlocked = intent.getBooleanExtra(BlockingService.EXTRA_USB_BLOCKED, false);
                boolean tetheringBlocked = intent.getBooleanExtra(BlockingService.EXTRA_TETHERING_BLOCKED, false);
                boolean wifiBlocked = intent.getBooleanExtra(BlockingService.EXTRA_WIFI_BLOCKED, false);
                boolean bluetoothBlocked = intent.getBooleanExtra(BlockingService.EXTRA_BLUETOOTH_BLOCKED, false);

                Log.d(TAG, "mReceiver get broadcast");
                updateBlockingStatus(usbBlocked, tetheringBlocked, wifiBlocked, bluetoothBlocked);
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 브로드캐스트 리시버 등록
        registerReceiver(mReceiver, new IntentFilter(BlockingService.ACTION_BLOCKING_STATUS));
    }

    @Override
    protected void onStop() {
        // 브로드캐스트 리시버 등록 해제
        unregisterReceiver(mReceiver);
        super.onStop();
    }

    // 기존의 updateBlockingStatus 메서드에서 매개변수를 받도록 수정
    private void updateBlockingStatus(boolean usbBlocked, boolean tetheringBlocked, boolean wifiBlocked, boolean bluetoothBlocked) {
        StringBuilder statusBuilder = new StringBuilder("Blocking status: ");
        if (usbBlocked) {
            statusBuilder.append("USB blocked, ");
        }
        if (tetheringBlocked) {
            statusBuilder.append("Tethering blocked, ");
        }
        if (wifiBlocked) {
            statusBuilder.append("Wifi blocked, ");
        }
        if (bluetoothBlocked) {
            statusBuilder.append("Bluetooth blocked, ");
        }

        // 마지막에 쉼표 및 공백 제거
        String statusText = statusBuilder.toString().replaceAll(", $", "");
        statusTextView.setText(statusText);
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        // 현재 실행 중인 서비스 목록 가져오기
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                // 서비스가 실행 중임
                return true;
            }
        }

        // 서비스가 실행 중이 아님
        return false;
    }
}
