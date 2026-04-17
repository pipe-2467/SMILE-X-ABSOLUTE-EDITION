#include <jni.h>
#include <stdint.h>
#include <string>
#include <android/log.h>
#include "offsets.h"

#define LOG_TAG "SMILE-X_NATIVE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// ฟังก์ชันเปลี่ยนสิทธิ์เป็น Identity 8
void set_identity(uintptr_t L, int level) {
    if (L != 0) {
        int* id_ptr = (int*)(L + IDENTITY_OFFSET);
        *id_ptr = level;
        LOGI("Identity changed to: %d", level);
    }
}

extern "C" {

    // เชื่อมต่อกับ NativeBridge.applyIdentity
    JNIEXPORT void JNICALL
    Java_com_smilex_absolute_NativeBridge_applyIdentity(JNIEnv* env, jobject thiz, jlong lua_ptr) {
        set_identity((uintptr_t)lua_ptr, 8);
    }

    // เชื่อมต่อกับ NativeBridge.runBytecode (รับเป็น byte array)
    JNIEXPORT void JNICALL
    Java_com_smilex_absolute_NativeBridge_runBytecode(JNIEnv* env, jobject thiz, jbyteArray data) {
        // ดึงข้อมูลจาก byte array ใน Java มาเป็นหน่วยความจำใน C++
        jbyte* buffer = env->GetByteArrayElements(data, NULL);
        jsize size = env->GetArrayLength(data);

        if (buffer != nullptr) {
            LOGI("Received bytecode size: %d bytes", size);
            
            // --- [ ใส่ Logic รัน Bytecode ของลูกพี่ตรงนี้ ] ---
            // นำ buffer (ข้อมูล) และ size (ขนาด) ไปใช้รันใน VM
        }

        // คืนค่าหน่วยความจำป้องกันแอปเด้ง
        env->ReleaseByteArrayElements(data, buffer, JNI_ABORT);
    }
}
