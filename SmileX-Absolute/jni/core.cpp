#include <jni.h>
#include <stdint.h>
#include <string>
#include <unistd.h>
#include <cstdio>
#include <android/log.h>
#include "offsets.h"

#define LOG_TAG "BFL_CORE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// ฟังก์ชันหา Base Address ของ libgame.so (หรือ libreal.so ในบางเวอร์ชัน)
uintptr_t get_module_base(const char* module_name) {
    uintptr_t addr = 0;
    char line[1024];
    FILE* fp = fopen("/proc/self/maps", "rt");
    if (fp) {
        while (fgets(line, sizeof(line), fp)) {
            if (strstr(line, module_name)) {
                addr = (uintptr_t)strtoul(line, NULL, 16);
                break;
            }
        }
        fclose(fp);
    }
    return addr;
}

extern "C" {

    // ฟังก์ชันใหม่: ใช้เช็คว่าเกมรันอยู่ไหมและดึง LuaState มาโดยอัตโนมัติ
    JNIEXPORT jlong JNICALL
    Java_com_smilex_absolute_NativeBridge_autoAttach(JNIEnv* env, jobject thiz) {
        uintptr_t base = get_module_base("libgame.so"); // แก้ชื่อ lib ตามความจริง
        if (base == 0) return 0;
        
        // คำนวณหาที่อยู่จริงของ LuaState จากพิกัดที่ลูกพี่มีใน offsets.h
        uintptr_t lua_ptr_addr = base + LUASTATE_PTR;
        return (jlong)(*(uintptr_t*)lua_ptr_addr);
    }

    JNIEXPORT void JNICALL
    Java_com_smilex_absolute_NativeBridge_applyIdentity(JNIEnv* env, jobject thiz, jlong lua_ptr) {
        if (lua_ptr != 0) {
            int* id_ptr = (int*)((uintptr_t)lua_ptr + IDENTITY_OFFSET);
            *id_ptr = 8;
            LOGI("BFL: Identity 8 Activated");
        }
    }

    JNIEXPORT void JNICALL
    Java_com_smilex_absolute_NativeBridge_runBytecode(JNIEnv* env, jobject thiz, jbyteArray data) {
        jbyte* buffer = env->GetByteArrayElements(data, NULL);
        if (buffer != nullptr) {
            // [ Logic Execute ของลูกพี่ ]
            LOGI("BFL: Executing Command...");
        }
        env->ReleaseByteArrayElements(data, buffer, JNI_ABORT);
    }
}
