# 1. กำหนดเวอร์ชันขั้นต่ำของ CMake (ปกติใช้ 3.10.2 หรือสูงกว่า)
cmake_minimum_required(VERSION 3.10.2)

# 2. ตั้งชื่อโปรเจกต์
project("smilex-lib")

# 3. สร้าง Library หลัก (ให้ชื่อว่า smilex เพื่อให้ตรงกับ System.loadLibrary("smilex"))
add_library(
    # ชื่อ Library ที่ Java จะมองเห็น (libsmilex.so)
    smilex
    
    # กำหนดให้เป็น Shared Library (.so)
    SHARED
    
    # ระบุไฟล์ C++ ทั้งหมดที่ต้องนำมาคอมไพล์ (ถ้ามีหลายไฟล์ให้ใส่ต่อกัน)
    core.cpp
)

# 4. ค้นหา Library สำคัญของระบบ Android เพื่อนำมาใช้
# - log: สำหรับใช้ __android_log_print
# - android: สำหรับฟังก์ชันพื้นฐาน
find_library(
    log-lib
    log
)

# 5. เชื่อมต่อ Library เข้าด้วยกัน (Linking)
target_link_libraries(
    # ตัวหลักของเรา
    smilex
    
    # เชื่อมต่อกับระบบ Log ของ Android
    ${log-lib}
    
    # เชื่อมต่อกับระบบจัดการหน่วยความจำ (ถ้ามีการใช้ฟังก์ชันพิเศษบางตัว)
    android
)

# 6. (เพิ่มเติม) กำหนดค่าการ Compile เพื่อความแม่นยำ
# -fexceptions: ยอมรับการจัดการ error แบบ exception
# -frtti: เปิดใช้งาน Run-Time Type Information
set_target_properties(
    smilex
    PROPERTIES
    LINKER_LANGUAGE CXX
)
