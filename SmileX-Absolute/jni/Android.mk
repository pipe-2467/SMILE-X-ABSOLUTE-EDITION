LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

# ชื่ออาวุธของเรา
LOCAL_MODULE    := smilex
LOCAL_SRC_FILES := core.cpp
LOCAL_LDLIBS    := -llog -landroid

include $(BUILD_SHARED_LIBRARY)
