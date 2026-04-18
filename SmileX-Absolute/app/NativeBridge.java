package com.smilex.absolute;

public class NativeBridge {
    static {
        System.loadLibrary("smilex");
    }

    // สแกนหาพิกัดเกมอัตโนมัติ (ไม่ต้องกรอกเอง)
    public static native long autoAttach();

    public static native void applyIdentity(long luaPtr);
    public static native void runBytecode(byte[] data);
}
