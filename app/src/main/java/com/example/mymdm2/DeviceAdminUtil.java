package com.example.mymdm2;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;


public class DeviceAdminUtil {
    private static final int ACTIVATE_ADMIN_REQUEST_CODE = 12345; // 원하는 값을 사용하세요
    private static final int ACTIVATE_ADMIN_SETTING_REQUEST_CODE = 6789; // 원하는 값을 사용하세요
    private static final String TAG = "DeviceAdminUtil";
    private static final String PREFS_NAME = "InstallBlockPrefs";
    private static final String KEY_INSTALL_BLOCK = "install_block";

    // 장치 관리자 권한을 활성화하기 위한 메서드
    public static void activateDeviceAdmin(Activity activity, int requestCode) {
        if (requestCode == ACTIVATE_ADMIN_REQUEST_CODE) {
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
        else if (requestCode == ACTIVATE_ADMIN_SETTING_REQUEST_CODE) {
            try {
                if (!Settings.System.canWrite(activity)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).setData(Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivityForResult(intent, requestCode);
                    Log.d(TAG, "activateDeviceAdmin 3");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
