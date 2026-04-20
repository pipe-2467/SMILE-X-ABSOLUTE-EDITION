#include <jni.h>
#include <string>
#include <android/log.h>
#include <dirent.h>
#include <stdio.h>
#include <stdlib.h>

#define LOG_TAG "BFL_LOG"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

long global_lua_ptr = 0;

// ฟังก์ชันหา PID ของ Roblox โดยเช็คจากชื่อ Package
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
                        fclose(f);
                        closedir(dir);
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

// ฟังก์ชันหา Base Address ของ LibRoblox.so
long get_module_base(const char* module_name) {
    int pid = get_roblox_pid();
    if (pid == -1) {
        LOGD("BFL: Roblox process not found!");
        return 0;
    }

    char maps_path[256];
    snprintf(maps_path, sizeof(maps_path), "/proc/%d/maps", pid);
    FILE* fp = fopen(maps_path, "r");
    if (!fp) {
        LOGD("BFL: Cannot open maps for PID %d (Permission Denied)", pid);
        return 0;
    }

    char line[512];
    long base = 0;
    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, module_name)) {
            base = strtoul(line, NULL, 16);
            break;
        }
    }
    fclose(fp);
    return base;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_smilex_absolute_NativeBridge_autoAttach(JNIEnv* env, jclass clazz) {
    LOGD("BFL: Searching for LibRoblox.so...");
    
    // ใช้ชื่อ LibRoblox.so ตามที่ลูกพี่เช็คเจอ
    long base = get_module_base("LibRoblox.so");

    if (base > 0) {
        LOGD("BFL: Target Found! Base: 0x%lx", base);
        global_lua_ptr = base + 0x1234567; // ใส่ Offset ของลูกพี่
        return global_lua_ptr;
    }
    return 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_smilex_absolute_NativeBridge_runBytecode(JNIEnv* env, jclass clazz, jstring code) {
    if (global_lua_ptr == 0) return;
    const char* nativeCode = env->GetStringUTFChars(code, nullptr);
    LOGD("BFL: Executing on LibRoblox: %s", nativeCode);
    // ... โค้ดส่งสคริปต์ ...
    env->ReleaseStringUTFChars(code, nativeCode);
}
