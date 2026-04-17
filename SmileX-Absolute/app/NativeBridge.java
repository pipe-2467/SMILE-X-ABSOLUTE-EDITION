package com.smilex.absolute;

public class NativeBridge {
    static {
        // ชื่อ library ต้องตรงกับใน Android.mk
        System.loadLibrary("smilex");
    }

    // สำหรับปลดล็อก Identity 8
    public static native void applyIdentity(long luaPtr);

    // สำหรับรันโค้ด (รับเป็น byte[] เพื่อแก้บัคที่ลูกพี่เจอ)
    public static native void runBytecode(byte[] data);
}
