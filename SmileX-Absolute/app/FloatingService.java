package com.smilex.absolute;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.view.WindowManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;

// บรรทัดนี้ห้ามหายเด็ดขาดครับ เพื่อให้รู้จัก R.layout และ R.id
import com.smilex.absolute.R; 

public class FloatingService extends Service {
    private WindowManager wm;
    private View v;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        // ถ้า import R ถูกต้อง บรรทัดนี้จะไม่แดง
        v = LayoutInflater.from(this).inflate(R.layout.floating_menu, null);

        Button runBtn = v.findViewById(R.id.btnRun);
        final EditText input = v.findViewById(R.id.editScript);

        runBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String code = input.getText().toString();
                String payload = "loadstring([[" + code + "]])()";
                
                // ตรวจสอบว่ามีคลาส NativeBridge ในโปรเจกต์ด้วยนะครับ
                try {
                    NativeBridge.runBytecode(payload.getBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
