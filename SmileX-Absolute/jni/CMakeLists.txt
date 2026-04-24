cmake_minimum_required(VERSION 3.10.2)

project("smilex-project")

# สร้าง Library ชื่อ smilex จากไฟล์ core.cpp
add_library(
    smilex
    SHARED
    core.cpp
)

# ค้นหา Library สำหรับทำ Log
find_library(
    log-lib
    log
)

# เชื่อมต่อ Library เข้าด้วยกัน
target_link_libraries(
    smilex
    ${log-lib}
    android
)

# ตั้งค่าให้รองรับ C++ มาตรฐานเดียวกับเพื่อนเก่า
set_target_properties(smilex PROPERTIES LINKER_LANGUAGE CXX)
