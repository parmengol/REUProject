package edu.fiu.mpact.reuproject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

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

	}

	// app in background
	@Override
	protected void onStop() {
		super.onStop();
		if (sessionDepth > 0)
			sessionDepth--;
		if (sessionDepth == 0) {
			isInBackground = true;

			connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

			if(!mWifi.isConnected()) { // also check if it was connected
				wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
				wifiManager.setWifiEnabled(false); // restart wifi
				wifiManager.setWifiEnabled(true);
			}

			//get mac address
			WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			String wifiMacString = wifiInfo.getMacAddress();
			try {
				IntentService.setMac(wifiMacString, IntentService.theInterface);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}


	public boolean getStatus(){
		return isInBackground;
	}

}
