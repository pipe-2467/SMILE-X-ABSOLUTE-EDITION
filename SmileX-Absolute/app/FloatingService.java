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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class FloatingService extends Service {
    private WindowManager wm;
    private View menuView;
    private Button collapsedView;
    private WindowManager.LayoutParams params;
    private boolean isMinimized = false;
    
    // ตัวแปรสำคัญ: เก็บสคริปต์ล่าสุดไว้กันหาย (แม้แอปจะ Restart)
    private static String savedScript = ""; 

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        
        // 1. ต้องเรียกสิ่งนี้เป็นอันดับแรกเพื่อป้องกันระบบ Android สั่งปิดแอป
        startMyForeground();

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // 2. ตั้งค่า Layout สำหรับ Android 14 (SDK 34)
        int layoutType = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ? 
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : 
            WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 100;
        params.y = 100;

        initViews();
    }

    private void initViews() {
        // ดึง Layout จาก XML
        menuView = LayoutInflater.from(this).inflate(R.layout.floating_menu, null);
        
        // สร้างปุ่มย่อขนาดฉบับ BFL
        collapsedView = new Button(this);
        collapsedView.setText("SX");
        collapsedView.setBackgroundColor(0xFF00FF00); // สีเขียวมรกต
        collapsedView.setTextColor(0xFF000000);

        setupLogic(menuView);
        setupDrag(menuView);
        setupDrag(collapsedView);

        collapsedView.setOnClickListener(v -> toggleView());

        try {
            wm.addView(menuView, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupLogic(View v) {
        final EditText input = v.findViewById(R.id.editScript);
        Button btnRun = v.findViewById(R.id.btnRun);
        Button btnMin = v.findViewById(R.id.btnMinimize);
        Button btnClose = v.findViewById(R.id.btnClose);

        // ดึงสคริปต์ที่เคยพิมพ์ไว้กลับมา (กันหาย)
        input.setText(savedScript);

        // --- แก้บัคคีย์บอร์ดไม่ขึ้น ---
        input.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
                wm.updateViewLayout(isMinimized ? collapsedView : menuView, params);
                input.requestFocus();
                
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }
            return false;
        });

        // --- ปุ่ม Execute ฉบับกันแอปเด้ง ---
        btnRun.setOnClickListener(view -> {
            final String code = input.getText().toString().trim();

            // ถ้าไม่มีสคริปต์ ไม่ต้องรัน (ป้องกันแอป Crash)
            if (code.isEmpty()) {
                Toast.makeText(this, "กรุณาใส่สคริปต์ก่อนรัน Fl0WERk1ng", Toast.LENGTH_SHORT).show();
                return;
            }

            // เซฟค่าเก็บไว้ทันที
            savedScript = code;

            // รันใน Thread แยก (Background) ไม่ให้กวน UI ของ Android
            new Thread(() -> {
                try {
                    // รันผ่าน NativeBridge
                    NativeBridge.runBytecode(("loadstring([[" + code + "]])()").getBytes());
                    
                    // เรียก Garbage Collector เพื่อคืน RAM ทันทีหลังรัน (ลดอาการแอปอืด)
                    System.gc();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            // คืน Focus ให้เกมทันทีหลังกด Run (กันตัวละครเดินไม่ได้)
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            wm.updateViewLayout(isMinimized ? collapsedView : menuView, params);
            input.clearFocus();
            
            // ซ่อนคีย์บอร์ด
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
            
            Toast.makeText(this, "Executing...", Toast.LENGTH_SHORT).show();
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
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        wm.updateViewLayout(isMinimized ? menuView : collapsedView, params);
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
        String CHANNEL_ID = "fl0werk1ng_service";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "BFL Party Guard", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);

            Notification n = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Fl0WERk1ng Active")
                    .setContentText("BFL Party กำลังทำงานในเบื้องหลัง...")
                    .setSmallIcon(android.R.drawable.ic_lock_power_off) // ไอคอนสัญลักษณ์ความดุ
                    .build();
            startForeground(101, n);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isMinimized) { if (collapsedView.getParent() != null) wm.removeView(collapsedView); }
        else { if (menuView.getParent() != null) wm.removeView(menuView); }
    }
}
