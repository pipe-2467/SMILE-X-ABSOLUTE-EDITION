#include <jni.h>
#include <string>
#include <android/log.h>
#include <dirent.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

// ตั้งค่า Log สำหรับดูใน Logcat
#define LOG_TAG "BFL_LOG"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ตัวแปรเก็บตำแหน่ง Memory หลัก
long global_lua_ptr = 0;

/**
 * ฟังก์ชันหา PID ของ Roblox
 * โดยการไล่เช็คทุก Process ใน /proc/
 */
int get_roblox_pid() {
    DIR* dir = opendir("/proc");
    if (!dir) {
        LOGE("BFL: Cannot open /proc directory!");
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
                    // เช็ค Package Name มาตรฐานของ Roblox
                    if (strstr(cmdline, "com.roblox.client")) {
                        fclose(f);
                        closedir(dir);
                        LOGD("BFL: Found Roblox PID: %d", pid);
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
 * ฟังก์ชันหา Base Address ของ Engine เกม
 * ปรับปรุงใหม่ให้ค้นหาแบบยืดหยุ่น (Case-Insensitive)
 */
long get_module_base(const char* module_name) {
    int pid = get_roblox_pid();
    if (pid == -1) {
        LOGE("BFL: Roblox process not found! (Make sure game is open)");
        return 0;
    }

    char maps_path[256];
    snprintf(maps_path, sizeof(maps_path), "/proc/%d/maps", pid);
    
    FILE* fp = fopen(maps_path, "r");
    if (!fp) {
        LOGE("BFL: Permission Denied! Cannot read maps for PID %d", pid);
        LOGE("BFL: Suggestion: Run app in Virtual Space or use Shizuku.");
        return 0;
    }

    char line[512];
    long base = 0;
    while (fgets(line, sizeof(line), fp)) {
        // ค้นหาแบบกวาดล้าง: เช็คทั้งชื่อเต็มและคำที่เกี่ยวข้อง
        if (strcasestr(line, module_name) || strcasestr(line, "roblox")) {
            // ดึงค่า Address ตัวแรกของบรรทัด (Base Address)
            base = strtoul(line, NULL, 16);
            if (base > 0x1000000) { // ป้องกันพวกตัวเลขขยะขนาดเล็ก
                LOGD("BFL: Target Found! Base: 0x%lx", base);
                LOGD("BFL: From Line: %s", line);
                break;
            }
        }
    }
    fclose(fp);
    return base;
}

/**
 * ฟังก์ชันหลักที่ Java จะเรียกใช้ตอนกดปุ่ม ATTACH
 */
extern "C" JNIEXPORT jlong JNICALL
Java_com_smilex_absolute_NativeBridge_autoAttach(JNIEnv* env, jclass clazz) {
    LOGD("BFL: Starting Emerald Blade Connection...");
    
    // ใช้ชื่อไฟล์ล่าสุดที่ลูกพี่เช็คมาคือ libroblox.so
    long base = get_module_base("libroblox.so");

    if (base > 0) {
        LOGD("BFL: Connection Established!");
        
        // 🔴 จุดนี้ต้องนำเลขจาก GitHub (NtReadVirtualMemory) มาใส่
        // สมมติเลขเวอร์ชัน 2.716.875 คือ 0x3BC1A2
        long current_offset = 0x3BC1A2; // <--- แก้เลขนี้ตาม GitHub ครับ
        
        global_lua_ptr = base + current_offset; 
        return global_lua_ptr;
    }

    LOGE("BFL: Attach Failed. Check Logcat for details.");
    global_lua_ptr = 0;
    return 0;
}

/**
 * ฟังก์ชันสำหรับส่งสคริปต์เข้าระบบ Native
 */
extern "C" JNIEXPORT void JNICALL
Java_com_smilex_absolute_NativeBridge_runBytecode(JNIEnv* env, jclass clazz, jstring code) {
    if (global_lua_ptr == 0) {
        LOGE("BFL: Execution blocked. Not attached to game.");
        return;
    }

    const char* nativeCode = env->GetStringUTFChars(code, nullptr);
    LOGD("BFL: Sending Bytecode: %s", nativeCode);
    
    // ตรงนี้คือส่วนที่ลูกพี่จะเชื่อมต่อกับ Lua VM 
    // โดยใช้ตำแหน่งจาก global_lua_ptr
    
    env->ReleaseStringUTFChars(code, nativeCode);
}
