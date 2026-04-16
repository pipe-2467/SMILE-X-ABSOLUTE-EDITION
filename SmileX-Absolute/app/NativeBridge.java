package com.smilex.absolute; // ตรวจสอบ package ให้ตรงกับโปรเจกต์

public class NativeBridge {
    static {
        System.loadLibrary("smilex"); // ชื่อ library ต้องตรงกับใน Android.mk
    }
    public static native void runBytecode(byte[] data);
}
