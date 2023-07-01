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
import java.util.ArrayList;
import java.util.List;

import mobi.omegacentauri.twopoint.R;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.Size;
import android.hardware.SensorEvent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.

// ----------------------------------------------------------------------

public class TwoPoint extends Activity implements SensorEventListener {
    public static final String ANGLE = "angle";
	public static final String DEVICE_HEIGHT = "devHeight";
	private static final String PREF_CAMERA = "cameraMode";
 
	private CameraPreview mPreview;
    Camera mCamera = null;
    FrameLayout mFrame;
    SharedPreferences mOptions;
    OverlayView mOverlay;
	private int pointCount;
	private double curAngle = 0f;
	private double timeConstant = 0.4f;
	private long prevSensorTime;
	double[] gravity = { 0f, 0f, 0f };
	double[] calibration1 = { 0f, 0f, 1f };
	double[] calibration2 = { 0f, 0f, 1f };
	double[] calibration = { 0f, 0f, 1f };

	private boolean zeroed;
	int axis;
	static final int CAMERA_AXIS = 2;
	static final int PHONE_AXIS = 1;
	static final short[] beep = sinewave(2000, 20, 0.01f);

	private AudioTrack mBeep;
	private Object cameraNumber;
	private Camera.CameraInfo mCameraInfo;

	static private short[] sinewave(float frequency, long duration, float amplitude) {
		int numSamples = (int)(44.100 * duration);
		double alpha = frequency / 44100 * 2 * Math.PI;
		short[] samples = new short[numSamples];
		for (int i = 0 ; i < numSamples ; i++)
			samples[i] = (short) (32767. * amplitude * Math.sin(alpha * i));
		return samples;
	}

	boolean haveCameraPermission() {
		return Build.VERSION.SDK_INT < 23 || PackageManager.PERMISSION_GRANTED == checkSelfPermission("android.permission.CAMERA");
	}

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mOptions = PreferenceManager.getDefaultSharedPreferences(this);

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
       // requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
       // getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);        

        getActionBar().show();
		getActionBar().setDisplayShowTitleEnabled(false);
//        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(
//        		Color.argb(100, 0, 0, 0)));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			getActionBar().setIcon(android.R.color.transparent);
		}

        mFrame = (FrameLayout) LayoutInflater.from(this).inflate(R.layout.main, null);
        setContentView(mFrame);

        pointCount = 0;
        zeroed = false;
        axis = mOptions.getBoolean(PREF_CAMERA, true) ? CAMERA_AXIS : PHONE_AXIS;
		calibration[0] = Double.longBitsToDouble(mOptions.getLong("CALIBRATE0", Double.doubleToLongBits(0.)));
		calibration[1] = Double.longBitsToDouble(mOptions.getLong("CALIBRATE1", Double.doubleToLongBits(0.)));
		calibration[2] = Double.longBitsToDouble(mOptions.getLong("CALIBRATE2", Double.doubleToLongBits(1.)));
		Log.v("twopoint", "calibration "+calibration[0]+","+calibration[1]+","+calibration[2]);

		if (!haveCameraPermission()) {
			Log.v("TPH", "requesting");
			requestPermissions(new String[] {"android.permission.CAMERA"}, 0);
		}

		if (!mOptions.getBoolean("showedHelp", false)) {
			Utils.show(this, "Help", "instructions.txt");
			mOptions.edit().putBoolean("showedHelp", true).apply();
		}
	}

    @Override
	public void onRequestPermissionsResult (int requestCode,
											String[] permissions,
											int[] grantResults) {
		Log.v("TPH", "results");
		boolean haveCameraPermission = false;
    	for (int i=0; i<permissions.length; i++)
    		if (permissions[i].equals("android.permission.CAMERA")) {
				if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
					haveCameraPermission = true;
				}
			}
		if (haveCameraPermission) {
			initViews();
		}
		else {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				finishAffinity();
			} else {
				finish();
			}
		}
	}


	@Override
	public void onSensorChanged(SensorEvent event)
    {
		double alpha;

		if (prevSensorTime < 0 || event.timestamp <= prevSensorTime)
			alpha = 1;
		else {
			double dt = (event.timestamp-prevSensorTime)/1e9f;
			alpha = timeConstant / (timeConstant + dt);
		}
		prevSensorTime = event.timestamp;

		gravity[0] = event.values[0] * (1-alpha) + gravity[0] * alpha;
		gravity[1] = event.values[1] * (1-alpha) + gravity[1] * alpha;
		gravity[2] = event.values[2] * (1-alpha) + gravity[2] * alpha;

		double total = Math.sqrt(gravity[0]*gravity[0]+gravity[1]*gravity[1]+gravity[2]*gravity[2]);
		if (total < 1e-5) {
		  curAngle = 0f;
		}
		else {
			if (axis == PHONE_AXIS)
		  		curAngle = (float) (Math.asin(gravity[axis]/total));
			else {
				curAngle = Math.PI/2-Math.acos((gravity[0]*calibration[0]+gravity[1]*calibration[1]+gravity[2]*calibration[2])/total);
				if (mCamera != null && mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
					curAngle = -curAngle;
			}
		}
		if(mOverlay != null)
		  mOverlay.setAngle(curAngle);
    }

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP ) {
			addAngleWithFeedback();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP ) {
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	protected void initViews() {
		Log.v("TPH", "setting up views");
    	if (Build.VERSION.SDK_INT < 9)
    		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

    	mFrame.removeAllViews();

		if (mCamera != null) {
			if (mPreview != null)
				mPreview.setCamera(null, 0);
            mCamera.release();
            mCamera = null;
		}

		if (axis == CAMERA_AXIS) {
	    	// Create a RelativeLayout container that will hold a SurfaceView,
	        // and set it as the content of our activity.
			if (haveCameraPermission()) {
				Log.v("TPH", "opening camera");
				int cm = mOptions.getInt("CAMERA_NUMBER", 0);
				if (cm >= Camera.getNumberOfCameras())
					cm = 0;
				mPreview = new CameraPreview(this);
				mCamera = Camera.open(cm);
				mCameraInfo = new Camera.CameraInfo();
				Camera.getCameraInfo(cm, mCameraInfo);
				mPreview.setCamera(mCamera, 0);
				mFrame.addView(mPreview);
			}
    	}
    	else {
    		mPreview = null;
    	}

        mOverlay = new OverlayView(this);
        mFrame.addView(mOverlay);
        mOverlay.bringToFront();
        mOverlay.setPointCount(pointCount);
        mOverlay.setAxis(axis);
        
        mOverlay.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					addAngleWithFeedback();
				}
				return false;
			}
		});
        
		SensorManager sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		if (sensorManager.getSensorList(Sensor.TYPE_GRAVITY).size() > 0) {
			gravity[0] = 0f;
			gravity[1] = 0f;
			gravity[2] = 0f;
			prevSensorTime = -1;
			timeConstant = 0.2f;
			sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
					SensorManager.SENSOR_DELAY_FASTEST);
		}
		else if (sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() > 0) {
			gravity[0] = 0f;
			gravity[1] = 0f; 
			gravity[2] = 0f;
			prevSensorTime = -1;
			timeConstant = 0.4f;
			sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
					SensorManager.SENSOR_DELAY_FASTEST);
		}

    }
    
	@SuppressLint("NewApi")
    void safeInvalidateOptionsMenu() {
		if (Build.VERSION.SDK_INT>=11)
			invalidateOptionsMenu();    	
    }

    protected void addAngleWithFeedback() {
		addAngleWithFeedback(curAngle);
	}

    protected void addAngleWithFeedback(double angle) {
		mOverlay.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
		mBeep.stop();
		mBeep.reloadStaticData();
		mBeep.play();
		addAngle(angle);
	}

	protected void addAngle(double curAngle) {
		mOptions.edit().putFloat(ANGLE+pointCount, (float)curAngle).commit();
		pointCount++;
		if (pointCount >= 2) {
			pointCount = 0;
			zeroed = false;
			safeInvalidateOptionsMenu();
			startActivity(new Intent(TwoPoint.this, Results.class));
		}
		else {
			mOverlay.setPointCount(pointCount);
		}
	}

	@Override
    protected void onResume() {
		Log.v("TwoPoint", "onResume()");
        super.onResume();

		mBeep = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
				AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT,
				beep.length * 2, AudioTrack.MODE_STATIC);
		mBeep.write(beep, 0, beep.length);

		initViews();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        if (mCamera != null) {
            mPreview.setCamera(null, 0);
            mCamera.release();
            mCamera = null;
        }

        mBeep.release();
        mBeep = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
		menu.findItem(R.id.zero).setVisible(! zeroed);
		menu.findItem(R.id.calibrate).setVisible(axis != PHONE_AXIS);
        return true;
    }

	void saveCalibration() {
		SharedPreferences.Editor ed = mOptions.edit();
		ed.putLong("CALIBRATE0", Double.doubleToLongBits(calibration[0]));
		ed.putLong("CALIBRATE1", Double.doubleToLongBits(calibration[1]));
		ed.putLong("CALIBRATE2", Double.doubleToLongBits(calibration[2]));
		ed.apply();
	}

	void calibrate() {
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		alertDialog.setTitle("Calibration Step 1");
		alertDialog.setMessage("Put device face up on flat surface and press Begin button.");
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Begin",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						double totalGravity = Math.sqrt(gravity[0]*gravity[0]+gravity[1]*gravity[1]+gravity[2]*gravity[2]);
						if (totalGravity >= 1e-5) {
							calibration1[0] = gravity[0] / totalGravity;
							calibration1[1] = gravity[1] / totalGravity;
							calibration1[2] = gravity[2] / totalGravity;
							calibrate2();
						}
					} });
		alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Reset",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						calibration[0] = 0;
						calibration[1] = 0;
						calibration[2] = 1;
						saveCalibration();
					} });
		alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {} });
		alertDialog.show();
	}

	void calibrate2() {
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		alertDialog.setTitle("Calibration Step 2");
		alertDialog.setMessage("Keeping device face up, rotate it 180 degrees and press Finish button.");
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Finish",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						double totalGravity = Math.sqrt(gravity[0]*gravity[0]+gravity[1]*gravity[1]+gravity[2]*gravity[2]);
						if (totalGravity >= 1e-5) {
							calibration2[0] = gravity[0] / totalGravity;
							calibration2[1] = gravity[1] / totalGravity;
							calibration2[2] = gravity[2] / totalGravity;
							calibration[0] = calibration1[0] + calibration2[0];
							calibration[1] = calibration1[1] + calibration2[1];
							calibration[2] = calibration1[2] + calibration2[2];
							double total = Math.sqrt(calibration[0]*calibration[0]+calibration[1]*calibration[1]+calibration[2]*calibration[2]);
							if (total < 1e-6) {
								calibration[0] = 0;
								calibration[1] = 0;
								calibration[2] = 1;
							}
							else {
								calibration[0] /= total;
								calibration[1] /= total;
								calibration[2] /= total;
							}
							saveCalibration();
						}
					} });
		alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Reset",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						calibration[0] = 0;
						calibration[1] = 0;
						calibration[2] = 1;
						saveCalibration();
					} });
		alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {} });
		alertDialog.show();
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.restart:
        	pointCount = 0;
        	zeroed = false;
        	mOverlay.setPointCount(pointCount);
        	return true;
		case R.id.camera_switch:
			int cm = mOptions.getInt("CAMERA_NUMBER", 0);
			cm = (cm+1) % Camera.getNumberOfCameras();
			mOptions.edit().putInt("CAMERA_NUMBER", cm).apply();
			Intent intent = getIntent();
			finish();
			startActivity(intent);
			return true;
        case R.id.mode:
        	if (axis == CAMERA_AXIS)
        		axis = PHONE_AXIS;
        	else
        		axis = CAMERA_AXIS;
        	mOptions.edit().putBoolean(PREF_CAMERA, axis == CAMERA_AXIS).apply();
        	initViews();
			safeInvalidateOptionsMenu();
        	return true;
        case R.id.zero:
        	zeroed = true;
        	addAngleWithFeedback(0.);
        	safeInvalidateOptionsMenu();
        	return true;
		case R.id.calibrate:
			calibrate();
			return true;
        case R.id.licenses:
        	Utils.show(this, "Licenses and Copyrights", "licenses.txt");
        	return true;
		case R.id.help:
			Utils.show(this, "Help", "instructions.txt");
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

