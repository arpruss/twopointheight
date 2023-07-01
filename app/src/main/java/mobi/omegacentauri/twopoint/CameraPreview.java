package mobi.omegacentauri.twopoint;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

// ----------------------------------------------------------------------

/**
 * A simple wrapper around a Camera and a SurfaceView that renders a centered preview of the Camera
 * to the surface. We need to center the SurfaceView because not all devices have cameras that
 * support preview sizes at the same aspect ratio as the device's display.
 */
class CameraPreview extends ViewGroup implements SurfaceHolder.Callback {
    private final String TAG = "CameraPreview";
    private final SharedPreferences mOptions;

    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    Size mPreviewSize;
    List<Size> mSupportedPreviewSizes;
    Camera mCamera;
    Camera.Parameters mParams;

	private boolean mPortrait;
    private int mScaledWidth = 0;
    private int mScaledHeight = 0;

    CameraPreview(Context context, SharedPreferences pref) {
        super(context);

        mOptions = pref;

        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);
        
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    
    public void setCamera(Camera camera, int zoom) {
    	setCamera(camera, zoom, true);
    }

    public void setCamera(Camera camera, int zoom, boolean portrait) {
        mCamera = camera;
        mPortrait = portrait;
        if (Build.VERSION.SDK_INT < 9)
        	mPortrait = false; // there are problems at SDK < 9 with portrait mode;
        
        if (mCamera != null) {
        	mParams = camera.getParameters();
        	if (mPortrait) {
        		Log.v(TAG, "display orientation set");
        		mCamera.setDisplayOrientation(90);
        	}
        	
        	if (Build.VERSION.SDK_INT >= 8 && mParams.isZoomSupported()) {
        		mParams.setZoom(zoom);
        	}
        	mCamera.setParameters(mParams);
            requestLayout();
        }
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;
//            width *= 4;
//            height *= 4;

        	mSupportedPreviewSizes = mParams.getSupportedPreviewSizes();
        	mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);

            int previewWidth;
			int previewHeight;
			//            int previewWidth = width;
//            int previewHeight = height;
//            if (mPreviewSize != null) {
        	if (mPortrait) {
                previewWidth = mPreviewSize.height;
                previewHeight = mPreviewSize.width;
        	}
        	else {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
        	}
//            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
                mScaledWidth = scaledChildWidth;
                mScaledHeight = height;
                Log.v(TAG, "scaled Width = "+scaledChildWidth+" height = "+height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
                mScaledWidth = width;
                mScaledHeight = scaledChildHeight;
                Log.v(TAG, "scaled Height = "+scaledChildHeight+" width = "+width);
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
    	if (mPortrait) {
    		int s = w;
    		w = h;
    		h = s;
    	}
    	
    	Log.v(TAG, "trying for "+w+"x"+h);
    	
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;
        
        Camera.Size picSize = mParams.getPictureSize();
        
        double picRatio = (double)picSize.width / picSize.height;
//        double picRatio = targetRatio;
        
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
        	Log.v(TAG, "size: "+size.width+"x"+size.height);
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
        
//        if (mPortrait) {
//        	Utils.swapSize(optimalSize);
//        }

    	Log.v(TAG, "optimal size: "+optimalSize.width+"x"+optimalSize.height);
        return optimalSize;
    }

    public int crosshairDelta(String coordinate) {
        try {
            int cm = mOptions.getInt("CAMERA_NUMBER", 0);
            double tweak = Double.parseDouble(mOptions.getString(coordinate + "_TWEAK_"+cm, "0"));
            if (coordinate == "X")
                return (int) (mScaledWidth * tweak);
            else
                return (int) (mScaledHeight * tweak);
        }
        catch(Exception e) {
            return 0;
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
    	mCamera.stopPreview();
    	Log.v(TAG, "setPreviewSize "+mPreviewSize.width+"x"+mPreviewSize.height);

    	
    	try {
    		Log.v(TAG, "setting "+mPreviewSize.width+" "+mPreviewSize.height);
    		mParams.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
    		mCamera.setParameters(mParams);
    	} catch (RuntimeException e) {
    		Log.e(TAG, "error setting camera parameters: "+e);

    		mPreviewSize = mSupportedPreviewSizes.get(0); 
//    		if (mPortrait) {
//    			Utils.swapSize(mPreviewSize);
//    		}

    		try {
		    	mParams.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
		    	mCamera.setParameters(mParams);
    		} catch(Exception e2) {
        		Log.e(TAG, "error setting camera parameters: "+e2);
    		}
    	}
        mCamera.startPreview();
    	Log.v(TAG, "picture="+(mCamera.getParameters().getPictureSize().width)+"x"+(mCamera.getParameters().getPictureSize().height));

//        mParams = mCamera.getParameters();
//        mParams.setPictureSize(mPreviewSize.width, mPreviewSize.height);
//        mParams.setPictureFormat(ImageFormat.JPEG);
//        mCamera.setParameters(mParams);
        
        requestLayout();
    }

}
