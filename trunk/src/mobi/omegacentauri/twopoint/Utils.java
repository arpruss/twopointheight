package mobi.omegacentauri.twopoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;

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

	public static void showLicenses(Context context) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle("Licenses and copyrights");
        alertDialog.setMessage(Html.fromHtml(getAssetFile(context, "licenses.txt")));
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", 
        	new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {} });
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {} });
        alertDialog.show();
	}
	

}
