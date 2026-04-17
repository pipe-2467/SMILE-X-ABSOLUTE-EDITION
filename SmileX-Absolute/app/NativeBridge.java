package com.smilex.absolute;

public class NativeBridge {
    static {
        // ชื่อต้องตรงกับ LOCAL_MODULE ใน Android.mk
        System.loadLibrary("smilex");
    }

    // ฟังก์ชันปลดล็อก Identity 8 (God Mode)
    public static native void applyIdentity(long luaPtr);

    // ฟังก์ชันส่งสคริปต์ไปรัน (รับเป็นข้อความ String)
    public static native void runBytecode(String code);
}
