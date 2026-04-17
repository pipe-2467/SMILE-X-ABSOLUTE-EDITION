#include <jni.h>
#include <stdint.h>
#include <string>
#include <android/log.h>
#include "offsets.h"

#define LOG_TAG "BFL_NATIVE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// ฟังก์ชันเปลี่ยนสิทธิ์ (Identity 8)
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

    // เชื่อมต่อกับ NativeBridge.runBytecode (รับ byte[] จาก FloatingService)
    JNIEXPORT void JNICALL
    Java_com_smilex_absolute_NativeBridge_runBytecode(JNIEnv* env, jobject thiz, jbyteArray data) {
        jbyte* buffer = env->GetByteArrayElements(data, NULL);
        jsize size = env->GetArrayLength(data);

        if (buffer != nullptr) {
            LOGI("Fl0WERk1ng: Received script size %d bytes", size);
            // ตรงนี้คือจุดที่ลูกพี่จะเอา buffer ไปรันใน VM ของ Roblox
        }

        env->ReleaseByteArrayElements(data, buffer, JNI_ABORT);
    }
}
