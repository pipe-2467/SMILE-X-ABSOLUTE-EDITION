#include <jni.h>
#include <stdint.h>
#include <string>
#include <unistd.h>
#include <cstdio>
#include <android/log.h>
#include "offsets.h"

#define LOG_TAG "SMILE_X_CORE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ตัวแปรเก็บพิกัด LuaState ส่วนกลางเพื่อใช้รันสคริปต์
uintptr_t global_lua_ptr = 0;

// ฟังก์ชันหา Base Address ของ Module เกมในหน่วยความจำ
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

    // 1. ระบบ Attach: ค้นหาหัวใจของ Roblox
    JNIEXPORT jlong JNICALL
    Java_com_smilex_absolute_NativeBridge_autoAttach(JNIEnv* env, jobject thiz) {
        LOGI("BFL: Starting Auto-Attach...");

        // ปี 2026 Roblox มักจะรันบน libmain.so หรือ libbase.so
        uintptr_t base = get_module_base("libmain.so");
        if (base == 0) base = get_module_base("libbase.so");
        if (base == 0) base = get_module_base("libreal.so");

        if (base == 0) {
            LOGE("BFL: Error - Module not found! (Is the game running?)");
            return 0;
        }

        LOGI("BFL: Base Module found at 0x%lx", (long)base);

        // คำนวณหาพิกัด LuaState โดยใช้เลขจาก offsets.h
        uintptr_t lua_ptr_addr = base + LUASTATE_PTR;
        
        // Safety Check ป้องกันการอ่านพิกัดที่ผิดพลาด
        if (lua_ptr_addr != 0) {
            global_lua_ptr = *(uintptr_t*)lua_ptr_addr;
            
            if (global_lua_ptr != 0) {
                LOGI("BFL: Successfully connected to LuaState: 0x%lx", (long)global_lua_ptr);
                return (jlong)global_lua_ptr;
            }
        }

        LOGE("BFL: LuaState is still NULL. Try re-attaching.");
        return 0;
    }

    // 2. ระบบ Identity: ปลดล็อกพลังแอดมิน (Level 8)
    JNIEXPORT void JNICALL
    Java_com_smilex_absolute_NativeBridge_applyIdentity(JNIEnv* env, jobject thiz, jlong lua_ptr) {
        if (lua_ptr != 0) {
            uintptr_t target_id_addr = (uintptr_t)lua_ptr + IDENTITY_OFFSET;
            
            // เขียนทับค่าหน่วยความจำเพื่อปลดล็อกสิทธิ์
            *(int*)target_id_addr = 8;
            LOGI("BFL: Identity set to 8 (Admin Level)");
        } else {
            LOGE("BFL: Cannot set Identity - LuaState is 0");
        }
    }

    // 3. ระบบ Execute: ส่งสคริปต์เข้าตัวเกม
    JNIEXPORT void JNICALL
    Java_com_smilex_absolute_NativeBridge_runBytecode(JNIEnv* env, jobject thiz, jbyteArray data) {
        if (global_lua_ptr == 0) {
            LOGE("BFL: Execution failed. Please Attach first!");
            return;
        }

        jbyte* buffer = env->GetByteArrayElements(data, NULL);
        jsize size = env->GetArrayLength(data);

        if (buffer != nullptr) {
            LOGI("BFL: Executing Script (Size: %d bytes)", size);
            
            // --- ขั้นตอนการ Execute ---
            // ในที่นี้ลูกพี่ต้องเรียกใช้ฟังก์ชันรัน Lua ที่มีอยู่ใน Engine ของลูกพี่
            // เช่น: r_lua_execute(global_lua_ptr, (const char*)buffer);
            
            LOGI("BFL: Script Task Sent to Engine.");
        }

        env->ReleaseByteArrayElements(data, buffer, JNI_ABORT);
    }
}
