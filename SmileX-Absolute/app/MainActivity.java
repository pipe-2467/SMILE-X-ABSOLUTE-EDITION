package com.smilex.absolute;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. เช็คสิทธิ์ Overlay (วาดทับหน้าจอ)
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 1234);
            Toast.makeText(this, "กรุณาอนุญาตสิทธิ์วาดทับแอปอื่น", Toast.LENGTH_LONG).show();
        } else {
            handleStartService();
        }
    }

    private void handleStartService() {
        Intent intent = new Intent(this, FloatingService.class);
        
        // 2. ใช้การเริ่มแบบ Foreground Service สำหรับ Android 8.0 ขึ้นไป
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        
        Toast.makeText(this, "Smile-X: กำลังเปิดเมนูมรกต...", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1234) {
            if (Settings.canDrawOverlays(this)) {
                handleStartService();
            } else {
                Toast.makeText(this, "สิทธิ์ถูกปฏิเสธ! ดาบมรกตออกจากฝักไม่ได้", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
