

package mobi.omegacentauri.twopoint;

import java.text.DecimalFormat;

import mobi.omegacentauri.twopoint.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

public class OverlayView extends View {
	Context context;
	private Bitmap crossImage;
	private Bitmap phoneImage;
	private Paint bitmapPaint;
	private Paint textPaint;
	private Paint textBackPaint;
	private Rect testRect;
	private RectF roundedRect;
	private int pointCount;
	private double angle;
	private DecimalFormat degreeFormat = new DecimalFormat("#.0");
	private int width;
	private int height;
	private int axis;
	private Paint bigTextPaint;
	
	public float sp(float sp) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
	}
	
	public OverlayView(Context context) {
		super(context);
		
		this.context = context;

		bitmapPaint = new Paint();

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.SANS_SERIF);
        textPaint.setTextSize(sp(16));
        
        bigTextPaint = new Paint();
        bigTextPaint.setColor(Color.WHITE);
        bigTextPaint.setAntiAlias(true);
        bigTextPaint.setTypeface(Typeface.SANS_SERIF);
        bigTextPaint.setTextSize(sp(36));
        
        textBackPaint = new Paint();
        textBackPaint.setColor(Color.DKGRAY);
        textBackPaint.setAlpha(180);
        
        testRect = new Rect();
        roundedRect = new RectF();
        
        angle = Double.NaN;
	}
	
	public void drawText(Canvas canvas, String s, int lineNumber) {
		drawText(canvas,s,s,lineNumber);
	}

	public void drawText(Canvas canvas, String s, String test, int lineNumber) {
		textPaint.setTextScaleX(1.0f);

		textPaint.getTextBounds(test, 0, test.length(), testRect);

		if (testRect.width()+sp(10) > canvas.getWidth()) {
			textPaint.setTextScaleX((float)canvas.getWidth() / (testRect.width()+sp(10)));
			textPaint.getTextBounds(test, 0, test.length(), testRect);
		}

		testRect.offset((width - testRect.width())/2,
				(int) (height - testRect.height()- (testRect.height()+2*sp(5))*lineNumber*3/2));

		RectF roundedRect = new RectF(testRect);
		
		roundedRect.left -= sp(5);
		roundedRect.top -= sp(5);
		roundedRect.right += sp(5);
		roundedRect.bottom += sp(5);
		
		canvas.drawRoundRect(roundedRect, sp(5f), sp(5f), textBackPaint);
		canvas.drawText(s, testRect.left, testRect.bottom, textPaint);

		textPaint.setTextScaleX(1.0f);
	}
	
	public void drawBigText(Canvas canvas, String s) {
		bigTextPaint.getTextBounds(s, 0, s.length(), testRect);

		if (testRect.width() > width) {
			Log.v("TwoPoint", "start size: "+bigTextPaint.getTextSize());
			bigTextPaint.setTextSize(bigTextPaint.getTextSize() * width / testRect.width());
			bigTextPaint.getTextBounds(s, 0, s.length(), testRect);
			Log.v("TwoPoint", "end size: "+bigTextPaint.getTextSize());
		}
		
		testRect.offset((width - testRect.width())/2, 
				height/2 - testRect.height()/2);

		RectF roundedRect = new RectF(testRect);
		
		roundedRect.left -= 10f;
		roundedRect.top -= 10f;
		roundedRect.right += 10f;
		roundedRect.bottom += 10f;
		
		canvas.drawRoundRect(roundedRect, 5f, 5f, textBackPaint);
		canvas.drawText(s, testRect.left, testRect.bottom, bigTextPaint);		
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		if (! Double.isNaN(angle))
			drawText(canvas, degreeFormat.format(angle)+"\u00B0","-90\u00B0", 1);

		if (axis == TwoPoint.CAMERA_AXIS) {
			drawText(canvas, "Point to "+(pointCount==0?"top or bottom":"other end")+" of target and tap screen or press volume buttom.",0);
			drawCentered(canvas, crossImage);
		}
		else {
			drawText(canvas, "Sight "+(pointCount==0?"top or bottom":"other end")+" of target and tap screen or press volume button.",0);
			drawCentered(canvas, phoneImage);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		width = MeasureSpec.getSize(widthMeasureSpec);
		height = MeasureSpec.getSize(heightMeasureSpec);
		setMeasuredDimension(width,height);
		crossImage = BitmapFactory.decodeResource(context.getResources(), R.drawable.ox);
		phoneImage = BitmapFactory.decodeResource(context.getResources(), R.drawable.angledphone);
		int w = phoneImage.getWidth();
		int h = phoneImage.getHeight();
		if (w>width || h>height) {
			float imageAspect = (float)phoneImage.getHeight()/phoneImage.getWidth();
			float viewAspect = (float)height/width;
			int newW;
			int newH;
	
			if (imageAspect > viewAspect) {
				newH = height;
				newW = (int)(height / imageAspect);
			}
			else {
				newW = width;
				newH = (int)(width * imageAspect);
			}

			phoneImage = Bitmap.createScaledBitmap(phoneImage, newW, newH, true);
		}

	}
    
	private void drawCentered(Canvas canvas, Bitmap bitmap) {
		bitmap.getWidth();
		canvas.drawBitmap(bitmap, (width-bitmap.getWidth())/2, 
				(height-bitmap.getHeight())/2, bitmapPaint);
	}

	public void setPointCount(int pointCount) {
		this.pointCount = pointCount;
		invalidate();
	}

	public void setAngle(double curAngle) {
		angle = curAngle * 180 / Math.PI;
		invalidate();
	}	
	
	
	public void setAxis(int axis) {
		this.axis = axis;
	}
}
