package edu.fiu.mpact.TrainingReuProject;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
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
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
	private LinkedList<ContentValues> mCachedResults = new LinkedList<ContentValues>();

	private WifiManager mWifiManager;
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mDialog.hide();

			mAttacher.removeLastMarkerAdded();
			final PhotoMarker mrk = Utils.createNewMarker(getApplicationContext(),
					mRelative, tempx, tempy, R.drawable.red_x);
			mrk.marker.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					PopupMenu popup = new PopupMenu(AutomaticTrainActivity.this,mrk.marker);
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
			//markerPlaced = false;

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
				values.put(Database.Readings.UPDATE_STATUS, 0);
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


		Map<Utils.TrainLocation, ArrayList<Utils.APValue>> mCachedMapData = Utils.gatherLocalizationData(getContentResolver(),
				mMapId);
		Deque<PhotoMarker> mrkrs = Utils.generateMarkers(mCachedMapData,
				getApplicationContext(), mRelative);
		for (final PhotoMarker point : mrkrs)
		{
			point.marker.setImageResource(R.drawable.grey_x);
			point.marker.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (markerPlaced)
						mAttacher.removeLastMarkerAdded();
					markerPlaced = true;
					tempx = point.x;
					tempy = point.y;
					mAttacher.addData(Utils.createNewMarker(getApplicationContext(),mRelative,point.x,point.y,R.drawable.x));
				}
			});
		}
		mAttacher.addData(mrkrs);

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
				markerPlaced = false;
				mDialog.show();
				mWifiManager.startScan();
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void saveTraining() {
		if (mCachedResults.isEmpty())
			return;
		List<ContentValues> valuesToInsert = new ArrayList<ContentValues>();
		for ( ContentValues values: mCachedResults ) {
			String[] mSelectionArgs = {String.valueOf(values.getAsFloat("mapx")-0.0001), String.valueOf(values.getAsFloat("mapx")+0.0001),
					String.valueOf(values.getAsFloat("mapy")-0.0001), String.valueOf(values.getAsFloat("mapy")+0.0001), values.getAsString("mac")};
			if ( getContentResolver().update( DataProvider.READINGS_URI, values,
					"mapx>? and mapx<? and mapy>? and mapy<? and mac=?", mSelectionArgs) == 0 ) {
				valuesToInsert.add( values );
			}
		}

		getContentResolver().bulkInsert(DataProvider.READINGS_URI,
				valuesToInsert.toArray(new ContentValues[] {}));
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
}