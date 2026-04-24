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

#define LOG_TAG "BFL_LOG"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

long global_lua_ptr = 0;
int roblox_pid = -1;

// ค้นหา PID ของ Roblox com.roblox.client
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

// ค้นหา Base Address ของ libroblox.so
long get_module_base(int pid, const char* module_name) {
    char maps_path[256];
    snprintf(maps_path, sizeof(maps_path), "/proc/%d/maps", pid);
    FILE* fp = fopen(maps_path, "r");
    if (!fp) return 0;
    char line[512];
    long base = 0;
    while (fgets(line, sizeof(line), fp)) {
        if (strcasestr(line, module_name) && strstr(line, "r-xp")) {
            base = strtoul(line, NULL, 16);
            break;
        }
    }
    fclose(fp);
    return base;
}

// ฟังก์ชันอ่านหน่วยความจำข้าม Process
bool read_mem(void* addr, void* buffer, size_t size) {
    struct iovec local[1], remote[1];
    local[0].iov_base = buffer; local[0].iov_len = size;
    remote[0].iov_base = addr; remote[0].iov_len = size;
    return (process_vm_readv(roblox_pid, local, 1, remote, 1, 0) == size);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_smilex_absolute_NativeBridge_autoAttach(JNIEnv* env, jclass clazz) {
    roblox_pid = get_roblox_pid();
    if (roblox_pid == -1) return 0;
    long base = get_module_base(roblox_pid, "libroblox.so");
    if (base > 0) {
        long offset = 0x3BC1A2; // เลขสมมติจาก GitHub ลูกพี่ต้องแก้ตามจริง
        global_lua_ptr = base + offset;
        return global_lua_ptr;
    }
    return 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_smilex_absolute_NativeBridge_applyIdentity(JNIEnv* env, jclass clazz, jlong luaPtr) {
    LOGD("BFL: Apply Identity at 0x%llx", luaPtr);
}

extern "C" JNIEXPORT void JNICALL
Java_com_smilex_absolute_NativeBridge_runBytecode(JNIEnv* env, jclass clazz, jstring code) {
    if (global_lua_ptr == 0) return;
    const char* c_code = env->GetStringUTFChars(code, nullptr);
    LOGD("BFL: Running: %s", c_code);
    env->ReleaseStringUTFChars(code, c_code);
}
