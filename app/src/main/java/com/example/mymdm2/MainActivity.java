package com.example.mymdm2;

import static java.lang.Thread.sleep;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity {
    private static final int ACTIVATE_ADMIN_REQUEST_CODE = 12345; // 원하는 값을 사용하세요
    private static final int ACTIVATE_ADMIN_SETTING_REQUEST_CODE = 6789; // 원하는 값을 사용하세요
    private static final String TAG = "MainActivity";
    private TextView statusTextView;
    private TextView statusPolicyView;
    private BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DeviceAdminUtil.activateDeviceAdmin(this, ACTIVATE_ADMIN_REQUEST_CODE);
//        DeviceAdminUtil.activateDeviceAdmin(this, ACTIVATE_ADMIN_SETTING_REQUEST_CODE);

        setContentView(R.layout.activity_main);
        statusTextView = findViewById(R.id.statusTextView);

        ImageView imageView = findViewById(R.id.circle);

        ObjectAnimator animator = ObjectAnimator.ofFloat(imageView, "rotation", 0f, 360f);
        animator.setDuration(4000); // 회전에 걸리는 시간 (밀리초)
        animator.setRepeatCount(ObjectAnimator.INFINITE); // 무한 반복
        animator.setInterpolator(new LinearInterpolator()); // 일정한 속도로 회전
        animator.start();

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean usbBlocked = intent.getBooleanExtra(BlockingService.EXTRA_USB_BLOCKED, false);
                boolean tetheringBlocked = intent.getBooleanExtra(BlockingService.EXTRA_TETHERING_BLOCKED, false);
                boolean wifiBlocked = intent.getBooleanExtra(BlockingService.EXTRA_WIFI_BLOCKED, false);
                boolean bluetoothBlocked = intent.getBooleanExtra(BlockingService.EXTRA_BLUETOOTH_BLOCKED, false);
                String policy_status = intent.getStringExtra(BlockingService.EXTRA_POLICY_STATUS);

                Log.d(TAG, "mReceiver get broadcast");
                updateBlockingStatus(usbBlocked, tetheringBlocked, wifiBlocked, bluetoothBlocked, policy_status);
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
    private void updateBlockingStatus(boolean usbBlocked, boolean tetheringBlocked, boolean wifiBlocked, boolean bluetoothBlocked, String policy_status) {
        StringBuilder statusBuilder = new StringBuilder("Blocking status: ");
        Log.d(TAG, "BlockingStatus " + usbBlocked + tetheringBlocked + wifiBlocked + bluetoothBlocked);
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
//        statusTextView.setText(statusText);

        // 외부 설치 앱 차단
        DeviceAdminUtil.setInstallBlockPolicy(this, true);
        // 스마트폰 잠금 기능 의무화
        DeviceAdminUtil.enforcePasswordPolicy(this);

        changeBackgroundColor(policy_status);
        DeviceAdminUtil.blockApp(this, policy_status);
        DeviceAdminUtil.blockGPS(this, policy_status);
        switch (policy_status) {
            case "YELLOW":
                break;
            case "ORANGE":
                DeviceAdminUtil.setMobileDataEnabled(this);
                break;
            case "RED":
                break;
        }
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
                if (isDeviceAdminActive(this)) {
                    Log.d(TAG, "onActivityResult1");
                    try
                    {
                        sleep(10000);
                    } catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }

                    // 서비스가 실행 중인지 확인하고 실행 중이 아니면 시작
                    if (!isServiceRunning(BlockingService.class)) {
                        Log.d(TAG, "onActivityResult2");

                        startService(new Intent(this, BlockingService.class));
                        Log.d(TAG, "onActivityResult3");

                    }

                    // 예를 들어, 기기 관리자 권한이 활성화된 후 다른 작업 수행
                    // activateDeviceAdmin 이후에 할 작업을 여기에 추가하세요.
                }
            } else {
                // 사용자가 기기 관리자 권한을 거부한 경우 또는 취소한 경우
                // 여기에서 적절한 처리를 수행할 수 있습니다.
                Log.d(TAG, "Device Admin GET failed~~~~~!");
            }
        }
        else if (requestCode == ACTIVATE_ADMIN_SETTING_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // 사용자가 기기 관리자 권한을 수락한 경우
                // 여기에서 기기 관리자 권한이 활성화되었음을 처리할 수 있습니다.
                Log.d(TAG, "SETTING Admin GET~~~~!");
            } else {
                // 사용자가 기기 관리자 권한을 거부한 경우 또는 취소한 경우
                // 여기에서 적절한 처리를 수행할 수 있습니다.
                Log.d(TAG, "SETTING Admin GET failed~~~~~!");
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void changeBackgroundColor(String POLICY_STATUS) {
        View rootLayout = findViewById(R.id.rootLayout);
        statusPolicyView = findViewById(R.id.statusPolicyView);
        statusPolicyView.setText("보안 정책 등급 : " + POLICY_STATUS);
        switch(POLICY_STATUS) {
            case "GREEN":
                rootLayout.setBackgroundColor(Color.parseColor("#22B14C")); // 연한 녹색
                statusTextView.setText("① 접근 제어: 비밀번호 잠금 의무화, 생체인식 비활성화, \n          3일 인증 실패 시 기기 초기화\n"
                        + "② USB, 테더링, Wi-Fi, 블루투스 기능 차단\n" +
                        "③ 외부 출처 애플리케이션 설치 차단");
                break;
            case "YELLOW":
                rootLayout.setBackgroundColor(Color.parseColor("#FFC90D")); // 연한 노란색
                statusTextView.setText("① 접근 제어: 비밀번호 잠금 의무화, 생체인식 비활성화, \n          3일 인증 실패 시 기기 초기화\n"
                        + "② USB, 테더링, Wifi, 블루투스 기능 차단\n" +
                        "③ 외부 출처 애플리케이션 설치 차단\n" +
                        "④ 비허가 애플리케이션 설치 차단\n" +
                        "⑤ 안전 채널 외 데이터 차단");
                break;
            case "ORANGE":
                rootLayout.setBackgroundColor(Color.parseColor("#FE7F28")); // 연한 주황색
                statusTextView.setText("① 접근 제어: 비밀번호 잠금 의무화, 생체인식 비활성화, \n          3일 인증 실패 시 기기 초기화\n"
                        + "② USB, 테더링, Wifi, 블루투스 기능 차단\n" +
                        "③ 외부 출처 애플리케이션 설치 차단\n" +
                        "④ 비허가 애플리케이션 설치 차단\n" +
                        "⑤ 데이터(3G, 4G, 5G), GPS 차단");
                break;
            case "RED":
                rootLayout.setBackgroundColor(Color.parseColor("#ED1C25")); // 연한 빨간색
                statusTextView.setText("① 접근 제어: 비밀번호 잠금 의무화, 생체인식 비활성화, \n          3일 인증 실패 시 기기 초기화\n"
                        + "② USB, 테더링, Wifi, 블루투스 기능 차단\n" +
                        "③ 외부 출처 애플리케이션 설치 차단\n" +
                        "④ 비허가 애플리케이션 설치 차단\n" +
                        "⑤ 데이터(3G, 4G, 5G), GPS 차단\n" +
                        "⑥ 전화, 문자(2G) 차단");
                break;
        }

    }

}
