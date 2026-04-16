package com.smilex.absolute;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import androidx.core.app.NotificationCompat;

public class FloatingService extends Service {
    private WindowManager wm;
    private View v;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        
        // --- 1. ป้องกันแอปหาย (Foreground Service) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("smilex_id", "Smile-X Running", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
            Notification notification = new NotificationCompat.Builder(this, "smilex_id").setContentTitle("Smile-X").setContentText("Executor is active").build();
            startForeground(1, notification);
        }

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        v = LayoutInflater.from(this).inflate(R.layout.floating_menu, null);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 100;
        params.y = 100;

        // --- 2. ระบบลาก (Drag) ที่ลื่นขึ้น ---
        v.setOnTouchListener(new View.OnTouchListener() {
            private int lastX, lastY;
            private float touchX, touchY;
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = params.x; lastY = params.y;
                        touchX = event.getRawX(); touchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = lastX + (int) (event.getRawX() - touchX);
                        params.y = lastY + (int) (event.getRawY() - touchY);
                        wm.updateViewLayout(v, params);
                        return true;
                }
                return false;
            }
        });

        Button runBtn = v.findViewById(R.id.btnRun);
        final EditText input = v.findViewById(R.id.editScript);

        runBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String code = input.getText().toString();
                // ล้างช่องรับข้อมูลทันทีเพื่อลดภาระ RAM (แก้ปัญหาต้องล้าง Cache)
                // input.setText(""); // เปิดบรรทัดนี้ถ้าหัวหน้าอยากให้รันเสร็จแล้วสคริปต์หายไป
                
                try {
                    NativeBridge.runBytecode(("loadstring([[" + code + "]])()").getBytes());
                } catch (Exception e) {
                    // ถ้าพังก็แค่ Log ไว้ ไม่ให้แอป Crash จนหายไป
                    e.printStackTrace();
                }
            }
        });

        wm.addView(v, params);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // บังคับให้ Service เริ่มใหม่เสมอถ้าโดนฆ่า แต่ห้ามหายไปเฉยๆ
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // ล้างทุกอย่างให้เกลี้ยงก่อนปิด (ป้องกัน Memory Leak)
        if (v != null) {
            wm.removeView(v);
            v = null;
        }
    }
}
