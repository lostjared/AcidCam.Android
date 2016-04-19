package com.lostsidedead.acidcam;

public class AcidCam_Filter {

	public AcidCam_Filter() {
		
	}
	
	public void load() {
		System.loadLibrary("AcidCam");
	}
	
	
	public native void Filter(int filter, long frame);
	
}
