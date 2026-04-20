#include <jni.h>
#include <string>
#include <android/log.h>
#include <unistd.h>

#define LOG_TAG "BFL_LOG"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

long global_lua_ptr = 0;

long get_module_base(const char* name) {
    FILE* fp = fopen("/proc/self/maps", "r");
    if (!fp) return 0;
    char line[512];
    long base = 0;
    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, name)) {
            base = strtoul(line, NULL, 16);
            break; 
        }
    }
    fclose(fp);
    return base;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_smilex_absolute_NativeBridge_autoAttach(JNIEnv* env, jclass clazz) {
    LOGD("BFL: Scanning for LibRoblox.so...");
    
    // เปลี่ยนมาใช้ชื่อที่ลูกพี่หาเจอ
    long base = get_module_base("LibRoblox.so");

    // เช็ค Address ว่าต้องไม่ใช่ขยะ (ต้องมีค่าสูงพอใน RAM)
    if (base > 0x1000000) { 
        LOGD("BFL: Target Acquired at 0x%lx", base);
        global_lua_ptr = base + 0x1234567; // แก้ Offset ให้ตรงเวอร์ชัน
        return global_lua_ptr;
    }

    global_lua_ptr = 0;
    return 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_smilex_absolute_NativeBridge_runBytecode(JNIEnv* env, jclass clazz, jstring code) {
    if (global_lua_ptr == 0) return;
    const char* nativeCode = env->GetStringUTFChars(code, nullptr);
    LOGD("BFL: Running Script via Native...");
    
    // โค้ดส่งสคริปต์เข้าระบบ Lua ของลูกพี่
    
    env->ReleaseStringUTFChars(code, nativeCode);
}
