#include <jni.h>
#include <stdint.h>
#include <string>
#include <unistd.h>
#include <cstdio>
#include <android/log.h>
#include "offsets.h"

#define LOG_TAG "BFL_CORE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// ฟังก์ชันหา Base Address ของ Module เกม
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

    // ระบบค้นหาหัวใจเกม (LuaState) อัตโนมัติ
    JNIEXPORT jlong JNICALL
    Java_com_smilex_absolute_NativeBridge_autoAttach(JNIEnv* env, jobject thiz) {
        // เวอร์ชัน 2.714.1091 ส่วนใหญ่ใช้ libgame.so หรือ libreal.so
        uintptr_t base = get_module_base("libgame.so"); 
        if (base == 0) base = get_module_base("libreal.so");
        
        if (base == 0) return 0;

        uintptr_t lua_ptr_addr = base + LUASTATE_PTR;
        
        // Safety Check: ป้องกันการอ่านพิกัดที่ยังไม่โหลด
        if (lua_ptr_addr < 0x1000000) return 0;

        uintptr_t actual_lua_ptr = *(uintptr_t*)lua_ptr_addr;
        
        if (actual_lua_ptr == 0) {
            LOGI("BFL: Game found, but LuaState is NULL.");
            return 0;
        }

        LOGI("BFL: Successfully attached to 0x%lx", (long)actual_lua_ptr);
        return (jlong)actual_lua_ptr;
    }

    // ระบบปลดล็อก Identity 8
    JNIEXPORT void JNICALL
    Java_com_smilex_absolute_NativeBridge_applyIdentity(JNIEnv* env, jobject thiz, jlong lua_ptr) {
        if (lua_ptr != 0) {
            uintptr_t target_id_addr = (uintptr_t)lua_ptr + IDENTITY_OFFSET;
            
            // เขียนทับค่าสิทธิ์เป็น 8
            int* id_ptr = (int*)target_id_addr;
            *id_ptr = 8;
            
            LOGI("BFL: Identity set to 8 (Admin Power)");
        }
    }

    // ระบบส่งสคริปต์เข้า Engine
    JNIEXPORT void JNICALL
    Java_com_smilex_absolute_NativeBridge_runBytecode(JNIEnv* env, jobject thiz, jbyteArray data) {
        jbyte* buffer = env->GetByteArrayElements(data, NULL);
        jsize size = env->GetArrayLength(data);

        if (buffer != nullptr) {
            LOGI("BFL: Executing Script (Size: %d)", size);
            
            // [จุดเชื่อมต่อกับ Executor Engine ของลูกพี่]
            // ตัวอย่าง: r_execute((uintptr_t)global_L, (const char*)buffer);
        }

        env->ReleaseByteArrayElements(data, buffer, JNI_ABORT);
    }
}
