package com.smilex.absolute;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import com.smilex.absolute.R;

public class FloatingService extends Service {
    private WindowManager wm;
    private View v;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        // 1. ดึงหน้าเมนูจาก XML
        v = LayoutInflater.from(this).inflate(R.layout.floating_menu, null);

        // 2. ตั้งค่าการแสดงผล (TYPE_APPLICATION_OVERLAY คือหัวใจสำคัญ)
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 100;
        params.y = 100;

        // 3. ปุ่ม Execute
        Button runBtn = v.findViewById(R.id.btnRun);
        final EditText input = v.findViewById(R.id.editScript);

        runBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String code = input.getText().toString();
                String payload = "loadstring([[" + code + "]])()";
                
                // ส่งค่าไปที่ C++ ผ่าน NativeBridge
                try {
                    NativeBridge.runBytecode(payload.getBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // 4. สั่งให้เมนูเด้งขึ้นหน้าจอ
        wm.addView(v, params);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (v != null) wm.removeView(v);
    }
}
