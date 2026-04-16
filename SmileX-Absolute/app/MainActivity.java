package com.smilex.absolute;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // เช็คว่าเคยขออนุญาต Draw Overlays ไปหรือยัง
        if (!Settings.canDrawOverlays(this)) {
            // ถ้ายัง... ให้เด้งไปหน้าตั้งค่า
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 1234);
            Toast.makeText(this, "กรุณาอนุญาตสิทธิ์วาดทับแอปอื่น", Toast.LENGTH_LONG).show();
        } else {
            // ถ้าอนุญาตแล้ว... เริ่มการทำงานของเมนูทันที
            startFloatingService();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1234) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingService();
            } else {
                Toast.makeText(this, "สิทธิ์ถูกปฏิเสธ แอปไม่สามารถทำงานได้", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startFloatingService() {
        // สั่งเปิดเมนู FloatingService
        startService(new Intent(this, FloatingService.class));
        // ปิดหน้า MainActivity ทิ้งเพื่อไม่ให้จอดำค้างไว้ (เหลือแค่เมนู)
        finish();
    }
}
