package com.smilex.absolute;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

public class FloatingService extends Service {
    private WindowManager wm;
    private View v;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        v = LayoutInflater.from(this).inflate(R.layout.floating_menu, null);

        // แก้ไข Flag ตรงนี้: เอา FLAG_NOT_FOCUSABLE ออก เพื่อให้คีย์บอร์ดเด้งขึ้นมาได้
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, // ใช้ตัวนี้แทนเพื่อให้กดอย่างอื่นได้ด้วย
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 100;
        params.y = 100;

        // --- ระบบลากเมนู (Touch Listener) ---
        v.setOnTouchListener(new View.OnTouchListener() {
            private int lastX, lastY;
            private float touchX, touchY;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = params.x;
                        lastY = params.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = lastX + (int) (event.getRawX() - touchX);
                        params.y = lastY + (int) (event.getRawY() - touchY);
                        wm.updateViewLayout(v, params); // อัปเดตตำแหน่งตามมือ
                        return true;
                }
                return false;
            }
        });

        Button runBtn = v.findViewById(R.id.btnRun);
        final EditText input = v.findViewById(R.id.editScript);

        // เมื่อกดที่ช่องพิมพ์ ให้คืน Focus เพื่อให้คีย์บอร์ดเด้ง
        input.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
                wm.updateViewLayout(v, params);
                return false;
            }
        });

        runBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String code = input.getText().toString();
                // สั่ง Execute ผ่าน NativeBridge
                NativeBridge.runBytecode(("loadstring([[" + code + "]])()").getBytes());
                
                // หลังกดรัน ให้ปลด Focus ออก เพื่อไม่ให้คีย์บอร์ดบังจอเกม
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                wm.updateViewLayout(v, params);
            }
        });

        wm.addView(v, params);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (v != null) wm.removeView(v);
    }
}
