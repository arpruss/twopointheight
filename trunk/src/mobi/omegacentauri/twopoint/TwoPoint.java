/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mobi.omegacentauri.twopoint;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import mobi.omegacentauri.twopoint.R;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.Size;
import android.hardware.SensorEvent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.

// ----------------------------------------------------------------------

@TargetApi(9)
public class TwoPoint extends SherlockActivity implements SensorEventListener {
    public static final String ANGLE = "angle";
	public static final String DEVICE_HEIGHT = "devHeight";
 
	private Preview mPreview;
    Camera mCamera;
    FrameLayout mFrame;
    SharedPreferences mOptions;
    OverlayView mOverlay;
	private int pointCount;
	private double curAngle = 0f;
	double[] gravity = { 0f, 0f, 0f };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mOptions = PreferenceManager.getDefaultSharedPreferences(this); 

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
       // requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
       // getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);        

        getSupportActionBar().show();
		getSupportActionBar().setDisplayShowTitleEnabled(false);
//        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(
//        		Color.argb(100, 0, 0, 0)));
        getSupportActionBar().setIcon(android.R.color.transparent);

        // Hide the window title.
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mFrame = (FrameLayout) LayoutInflater.from(this).inflate(R.layout.main, null);
        setContentView(mFrame);
//        mFrame = (FrameLayout)findViewById(android.R.id.content);
//        mFrame = View.inflate(this, R.layout.main, root)
//        mFrame.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, 
//        		LinearLayout.LayoutParams.FILL_PARENT));
//        
//        setContentView(R.layout.main);
//        setContentView(mFrame);
        
        pointCount = 0;
    }
    
	@Override
	public void onSensorChanged(SensorEvent event)
    {
          gravity[0] = 0.8 * gravity[0] + 0.2f * event.values[0];
          gravity[1] = 0.8 * gravity[1] + 0.2 * event.values[1];
          gravity[2] = 0.8 * gravity[2] + 0.2 * event.values[2];

          double total = Math.sqrt(gravity[0]*gravity[0]+gravity[1]*gravity[1]+gravity[2]*gravity[2]);
          if (total < 1e-5) {
        	  curAngle = 0f;
          }
          else {
        	  curAngle = (float) (Math.asin(-gravity[2]/total));
          }
          if(mOverlay != null)
        	  mOverlay.setAngle(curAngle);
    }    
	
    protected void initViews() {
    	
    	if (Build.VERSION.SDK_INT >= 9)
    		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

    	mFrame.removeAllViews();
    	
    	// Create a RelativeLayout container that will hold a SurfaceView,
        // and set it as the content of our activity.
        mPreview = new Preview(this);

        mFrame.addView(mPreview);
        mOverlay = new OverlayView(this);
        mFrame.addView(mOverlay);
        mOverlay.bringToFront();
        mOverlay.setPointCount(pointCount);
        
        mOverlay.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					mOptions.edit().putFloat(ANGLE+pointCount, (float)curAngle).commit();
					pointCount++;
					if (pointCount >= 2) {
						pointCount = 0;
						startActivity(new Intent(TwoPoint.this, Results.class));
					}
					else {
						mOverlay.setPointCount(pointCount);
					}
				}
				return false;
			}
		});
        
		SensorManager sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		if (sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() > 0) {
			gravity[0] = 0f;
			gravity[1] = 0f; 
			gravity[2] = 0f;
			sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
					SensorManager.SENSOR_DELAY_FASTEST);
		}
    }

	@Override
    protected void onResume() {
		Log.v("TwoPoint", "onResume()");
        super.onResume();

        initViews();
        mCamera = Camera.open();
        mPreview.setCamera(mCamera);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        if (mCamera != null) {
            mPreview.setCamera(null);
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.restart:
        	pointCount = 0;
        	mOverlay.setPointCount(pointCount);
        	return true;
        case R.id.licenses:
        	Utils.showLicenses(this);
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}
}

// ----------------------------------------------------------------------

/**
 * A simple wrapper around a Camera and a SurfaceView that renders a centered preview of the Camera
 * to the surface. We need to center the SurfaceView because not all devices have cameras that
 * support preview sizes at the same aspect ratio as the device's display.
 */
class Preview extends ViewGroup implements SurfaceHolder.Callback {
    private final String TAG = "Preview";

    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    Size mPreviewSize;
    List<Size> mSupportedPreviewSizes;
    Camera mCamera;
    Camera.Parameters mParams;

    Preview(Context context) {
        super(context);

        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);
        
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
        if (mCamera != null) {
        	mParams = camera.getParameters();
        	if (Build.VERSION.SDK_INT >= 9) {
	        	mCamera.setDisplayOrientation(90);
        	}
        	mCamera.setParameters(mParams);
            requestLayout();
        }
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mSupportedPreviewSizes = mParams.getSupportedPreviewSizes();

        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);
        
        Log.v("TwoPoint", "prefer "+width+" "+height+" "+widthMeasureSpec+" "+heightMeasureSpec);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
                Log.v("TwoPoint", "scaled Width = "+scaledChildWidth+" height = "+height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
                Log.v("TwoPoint", "scaled Height = "+scaledChildHeight+" width = "+width);
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }


    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
    	if (Build.VERSION.SDK_INT >= 9) {
    		int s = w;
    		w = h;
    		h = s;
    	}
    	
    	Log.v("TwoPoint", "trying for "+w+"x"+h);
    	
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        
        Camera.Size picSize = mParams.getPictureSize();
        
        double picRatio = (double)picSize.width / picSize.height;
        
        if (targetRatio > 1.) {
        	if (picRatio > 1.)
        		targetRatio = picRatio;
        	else
        		targetRatio = 1 / picRatio;
        }
        else {
        	if (picRatio > 1.)
        		targetRatio = 1 / picRatio;
        	else
        		targetRatio = picRatio;
        }

        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
        	Log.v("TwoPoint", "size: "+size.width+"x"+size.height);
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        
        if (Build.VERSION.SDK_INT >= 9) {
        	int s = optimalSize.height;
        	optimalSize.height = optimalSize.width;
        	optimalSize.width = s;
        }

    	Log.v("TwoPoint", "optimal size: "+optimalSize.width+"x"+optimalSize.height);
        return optimalSize;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
    	Log.v("TwoPoint", "picture="+(mCamera.getParameters().getPictureSize().width)+"x"+(mCamera.getParameters().getPictureSize().height));
    	mCamera.stopPreview();
    	Log.v("TwoPoint", "setPreviewSize "+mPreviewSize.width+"x"+mPreviewSize.height);
    	mParams.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
    	
        mCamera.setParameters(mParams);
        mCamera.startPreview();
    	Log.v("TwoPoint", "picture="+(mCamera.getParameters().getPictureSize().width)+"x"+(mCamera.getParameters().getPictureSize().height));

//        mParams = mCamera.getParameters();
//        mParams.setPictureSize(mPreviewSize.width, mPreviewSize.height);
//        mParams.setPictureFormat(ImageFormat.JPEG);
//        mCamera.setParameters(mParams);
        
        requestLayout();
    }

}
