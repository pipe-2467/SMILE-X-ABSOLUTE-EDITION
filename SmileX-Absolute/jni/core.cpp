#include <jni.h>
#include <stdint.h>
#include <string>
#include <android/log.h> // สำหรับดู Log เวลาบัค
#include "offsets.h"

// กำหนด Tag สำหรับดูใน Logcat (ชื่อมรกต)
#define LOG_TAG "SMILE-X_NATIVE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// ฟังก์ชันหลักในการเปลี่ยนสิทธิ์เป็น Identity 8
void set_identity(uintptr_t L, int level) {
    if (L != 0) {
        // ใช้ IDENTITY_OFFSET (0x48) จาก offsets.h
        int* id_ptr = (int*)(L + IDENTITY_OFFSET);
        *id_ptr = level;
        LOGI("Identity changed to: %d", level);
    }
}

extern "C" {

    // เชื่อมต่อกับ NativeBridge.java -> applyIdentity
    JNIEXPORT void JNICALL
    Java_com_smilex_absolute_NativeBridge_applyIdentity(JNIEnv* env, jobject thiz, jlong lua_ptr) {
        set_identity((uintptr_t)lua_ptr, 8);
    }

    // เชื่อมต่อกับ NativeBridge.java -> runBytecode
    JNIEXPORT void JNICALL
    Java_com_smilex_absolute_NativeBridge_runBytecode(JNIEnv* env, jobject thiz, jstring code) {
        const char* nativeCode = env->GetStringUTFChars(code, 0);
        
        if (nativeCode != nullptr) {
            LOGI("Executing Script: %s", nativeCode);
            
            // --- [ จุดที่ลูกพี่ต้องทำต่อ ] ---
            // ลูกพี่ต้องมีฟังก์ชันสำหรับการ Execute สคริปต์ 
            // ตัวอย่างเช่น: 
            // TaskScheduler::get_singleton()->execute(nativeCode);
            
            // หมายเหตุ: ตรงนี้ต้องอาศัย Offset ของตัว Execute ในเกม 
            // ซึ่งลูกพี่ต้องหามาใส่เพิ่มใน offsets.h ครับ
        }

        env->ReleaseStringUTFChars(code, nativeCode);
    }
}
