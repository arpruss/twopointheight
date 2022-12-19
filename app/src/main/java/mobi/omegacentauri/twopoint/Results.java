package mobi.omegacentauri.twopoint;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

public class Results extends Activity {
	private static final double EPS = 1e-10;
	private SharedPreferences options;
	double angleLow;
	double angleHigh;
	private EditText deviceHeightText;
	private EditText distanceText;
	private TextView estDistanceText;
	private TextView heightText;
	private DecimalFormat degreeFormat = new DecimalFormat("#.0");
	private DecimalFormat distanceFormat = new DecimalFormat("#.###");
	private TextView messageText;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		Log.v("TwoPoint", "Results:onCreate");
        super.onCreate(savedInstanceState);

        options = PreferenceManager.getDefaultSharedPreferences(this); 

        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.results);
        
        deviceHeightText = (EditText)findViewById(R.id.deviceHeight);
        deviceHeightText.setText(options.getString(TwoPoint.DEVICE_HEIGHT, "1.8"));
        distanceText = (EditText)findViewById(R.id.distance);
        estDistanceText = (TextView)findViewById(R.id.estDistance);
        heightText = (TextView)findViewById(R.id.height);
        messageText = (TextView)findViewById(R.id.message);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		Log.v("TwoPoint", "Results:onPause");

		options.edit().putString(TwoPoint.DEVICE_HEIGHT, deviceHeightText.getText().toString().trim()).commit();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Log.v("TwoPoint", "Results:onPause");

		double angle0 = (double)options.getFloat(TwoPoint.ANGLE+"0", 0f);
		double angle1 = (double)options.getFloat(TwoPoint.ANGLE+"1", 0f);
		if (angle0 < angle1) {
			angleLow = angle0;
			angleHigh = angle1;
		}
		else {
			angleLow = angle1;
			angleHigh = angle0;
		}
		
		Log.v("TwoPoint", "angles "+angleLow+" "+angleHigh);

		((TextView)findViewById(R.id.angle0)).setText("Bottom angle: "+degreeFormat.format(angleLow*180/Math.PI)+"\u00B0");
		((TextView)findViewById(R.id.angle1)).setText("Top angle: "+degreeFormat.format(angleHigh*180/Math.PI)+"\u00B0");
		
		recalculate();

		TextWatcher tw = new TextWatcher() {

			@Override
			public void afterTextChanged(Editable arg0) {
				recalculate();
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {
				// TODO Auto-generated method stub
				
			}
			
		};
		
		deviceHeightText.addTextChangedListener(tw);
		distanceText.addTextChangedListener(tw);
	}
	

	private void recalculate() {
		try {
			Log.v("TwoPoint", "calculating");
			double deviceHeight = Double.parseDouble(deviceHeightText.getText().toString().trim());
			if (Math.abs(deviceHeight) <= EPS) 
				throw new NumberFormatException();
			
			Log.v("TwoPoint", "dev height = "+deviceHeight);
			
			String distanceString = distanceText.getText().toString().trim();
			
			double distance;
			
			try {
				distance = Double.parseDouble(distanceString);
				if (distance < EPS)
					distance = Double.NaN;
			}
			catch (NumberFormatException e) {
				distance = Double.NaN;
			}

			Log.v("TwoPoint", "distance = "+distance);

			double estDistance = Double.NaN;
			
			if (angleLow < -EPS)
				estDistance = deviceHeight / Math.tan(-angleLow);
			
			if (Double.isNaN(distance))
				distance = estDistance;
			
			Log.v("TwoPoint", "distance = "+distance);

			if (Double.isNaN(distance)) 
				throw new Exception();
			
			double height = distance * ( Math.tan(angleHigh) - Math.tan(angleLow) );
			
			if (Math.abs(angleLow)<EPS)
				height += deviceHeight;
			
			if (Double.isNaN(estDistance)) {
				estDistanceText.setVisibility(View.INVISIBLE);
			}
			else {
				estDistanceText.setVisibility(View.VISIBLE);
				estDistanceText.setText("Est. distance: "+distanceFormat.format(estDistance));
			}
			
			heightText.setText("Target height: "+distanceFormat.format(height));
			messageText.setText("");
		}
		catch(Exception e) {
			Log.v("TwoPoint", "invalid data");
			estDistanceText.setVisibility(View.INVISIBLE);
			messageText.setText("Please ensure distance and height are filled out.");
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			startActivity(new Intent(this, TwoPoint.class));
			return true;
		}
		return false;
	}
}

