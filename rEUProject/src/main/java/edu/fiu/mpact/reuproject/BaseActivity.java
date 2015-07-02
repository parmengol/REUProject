package edu.fiu.mpact.reuproject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

public class BaseActivity extends Activity {
	private static int sessionDepth = 0;
	public static boolean isInBackground = false;
	WifiManager wifiManager;
	ConnectivityManager connManager;
	NetworkInfo mWifi;

	// app in foreground
	@Override
	protected void onStart() {
		super.onStart();
		sessionDepth++;
		isInBackground = false;
		Toast.makeText(getApplicationContext(), "App is in foreground",
				Toast.LENGTH_SHORT).show();

	}

	// app in background
	@Override
	protected void onStop() {
		super.onStop();
		if (sessionDepth > 0)
			sessionDepth--;
		if (sessionDepth == 0) {
			Toast.makeText(getApplicationContext(), "App is in background",
 					Toast.LENGTH_SHORT).show();
			isInBackground = true;
			Log.d("My log2", "background " + isInBackground);

			connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

			if(!mWifi.isConnected()) {
				wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
				wifiManager.setWifiEnabled(false); // restart wifi
				wifiManager.setWifiEnabled(true);
			}
		}
	}


	public boolean getStatus(){
		return isInBackground;
	}

}
