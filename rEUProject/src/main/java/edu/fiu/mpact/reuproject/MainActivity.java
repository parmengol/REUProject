package edu.fiu.mpact.reuproject;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.tv.TvContract;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;


import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
//import com.parse.Parse;
//import com.parse.ParseObject;


/**
 * Activity Flow:
 * 
 * (1) MainActivity -> (2) ViewMapActivity -> (3a) TrainActivity -or- (3b)
 * LocalizeActivity
 * 
 * @author oychang
 *
 */
public class MainActivity extends BaseActivity {

	private ProgressDialog syncPrgDialog, metaPrgDialog;
	private Database controller;
	String list[];
	public static final String PREFS_NAME = "Interface";
	String inter;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//get mac address
		WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		String wifiMacString = wifiInfo.getMacAddress();

		if(rootUtil.isDeviceRooted()) { //only called if device is rooted
			Log.d("root", "yes");

			//for MAC spoofing
			inter = wifiInterfaceName(getApplicationContext());
			Intent myIntent = new Intent(this, IntentService.class);
			myIntent.putExtra("Interface", inter);
			myIntent.putExtra("mac", wifiMacString);
			startService(myIntent);
		}

            // save interface name in shared preference



		syncPrgDialog = new ProgressDialog(this);
		syncPrgDialog.setMessage("Synching SQLite Data with Remote MySQL DB. Please wait...");
		syncPrgDialog.setCancelable(false);

		metaPrgDialog = new ProgressDialog(this);
		metaPrgDialog.setMessage("Retrieving Meta-Data from Remote DB. Please wait...");
		metaPrgDialog.setCancelable(false);

    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_add:
			final Intent addIntent = new Intent(this, ImportMapActivity.class);
			startActivityForResult(addIntent, Utils.Constants.IMPORT_ACT);
			return true;
		case R.id.action_dbm:
			final Intent dbmIntent = new Intent(this, AndroidDatabaseManager.class);
			startActivity(dbmIntent);
			return true;
		case R.id.action_selectMap:
			Intent myIntent = new Intent(this, SelectMap.class);
			startActivityForResult(myIntent, Utils.Constants.SELECT_MAP_ACT);
			return true;
		case R.id.action_info:
			Intent myIntent2 = new Intent(this, Info.class);
			startActivityForResult(myIntent2, Utils.Constants.SELECT_MAP_ACT);
			return true;
		case R.id.action_syncDB:
			syncSQLiteMySQLDB();
			return true;
		case R.id.action_getMetaData:
			getMetaData();
			return true;
		case R.id.action_macs_file:
				IntentService.macsToFile();
				return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void getMetaData() {
		AsyncHttpClient client = new AsyncHttpClient();
		RequestParams params = new RequestParams();
		metaPrgDialog.show();
		client.post("http://eic15.eng.fiu.edu:80/wifiloc/getmeta.php", params, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(int i, Header[] headers, byte[] response) {
				metaPrgDialog.hide();
				updateSQLite(new String(response));
			}

			@Override
			public void onFailure(int statusCode, Header[] headers, byte[] bytes, Throwable throwable) {
				metaPrgDialog.hide();
				if (statusCode == 404) {
					Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
				} else if (statusCode == 500) {
					Toast.makeText(getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(getApplicationContext(), "Unexpected Error occcured! [Most common Error: Device might not be connected to Internet]",
							Toast.LENGTH_LONG).show();
				}
			}
		});

	}

	public void updateSQLite(String response){

		ArrayList<ContentValues> cache = new ArrayList<>();
		try {
			// Extract JSON array from the response
			JSONArray arr = new JSONArray(response);
			System.out.println(arr.length());
			// If no of array elements is not zero
			if(arr.length() != 0){
				// Loop through each array element, get JSON object which has userid and username
				for (int i = 0; i < arr.length(); i++) {
					// Get JSON object
					JSONObject obj = (JSONObject) arr.get(i);
					System.out.println(obj.get("mapx"));
					System.out.println(obj.get("mapy"));

					ContentValues cv = new ContentValues();
					// Add userID extracted from Object
					cv.put("mapx", Float.valueOf(obj.get("mapx").toString()));
					// Add userName extracted from Object
					cv.put("mapy", Float.valueOf(obj.get("mapy").toString()));
					// Insert User into SQLite DB
					cache.add(cv);
				}
				if (cache.isEmpty())
					return;
				// Add readings
				getContentResolver().delete(DataProvider.META_URI,null,null);
				getContentResolver().bulkInsert(DataProvider.META_URI,
						cache.toArray(new ContentValues[] {}));
//				// Reload the Main Activity
//				reloadActivity();
			}
			else
			{
				getContentResolver().delete(DataProvider.META_URI,null,null);
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void syncSQLiteMySQLDB() {
		//Create AsycHttpClient object
		AsyncHttpClient client = new AsyncHttpClient();
		RequestParams params = new RequestParams();
		controller = Database.getInstance(getApplicationContext());
		String jsondata = controller.composeJSONfromSQLite();
		if (!jsondata.isEmpty()) {
			if (controller.dbSyncCount() != 0) {
				syncPrgDialog.show();
				params.put("readingsJSON", jsondata);
				client.post("http://eic15.eng.fiu.edu:80/wifiloc/inserttestreading.php", params, new AsyncHttpResponseHandler() {

					@Override
					public void onSuccess(int i, Header[] headers, byte[] bytes) {
						onSuccess(new String(bytes));
					}

					@Override
					public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
						onFailure(i, throwable, String.valueOf(bytes));
					}

					public void onSuccess(String response) {
						Log.d("onSuccess", response);
						syncPrgDialog.hide();
						try {
							JSONArray arr = new JSONArray(response);
							Log.d("onSuccess", "" + arr.length());
							for (int i = 0; i < arr.length(); i++) {
								JSONObject obj = (JSONObject) arr.get(i);

//								Log.d("onSuccess", "id = " + obj.get("id"));
//								Log.d("onSuccess", "status = " + obj.get("status"));
//								Log.d("onSuccess", "datetime = " + obj.get("datetime"));
//								Log.d("onSuccess", "mapx = " + obj.get("mapx"));
//								Log.d("onSuccess", "mapy = " + obj.get("mapy"));
//								Log.d("onSuccess", "rss = " + obj.get("rss"));

//								Log.d("onSuccess", "ap_name = " + obj.get("ap_name"));
//								Log.d("onSuccess", "mac = " + obj.get("mac"));
//								Log.d("onSuccess", "map = " + obj.get("map"));
								controller.updateSyncStatus(obj.get("id").toString(), obj.get("status").toString());
							}
							Toast.makeText(getApplicationContext(), "DB Sync completed!", Toast.LENGTH_LONG).show();
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							Toast.makeText(getApplicationContext(), "Error Occured [Server's JSON response might be invalid]!", Toast.LENGTH_LONG).show();
							e.printStackTrace();
						}
					}

					public void onFailure(int statusCode, Throwable error,
										  String content) {
						// TODO Auto-generated method stub
						syncPrgDialog.hide();
						if (statusCode == 404) {
							Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
						} else if (statusCode == 500) {
							Toast.makeText(getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
						} else {
							Toast.makeText(getApplicationContext(), "Unexpected Error occcured! [Most common Error: Device might not be connected to Internet]", Toast.LENGTH_LONG).show();
						}
					}
				});
			} else {
				Toast.makeText(getApplicationContext(), "SQLite and Remote MySQL DBs are in Sync!", Toast.LENGTH_LONG).show();
			}
		} else {
			Toast.makeText(getApplicationContext(), "No data in SQLite DB, please do enter User name to perform Sync action", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		final ContentValues values = new ContentValues();

		switch (requestCode) {
		case Utils.Constants.IMPORT_ACT:
			if (resultCode == RESULT_OK) {
				values.put(Database.Maps.NAME,
						data.getStringExtra(Utils.Constants.MAP_NAME_EXTRA));
				values.put(Database.Maps.DATA,
						data.getStringExtra(Utils.Constants.MAP_URI_EXTRA));
				values.put(Database.Maps.DATE_ADDED, System.currentTimeMillis());
				getContentResolver().insert(DataProvider.MAPS_URI, values);

			}
			break;
			case Utils.Constants.SELECT_MAP_ACT:
				if(resultCode == RESULT_OK){
					values.put(Database.Maps.NAME, data.getStringExtra(Utils.Constants.MAP_NAME_EXTRA));
					values.put(Database.Maps.DATA, data.getStringExtra(Utils.Constants.MAP_URI_EXTRA));
					values.put(Database.Maps.DATE_ADDED, System.currentTimeMillis());
					getContentResolver().insert(DataProvider.MAPS_URI, values);

				}

			default:
			super.onActivityResult(requestCode, resultCode, data);
			break;
		}
	}

    private void showAlertDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Instructions")
                .setMessage("Hello! To begin, select a preexisting map by clicking the gallery icon or upload your" +
						" own map by navigating to the + icon.")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setIcon(R.drawable.ic_launcher)
                .show();
    }

	public static NetworkInterface getActiveWifiInterface(Context context) throws SocketException, UnknownHostException {
		WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		//Return dynamic information about the current Wi-Fi connection, if any is active.
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		if(wifiInfo == null){
			Log.d("my log", "wifi null");
			return null;
		}
		InetAddress address = intToInet(wifiInfo.getIpAddress());
		return NetworkInterface.getByInetAddress(address);
	}

	public static byte byteOfInt(int value, int which) {
		int shift = which * 8;
		return (byte)(value >> shift);
	}

	public static InetAddress intToInet(int value) {
		byte[] bytes = new byte[4];
		for(int i = 0; i<4; i++) {
			bytes[i] = byteOfInt(value, i);
		}
		try {
			return InetAddress.getByAddress(bytes);
		} catch (UnknownHostException e) {
			// This only happens if the byte array has a bad length
			return null;
		}
	}

	public static String wifiInterfaceName(final Context context) {

		// Get WiFi interface's MAC address as a BigInteger.
		WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		String wifiMacString = wifiInfo.getMacAddress();




		byte[] wifiMacBytes = macAddressToByteArray(wifiMacString);
		BigInteger wifiMac = new BigInteger(wifiMacBytes);

		String result = null;
		try {
			List<NetworkInterface> networkInterfaces
					= Collections.list(NetworkInterface.getNetworkInterfaces());

			for (NetworkInterface currentInterface : networkInterfaces) {

				byte[] hardwareAddress = currentInterface.getHardwareAddress();
				if (hardwareAddress != null) {
					BigInteger currentMac = new BigInteger(hardwareAddress);

					if (currentMac.equals(wifiMac)) {
						result = currentInterface.getName();
						break;
					}
				}
			}
		} catch (SocketException ex) {
			Log.e("WifiGet", "Socket excpetion: " + ex.getMessage());
		}


		return result;
	}

	protected static byte[] macAddressToByteArray(String macString) {
		String[] mac = macString.split("[:\\s-]");
		byte[] macAddress = new byte[6];
		for (int i = 0; i < mac.length; i++) {
			macAddress[i] = Integer.decode("0x" + mac[i]).byteValue();
		}

		return macAddress;
	}




}
