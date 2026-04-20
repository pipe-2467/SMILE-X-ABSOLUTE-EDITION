#include <jni.h>
#include <string>
#include <android/log.h>
#include <unistd.h>
#include <vector>

#define LOG_TAG "BFL_LOG"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

long global_lua_ptr = 0;

// ฟังก์ชันหาตำแหน่งเกม (ปรับปรุงให้หาไฟล์ได้หลายชื่อ)
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
    LOGD("BFL: Scanning for Roblox modules...");
    
    // ลองหาจากหลายชื่อที่ Roblox ชอบใช้
    long base = get_module_base("libmain.so");
    if (base == 0) base = get_module_base("libreal.so");
    if (base == 0) base = get_module_base("libbase.so");

    if (base != 0) {
        LOGD("BFL: Found Module at 0x%lx", base);
        // ตรงนี้ต้องบวก Offset ของ LuaState จริงๆ ของลูกพี่เข้าไปด้วยนะ
        global_lua_ptr = base + 0x1234567; // ตัวอย่าง Offset
        return global_lua_ptr;
    }
    
    return 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_smilex_absolute_NativeBridge_applyIdentity(JNIEnv* env, jclass clazz, jlong luaPtr) {
    if (luaPtr == 0) return;
    LOGD("BFL: Applying Identity 8 to 0x%lx", luaPtr);
    // โค้ดแก้ Identity ของลูกพี่ใส่ตรงนี้
}

extern "C" JNIEXPORT void JNICALL
Java_com_smilex_absolute_NativeBridge_runBytecode(JNIEnv* env, jclass clazz, jstring code) {
    if (global_lua_ptr == 0) {
        LOGE("BFL: Cannot run, LuaState is NULL");
        return;
    }

    const char* nativeCode = env->GetStringUTFChars(code, nullptr);
    LOGD("BFL: Executing Script: %s", nativeCode);
    
    // โค้ดส่งสคริปต์เข้า Lua VM ของลูกพี่ใส่ตรงนี้
    
    env->ReleaseStringUTFChars(code, nativeCode);
}
