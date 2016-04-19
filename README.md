# AcidCam.Android

Project requires appcompat_v7 and OpenCV Library 3.1.0 
imported into your workspace and open.
In jni/Android.mk you should put the full path to your OpenCV.mk file
If you want to output a signed or unsigned package you should disable
lint error checking because the OpenCV library will not pass.
