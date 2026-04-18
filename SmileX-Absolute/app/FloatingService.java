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
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.nio.charset.StandardCharsets;

public class FloatingService extends Service {
    private WindowManager wm;
    private View menuView;
    private Button collapsedView;
    private WindowManager.LayoutParams params;
    private boolean isMinimized = false;
    private static String savedScript = ""; 
    private static final String TAG = "BFL_LOG";

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
        params.x = 100; params.y = 100;
    }

    private void initViews() {
        menuView = LayoutInflater.from(this).inflate(R.layout.floating_menu, null);
        collapsedView = new Button(this);
        collapsedView.setText("BFL");
        collapsedView.setBackgroundColor(0xFF00FF00); // สีเขียวมรกต
        collapsedView.setTextColor(0xFF000000);

        setupLogic(menuView);
        setupDrag(menuView);
        setupDrag(collapsedView);

        collapsedView.setOnClickListener(v -> toggleView());
        try { wm.addView(menuView, params); } catch (Exception e) { Log.e(TAG, "Fail to add view", e); }
    }

    private void setupLogic(View v) {
        final EditText input = v.findViewById(R.id.editScript);
        Button btnRun = v.findViewById(R.id.btnRun);
        Button btnMin = v.findViewById(R.id.btnMinimize);
        Button btnClose = v.findViewById(R.id.btnClose);

        input.setText(savedScript);

        // ระบบ Focus คีย์บอร์ด
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

        // --- ระบบสั่งการ BFL Party (V.2.714.1091) ---
        btnRun.setOnClickListener(view -> {
            final String code = input.getText().toString().trim();
            if (code.isEmpty()) return;
            savedScript = code;

            new Thread(() -> {
                try {
                    // 1. ค้นหาที่อยู่เกมอัตโนมัติ
                    long luaPtr = NativeBridge.autoAttach();
                    
                    if (luaPtr == 0) {
                        Log.e(TAG, "Roblox not found. Please open the game first!");
                        return;
                    }

                    // 2. ปลดล็อกพลัง Identity 8
                    NativeBridge.applyIdentity(luaPtr);

                    // 3. ห่อหุ้มสคริปต์ด้วย pcall กันเกมหลุด
                    String finalCode = "local success, err = pcall(function() " + code + " end) if not success then warn('BFL Error: '..tostring(err)) end";
                    byte[] bytecode = finalCode.getBytes(StandardCharsets.UTF_8);

                    // 4. สั่งพิพากษา
                    NativeBridge.runBytecode(bytecode);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Execution Error", e);
                }
            }).start();

            // คืนค่า Focus หน้าจอปกติ
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            wm.updateViewLayout(isMinimized ? collapsedView : menuView, params);
            input.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
            
            Toast.makeText(this, "BFL Party: คำสั่งถูกส่งออกไปแล้ว!", Toast.LENGTH_SHORT).show();
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
            private int lastX, lastY; private float touchX, touchY; private long downTime;
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
        String ID = "bfl_guard_v2";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel c = new NotificationChannel(ID, "BFL Protection", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(c);
            startForeground(101, new Notification.Builder(this, ID)
                .setContentTitle("BFL Party: ระบบพร้อมทำงาน")
                .setSmallIcon(android.R.drawable.ic_lock_power_off)
                .build());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isMinimized) wm.removeView(collapsedView);
        else wm.removeView(menuView);
    }
}
