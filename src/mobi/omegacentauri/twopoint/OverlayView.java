

package mobi.omegacentauri.twopoint;

import java.text.DecimalFormat;

import mobi.omegacentauri.twopoint.R;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.view.MotionEvent;
import android.view.View;

public class OverlayView extends View {
	Context context;
	private Bitmap crossImage;
	private Paint bitmapPaint;
	private Paint textPaint;
	private Paint textBackPaint;
	private Rect testRect;
	private RectF roundedRect;
	private int pointCount;
	private double angle;
	private DecimalFormat degreeFormat = new DecimalFormat("#");
	private int width;
	private int height;
	
	public OverlayView(Context context) {
		super(context);

		crossImage = BitmapFactory.decodeResource(context.getResources(),R.drawable.ox);
		bitmapPaint = new Paint();

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.SANS_SERIF);
        textPaint.setTextSize(20);
        
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
		textPaint.getTextBounds(test, 0, test.length(), testRect);
		
		testRect.offset((width - testRect.width())/2, 
				height - testRect.height()- (testRect.height()+2*10)*lineNumber*3/2);

		RectF roundedRect = new RectF(testRect);
		
		roundedRect.left -= 10f;
		roundedRect.top -= 10f;
		roundedRect.right += 10f;
		roundedRect.bottom += 10f;
		
		canvas.drawRoundRect(roundedRect, 5f, 5f, textBackPaint);
		canvas.drawText(s, testRect.left, testRect.bottom, textPaint);		
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		drawCentered(canvas, crossImage);
		drawText(canvas, "Point to "+(pointCount==0?"first":"second")+" point and tap screen.",0);
		if (! Double.isNaN(angle)) 
			drawText(canvas, degreeFormat.format(angle)+"°","-90°", 1);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		width = MeasureSpec.getSize(widthMeasureSpec);
		height = MeasureSpec.getSize(heightMeasureSpec);
		setMeasuredDimension(width,height);
	}
    
	private void drawCentered(Canvas canvas, Bitmap bitmap) {
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
}
