package com.example.mymdm2;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MyDeviceAdminService extends AppCompatActivity {

    private DevicePolicyManager devicePolicyManager;
    private ComponentName componentName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        componentName = new ComponentName(this, MyDeviceAdminReceiver.class);

        // 디바이스 관리자가 활성화되어 있지 않으면 활성화 요청
        if (!devicePolicyManager.isAdminActive(componentName)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable device administration to block apps");
            startActivityForResult(intent, 1);
        } else {
            // 디바이스 관리자가 이미 활성화되어 있으면 Google 앱 차단
            blockApp("com.google.android.apps.maps");
        }
    }

    // 특정 앱을 차단하는 메서드
    private void blockApp(String packageName) {
        if (devicePolicyManager.isDeviceOwnerApp(getPackageName())) {
            // 디바이스 소유권 앱이면 앱 차단
            devicePolicyManager.setApplicationHidden(componentName, packageName, true);
        } else {
            // 디바이스 소유권 앱이 아니면 오류 메시지 표시 또는 관리자 권한 활성화 요청
        }
    }

    // 디바이스 관리자 권한 활성화 결과 처리
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                // 디바이스 관리자가 활성화되었으면 Google 앱 차단
                blockApp("com.google.android.apps.maps");
            } else {
                // 사용자가 권한 부여를 거부한 경우에 대한 처리
            }
        }
    }
}
