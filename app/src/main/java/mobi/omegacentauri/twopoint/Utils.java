package mobi.omegacentauri.twopoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.hardware.Camera.Size;
import android.text.Html;
import android.util.DisplayMetrics;

public class Utils {
	static public String getAssetFile(Context context, String assetName) {
		try {
			return getStreamFile(context.getAssets().open(assetName));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return "";
		}
	}

	static private String getStreamFile(InputStream stream) {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(stream));

			String text = "";
			String line;
			while (null != (line=reader.readLine()))
				text = text + line;
			return text;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return "";
		}
	}

	public static void show(Context context, String title, String assetName) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(Html.fromHtml(getAssetFile(context, assetName),
				new ImageGetter(context), null));
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", 
        	new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {} });
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {} });
        alertDialog.show();
	}

	public static void swapSize(Size s) {
		int a = s.width;
		s.width = s.height;
		s.height = a;		
	}

	static class ImageGetter implements Html.ImageGetter {

		private final Context context;

		public ImageGetter(Context c) {
			context = c;
		}

		@Override
		public Drawable getDrawable(String s) {
			int id;
			double scale = 2./3;

			if (s.equals("measure.png")) {
				id = R.drawable.measure;
			}
			else if (s.equals("r180.png")) {
				id = R.drawable.r180;
			}
			else if (s.equals("r0.png")) {
				id = R.drawable.r0;
			}
			else {
				return null;
			}
			Drawable d = context.getResources().getDrawable(id);
			DisplayMetrics metrics = context.getResources().getDisplayMetrics();
			int width = (int) (Math.min(metrics.widthPixels,  metrics.heightPixels) * scale);
			int height = width * d.getIntrinsicHeight()/d.getIntrinsicWidth();
			d.setBounds(0,0,width,height); //d.getIntrinsicWidth(),d.getIntrinsicHeight());
			return d;
		}
	}
}

