package com.smilex.absolute;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
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

public class FloatingService extends Service {
    private WindowManager wm;
    private View menuView, collapsedView;
    private WindowManager.LayoutParams params;
    private boolean isMinimized = false;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        initForeground();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        // เตรียม Layout หลัก
        menuView = LayoutInflater.from(this).inflate(R.layout.floating_menu, null);
        
        // เตรียม Layout ตอนย่อ (สร้างปุ่มเล็กๆ ขึ้นมา)
        collapsedView = new Button(this);
        collapsedView.setBackgroundColor(0xFF00FF00); // สีเขียวมรกต
        collapsedView.setText("SX");

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? 
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : 
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, // เริ่มต้นห้ามดักคีย์บอร์ด
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 100;
        params.y = 100;

        setupLogic(menuView);
        setupDrag(menuView);
        setupDrag(collapsedView);

        // เมื่อคลิกปุ่มที่ย่อไว้ ให้ขยายกลับ
        collapsedView.setOnClickListener(v -> toggleView());

        wm.addView(menuView, params);
    }

    private void setupLogic(View v) {
        Button btnRun = v.findViewById(R.id.btnRun);
        Button btnMin = v.findViewById(R.id.btnMinimize);
        Button btnClose = v.findViewById(R.id.btnClose);
        final EditText input = v.findViewById(R.id.editScript);

        // แก้บัคคีย์บอร์ด: โฟกัสเฉพาะตอนแตะช่องพิมพ์
        input.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            } else {
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            }
            wm.updateViewLayout(menuView, params);
        });

        btnRun.setOnClickListener(view -> {
            String code = input.getText().toString();
            try {
                NativeBridge.runBytecode(("loadstring([[" + code + "]])()").getBytes());
            } finally {
                System.gc();
            }
        });

        btnMin.setOnClickListener(view -> toggleView());
        btnClose.setOnClickListener(view -> stopSelf());
    }

    private void toggleView() {
        if (!isMinimized) {
            wm.removeView(menuView);
            wm.addView(collapsedView, params);
        } else {
            wm.removeView(collapsedView);
            wm.addView(menuView, params);
        }
        isMinimized = !isMinimized;
    }

    private void setupDrag(View v) {
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
    }

    private void initForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("sx", "SX", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            startForeground(1, new Notification.Builder(this, "sx").setContentTitle("S-X").build());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isMinimized) wm.removeView(collapsedView);
        else wm.removeView(menuView);
    }
}
