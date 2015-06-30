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
import android.media.tv.TvContract;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

//import com.loopj.android.http.AsyncHttpClient;
//import com.loopj.android.http.AsyncHttpResponseHandler;
//import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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

	private ProgressDialog prgDialog;
	private Database controller;
	Process p = null;
	String list[];
	private static boolean inBackground = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if(rootUtil.isDeviceRooted()){
			Log.d("root", "yes");
		}
		else
			Log.d("root", "no");

		   // for MAC spoofing
//		    Intent myIntent = new Intent(this, IntentService.class);
//			startService(myIntent);

        if (savedInstanceState == null) {
            showAlertDialog();
        }

		prgDialog = new ProgressDialog(this);
		prgDialog.setMessage("Synching SQLite Data with Remote MySQL DB. Please wait...");
		prgDialog.setCancelable(false);
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
//		case R.id.action_syncDB:
//			syncSQLiteMySQLDB();
//			return true;
//			//syncSQLiteMySQLDB();

		default:
			return super.onOptionsItemSelected(item);
		}
	}

//	public void syncSQLiteMySQLDB(){
//		//Create AsycHttpClient object
//		AsyncHttpClient client = new AsyncHttpClient();
//		RequestParams params = new RequestParams();
//		controller = Database.getInstance(getApplicationContext());
//		String jsondata = controller.composeJSONfromSQLite();
//		Log.d("sync", jsondata);
//		if(!jsondata.isEmpty()){
//			if(controller.dbSyncCount() != 0){
//				prgDialog.show();
//				params.put("readingsJSON", jsondata);
//				client.post("http://eic15.eng.fiu.edu:80/wifiloc/insertreading.php",params ,new AsyncHttpResponseHandler() {
//
//					@Override
//					public void onSuccess(int i, Header[] headers, byte[] bytes) {
//						onSuccess(new String(bytes));
//					}
//
//					@Override
//					public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
//						onFailure(i, throwable, String.valueOf(bytes));
//					}
//
//					public void onSuccess(String response) {
//						Log.d("onSuccess", response);
//						prgDialog.hide();
//						try {
//							JSONArray arr = new JSONArray(response);
//							Log.d("onSuccess", ""+arr.length());
//							for(int i=0; i<arr.length();i++){
//								JSONObject obj = (JSONObject)arr.get(i);
//								Log.d("onSuccess", "id = " + obj.get("id"));
//								Log.d("onSuccess", "status = " + obj.get("status"));
//								Log.d("onSuccess", "datetime = " + obj.get("datetime"));
//								Log.d("onSuccess", "mapx = " + obj.get("mapx"));
//								Log.d("onSuccess", "mapy = " + obj.get("mapy"));
//								Log.d("onSuccess", "rss = " + obj.get("rss"));
//								Log.d("onSuccess", "apname = " + obj.get("apname"));
//								Log.d("onSuccess", "mac = " + obj.get("mac"));
//								Log.d("onSuccess", "map = " + obj.get("map"));
//								controller.updateSyncStatus(obj.get("id").toString(),obj.get("status").toString());
//							}
//							Toast.makeText(getApplicationContext(), "DB Sync completed!", Toast.LENGTH_LONG).show();
//						} catch (JSONException e) {
//							// TODO Auto-generated catch block
//							Toast.makeText(getApplicationContext(), "Error Occured [Server's JSON response might be invalid]!", Toast.LENGTH_LONG).show();
//							e.printStackTrace();
//						}
//					}
//
//					public void onFailure(int statusCode, Throwable error,
//										  String content) {
//						// TODO Auto-generated method stub
//						prgDialog.hide();
//						if(statusCode == 404){
//							Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
//						}else if(statusCode == 500){
//							Toast.makeText(getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
//						}else{
//							Toast.makeText(getApplicationContext(), "Unexpected Error occcured! [Most common Error: Device might not be connected to Internet]", Toast.LENGTH_LONG).show();
//						}
//					}
//				});
//			}else{
//				Toast.makeText(getApplicationContext(), "SQLite and Remote MySQL DBs are in Sync!", Toast.LENGTH_LONG).show();
//			}
//		}else{
//			Toast.makeText(getApplicationContext(), "No data in SQLite DB, please do enter User name to perform Sync action", Toast.LENGTH_LONG).show();
//		}
//	}

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
	private static int sessionDepth = 0;

	@Override
	protected void onStart() {
		super.onStart();
		sessionDepth++;
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (sessionDepth > 0)
			sessionDepth--;
		if (sessionDepth == 0) {
			inBackground = true;
		}
		else{
			inBackground = false;
		}
	}

}
