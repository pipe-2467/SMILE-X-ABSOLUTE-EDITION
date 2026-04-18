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
    private static final String TAG = "BFL_EXECUTION";

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
        // ดึง Layout มาจาก floating_menu.xml
        menuView = LayoutInflater.from(this).inflate(R.layout.floating_menu, null);
        
        // สร้างปุ่มย่อขนาด (SX)
        collapsedView = new Button(this);
        collapsedView.setText("SX");
        collapsedView.setBackgroundColor(0xFF00FF00); // สีเขียวมรกต
        collapsedView.setTextColor(0xFF000000);

        setupLogic(menuView);
        setupDrag(menuView);
        setupDrag(collapsedView);

        collapsedView.setOnClickListener(v -> toggleView());
        try { wm.addView(menuView, params); } catch (Exception e) { Log.e(TAG, "Add View Failed", e); }
    }

    private void setupLogic(View v) {
        final EditText input = v.findViewById(R.id.editScript);
        Button btnRun = v.findViewById(R.id.btnRun);
        Button btnMin = v.findViewById(R.id.btnMinimize);
        Button btnClose = v.findViewById(R.id.btnClose);

        input.setText(savedScript);

        // ระบบ Focus เมื่อแตะที่ช่องกรอกสคริปต์
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

        // --- ระบบปุ่ม Execute (จุดรวมพลัง) ---
        btnRun.setOnClickListener(view -> {
            final String code = input.getText().toString().trim();
            if (code.isEmpty()) {
                Toast.makeText(this, "ระบุสคริปต์ก่อนลูกพี่!", Toast.LENGTH_SHORT).show();
                return;
            }

            savedScript = code;

            new Thread(() -> {
                try {
                    // 1. ห่อหุ้มสคริปต์ด้วย pcall ป้องกันเกมเด้งเมื่อสคริปต์ Error
                    String wrappedCode = "local s, e = pcall(function() " + code + " end) if not s then warn('BFL ERROR: '..tostring(e)) end";
                    byte[] bytecode = wrappedCode.getBytes(StandardCharsets.UTF_8);

                    // 2. ดึงพิกัด LuaState (ลูกพี่ต้องเปลี่ยนเลข 0x0 เป็นตัวแปรที่ดึงจาก Scan Memory จริงๆ)
                    long currentLuaPtr = 0x0; // <--- ใส่ตัวดึงค่าตรงนี้

                    // 3. ปลดล็อก Identity 8 ผ่าน NativeBridge
                    if (currentLuaPtr != 0) {
                        NativeBridge.applyIdentity(currentLuaPtr);
                    }

                    // 4. ส่งสคริปต์เข้าสู่เครื่องยนต์ C++
                    NativeBridge.runBytecode(bytecode);
                    
                    Log.d(TAG, "Fl0WERk1ng: Script sent to bridge.");
                } catch (Exception e) {
                    Log.e(TAG, "Execution Failed", e);
                }
            }).start();

            // คืนสิทธิ์ Focus ให้หน้าจอเกมหลังกด Run
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            wm.updateViewLayout(isMinimized ? collapsedView : menuView, params);
            input.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
            
            Toast.makeText(this, "BFL Party: Power Sent!", Toast.LENGTH_SHORT).show();
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
        String CHANNEL_ID = "smilex_service";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "SMILE-X ABSOLUTE", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
            
            Notification notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("SMILE-X: Fl0WERk1ng")
                    .setContentText("Service is active")
                    .setSmallIcon(android.R.drawable.ic_menu_compass)
                    .build();
            startForeground(101, notification);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isMinimized) wm.removeView(collapsedView);
        else wm.removeView(menuView);
    }
}
