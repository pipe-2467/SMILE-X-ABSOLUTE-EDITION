package com.smilex.absolute; // เก็บไว้! ห้ามลบ เพราะโปรเจกต์ลูกพี่ใช้แบบนี้

public class NativeBridge {
    static {
        System.loadLibrary("smilex");
    }
    // เพิ่มอันนี้เข้าไป
    public static native void applyIdentity(long luaPtr);
    // แก้จาก byte[] data เป็น String code
    public static native void runBytecode(String code);
}
