package edu.fiu.mpact.reuproject;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import uk.co.senab.photoview.PhotoViewAttacher;
import uk.co.senab.photoview.PhotoViewAttacher.OnPhotoTapListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class TrainActivity extends Activity {
	private long mMapId;

	private boolean markerPlaced = false;
	private Deque<ContentValues> mCachedResults = new LinkedList<ContentValues>();

	private ImageView mImg;
	private float[] mImgLocation = new float[2];
	private PhotoViewAttacher mAttacher;
	private RelativeLayout mRelative;

	private AlertDialog mDialog;
	private WifiManager mWifiManager;
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mDialog.hide();

			//mAttacher.addData(Utils.createNewMarker(getApplicationContext(),
			//		mRelative, mImgLocation[0], mImgLocation[1]));

			final List<ScanResult> results = mWifiManager.getScanResults();
			for (ScanResult result : results) {
				ContentValues values = new ContentValues();
				values.put(Database.Readings.DATETIME,
						System.currentTimeMillis());
				values.put(Database.Readings.MAP_X, mImgLocation[0]);
				values.put(Database.Readings.MAP_Y, mImgLocation[1]);
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
		setContentView(R.layout.activity_train);

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

		final int[] imgSize = Utils.getImageSize(img);
		mImg.setImageURI(img);
		mAttacher = new PhotoViewAttacher(mImg, imgSize);
		mAttacher.setOnPhotoTapListener(new OnPhotoTapListener() {
			@Override
			public void onPhotoTap(View view, float x, float y) {

				mImgLocation[0] = x * imgSize[0];
				mImgLocation[1] = y * imgSize[1];

                if (markerPlaced)
                    mAttacher.removeLastMarkerAdded();
				mAttacher.addData(Utils.createNewMarker(getApplicationContext(),
						mRelative, mImgLocation[0], mImgLocation[1]));
                markerPlaced = true;

				// need to imp a lock feature so i can move before scanning
				// show temp marker here then replace with locked marker
				// how to remove temp makers?
				//mDialog.show();
				//mWifiManager.startScan();
			}
		});

		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		registerReceiver(mReceiver, filter);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mReceiver);
		mDialog.dismiss();
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
                markerPlaced = false;
                mDialog.show();
                mWifiManager.startScan();
            }
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
