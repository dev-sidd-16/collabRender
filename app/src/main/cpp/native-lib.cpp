#include <jni.h>
#include <string>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/imgcodecs/imgcodecs.hpp>

using namespace cv;
extern "C"
JNIEXPORT void JNICALL
Java_com_example_siddprakash_collabar_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */,
        jlong addrRgba, jlong addrGray) {


    Mat &mRgb = *(Mat*)addrRgba;
    Mat &mGray = *(Mat*)addrGray;

    cvtColor(mRgb, mGray, CV_RGB2GRAY);

//    Mat img = imread("/mnt/sdcard/Android/Data/CollabAR/image.jpg");

    /*
    mGray.data = img.data;
    mGray = img;
    cvCopy(&img, &mGray);
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
     */
    return;

}
