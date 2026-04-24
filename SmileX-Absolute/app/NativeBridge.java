package com.smilex.absolute;

import android.util.Log;

/**
 * 🔗 NativeBridge: สะพานเชื่อมระหว่าง Java (UI) และ C++ (Core Logic)
 * พัฒนาโดย: BlueCheese Bedrock (BFL Party)
 */
public class NativeBridge {
    
    private static final String TAG = "BFL_LOG";

    // 1. ส่วนการโหลด Library (.so)
    static {
        try {
            // ชื่อ "smilex" ต้องตรงกับที่ระบุใน CMakeLists.txt (libsmilex.so)
            System.loadLibrary("smilex");
            Log.i(TAG, "✅ [SUCCESS] Native Library 'smilex' loaded successfully!");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "❌ [ERROR] Could not load native library: " + e.getMessage());
            Log.e(TAG, "💡 ตรวจสอบว่าชื่อใน System.loadLibrary ตรงกับ CMake และสถาปัตยกรรม CPU (ABI) ถูกต้อง");
        } catch (Exception e) {
            Log.e(TAG, "❌ [ERROR] Unexpected error during library loading: " + e.getMessage());
        }
    }

    /**
     * 🟢 ฟังก์ชันค้นหา PID และ Base Address ของ Roblox (libroblox.so)
     * @return Address ของ Lua State หรือ Base Address (ตามที่คำนวณใน C++)
     */
    public static native long autoAttach();

    /**
     * 🔵 ฟังก์ชันสำหรับปรับระดับ Identity (สิทธิ์) ของสคริปต์
     * @param luaPtr ตำแหน่งหน่วยความจำของ Lua State
     */
    public static native void applyIdentity(long luaPtr);

    /**
     * 🔴 ฟังก์ชันส่งสคริปต์ในรูปแบบ String เข้าไปรันในเอนจิ้นของเกม
     * @param code สคริปต์ Luau ที่ต้องการรัน
     */
    public static native void runBytecode(String code);

    // --- Helper Methods (ตัวช่วยจัดการสถานะในฝั่ง Java) ---

    /**
     * ตรวจสอบว่าการเชื่อมต่อกับเกมสำเร็จหรือไม่
     * @param result ค่าที่ส่งกลับมาจาก autoAttach()
     */
    public static void checkStatus(long result) {
        if (result > 0) {
            Log.i(TAG, "🚀 [STATUS] Emerald Blade is READY. Ptr: 0x" + Long.toHexString(result));
        } else {
            Log.w(TAG, "⚠️ [STATUS] Emerald Blade is NOT FOUND. Please check if Roblox is open.");
        }
    }
}
