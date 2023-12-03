package com.example.mymdm2;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MainActivity extends Activity {

    private TextView statusTextView;
    private BroadcastReceiver blockingStatusReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = findViewById(R.id.statusTextView);

        // 서비스의 상태를 받아와 UI 업데이트
        updateBlockingStatus();

        // 서비스가 실행 중인지 확인하고 실행 중이 아니면 시작
        if (!isServiceRunning(BlockingService.class)) {
            startService(new Intent(this, BlockingService.class));
        }

        // 브로드캐스트 리시버 등록
        blockingStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // 브로드캐스트를 통해 차단 상태를 업데이트하고 UI를 갱신
                updateBlockingStatus();
            }
        };

        // 로컬 브로드캐스트 리시버 등록
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(blockingStatusReceiver, new IntentFilter(BlockingService.ACTION_BLOCKING_STATUS));
    }

    private void updateBlockingStatus() {
        // 로컬 브로드캐스트로부터 차단 상태를 받아와 UI 업데이트 코드 추가
        boolean usbBlocked = getIntent().getBooleanExtra(BlockingService.EXTRA_USB_BLOCKED, false);
        boolean tetheringBlocked = getIntent().getBooleanExtra(BlockingService.EXTRA_TETHERING_BLOCKED, false);
        boolean wifiBlocked = getIntent().getBooleanExtra(BlockingService.EXTRA_WIFI_BLOCKED, false);
        boolean bluetoothBlocked = getIntent().getBooleanExtra(BlockingService.EXTRA_BLUETOOTH_BLOCKED, false);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 로컬 브로드캐스트 리시버 등록 해제
        LocalBroadcastManager.getInstance(this).unregisterReceiver(blockingStatusReceiver);
    }
}
