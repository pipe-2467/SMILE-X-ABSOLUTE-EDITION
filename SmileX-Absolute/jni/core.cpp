#include <jni.h>
#include <stdint.h>
#include <string>
#include "offsets.h"

// ฟังก์ชันหลักในการเปลี่ยนสิทธิ์เป็น Identity 8
void set_identity(uintptr_t L, int level) {
    if (L != 0) {
        int* id_ptr = (int*)(L + IDENTITY_OFFSET);
        *id_ptr = level;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_smilex_absolute_NativeBridge_applyIdentity(JNIEnv* env, jobject thiz, jlong lua_ptr) {
    set_identity((uintptr_t)lua_ptr, 8);
}

extern "C" JNIEXPORT void JNICALL
Java_com_smilex_absolute_NativeBridge_runBytecode(JNIEnv* env, jobject thiz, jstring code) {
    const char* nativeCode = env->GetStringUTFChars(code, 0);
    // Logic สำหรับส่ง Bytecode เข้าสู่ VM
    env->ReleaseStringUTFChars(code, nativeCode);
}

