package com.smilex.absolute;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class FloatingService extends Service {
    private WindowManager wm;
    private View menuView;
    private Button collapsedView;
    private WindowManager.LayoutParams params;
    private boolean isMinimized = false;
    private static String savedScript = ""; 
    private static final String TAG = "BFL_LOG";
    private long currentLuaPtr = 0;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        startMyForeground();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        setupWindowManager();
        initViews();
    }

    private void setupWindowManager() {
        // กำหนดประเภท Layout ตามเวอร์ชัน Android
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
    }

    private void initViews() {
        // สร้าง View เมนูหลักจาก XML
        menuView = LayoutInflater.from(this).inflate(R.layout.floating_menu, null);
        
        // สร้างปุ่มตอนย่อเมนู (ปุ่ม BFL สีเขียว)
        collapsedView = new Button(this);
        collapsedView.setText("BFL");
        collapsedView.setBackgroundColor(0xFF00FF00); // สีเขียวมรกต
        collapsedView.setTextColor(0xFF000000); // ตัวหนังสือสีดำ

        setupLogic(menuView);
        setupDrag(menuView);
        setupDrag(collapsedView);

        collapsedView.setOnClickListener(v -> toggleView());
        
        try { 
            wm.addView(menuView, params); 
        } catch (Exception e) { 
            Log.e(TAG, "Fail to add view", e); 
        }
    }

    private void setupLogic(View v) {
        final EditText input = v.findViewById(R.id.editScript);
        Button btnAttach = v.findViewById(R.id.btnAttach);
        Button btnRun = v.findViewById(R.id.btnRun);
        Button btnMin = v.findViewById(R.id.btnMinimize);
        Button btnClose = v.findViewById(R.id.btnClose);

        input.setText(savedScript);

        // ระบบ ATTACH (เจาะ LibRoblox)
        btnAttach.setOnClickListener(view -> {
            btnAttach.setText("SCANNING...");
            new Thread(() -> {
                Log.d(TAG, "BFL: Attempting to Attach via PID Hunter...");
                currentLuaPtr = NativeBridge.autoAttach();
                
                v.post(() -> {
                    if (currentLuaPtr != 0) {
                        Toast.makeText(this, "BFL: Attached Success! 0x" + Long.toHexString(currentLuaPtr), Toast.LENGTH_SHORT).show();
                        btnAttach.setText("READY");
                        btnAttach.setBackgroundColor(0xFF006400); // เขียวเข้ม
                    } else {
                        Toast.makeText(this, "BFL: Not Found! กรุณาเข้าเกมก่อนกด", Toast.LENGTH_SHORT).show();
                        btnAttach.setText("ATTACH");
                        btnAttach.setBackgroundColor(Color.RED);
                    }
                });
            }).start();
        });

        // ระบบ EXECUTE (รันสคริปต์)
        btnRun.setOnClickListener(view -> {
            final String code = input.getText().toString().trim();
            if (code.isEmpty()) return;
            if (currentLuaPtr == 0) {
                Toast.makeText(this, "ดาบมรกตยังไม่ได้ ATTACH!", Toast.LENGTH_SHORT).show();
                return;
            }
            savedScript = code;

            new Thread(() -> {
                // ครอบ pcall เพื่อกันเกมเด้งเวลาสคริปต์ผิด
                String finalCode = "local success, err = pcall(function() " + code + " end) if not success then warn('BFL Error: '..tostring(err)) end";
                NativeBridge.runBytecode(finalCode);
                v.post(() -> Toast.makeText(this, "BFL: Execute Sent!", Toast.LENGTH_SHORT).show());
            }).start();
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
            private long downTime;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downTime = System.currentTimeMillis();
                        lastX = params.x; 
                        lastY = params.y;
                        touchX = event.getRawX(); 
                        touchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = lastX + (int) (event.getRawX() - touchX);
                        params.y = lastY + (int) (event.getRawY() - touchY);
                        wm.updateViewLayout(view, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        // ถ้ากดสั้นๆ ให้ถือว่าเป็น Click
                        if (System.currentTimeMillis() - downTime < 200) view.performClick();
                        return true;
                }
                return false;
            }
        });
    }

    private void startMyForeground() {
        String ID = "bfl_guard_v2";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel c = new NotificationChannel(ID, "BFL Protection", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(c);
            startForeground(101, new Notification.Builder(this, ID)
                .setContentTitle("BFL Party: Active")
                .setSmallIcon(android.R.drawable.ic_menu_compass) // เปลี่ยนไอคอนได้ตามชอบ
                .build());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isMinimized) { 
            if (collapsedView.getParent() != null) wm.removeView(collapsedView); 
        } else { 
            if (menuView.getParent() != null) wm.removeView(menuView); 
        }
    }
}
