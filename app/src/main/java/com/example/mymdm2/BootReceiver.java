package com.example.mymdm2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // 부팅이 완료되면 서비스를 시작
            Intent serviceIntent = new Intent(context, BlockingService.class);
            context.startForegroundService(serviceIntent);
        }
    }
}
