LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

ifdef OPENCV_ANDROID_SDK
  ifneq ("","$(wildcard $(OPENCV_ANDROID_SDK)/OpenCV.mk)")
    include ${OPENCV_ANDROID_SDK}/OpenCV.mk
  else
    include ${OPENCV_ANDROID_SDK}/sdk/native/jni/OpenCV.mk
  endif
else
 # full path is here because I cannot seem to get it to work with realative path
 include /Volumes/LostSideDrive/Android/workspace/OpenCV-android-sdk/sdk/native/jni/OpenCV.mk
 
endif

LOCAL_MODULE    := AcidCam
LOCAL_SRC_FILES := AcidCam.cpp ac.cpp fractal.cpp
LOCAL_LDLIBS +=  -llog -ldl

include $(BUILD_SHARED_LIBRARY)
