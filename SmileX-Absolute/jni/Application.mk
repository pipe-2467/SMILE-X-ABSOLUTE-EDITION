# สร้างไฟล์สำหรับทั้งสองสถาปัตยกรรม
APP_ABI := armeabi-v7a arm64-v8a

# กำหนดเวอร์ชัน Android ขั้นต่ำ
APP_PLATFORM := android-21

# ใช้ Standard C++ ล่าสุดเพื่อให้โค้ดไม่งอแง
APP_STL := c++_static
APP_CPPFLAGS := -frtti -fexceptions -std=c++11
