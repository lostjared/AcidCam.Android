#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include"com_lostsidedead_acidcam_AcidCam_Filter.h"
#include<vector>
#include "ac.h"
using namespace cv;



JNIEXPORT void JNICALL Java_com_lostsidedead_acidcam_AcidCam_1Filter_Filter
  (JNIEnv *env, jobject pObject, jint filter, jlong frame) {

	Mat& mRgb = *(Mat*)frame;

	if(filter >= 23) {
		current_filterx = filter-23;
		filter = 23;
	}

	ac::draw_func[filter](mRgb);

}

void custom_filter(cv::Mat &frame) {

}
