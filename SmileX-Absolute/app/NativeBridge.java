package com.smilex.absolute;

public class NativeBridge {
    static {
        // ต้องตรงกับ LOCAL_MODULE ใน Android.mk เป๊ะๆ
        System.loadLibrary("smilex");
    }

    // ฟังก์ชันปลดล็อก Identity 8
    public static native void applyIdentity(long luaPtr);

    // ฟังก์ชันรันโค้ด รับเป็น byte[] เพื่อให้ตรงกับ FloatingService
    public static native void runBytecode(byte[] data);
}
