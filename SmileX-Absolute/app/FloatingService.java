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
        
        // 1. ต้องเรียก Foreground ก่อนทำอย่างอื่น
        startMyForeground();

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        menuView = LayoutInflater.from(this).inflate(R.layout.floating_menu, null);
        
        collapsedView = new Button(this);
        collapsedView.setText("SX");
        collapsedView.setBackgroundColor(0xFF00FF00);
        collapsedView.setTextColor(0xFF000000);

        // 2. ปรับ TYPE และ FLAG ให้เด้งชัวร์ๆ
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

        setupLogic(menuView);
        setupDrag(menuView);
        setupDrag(collapsedView);

        collapsedView.setOnClickListener(v -> toggleView());

        // 3. สั่งแสดงผล
        try {
            wm.addView(menuView, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startMyForeground() {
        String CHANNEL_ID = "smilex_abs";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "SmileX", NotificationManager.IMPORTANCE_MIN);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);

            Notification notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Smile-X Absolute")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .build();
            startForeground(1, notification);
        }
    }

    private void setupLogic(View v) {
        Button btnRun = v.findViewById(R.id.btnRun);
        Button btnMin = v.findViewById(R.id.btnMinimize);
        Button btnClose = v.findViewById(R.id.btnClose);
        final EditText input = v.findViewById(R.id.editScript);

        // ระบบคืนคีย์บอร์ดให้แอปอื่น
        input.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
                wm.updateViewLayout(isMinimized ? collapsedView : menuView, params);
            }
            return false;
        });

        btnRun.setOnClickListener(view -> {
            String code = input.getText().toString();
            NativeBridge.runBytecode(("loadstring([[" + code + "]])()").getBytes());
            // หลังรัน คืน Focus ทันที
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            wm.updateViewLayout(menuView, params);
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
                        wm.updateViewLayout(view, params);
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isMinimized) { if (collapsedView.getParent() != null) wm.removeView(collapsedView); }
        else { if (menuView.getParent() != null) wm.removeView(menuView); }
    }
}
