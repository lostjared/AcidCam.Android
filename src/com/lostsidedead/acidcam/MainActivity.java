package com.lostsidedead.acidcam;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.*;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Toast;


public class MainActivity extends Activity implements CvCameraViewListener2 {
    private static final String    TAG = "OCVAcidCam::Activity";

    private Mat                    mRgba;
    private Mat                    mIntermediateMat;
    private static int offset = 0;
    private MenuItem reset;
    
    private CameraBridgeViewBase   mOpenCvCameraView;
    public AcidCam_Filter filter = new AcidCam_Filter();
    private MediaPlayer mp;
    
    
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    //System.loadLibrary("mixed_sample");
                    filter.load();

                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    //    android:theme="@android:style/Theme.NoTitleBar.Fullscreen"
    
    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    private boolean isfront = false;
    private int num_cameras = 0;
    private int camera_index = 0, image_index = 0;
    private static final String STATE_CAMERA_INDEX = "cameraIndex";
    private static final String STATE_IMAGE_SIZE_INDEX = "imageSizeIndex";
    private static final String STATE_FIRST_LOADED = "firstLoaded";
    private static final String STATE_OLD = "oldCamera";
    private int MENU_GROUP_ID_SIZE = 2;
    private List<android.hardware.Camera.Size> image_sizes;
    boolean menu_locked = false;
    private boolean first_loaded = true;
    private int old_camera = 0;
    
    /** Called when the activity is first created. */
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if(savedInstanceState != null) {
        	camera_index = savedInstanceState.getInt(STATE_CAMERA_INDEX, 0);
        	image_index = savedInstanceState.getInt(STATE_IMAGE_SIZE_INDEX, 0);
        	first_loaded = savedInstanceState.getBoolean(STATE_FIRST_LOADED);
        	old_camera = savedInstanceState.getInt(STATE_OLD, 0);
        	
        } else {
        	camera_index = 0;
        	image_index = 0;
        	first_loaded = true;
        	old_camera = 0;
        }
        
        Camera camera = null;
        
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
	        CameraInfo info = new CameraInfo();
	        Camera.getCameraInfo(camera_index, info);
	        //isfront = (info.facing == CameraInfo.CAMERA_FACING_FRONT);
	        num_cameras = Camera.getNumberOfCameras();
	        camera = camera.open(camera_index);
	        isfront = false;
        
        } else {
        	isfront = false;
        	num_cameras = 1;
        	camera = Camera.open();
        }
   
        Parameters param = camera.getParameters();
        camera.release();
        
       image_sizes = param.getSupportedPreviewSizes();
       int max_size = 0;
       
       if(old_camera == 0) {	
	       for(int i = 0; i < image_sizes.size(); ++i) {
	    	   android.hardware.Camera.Size s = image_sizes.get(i);
	    	   if(s.height > max_size && s.height <= 1080) {
	    		   max_size = s.height;
	    		   image_index = i;
	    	   }
	       }
	       
	       first_loaded = false;
       }
       
       final android.hardware.Camera.Size size = image_sizes.get(image_index);
       
       
        mOpenCvCameraView = new JavaCameraView(this, camera_index);
        //mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial2_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setMaxFrameSize(size.width, size.height);
        mOpenCvCameraView.setCvCameraViewListener(this);
      
        setContentView(mOpenCvCameraView);
          
        mp = MediaPlayer.create(this, R.raw.beep);
        getOffset();      
        mOrientation = getResources().getConfiguration().orientation;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)  {
    	savedInstanceState.putInt(STATE_CAMERA_INDEX, camera_index);
    	savedInstanceState.putInt(STATE_IMAGE_SIZE_INDEX,  image_index);
    	savedInstanceState.putBoolean(STATE_FIRST_LOADED,  first_loaded);
    	savedInstanceState.putInt(STATE_OLD, old_camera);
    	super.onSaveInstanceState(savedInstanceState);
    }
    
    @Override
    public void recreate() {
    	if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
    		super.recreate();
    	} else {
    		finish();
    		startActivity(getIntent());
    	}
    }
    
    public void saveOffset() {
    	 SharedPreferences sp = getSharedPreferences("acidcam_prefs", Activity.MODE_PRIVATE);
         SharedPreferences.Editor editor = sp.edit();
         editor.putInt("acidcam.key", snap_shot_index);
         editor.commit();
    }
    
    public void getOffset() {
    	 SharedPreferences sp = getSharedPreferences("acidcam_prefs", Activity.MODE_PRIVATE);
    	 snap_shot_index = sp.getInt("acidcam.key", 0);
    }
    
    private static int snap_shot_index = 0;
    private boolean take_snapshot = false;
    
    public void showImageSaved() {
		Toast.makeText(this, "Save Image: " +  snap_shot_index + " in 2 seconds" , Toast.LENGTH_SHORT).show();
		++snap_shot_index;	
		saveOffset();
		take_snapshot = true;
    }
    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        //reset = menu.add("Reset");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        
        if(num_cameras < 2) {
        	menu.removeItem(R.id.switchcam);
        }
        
        int num_sizes = image_sizes.size();
       if(num_sizes > 1) {
    	   
    	   final SubMenu subSizes = menu.addSubMenu("Resolutions");
    	   for(int i = 0; i < num_sizes; ++i) {
    		   final Size s = image_sizes.get(i);
    		   subSizes.add(MENU_GROUP_ID_SIZE, i , Menu.NONE, "" + s.width + "x" + s.height);
    	   }
       }
        
         return true;
    }
  
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
    	if(menu_locked == true) return true;
    	
    	if(item == reset) {
    		offset = 0;
    	}
    	
    	if(item.getGroupId() == MENU_GROUP_ID_SIZE) {
    		image_index = item.getItemId();
    		old_camera = 1;
    		recreate();
    		return true;
    	}
    	
    	switch(item.getItemId()) {
    	case R.id.switchcam:
    		menu_locked = true;
    		++camera_index;
    		if(camera_index >= num_cameras) {
    			camera_index = 0;
    		}
    		image_index = 0;
    		old_camera = 0;
    		recreate();
    		break;
    	case R.id.takesnapshot:
    		showImageSaved();
    		break;
    	case R.id.moveleft:
    		moveLeft();
    		break;
    	case R.id.moveright:
    		moveRight();
    		break;
    	case R.id.flipi:
    		isfront = !isfront;
    		break;
    	}
    	
        return true;
    }
  

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        
        menu_locked = false;
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mIntermediateMat.release();
    }

    
    private int takesnapshot_wait = 0;
    
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	mRgba = inputFrame.rgba();
    	if(isfront == true) Core.flip(mRgba, mRgba, 0);
    	
    	filter.Filter(offset, mRgba.getNativeObjAddr());
        
        if(take_snapshot == true) {
        	
        	++takesnapshot_wait;
        	
        	if(takesnapshot_wait > 30) {
        		takesnapshot_wait = 0;
        		take_snapshot = false;
        		saveImage(mRgba);
        	}
        }
        
       
       return mRgba;
    }

 
    String draw_strings[] = { "Self AlphaBlend", "StrobeEffect", "Blend #3", "Negative Paradox", "ThoughtMode", "RandTriBlend", "Blank", "Tri", "Distort", "CDraw", "Type", "NewOne", "SelfAlphaBlend/RainbowBlend","Mood", "CosSinMultiply", "Color Accumlate1", "Color Accumulate2", "Color Accumulate3", "filter8","filter3","Rainbow Blend","Rand Blend","New Blend", "Alpha Flame Filters", "Custom", "Blend With Image #1",  "TriBlend with Image", "Image Strobe", "Image distraction" };
    
    
    public void moveRight() {
    	if(offset < 58) ++offset;
    	
    	if(offset > 23) {
    		int index = offset-23;
    		Toast.makeText(this, "Filter changed to: Alpha Flame: " + index, Toast.LENGTH_SHORT).show();
    	}
    	else {
    		Toast.makeText(this, "Filter changed to: " + draw_strings[offset], Toast.LENGTH_SHORT).show();
    	}
    }
    
    public void moveLeft() {
    	if(offset > 0) --offset;
    	
    	if(offset > 23) {
    		int index = offset-23;
    		Toast.makeText(this, "Filter changed to: Alpha Flame: #" + index, Toast.LENGTH_SHORT).show();
    	} else {
    		Toast.makeText(this, "Filter changed to: " + draw_strings[offset], Toast.LENGTH_SHORT).show();
    	}
    }
     
    float x1,x2;
    float y1, y2;
    float diffx, diffy;
    
    int mOrientation = 0;
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mOrientation = newConfig.orientation;
    }
    

    public boolean onTouchEvent(MotionEvent touchevent) {
    	
    	
    	switch (touchevent.getAction()) {
        		case MotionEvent.ACTION_DOWN: {
        			if(mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                             x1 = touchevent.getX();
                             y1 = touchevent.getY();        
        			} else if(mOrientation == Configuration.ORIENTATION_PORTRAIT) {
        					y1 = touchevent.getX();
        					x1 = touchevent.getY();
        			}
                             break;
                 }
    			case MotionEvent.ACTION_UP: 
    			{
    				
        			if(mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
        				x2 = touchevent.getX();
        				y2 = touchevent.getY(); 
        				diffx = x2-x1;
        				diffy = y2-y1;
        				
        			} else if(mOrientation == Configuration.ORIENTATION_PORTRAIT) {
        				x2 = touchevent.getY();
        				y2 = touchevent.getX(); 
        				diffx = x2-x1;
        				diffy = y2-y1;
        			}	
    				
    				
   
    				if (x1 < x2 && Math.abs(diffy) < Math.abs(diffx)) {
        						moveLeft();
        						return true;
                    }
                            
     				if (x1 > x2 && Math.abs(diffy) < Math.abs(diffx)) {
    							moveRight();
    							return true;
                    }
     				
     				if(y2 < y1 && Math.abs(diffy) > 50) {
     					showImageSaved();
     					return true;
     				}
     				
                            
                    break;
                         
                 }
    	}
                 return false;
    }
    
    
    
   
    public void saveImage (Mat mat) {
    	File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
    	String filename = "AcidCam_Image_" + "0000" + snap_shot_index + ".jpg";
    	File file = new File(path+"/Camera", filename);

    	  Boolean bool = null;
    	  filename = file.toString();
    	  mp.start();
    	  
    	  Mat cmat = new Mat();
    	  Imgproc.cvtColor(mat,  cmat, Imgproc.COLOR_RGBA2BGR, 3);
    	  
    	  bool = Imgcodecs.imwrite(filename, cmat);

    	  if (bool == true)
    	    Log.d("com.lostsidedead.AcidCam", "SUCCESS writing " + filename + " image to external storage");
    	  else
    	    Log.d("com.lostsidedead.AcidCam", "Fail writing image to external storage");
    	  
    	  MediaScannerConnection.scanFile(this, new String[] { file.getPath() }, new String[] { "image/jpeg" }, null);
    	 
	}

}

