LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := SmileXCore
LOCAL_SRC_FILES := core.cpp
LOCAL_LDLIBS    := -llog -landroid

include $(BUILD_SHARED_LIBRARY)
