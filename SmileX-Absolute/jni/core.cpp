#include <jni.h>
#include <string>
#include <android/log.h>
#include <dirent.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#define LOG_TAG "BFL_LOG"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

long global_lua_ptr = 0;

// 1. ฟังก์ชันหา PID ของ Roblox (รองรับ Package Name มาตรฐาน)
int get_roblox_pid() {
    DIR* dir = opendir("/proc");
    if (!dir) {
        LOGE("BFL: ไม่สามารถเปิดโฟลเดอร์ /proc ได้!");
        return -1;
    }

    struct dirent* entry;
    while ((entry = readdir(dir)) != NULL) {
        int pid = atoi(entry->d_name);
        if (pid > 0) {
            char path[256];
            snprintf(path, sizeof(path), "/proc/%d/cmdline", pid);
            FILE* f = fopen(path, "r");
            if (f) {
                char cmdline[256];
                if (fgets(cmdline, sizeof(cmdline), f)) {
                    // ตรวจสอบชื่อ Package (ปกติคือ com.roblox.client)
                    if (strstr(cmdline, "com.roblox.client")) {
                        fclose(f);
                        closedir(dir);
                        LOGD("BFL: พบ Roblox PID: %d", pid);
                        return pid;
                    }
                }
                fclose(f);
            }
        }
    }
    closedir(dir);
    return -1;
}

// 2. ฟังก์ชันหา Base Address ของ LibRoblox.so
long get_module_base(const char* module_name) {
    int pid = get_roblox_pid();
    if (pid == -1) {
        LOGE("BFL: ไม่พบ Process ของ Roblox กรุณาเปิดเกมก่อน!");
        return 0;
    }

    char maps_path[256];
    snprintf(maps_path, sizeof(maps_path), "/proc/%d/maps", pid);
    
    FILE* fp = fopen(maps_path, "r");
    if (!fp) {
        // ถ้าเข้าตรงนี้ แปลว่าเจอ PID แต่ Android บล็อกไม่ให้อ่านไฟล์ maps ของแอปอื่น
        LOGE("BFL: Permission Denied! เข้าถึงหน่วยความจำ Roblox ไม่ได้ (ต้องใช้ Virtual Space หรือ Root)");
        return 0;
    }

    char line[512];
    long base = 0;
    while (fgets(line, sizeof(line), fp)) {
        // ใช้ strcasestr เพื่อหาแบบไม่สนตัวพิมพ์เล็ก-ใหญ่ (LibRoblox.so หรือ libroblox.so)
        if (strcasestr(line, module_name)) {
            base = strtoul(line, NULL, 16);
            LOGD("BFL: พบเป้าหมาย! %s อยู่ที่ 0x%lx", module_name, base);
            break;
        }
    }
    fclose(fp);
    return base;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_smilex_absolute_NativeBridge_autoAttach(JNIEnv* env, jclass clazz) {
    LOGD("BFL: เริ่มการเชื่อมต่อดาบมรกต...");
    
    // ค้นหาหัวใจหลักของ Roblox
    long base = get_module_base("LibRoblox.so");

    if (base > 0x1000000) {
        LOGD("BFL: เชื่อมต่อสำเร็จ!");
        // หมายเหตุ: Offset 0x1234567 ต้องเปลี่ยนตามเวอร์ชันของ Roblox ที่ลูกพี่ใช้นะครับ
        global_lua_ptr = base + 0x1234567; 
        return global_lua_ptr;
    }

    LOGE("BFL: เชื่อมต่อล้มเหลว (Not Found หรือโดนบล็อก)");
    global_lua_ptr = 0;
    return 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_smilex_absolute_NativeBridge_runBytecode(JNIEnv* env, jclass clazz, jstring code) {
    if (global_lua_ptr == 0) {
        LOGE("BFL: ไม่สามารถรันสคริปต์ได้ เพราะยังไม่ได้ Attach!");
        return;
    }

    const char* nativeCode = env->GetStringUTFChars(code, nullptr);
    LOGD("BFL: กำลังส่งสคริปต์เข้า Native Layer...");
    
    // TODO: ใส่ฟังก์ชันส่ง Bytecode เข้า VM ของ Roblox ที่นี่
    
    env->ReleaseStringUTFChars(code, nativeCode);
}
