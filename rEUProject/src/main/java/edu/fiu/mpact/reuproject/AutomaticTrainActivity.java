package edu.fiu.mpact.reuproject;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.Image;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import uk.co.senab.photoview.PhotoMarker;
import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * FIXME make TrainActivity and AutomaticTrainActivity extend from a common
 * class
 * 
 * @author oychang
 *
 */
public class AutomaticTrainActivity extends Activity {

	private TextView mCountView;
	private AlertDialog mDialog;
	private RelativeLayout mRelative;
	private PhotoViewAttacher mAttacher;
	private ImageView mImg;
	private Map<Utils.TrainLocation, ArrayList<Utils.APValue>> mCachedMapData;
	private Deque<PhotoMarker> mPoints;
	private float tempx, tempy;
	private boolean markerPlaced;

	private int mCount = 0;

	private long mMapId;
	private Deque<ContentValues> mCachedResults = new LinkedList<ContentValues>();

	private WifiManager mWifiManager;
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mDialog.hide();

			mAttacher.removeLastMarkerAdded();
			PhotoMarker mrk = Utils.createNewMarker(getApplicationContext(),
					mRelative, tempx, tempy, R.drawable.red_x);
			markerPlaced = false;

			//registerForContextMenu(mrk.marker);

			mAttacher.addData(mrk);

			final List<ScanResult> results = mWifiManager.getScanResults();
			for (ScanResult result : results) {
				ContentValues values = new ContentValues();
				values.put(Database.Readings.DATETIME,
						System.currentTimeMillis());
				values.put(Database.Readings.MAP_X, tempx);
				values.put(Database.Readings.MAP_Y, tempy);
				values.put(Database.Readings.SIGNAL_STRENGTH, result.level);
				values.put(Database.Readings.AP_NAME, result.SSID);
				values.put(Database.Readings.MAC, result.BSSID);
				values.put(Database.Readings.MAP_ID, mMapId);
				mCachedResults.add(values);
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_automatic_train);

		mMapId = getIntent().getExtras().getLong(Utils.Constants.MAP_ID_EXTRA);
		mDialog = new AlertDialog.Builder(this).create();
		mDialog.setTitle(getString(R.string.dialog_scanning_title));
		mDialog.setMessage(getString(R.string.dialog_scanning_description));
		mDialog.setCancelable(false);
		mDialog.setCanceledOnTouchOutside(false);

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

		final int[] imgSize = Utils.getImageSize(img, getApplicationContext());
		mImg.setImageURI(img);
		mAttacher = new PhotoViewAttacher(mImg, imgSize);
		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		registerReceiver(mReceiver, filter);


		mPoints = Utils.gatherSamples(
				getContentResolver(), getApplicationContext(), mRelative,
				mMapId);
		for (final PhotoMarker point : mPoints)
		{
			point.marker.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (markerPlaced)
						mAttacher.removeLastMarkerAdded();
					markerPlaced = true;
					tempx = point.x;
					tempy = point.y;
					mAttacher.addData(Utils.createNewMarker(getApplicationContext(),mRelative,point.x,point.y,R.drawable.o));
				}
			});
		}
		mAttacher.addData(mPoints);

		// get points from metadata table and draw markers
		// set onclick to add to cache and add different color marker
		// save to write to db

		//showAlertDialog();

		//mCountView = (TextView) findViewById(R.id.text_readings_count);
	}

	@Override
	protected void onResume() {
		super.onResume();

		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		registerReceiver(mReceiver, filter);
	}

	@Override
	protected void onPause() {
		super.onPause();

		unregisterReceiver(mReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.automatic_train, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_save:
			saveTraining();
			finish();
			return true;
		case R.id.action_lock:
			if (markerPlaced) {
				mDialog.show();
				mWifiManager.startScan();
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void saveTraining() {
		// Add readings
		getContentResolver().bulkInsert(DataProvider.READINGS_URI,
				mCachedResults.toArray(new ContentValues[] {}));

		// Add this as a session
		ContentValues session = new ContentValues();
		session.put(Database.Sessions.TIME, System.currentTimeMillis());
		session.put(Database.Sessions.MAP_ID, mMapId);
		session.put(Database.Sessions.SDK_VERSION, Build.VERSION.SDK_INT);
		session.put(Database.Sessions.MANUFACTURER, Build.MANUFACTURER);
		session.put(Database.Sessions.MODEL, Build.MODEL);
		getContentResolver().insert(DataProvider.SESSIONS_URI, session);
	}
}
