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
        
        // --- 1. ต้องเรียกอันนี้ก่อนเป็นอันดับแรก ห้ามมีอะไรคั่น! ---
        startMyForeground();

        // --- 2. หลังจากรัน Foreground สำเร็จ ค่อยสร้าง GUI ---
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        initViews();
    }

    private void startMyForeground() {
        String CHANNEL_ID = "smilex_channel_v2";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "BFL Party Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);

            Notification notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("BFL Party - Smile-X")
                    .setContentText("Flower King กำลังดูแลระบบ...")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setOngoing(true) // ห้ามปัดทิ้ง
                    .build();
            
            // ใช้ ID 101 (ห้ามใช้ 0)
            startForeground(101, notification);
        }
    }

    private void initViews() {
        menuView = LayoutInflater.from(this).inflate(R.layout.floating_menu, null);
        collapsedView = new Button(this);
        collapsedView.setText("SX");
        collapsedView.setBackgroundColor(0xFF00FF00); // สีเขียวมรกต BFL

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? 
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : 
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 100; params.y = 100;

        setupLogic(menuView);
        setupDrag(menuView);
        setupDrag(collapsedView);

        collapsedView.setOnClickListener(v -> toggleView());

        // ลองแสดงผล
        try {
            wm.addView(menuView, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- ส่วน Logic การลากและสลับหน้าจอ (Toggle) ---
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

    private void setupLogic(View v) {
        Button btnRun = v.findViewById(R.id.btnRun);
        Button btnMin = v.findViewById(R.id.btnMinimize);
        Button btnClose = v.findViewById(R.id.btnClose);
        final EditText input = v.findViewById(R.id.editScript);

        btnRun.setOnClickListener(view -> {
            String code = input.getText().toString();
            NativeBridge.runBytecode(("loadstring([[" + code + "]])()").getBytes());
        });

        btnMin.setOnClickListener(view -> toggleView());
        btnClose.setOnClickListener(view -> stopSelf());
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isMinimized) { if (collapsedView.getParent() != null) wm.removeView(collapsedView); }
        else { if (menuView.getParent() != null) wm.removeView(menuView); }
    }
}
