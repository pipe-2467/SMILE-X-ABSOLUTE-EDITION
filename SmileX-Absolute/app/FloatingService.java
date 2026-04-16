package com.smilex.absolute;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.view.WindowManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;

// *** เพิ่มบรรทัดนี้เข้าไปครับ ***
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
        // ตอนนี้มันจะรู้จัก R.layout.floating_menu แล้ว
        v = LayoutInflater.from(this).inflate(R.layout.floating_menu, null);
        
        // และจะรู้จัก R.id.btnRun กับ R.id.editScript ด้วย
        Button runBtn = v.findViewById(R.id.btnRun);
        final EditText input = v.findViewById(R.id.editScript);

        runBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String code = input.getText().toString();
                String payload = "loadstring([[" + code + "]])()";
                
                // อย่าลืมเรื่อง NativeBridge ถ้ายังไม่ได้สร้างไฟล์แยกไว้
                // NativeBridge.runBytecode(payload.getBytes());
            }
        });
    }
}
