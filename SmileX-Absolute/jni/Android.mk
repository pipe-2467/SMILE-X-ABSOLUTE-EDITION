LOCAL_PATH := $(call my-dir)

# ล้างค่าตัวแปรเก่า
include $(CLEAR_VARS)

# ชื่อ Library (ต้องตรงกับ System.loadLibrary("smilex"))
LOCAL_MODULE    := smilex

# ระบุไฟล์ Source Code ทั้งหมด
LOCAL_SRC_FILES := core.cpp

# เชื่อมต่อ Library ระบบ (Log และ Android)
LOCAL_LDLIBS    := -llog -landroid

# สั่งให้สร้างเป็น Shared Library (.so)
include $(BUILD_SHARED_LIBRARY)
