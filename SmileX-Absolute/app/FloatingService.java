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

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        startMyForeground();

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        menuView = LayoutInflater.from(this).inflate(R.layout.floating_menu, null);
        
        // สร้างปุ่มย่อ [SX]
        collapsedView = new Button(this);
        collapsedView.setText("SX");
        collapsedView.setBackgroundColor(0xFF00FF00); // เขียวมรกต
        collapsedView.setTextColor(0xFF000000);

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

        // --- แก้บัคที่ 1: กดปุ่มย่อแล้วต้องขยายกลับได้ ---
        collapsedView.setOnClickListener(v -> toggleView());

        wm.addView(menuView, params);
    }

    private void setupLogic(View v) {
        Button btnRun = v.findViewById(R.id.btnRun);
        Button btnMin = v.findViewById(R.id.btnMinimize);
        Button btnClose = v.findViewById(R.id.btnClose);
        final EditText input = v.findViewById(R.id.editScript);

        // --- แก้บัคที่ 2: ระบบจัดการคีย์บอร์ด (Focus Management) ---
        // เมื่อแตะที่ช่องพิมพ์ ให้ดึงคีย์บอร์ดขึ้นมา
        input.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
                wm.updateViewLayout(isMinimized ? collapsedView : menuView, params);
                input.requestFocus();
            }
            return false;
        });

        // เมื่อกดรัน ให้คืนคีย์บอร์ดให้ระบบทันที
        btnRun.setOnClickListener(view -> {
            String code = input.getText().toString();
            NativeBridge.runBytecode(("loadstring([[" + code + "]])()").getBytes());
            
            // คืน Focus ให้เกมทันที
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            wm.updateViewLayout(isMinimized ? collapsedView : menuView, params);
            input.clearFocus();
            System.gc();
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
        // ทุกครั้งที่สลับหน้าจอ ให้ reset คีย์บอร์ดเป็นโหมดห้ามดักไว้ก่อน (เพื่อความชัวร์)
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        wm.updateViewLayout(isMinimized ? menuView : collapsedView, params);
        
        isMinimized = !isMinimized;
    }

    private void setupDrag(View v) {
        v.setOnTouchListener(new View.OnTouchListener() {
            private int lastX, lastY;
            private float touchX, touchY;
            private long lastDownTime;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastDownTime = System.currentTimeMillis();
                        lastX = params.x; lastY = params.y;
                        touchX = event.getRawX(); touchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = lastX + (int) (event.getRawX() - touchX);
                        params.y = lastY + (int) (event.getRawY() - touchY);
                        wm.updateViewLayout(view, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        // ถ้ากดแป๊บเดียว (ไม่ลาก) ให้ถือว่าเป็นการคลิก (สำหรับปุ่มย่อ)
                        if (System.currentTimeMillis() - lastDownTime < 200) {
                            view.performClick();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void startMyForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("sx", "SmileX", NotificationManager.IMPORTANCE_MIN);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
            startForeground(1, new Notification.Builder(this, "sx").setContentTitle("Smile-X").build());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isMinimized) { if (collapsedView.getParent() != null) wm.removeView(collapsedView); }
        else { if (menuView.getParent() != null) wm.removeView(menuView); }
    }
}
