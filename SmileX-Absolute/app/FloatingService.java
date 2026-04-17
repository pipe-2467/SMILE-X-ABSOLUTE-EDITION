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
    private View menuView;
    private Button collapsedView;
    private WindowManager.LayoutParams params;
    private boolean isMinimized = false;
    private String savedScript = ""; // ตัวเก็บสคริปต์กันหาย

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        startMyForeground();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        initViews();
    }

    private void initViews() {
        menuView = LayoutInflater.from(this).inflate(R.layout.floating_menu, null);
        collapsedView = new Button(this);
        collapsedView.setText("SX");
        collapsedView.setBackgroundColor(0xFF00FF00);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? 
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : 
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 100; params.y = 100;

        setupLogic(menuView);
        setupDrag(menuView);
        setupDrag(collapsedView);

        collapsedView.setOnClickListener(v -> toggleView());
        wm.addView(menuView, params);
    }

    private void setupLogic(View v) {
        Button btnRun = v.findViewById(R.id.btnRun);
        Button btnMin = v.findViewById(R.id.btnMinimize);
        Button btnClose = v.findViewById(R.id.btnClose);
        final EditText input = v.findViewById(R.id.editScript);

        // ดึงสคริปต์เก่ากลับมา (ถ้ามี)
        input.setText(savedScript);

        // แก้บัคคีย์บอร์ดค้าง: ให้คืนสิทธิ์ทันทีที่เลิกจิ้ม
        input.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            } else {
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            }
            wm.updateViewLayout(isMinimized ? collapsedView : menuView, params);
        });

        btnRun.setOnClickListener(view -> {
            savedScript = input.getText().toString(); // เซฟไว้ก่อนรัน
            try {
                // รันแบบไม่ให้ Block UI Thread (กันแอปค้าง)
                new Thread(() -> {
                    NativeBridge.runBytecode(("loadstring([[" + savedScript + "]])()").getBytes());
                    System.gc(); 
                }).start();
                
                // คืนคีย์บอร์ดให้เกมทันที
                input.clearFocus();
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                wm.updateViewLayout(menuView, params);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        btnMin.setOnClickListener(view -> toggleView());
        btnClose.setOnClickListener(view -> stopSelf());
    }

    // --- ส่วนลาก (Drag) และ Foreground เหมือนเดิม แต่เสถียรขึ้น ---
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
            private long downTime;
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downTime = System.currentTimeMillis();
                        lastX = params.x; lastY = params.y;
                        touchX = event.getRawX(); touchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = lastX + (int) (event.getRawX() - touchX);
                        params.y = lastY + (int) (event.getRawY() - touchY);
                        wm.updateViewLayout(view, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (System.currentTimeMillis() - downTime < 200) view.performClick();
                        return true;
                }
                return false;
            }
        });
    }

    private void startMyForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("sx", "SmileX", NotificationManager.IMPORTANCE_MIN);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            startForeground(1, new Notification.Builder(this, "sx").setContentTitle("Smile-X Active").build());
        }
    }
}
