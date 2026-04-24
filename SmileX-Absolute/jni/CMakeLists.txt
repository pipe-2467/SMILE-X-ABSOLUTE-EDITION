# กำหนดเวอร์ชันขั้นต่ำของ CMake
cmake_minimum_required(VERSION 3.10.2)

# ชื่อโปรเจกต์หลัก
project("smilex_native")

# ตั้งค่า Compiler Flags แบบจัดเต็ม
# -O3: เพื่อความเร็วสูงสุดในการรัน
# -Wall: ให้โชว์คำเตือนทั้งหมดตอนคอมไพล์
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -O3 -Wall -fexceptions -frtti")

# เพิ่มตำแหน่งที่ CMake จะไปควานหา Header Files (ถ้ามี)
include_directories(${CMAKE_CURRENT_SOURCE_DIR})

# สร้าง Shared Library ชื่อ smilex
add_library(
    # ชื่อ Library ที่ Java จะเรียก (libsmilex.so)
    smilex
    
    # ประเภทของ Library
    SHARED
    
    # ไฟล์ต้นฉบับ (ใส่ให้ครบทุกไฟล์)
    core.cpp
)

# ค้นหา Library สำหรับระบบ Log ของ Android
find_library(
    log-lib
    log
)

# ค้นหา Library สำหรับจัดการ Native Windows/Memory ของ Android
find_library(
    android-lib
    android
)

# เชื่อมต่อ (Link) ทุกอย่างเข้าด้วยกัน
target_link_libraries(
    # ตัวแอปหลักของเรา
    smilex
    
    # เชื่อมกับระบบ Log (__android_log_print)
    ${log-lib}
    
    # เชื่อมกับระบบ Android พื้นฐาน
    ${android-lib}
    
    # เชื่อมกับ Library มาตรฐาน C++
    m
    z
)

# ปรับจูนคุณสมบัติของไฟล์ .so ที่ออกมา
set_target_properties(
    smilex
    PROPERTIES
    LINKER_LANGUAGE CXX
    OUTPUT_NAME "smilex"
)
