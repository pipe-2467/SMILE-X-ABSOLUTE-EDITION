#define _GNU_SOURCE
#include <jni.h>
#include <string>
#include <android/log.h>
#include <dirent.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/uio.h>

#define LOG_TAG "BFL_LOG"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

long global_lua_ptr = 0;
int target_pid = -1;

// ฟังก์ชันหา PID
int get_roblox_pid() {
    DIR* dir = opendir("/proc");
    if (!dir) return -1;
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
                    if (strstr(cmdline, "com.roblox.client")) {
                        fclose(f); closedir(dir);
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

// ฟังก์ชันหา Base Address
long get_module_base(const char* module_name) {
    target_pid = get_roblox_pid();
    if (target_pid == -1) return 0;

    char maps_path[256];
    snprintf(maps_path, sizeof(maps_path), "/proc/%d/maps", target_pid);
    FILE* fp = fopen(maps_path, "r");
    if (!fp) return 0;

    char line[512];
    long base = 0;
    while (fgets(line, sizeof(line), fp)) {
        if (strcasestr(line, module_name) || strcasestr(line, "roblox")) {
            base = strtoul(line, NULL, 16);
            if (base > 0x1000000) break;
        }
    }
    fclose(fp);
    return base;
}

// --- ฟังก์ชัน JNI เริ่มต้นตรงนี้ ---

extern "C" JNIEXPORT jlong JNICALL
Java_com_smilex_absolute_NativeBridge_autoAttach(JNIEnv* env, jclass clazz) {
    LOGD("BFL: Starting Emerald Blade Connection...");
    long base = get_module_base("libroblox.so");
    if (base > 0) {
        // อ้างอิงจาก GitHub ของเวอร์ชัน 2.716.875
        long current_offset = 0x3BC1A2; 
        global_lua_ptr = base + current_offset; 
        LOGD("BFL: Connection Established! 0x%lx", global_lua_ptr);
        return global_lua_ptr;
    }
    LOGE("BFL: Attach Failed.");
    return 0;
}

// 🔴 เพิ่มฟังก์ชันนี้เพื่อไม่ให้ Java Crash
extern "C" JNIEXPORT void JNICALL
Java_com_smilex_absolute_NativeBridge_applyIdentity(JNIEnv* env, jclass clazz, jlong luaPtr) {
    LOGD("BFL: Applying Identity to Ptr: 0x%llx", luaPtr);
    // ใส่ Logic การแก้ Identity ตรงนี้
}

extern "C" JNIEXPORT void JNICALL
Java_com_smilex_absolute_NativeBridge_runBytecode(JNIEnv* env, jclass clazz, jstring code) {
    if (global_lua_ptr == 0) {
        LOGE("BFL: Not attached to game.");
        return;
    }
    const char* nativeCode = env->GetStringUTFChars(code, nullptr);
    LOGD("BFL: Executing: %s", nativeCode);
    
    // Logic การส่ง Bytecode เข้า VM
    
    env->ReleaseStringUTFChars(code, nativeCode);
}
