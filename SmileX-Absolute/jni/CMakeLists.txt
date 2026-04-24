cmake_minimum_required(VERSION 3.10.2)
project("smilex")

# ตั้งค่า Flags ให้แม่นยำ
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -O3 -fexceptions -frtti")

add_library(
    smilex
    SHARED
    core.cpp
)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(
    smilex
    ${log-lib}
    ${android-lib}
)
