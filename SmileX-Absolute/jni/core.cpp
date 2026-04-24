#define _GNU_SOURCE
#include <jni.h>
#include <string>
#include <android/log.h>
#include <dirent.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/uio.h>
#include <string.h>

// --- Configuration ---
#define LOG_TAG "BFL_LOG"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global Variables
long global_lua_ptr = 0;
int roblox_pid = -1;

/**
 * 🕵️ ฟังก์ชันล่า PID ของ Roblox (Deep Scan)
 */
int get_roblox_pid() {
    DIR* dir = opendir("/proc");
    if (!dir) {
        LOGE("BFL: [Critical] Cannot open /proc directory!");
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
                    // เช็คทั้ง Package หลัก และเผื่อ Mod บางตัว
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

/**
 * 🔍 ฟังก์ชันหา Base Address ของ Engine (libroblox.so)
 */
long get_module_base(int pid, const char* module_name) {
    char maps_path[256];
    snprintf(maps_path, sizeof(maps_path), "/proc/%d/maps", pid);
    
    FILE* fp = fopen(maps_path, "r");
    if (!fp) {
        LOGE("BFL: [Permission Denied] Cannot read maps for PID %d", pid);
        return 0;
    }

    char line[512];
    long base = 0;
    while (fgets(line, sizeof(line), fp)) {
        // ค้นหาแบบไม่สนตัวพิมพ์เล็กใหญ่ และเน้นบรรทัดที่มีสิทธิ์ Execute (r-xp)
        if (strcasestr(line, module_name) && strstr(line, "r-xp")) {
            base = strtoul(line, NULL, 16);
            if (base > 0x1000000) {
                LOGD("BFL: [Found] libroblox.so at 0x%lx", base);
                break;
            }
        }
    }
    fclose(fp);
    return base;
}

/**
 * 📖 ฟังก์ชันอ่านหน่วยความจำข้าม Process
 */
bool read_memory(void* addr, void* buffer, size_t size) {
    struct iovec local[1];
    struct iovec remote[1];
    local[0].iov_base = buffer;
    local[0].iov_len = size;
    remote[0].iov_base = addr;
    remote[0].iov_len = size;

    return (process_vm_readv(roblox_pid, local, 1, remote, 1, 0) == size);
}

// --- JNI Interface (เชื่อมต่อกับ NativeBridge.java) ---

extern "C" JNIEXPORT jlong JNICALL
Java_com_smilex_absolute_NativeBridge_autoAttach(JNIEnv* env, jclass clazz) {
    LOGD("BFL: Starting Auto-Attach Sequence...");
    
    roblox_pid = get_roblox_pid();
    if (roblox_pid == -1) {
        LOGE("BFL: [Failed] Roblox is not running!");
        return 0;
    }

    // เจาะจงหา libroblox.so ตามที่ลูกพี่เช็คมาล่าสุด
    long base = get_module_base(roblox_pid, "libroblox.so");

    if (base > 0) {
        // 🔴 Offset สำหรับ v2.716.875 จาก GitHub
        long current_offset = 0x3BC1A2; 
        global_lua_ptr = base + current_offset;

        // ทดสอบการเข้าถึง Memory
        int test_buffer = 0;
        if (read_memory((void*)base, &test_buffer, sizeof(test_buffer))) {
            LOGD("BFL: [Success] Memory access granted!");
        } else {
            LOGE("BFL: [Warning] Process VM Read failed. Permission issue?");
        }

        return global_lua_ptr;
    }

    LOGE("BFL: [Failed] libroblox.so NOT FOUND in memory!");
    return 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_smilex_absolute_NativeBridge_applyIdentity(JNIEnv* env, jclass clazz, jlong luaPtr) {
    LOGD("BFL: Applying Identity to: 0x%llx", luaPtr);
    // TODO: ใส่ Logic การแก้ระดับ Identity ของ Lua State
}

extern "C" JNIEXPORT void JNICALL
Java_com_smilex_absolute_NativeBridge_runBytecode(JNIEnv* env, jclass clazz, jstring code) {
    if (global_lua_ptr == 0) {
        LOGE("BFL: Cannot execute. Attach first!");
        return;
    }

    const char* native_code = env->GetStringUTFChars(code, nullptr);
    LOGD("BFL: [Execute] %s", native_code);
    
    // Logic สำหรับการส่งสคริปต์เข้า Lua VM โดยใช้ global_lua_ptr
    
    env->ReleaseStringUTFChars(code, native_code);
}
