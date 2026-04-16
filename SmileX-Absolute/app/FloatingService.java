package com.smilex.absolute;

import android.app.Service;
import android.content.Intent; // เพิ่ม
import android.os.IBinder;   // เพิ่ม
import android.view.WindowManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import com.smilex.absolute.R; // เพิ่ม (สำคัญมากเพื่อให้รู้จัก R.layout และ R.id)

public class FloatingService extends Service {
    private WindowManager wm;
    private View v;

    // จุดที่ 1: ต้องมี onBind เสมอสำหรับ Service
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        // จุดที่ 2: ตรวจสอบว่ามีไฟล์ res/layout/floating_menu.xml ในโปรเจกต์
        v = LayoutInflater.from(this).inflate(R.layout.floating_menu, null);

        Button runBtn = v.findViewById(R.id.btnRun);
        final EditText input = v.findViewById(R.id.editScript);

        runBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String code = input.getText().toString();
                // หุ้มด้วย loadstring
                String payload = "loadstring([[" + code + "]])()";
                
                // จุดที่ 3: เรียกใช้ NativeBridge (ต้องมีไฟล์ NativeBridge.java ด้วย)
                NativeBridge.runBytecode(payload.getBytes()); // แนะนำให้ส่งเป็น byte[] เพื่อความชัวร์ในฝั่ง C++
            }
        });
    }
}
