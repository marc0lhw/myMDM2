package com.example.mymdm2;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import androidx.biometric.BiometricManager;


public class DeviceAdminUtil {
    private static final int ACTIVATE_ADMIN_REQUEST_CODE = 12345; // 원하는 값을 사용하세요
    private static final int ACTIVATE_ADMIN_SETTING_REQUEST_CODE = 6789; // 원하는 값을 사용하세요
    private static final String TAG = "DeviceAdminUtil";
    private static final long CHECK_INTERVAL = 15000; // 15초 간격으로 체크
    private static final long LOCK_DELAY = 30000; // 30초 후 잠금

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
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "MDM 애플리케이션 실행을 위한 권한을 허용해주시기 바랍니다.");
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
        Log.d(TAG, "setInstallBlockPolicy1");
        if (devicePolicyManager.isAdminActive(componentName)) {
            Log.d(TAG, "setInstallBlockPolicy2");
            devicePolicyManager.addUserRestriction(componentName, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES);
            Log.d(TAG, "Install from unknown sources blocked: " + blockInstall);
        } else {
            Log.e(TAG, "Device admin not active. Cannot set policy.");
        }
    }

    public static void enforcePasswordPolicy(final Activity activity) {
        ComponentName componentName = new ComponentName(activity, MyDeviceAdminReceiver.class);
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);

        if (devicePolicyManager.isAdminActive(componentName)) {
            // 비밀번호 품질을 더 높은 수준으로 설정
            devicePolicyManager.setPasswordQuality(componentName, DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC);
            devicePolicyManager.setPasswordMinimumLength(componentName, 6); // 예시: 최소 6자리

            if (!devicePolicyManager.isActivePasswordSufficient()) {
                showAlertAndStartLockTimer(activity, devicePolicyManager, componentName);
            } else {
                Log.d(TAG, "Current password is already sufficient");
                enforceBiometricPolicy(activity, devicePolicyManager);
            }
        }
    }

    private static void showAlertAndStartLockTimer(final Activity activity, final DevicePolicyManager devicePolicyManager, final ComponentName componentName) {
        new AlertDialog.Builder(activity)
                .setTitle("보안 경고")
                .setMessage("비밀번호 정책이 충족되지 않았습니다. 20초 후에 디바이스가 잠깁니다. 지금 설정해주세요.")
                .setPositiveButton("지금 설정", (dialog, which) -> {
                    Intent intent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
                    activity.startActivity(intent);
                })
                .setNegativeButton("나중에", null)
                .show();

        Handler handler = new Handler();
        Runnable checkPolicyRunnable = new Runnable() {
            @Override
            public void run() {
                if (devicePolicyManager.isActivePasswordSufficient()) {
                    Log.d(TAG, "비밀번호 정책 충족됨, 잠금 타이머 중단");
                    handler.removeCallbacks(this);
                    enforceBiometricPolicy(activity, devicePolicyManager);
                } else {
                    Log.d(TAG, "비밀번호 정책 여전히 미충족, 계속 체크");
                    handler.postDelayed(this, LOCK_DELAY);
                    new AlertDialog.Builder(activity)
                            .setTitle("보안 경고")
                            .setMessage("비밀번호 정책이 충족되지 않았습니다. 20초 후에 디바이스가 잠깁니다. 지금 설정해주세요.")
                            .setPositiveButton("지금 설정", (dialog, which) -> {
                                Intent intent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
                                activity.startActivity(intent);
                            })
                            .setNegativeButton("나중에", null)
                            .show();
                }
            }
        };
        handler.postDelayed(checkPolicyRunnable, CHECK_INTERVAL);
        handler.postDelayed(() -> {
            if (!devicePolicyManager.isActivePasswordSufficient()) {
                devicePolicyManager.lockNow();
            }
        }, LOCK_DELAY);
    }

    public static boolean isBiometricAvailable(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);

        switch (biometricManager.canAuthenticate()) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                Log.d(TAG, "App can authenticate using biometrics.");
                return true;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Log.d(TAG, "No biometric features available on this device.");
                return false;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Log.d(TAG, "Biometric features are currently unavailable.");
                return false;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Log.d(TAG, "Biometric features are available but no biometrics are enrolled.");
                return false;
            default:
                return false;
        }
    }

    public static void enforceBiometricPolicy(final Activity activity, final DevicePolicyManager devicePolicyManager) {

        if (isBiometricAvailable(activity)) {
            // 생체 인식이 활성화되어 있을 때 설정 화면으로 유도
            showBiometricDisableDialog(activity, devicePolicyManager);
        }
        else {
            Log.d(TAG, "BiometricPolicy is already sufficient");
        }
    }
    private static void showBiometricDisableDialog(final Activity activity, final DevicePolicyManager devicePolicyManager) {
        new AlertDialog.Builder(activity)
                .setTitle("보안 경고")
                .setMessage("생체 인식 비활성화 필요 - '지문'으로 이동한 후, 등록된 지문을 삭제해 주세요.")
                .setPositiveButton("지금 설정", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                    activity.startActivity(intent);
                })
                .setNegativeButton("나중에", null)
                .show();

        Handler handler = new Handler();

        Runnable checkBiometricRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isBiometricAvailable(activity)) {
                    // 생체 인식이 비활성화되었으면 타이머 중단
                    Log.d(TAG, "생체인식 정책 충족됨, 잠금 타이머 중단");
                    handler.removeCallbacks(this);
                } else {
                    // 생체 인식이 여전히 활성화되어 있으면 경고 메시지 재표시
                    Log.d(TAG, "생체인식 정책 여전히 미충족, 계속 체크");
                    handler.postDelayed(this, LOCK_DELAY); // 20초 후에 다시 확인
                    new AlertDialog.Builder(activity)
                            .setTitle("보안 경고")
                            .setMessage("생체 인식 비활성화 정책이 충족되지 않았습니다. '지문'으로 이동한 후, 등록된 지문을 삭제해 주세요.")
                            .setPositiveButton("지금 설정", (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                                activity.startActivity(intent);
                            })
                            .setNegativeButton("나중에", null)
                            .show();
                }
            }
        };
        handler.postDelayed(checkBiometricRunnable, CHECK_INTERVAL);
        handler.postDelayed(() -> {
            if (isBiometricAvailable(activity)) {
                devicePolicyManager.lockNow();
            }
        }, LOCK_DELAY);
    }

    public static void wipeData(Context context) {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(context, MyDeviceAdminReceiver.class);

        if (devicePolicyManager.isAdminActive(adminComponent)) {
            // 여기서 0은 디바이스를 초기화할 때 추가적인 옵션을 설정하지 않음을 의미합니다.
            devicePolicyManager.wipeData(0);
        }
    }
}
