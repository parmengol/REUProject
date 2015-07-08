package edu.fiu.mpact.TrainingReuProject;


import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import uk.co.senab.photoview.PhotoMarker;
import uk.co.senab.photoview.PhotoViewAttacher;
import uk.co.senab.photoview.PhotoViewAttacher.OnPhotoTapListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TrainActivity extends Activity {
	private long mMapId;

	private boolean markerPlaced = false;
	private LinkedList<ContentValues> mCachedResults = new LinkedList<ContentValues>();

	private ImageView mImg;
	private float[] mImgLocation = new float[2];
	private PhotoViewAttacher mAttacher;
	private RelativeLayout mRelative;
	private Database controller;


	private WifiManager mWifiManager;
	public static final String PREFS_NAME = "MyPrefsFile2";
	private int scanNum = 0;
	private HashSet bssidSet;


	private PhotoMarker mrk;
	private ImageView selMrk;

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final List<ScanResult> results = mWifiManager.getScanResults();
			for (ScanResult result : results) {
				if (bssidSet.contains(result.BSSID))
					continue;
				bssidSet.add(result.BSSID);
				ContentValues values = new ContentValues();
				values.put(Database.Readings.DATETIME,
						System.currentTimeMillis());
				values.put(Database.Readings.MAP_X, mImgLocation[0]);
				values.put(Database.Readings.MAP_Y, mImgLocation[1]);
				values.put(Database.Readings.SIGNAL_STRENGTH, result.level);
				values.put(Database.Readings.AP_NAME, result.SSID);
				values.put(Database.Readings.MAC, result.BSSID);
				values.put(Database.Readings.MAP_ID, mMapId);
				values.put(Database.Readings.UPDATE_STATUS, 0);

				mCachedResults.add(values);
			}
			System.out.println(bssidSet.size());
			scanNum++;
			mPrgBarDialog.setProgress(scanNum	);
			if (scanNum < 8) {
				mWifiManager.startScan();
				return;
			}
			scanNum = 0;
			mPrgBarDialog.hide();
			Toast.makeText(getApplicationContext(), "If you are done training locations, please don't forget to SAVE above!", Toast.LENGTH_LONG).show();

			mAttacher.removeLastMarkerAdded();
			final PhotoMarker mrk = Utils.createNewMarker(getApplicationContext(),
					mRelative, mImgLocation[0], mImgLocation[1], R.drawable.red_x);
			mrk.marker.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					PopupMenu popup = new PopupMenu(TrainActivity.this,mrk.marker);
					popup.getMenuInflater().inflate(R.menu.marker,popup.getMenu());
					popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							switch (item.getItemId()) {
								case R.id.action_delete_cmenu:
									mrk.marker.setVisibility(View.GONE);
									onDelete(mrk.x, mrk.y);
									return true;
								default:
									return true;
							}
						}
					});
					popup.show();
					return true;
				}});

			mAttacher.addData(mrk);
		}
	};
	private ProgressDialog mPrgBarDialog;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_train);

		mMapId = getIntent().getExtras().getLong(Utils.Constants.MAP_ID_EXTRA);

		mPrgBarDialog = new ProgressDialog(this);
		mPrgBarDialog.setTitle(getString(R.string.dialog_scanning_title));
		mPrgBarDialog.setMessage(getString(R.string.dialog_scanning_description));
		mPrgBarDialog.setCancelable(false);
		mPrgBarDialog.setCanceledOnTouchOutside(false);
		mPrgBarDialog.setProgressStyle(mPrgBarDialog.STYLE_HORIZONTAL);
		mPrgBarDialog.setProgress(0);
		mPrgBarDialog.setMax(8);


		mRelative = (RelativeLayout) findViewById(R.id.image_map_container);

		mImg = (ImageView) findViewById(R.id.image_map);
		final Cursor cursor = getContentResolver().query(
				ContentUris.withAppendedId(DataProvider.MAPS_URI, mMapId),
				null, null, null, null);
		if (!cursor.moveToFirst()) {
			Toast.makeText(this,
					getResources().getText(R.string.toast_map_id_warning),
					Toast.LENGTH_LONG).show();
			cursor.close();
			finish();
			return;
		}
		final Uri img = Uri.parse(cursor.getString(cursor
				.getColumnIndex(Database.Maps.DATA)));
		cursor.close();

		bssidSet = new HashSet();

		final int[] imgSize = Utils.getImageSize(img, getApplicationContext());
		mImg.setImageURI(img);
		mAttacher = new PhotoViewAttacher(mImg, imgSize);
		mAttacher.setOnPhotoTapListener(new OnPhotoTapListener() {
			@Override
			public void onPhotoTap(View view, float x, float y) {

				mImgLocation[0] = x * imgSize[0];
				mImgLocation[1] = y * imgSize[1];

				if (markerPlaced)
					mAttacher.removeLastMarkerAdded();
				PhotoMarker tmpmrk = Utils.createNewMarker(getApplicationContext(),
						mRelative, mImgLocation[0], mImgLocation[1]);
				mAttacher.addData(tmpmrk);
				markerPlaced = true;
			}
		});

		Map<Utils.TrainLocation, ArrayList<Utils.APValue>> mCachedMapData = Utils.gatherLocalizationData(getContentResolver(),
				mMapId);
		Deque<PhotoMarker> mrkrs = Utils.generateMarkers(mCachedMapData,
				getApplicationContext(), mRelative);
		for (PhotoMarker mrk : mrkrs) {
			mrk.marker.setAlpha(0.8f);
			mrk.marker.setImageResource(R.drawable.grey_x);
		}
		mAttacher.addData(mrkrs);


		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		registerReceiver(mReceiver, filter);

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		boolean dialogShown = settings.getBoolean("dialogShown2", false);

		if (!dialogShown) {
			showAlertDialog();

			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("dialogShown2", true);
			editor.commit();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mReceiver);
		mPrgBarDialog.dismiss();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.train, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_save:
				saveTraining();
				setResult(RESULT_OK);
				finish();
				return true;
			case R.id.action_lock:
				if (markerPlaced) {
					bssidSet = new HashSet();
					markerPlaced = false;
					mPrgBarDialog.setProgress(0);
					mPrgBarDialog.show();
					mWifiManager.startScan();
				}
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void saveTraining() {
		if (mCachedResults.isEmpty())
			return;
		// Add readings

		getContentResolver().bulkInsert(DataProvider.READINGS_URI,
				mCachedResults.toArray(new ContentValues[] {}));

	}

	private void onDelete(float x, float y)
	{
		float cachex, cachey;
		ContentValues val;
		ListIterator<ContentValues> iter = mCachedResults.listIterator();
		while (iter.hasNext())
		{
			val = iter.next();
			cachex = val.getAsFloat(Database.Readings.MAP_X);
			cachey = val.getAsFloat(Database.Readings.MAP_Y);
			if (cachex == x && cachey == y)
			{
				iter.remove();
			}
		}
	}

	private void showAlertDialog() {
		new AlertDialog.Builder(this)
				.setTitle("Instructions")
				.setMessage("Find where you are on the map and click on your location. You can change where" +
						" you placed your X by clicking elsewhere. When you are sure of where your X is placed (your exact location), click the " +
						"\"Lock \" button. You can train multiple spots, one after another after locking. Make" +
						" sure to \"Save\" above at the end.")
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				})
				.setIcon(R.drawable.ic_launcher)
				.show();
	}

	public void syncSQLiteMySQLDB(){
		//Create AsycHttpClient object
		AsyncHttpClient client = new AsyncHttpClient();
		RequestParams params = new RequestParams();
		controller = Database.getInstance(getApplicationContext());
		String jsondata = controller.composeJSONfromSQLite();
		if(!jsondata.isEmpty()){
			if(controller.dbSyncCount() != 0){
				//syncPrgDialog.show();
				params.put("readingsJSON", jsondata);
				client.post("http://eic15.eng.fiu.edu:80/wifiloc/inserttestreading.php",params ,new AsyncHttpResponseHandler() {

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
						//syncPrgDialog.hide();
						try {
							JSONArray arr = new JSONArray(response);
							Log.d("onSuccess", ""+arr.length());
							for(int i=0; i<arr.length();i++){
								JSONObject obj = (JSONObject)arr.get(i);
//								Log.d("onSuccess", "id = " + obj.get("id"));
//								Log.d("onSuccess", "status = " + obj.get("status"));
//								Log.d("onSuccess", "datetime = " + obj.get("datetime"));
//								Log.d("onSuccess", "mapx = " + obj.get("mapx"));
//								Log.d("onSuccess", "mapy = " + obj.get("mapy"));
//								Log.d("onSuccess", "rss = " + obj.get("rss"));
//								Log.d("onSuccess", "ap_name = " + obj.get("ap_name"));
//								Log.d("onSuccess", "mac = " + obj.get("mac"));
//								Log.d("onSuccess", "map = " + obj.get("map"));
								controller.updateSyncStatus(obj.get("id").toString(),obj.get("status").toString());
							}
							Toast.makeText(getApplicationContext(), "Thanks for training!", Toast.LENGTH_LONG).show();
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							Toast.makeText(getApplicationContext(), "Error Occured [Server's JSON response might be invalid]!", Toast.LENGTH_LONG).show();
							e.printStackTrace();
						}
					}

					public void onFailure(int statusCode, Throwable error,
										  String content) {
						// TODO Auto-generated method stub
						//syncPrgDialog.hide();
						if(statusCode == 404){
							Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
						}else if(statusCode == 500){
							Toast.makeText(getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
						}else{
							Toast.makeText(getApplicationContext(), "Unexpected Error occcured! [Most common Error: Device might not be connected to Internet]", Toast.LENGTH_LONG).show();
						}
					}
				});
			}else{
				Toast.makeText(getApplicationContext(), "SQLite and Remote MySQL DBs are in Sync!", Toast.LENGTH_LONG).show();
			}
		}else{
			Toast.makeText(getApplicationContext(), "No data in SQLite DB, please do enter User name to perform Sync action", Toast.LENGTH_LONG).show();
		}
	}


}
