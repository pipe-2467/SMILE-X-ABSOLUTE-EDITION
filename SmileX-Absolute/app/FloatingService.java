package com.smilex.absolute;

import android.app.Service;
import android.view.WindowManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;

public class FloatingService extends Service {
    private WindowManager wm;
    private View v;

    @Override
    public void onCreate() {
        super.onCreate();
        v = LayoutInflater.from(this).inflate(R.layout.floating_menu, null);
        // โค้ดสำหรับแสดงหน้าต่างลอยสีเขียวมรกต
        Button runBtn = v.findViewById(R.id.btnRun);
        final EditText input = v.findViewById(R.id.editScript);

        runBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String code = input.getText().toString();
                // หุ้มด้วย loadstring เพื่อรัน ServerScript อิสระ
                String payload = "loadstring([[" + code + "]])()";
                NativeBridge.runBytecode(payload);
            }
        });
    }
}

