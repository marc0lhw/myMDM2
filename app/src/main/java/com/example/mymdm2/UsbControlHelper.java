package com.example.mymdm2;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class UsbControlHelper {

    public static boolean disableUsbDataSignaling(Context context) {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(context, MyDeviceAdminReceiver.class);

        if (devicePolicyManager.isAdminActive(adminComponent)) {
            // Check if USB data signaling can be disabled
            devicePolicyManager.setUsbDataSignalingEnabled(false);
            return true;

        } else {
            // Device admin not active, request activation
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable device admin to control USB data signaling.");
            context.startActivity(intent);
            return false; // Device admin not active
        }
    }

    public static boolean isUsbDataSignalingEnabled(Context context) {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

        // Check if USB data signaling is enabled
        return devicePolicyManager.isUsbDataSignalingEnabled();
    }

}
