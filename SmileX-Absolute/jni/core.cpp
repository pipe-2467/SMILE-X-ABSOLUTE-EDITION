#include <jni.h>
#include <stdint.h>
#include <string>
#include "offsets.h"

// --- ส่วนดั้งเดิมของลูกพี่ ---
// ฟังก์ชันหลักในการเปลี่ยนสิทธิ์เป็น Identity 8
void set_identity(uintptr_t L, int level) {
    if (L != 0) {
        // ใช้พิกัด IDENTITY_OFFSET (0x48) จาก offsets.h
        int* id_ptr = (int*)(L + IDENTITY_OFFSET);
        *id_ptr = level;
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
        // แปลงข้อความจาก Java เป็น C++
        const char* nativeCode = env->GetStringUTFChars(code, 0);
        
        if (nativeCode != nullptr) {
            // [ Logic สำหรับส่งสคริปต์เข้าสู่ VM ]
            // ลูกพี่นำ nativeCode ไปรันผ่านฟังก์ชัน Execute ของเกมได้เลย
        }
        
        // คืนค่าหน่วยความจำ
        env->ReleaseStringUTFChars(code, nativeCode);
    }
}
