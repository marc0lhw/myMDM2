package com.example.mymdm2;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;


public class DeviceAdminUtil {
    private static final String TAG = "DeviceAdminUtil";
    private static final String PREFS_NAME = "InstallBlockPrefs";
    private static final String KEY_INSTALL_BLOCK = "install_block";

    // 장치 관리자 권한을 활성화하기 위한 메서드
    public static void activateDeviceAdmin(Activity activity, int requestCode) {
        try {
            ComponentName componentName = new ComponentName(activity, MyDeviceAdminReceiver.class);
            DevicePolicyManager devicePolicyManager = (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
            Log.d(TAG, "activateDeviceAdmin 1");

            if (!devicePolicyManager.isAdminActive(componentName)) {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Additional explanation about why this permission is needed.");
                // FLAG_ACTIVITY_NEW_TASK 플래그 대신 startActivityForResult 사용
                activity.startActivityForResult(intent, requestCode);
                Log.d(TAG, "activateDeviceAdmin 2");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 차단 상태를 가져오는 메서드
    public static boolean getInstallBlockStatus(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // "install_block"은 차단 상태를 저장하는 키입니다.
        return preferences.getBoolean(KEY_INSTALL_BLOCK, true);
    }

    // 차단 상태를 저장하는 메서드
    public static void setInstallBlockStatus(Context context, boolean blockInstall) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_INSTALL_BLOCK, blockInstall);
        editor.apply();
    }

    public static void setInstallBlockPolicy(Context context, boolean blockInstall) {
        ComponentName componentName = new ComponentName(context, MyDeviceAdminReceiver.class);
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

        // 장치 관리자 권한이 활성화되어 있는지 확인
        if (devicePolicyManager.isAdminActive(componentName)) {
            // 정책을 설정할 때마다 기존의 정책을 모두 초기화하고 설정
            Bundle restrictions = new Bundle();
            restrictions.putBoolean("no_install_apps", blockInstall);
            Log.d(TAG, "set Install Block Policy : " + blockInstall);
            devicePolicyManager.setApplicationRestrictions(componentName, context.getPackageName(), restrictions);
        } else {
            Log.e(TAG, "Device admin not active. Cannot set policy.");
        }
    }
}
