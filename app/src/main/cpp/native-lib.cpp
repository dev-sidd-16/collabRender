#include <jni.h>
#include <iostream>
#include <string>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/imgcodecs/imgcodecs.hpp>

using namespace cv;
using namespace std;

int numFeautresReference = 500;
int numFeaturesDest = 500;

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_siddprakash_collabar_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */,
        jlong addrRgba, jlong addrGray) {

    string hello = "";

    Mat &mRgb = *(Mat*)addrRgba;
    Mat &mGray = *(Mat*)addrGray;

    cvtColor(mRgb, mGray, CV_RGB2GRAY);

    Mat img = imread("/mnt/sdcard/Android/Data/CollabAR/image.jpg");
    Mat ref = imread("/mnt/sdcard/Android/Data/CollabAR/marker.jpg");

    // convert images to grayscale
    Mat imgG, refG;
    cvtColor(img, imgG, CV_RGB2GRAY);
    cvtColor(ref, refG, CV_RGB2GRAY);

    if( !imgG.data || !refG.data )
    {
        cout<< " --(!) Error reading images " << endl;
    }

//    imwrite( "/mnt/sdcard/Android/Data/CollabAR/grayIMG.jpg", imgG );
//    imwrite( "/mnt/sdcard/Android/Data/CollabAR/grayREF.jpg", refG );

    //-- Step 1: Detect the keypoints using SURF Detector

    Ptr<FeatureDetector> detector = ORB::create(numFeaturesDest,1.2f,8,31,0,2,ORB::HARRIS_SCORE,31,20);
    vector<KeyPoint> keypoints_1, keypoints_2;
    detector->detect(imgG, keypoints_1);
    detector->detect(refG, keypoints_2);

    //-- Step 2: Calculate descriptors (feature vectors)
    Mat descriptors_1, descriptors_2;
    detector->compute(imgG, keypoints_1, descriptors_1);
    detector->compute(refG, keypoints_2, descriptors_2);

    //-- Step 3: Matching descriptor vectors using FLANN matcher
    FlannBasedMatcher flannBasedMatcher(new flann::LshIndexParams(20,10,2));
    vector<vector< DMatch > >matches;
    flannBasedMatcher.knnMatch(descriptors_1,descriptors_2,matches,2);

    double max_dist = 0; double min_dist = 100;

    //-- Quick calculation of max and min distances between keypoints
    for( int i = 0; i < descriptors_1.rows; i++ )
    {
        double dist = matches[i][1].distance;
        if( dist < min_dist )
            min_dist = dist;
        if( dist > max_dist )
            max_dist = dist;
    }

    printf("-- Max dist : %f \n", max_dist );
    printf("-- Min dist : %f \n", min_dist );

    //-- Draw only "good" matches (i.e. whose distance is less than 2*min_dist,
    //-- or a small arbitary value ( 0.02 ) in the event that min_dist is very
    //-- small)
    //-- PS.- radiusMatch can also be used here.
    vector< DMatch > good_matches;

    for( int i = 0; i < descriptors_1.rows; i++ )
    {
        if( matches[i][0].distance <= max(2*min_dist, 0.02) )
        {
            good_matches.push_back( matches[i][0]);
        }
    }

    //-- Draw only "good" matches
    Mat img_matches;
    drawMatches( img, keypoints_1, ref, keypoints_2,
                 good_matches, img_matches, Scalar::all(-1), Scalar::all(-1),
                 vector<char>(), DrawMatchesFlags::NOT_DRAW_SINGLE_POINTS );

    //-- Show detected matches
//    imwrite( "/mnt/sdcard/Android/Data/CollabAR/Good_Matches.jpg", img_matches );

    int nGoodMatches = good_matches.size();

    stringstream ss;
    ss << nGoodMatches;
    hello = hello + "No. of Good Matches: " + ss.str();

    if(nGoodMatches < 4){
        hello = hello + "; Not enough good matches to estimate pose";
    } else{

        vector<cv::Point2f> pts1(nGoodMatches);
        vector<cv::Point2f> pts2(nGoodMatches);
        for (size_t i = 0; i < nGoodMatches; i++) {
            pts1[i] = keypoints_2[good_matches[i].trainIdx].pt;
            pts2[i] = keypoints_1[good_matches[i].queryIdx].pt;
        }



    }

    return env->NewStringUTF(hello.c_str());

}
