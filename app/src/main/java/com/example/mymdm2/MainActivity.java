package com.example.mymdm2;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends Activity {
    private static final int ACTIVATE_ADMIN_REQUEST_CODE = 12345; // 원하는 값을 사용하세요
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

        DeviceAdminUtil.activateDeviceAdmin(this, ACTIVATE_ADMIN_REQUEST_CODE);

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

    // 기기 관리자 권한이 활성화되었는지 확인하는 메서드
    private boolean isDeviceAdminActive(Context context) {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName componentName = new ComponentName(context, MyDeviceAdminReceiver.class);
        return devicePolicyManager.isAdminActive(componentName);
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        // 현재 실행 중인 프로세스 목록 가져오기
        List<ActivityManager.RunningAppProcessInfo> runningProcesses = manager.getRunningAppProcesses();

        // 프로세스 목록에서 서비스가 실행 중인지 확인
        for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
            String serviceName = serviceClass.getName();

            // 프로세스에 속한 서비스 목록 확인
            if (processInfo.pkgList != null) {
                for (String pkg : processInfo.pkgList) {
                    if (serviceName.equals(pkg)) {
                        // 서비스가 실행 중임
                        return true;
                    }
                }
            }
        }
        // 서비스가 실행 중이 아님
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ACTIVATE_ADMIN_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // 사용자가 기기 관리자 권한을 수락한 경우
                // 여기에서 기기 관리자 권한이 활성화되었음을 처리할 수 있습니다.
                Log.d(TAG, "Device Admin GET~~~~!");

                DeviceAdminUtil.setInstallBlockPolicy(this,true);

                // 예를 들어, 기기 관리자 권한이 활성화된 후 다른 작업 수행
                // activateDeviceAdmin 이후에 할 작업을 여기에 추가하세요.
            } else {
                // 사용자가 기기 관리자 권한을 거부한 경우 또는 취소한 경우
                // 여기에서 적절한 처리를 수행할 수 있습니다.
                Log.d(TAG, "Device Admin GET failed~~~~~!");
            }
        }
    }
}
