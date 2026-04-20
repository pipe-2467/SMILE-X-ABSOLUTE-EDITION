#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "BFL_LOG"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

long global_lua_ptr = 0;

// ฟังก์ชันหาตำแหน่ง Module ที่แม่นยำขึ้น
long get_module_base(const char* name) {
    FILE* fp = fopen("/proc/self/maps", "r");
    if (!fp) return 0;
    char line[512];
    long base = 0;
    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, name) && strstr(line, "r-xp")) { // ต้องเป็นโซนที่รันโค้ดได้ (executable)
            base = strtoul(line, NULL, 16);
            break;
        }
    }
    fclose(fp);
    return base;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_smilex_absolute_NativeBridge_autoAttach(JNIEnv* env, jclass clazz) {
    LOGD("BFL: Scanning for real Roblox process...");
    
    // ค้นหา libmain.so
    long base = get_module_base("libmain.so");

    // 🔴 จุดสำคัญ: เช็คว่า Base Address ต้องอยู่ในช่วงที่ถูกต้อง (ไม่ใช่เลขมั่วๆ หรือเลขน้อยเกินไป)
    if (base > 0x10000000) { 
        LOGD("BFL: Target Found! Base: 0x%lx", base);
        
        // ตรงนี้ลูกพี่ต้องบวก Offset จริงๆ นะครับ 
        // ถ้าใส่เลขมั่ว (เช่น 0x123) แล้วรันสคริปต์ เกมจะเด้งทันที
        global_lua_ptr = base + 0x1234567; 
        return global_lua_ptr;
    }

    // ถ้าไม่เข้าเงื่อนไข ให้คืนค่า 0 เพื่อให้ Java รู้ว่า "ยังไม่เจอเกม"
    LOGE("BFL: Roblox is not running or not ready.");
    global_lua_ptr = 0;
    return 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_smilex_absolute_NativeBridge_runBytecode(JNIEnv* env, jclass clazz, jstring code) {
    // เช็คอีกรอบก่อนรันว่าเรามีตั๋วเข้ารหัสเกมจริงๆ หรือไม่
    if (global_lua_ptr == 0) {
        LOGE("BFL: Execution blocked! Not attached to Roblox.");
        return;
    }

    const char* nativeCode = env->GetStringUTFChars(code, nullptr);
    LOGD("BFL: Executing... %s", nativeCode);
    
    // TODO: ใส่ฟังก์ชันส่งโค้ดเข้า Lua VM ของลูกพี่ที่นี่
    
    env->ReleaseStringUTFChars(code, nativeCode);
}
