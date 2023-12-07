package com.example.mymdm2;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class MyDeviceAdminReceiver extends DeviceAdminReceiver {
    private static final String TAG = "MyDeviceAdminReceiver";

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);

        // 프로필 관리자 설정
        try {
            DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName componentName = new ComponentName(context, MyDeviceAdminReceiver.class);

            // 프로필 관리자가 아닌 경우, 프로필 관리자로 설정
            if (!devicePolicyManager.isProfileOwnerApp(context.getPackageName())) {
                Intent profileOwnerIntent = new Intent(DevicePolicyManager.ACTION_PROFILE_OWNER_CHANGED);
                profileOwnerIntent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
                context.sendBroadcast(profileOwnerIntent);
                Log.d(TAG, "Profile owner set");
            } else {
                Log.d(TAG, "Already profile owner");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        showToast(context, "Device admin enabled");
        Log.d(TAG, "ADMIN onEnabled");
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        Log.d(TAG, "ADMIN onDisabled");
        showToast(context, "Device admin disabled");
    }

    private void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

}

