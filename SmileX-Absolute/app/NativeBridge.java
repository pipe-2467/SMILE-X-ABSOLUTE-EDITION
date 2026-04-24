package com.smilex.absolute;
import android.util.Log;

public class NativeBridge {
    static {
        try {
            System.loadLibrary("smilex");
            Log.i("BFL_LOG", "✅ Library loaded!");
        } catch (UnsatisfiedLinkError e) {
            Log.e("BFL_LOG", "❌ Load failed: " + e.getMessage());
        }
    }
    public static native long autoAttach();
    public static native void applyIdentity(long luaPtr);
    public static native void runBytecode(String code);
}
