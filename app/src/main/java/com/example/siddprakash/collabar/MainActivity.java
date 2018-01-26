package com.example.siddprakash.collabar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{

    private static final int REQUEST_WRITE_CAMERA_PERMISSION = 200;
    private int permission;
    private String messg="Capture an Image!";
    private TextView tv;

    private String appFolderPath = Environment.getExternalStorageDirectory() + "/Android/Data/CollabAR/";


    private String TAG = "OpenCVDebug";
    private JavaCameraView javaCameraView;
    private Mat myFrame, newFrame;

    private Button click;
    private boolean processing = false;
    private boolean flag = false;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java3");
    }

    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case BaseLoaderCallback.SUCCESS:{
                    javaCameraView.enableView();
                    break;
                }
                default:{
                    super.onManagerConnected(status);
                }
            }
            super.onManagerConnected(status);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                    REQUEST_WRITE_CAMERA_PERMISSION);
        }

        if(OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV Initialized!");
        }
        else{
            Log.d(TAG, "OpenCV Initialization Failed!");
        }
        setContentView(R.layout.activity_main);

        CreateAppFolderIfNeed();
        copyAssetsDataIfNeed();

        // Example of a call to a native method
        tv = (TextView) findViewById(R.id.sample_text);
        tv.setText(messg);

        javaCameraView = (JavaCameraView)findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(View.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);

        click = (Button) findViewById(R.id.btnCap);
        click.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { processing = true; }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_CAMERA_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Write to the storage (ex: call appendByteBuffer(byte[] data) here)

                } else {
                    Toast.makeText(getApplicationContext(), "Please grant permission.", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }

    }

    @Override
    protected void onPause(){
        super.onPause();
        if(javaCameraView != null){
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(javaCameraView != null){
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV Initialized!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else{
            Log.d(TAG, "OpenCV Initialization Failed!");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        }
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI(long matAddrRgba, long matAddrGray, boolean p);

    @Override
    public void onCameraViewStarted(int width, int height) {
        myFrame = new Mat(width, height, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        myFrame.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        myFrame = inputFrame.rgba();
        newFrame = new Mat();
        //Imgproc.cvtColor(newFrame,newFrame,Imgproc.COLOR_BGR2BGRA);
        messg = stringFromJNI(myFrame.getNativeObjAddr(), newFrame.getNativeObjAddr(), processing);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv.setText(messg);
                if(processing) {
                    flag = true;
                }
            }
        });
        if(flag){
            processing = false;
            flag = false;
        }

        return newFrame;
    }

    private void CreateAppFolderIfNeed(){
        Log.d(TAG, appFolderPath);
        File folder = new File(appFolderPath);
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
            if(!success)
                Log.d(TAG,"App folder does not exist");
            else
                Log.d(TAG,"App folder created!");
        } else {
            Log.d(TAG,"App folder exists");
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    private boolean copyAsset(AssetManager assetManager, String fromAssetPath, String toPath) {
        InputStream in = null;
        OutputStream out = null;
        try {
            assetManager = getAssets();
            in = assetManager.open(fromAssetPath);
            new File(toPath).createNewFile();
            out = new FileOutputStream(toPath);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            Log.e(TAG, "[ERROR]: copyAsset: unable to copy file = "+fromAssetPath);
            return false;
        }
    }

    private void copyAssetsDataIfNeed(){
        String assetsToCopy[] = {"image.jpg", "marker.jpg"};
        for(int i=0; i<assetsToCopy.length; i++){
            String from = assetsToCopy[i];
            String to = appFolderPath+from;

            // 1. check if file exist
            File file = new File(to);
            if(file.exists()){
                Log.d(TAG, "copyAssetsDataIfNeed: file exist, no need to copy:"+from);
            } else {
                // do copy
                boolean copyResult = copyAsset(getAssets(), from, to);
                Log.d(TAG, "copyAssetsDataIfNeed: copy result = " + copyResult + " of file = " + from);
            }
        }
    }
}
