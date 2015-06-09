package edu.fiu.mpact.reuproject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.co.senab.photoview.PhotoMarker;
import uk.co.senab.photoview.PhotoViewAttacher;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import edu.fiu.mpact.reuproject.Utils.APValue;
import edu.fiu.mpact.reuproject.Utils.TrainLocation;

/**
 * A lot in common with TrainActivity.java
 * 
 * @author oychang
 *
 */
public class LocalizeActivity extends Activity {
	private long mMapId;
	private ImageView mImg;
	private RelativeLayout mRelative;
	private PhotoViewAttacher mAttacher;
	private boolean mHavePlacedMarker = false;

	protected Map<TrainLocation, ArrayList<APValue>> mCachedMapData;
	protected LocalizationEuclideanDistance mAlgo = null;

	private WifiManager mWifiManager;
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final List<ScanResult> results = mWifiManager.getScanResults();
			float[] bestGuess = mAlgo.localize(results);

			final PhotoMarker mark = Utils.createNewMarker(
					getApplicationContext(), mRelative, bestGuess[0],
					bestGuess[1], R.drawable.o);

			if (mHavePlacedMarker)
				mAttacher.removeLastMarkerAdded();
			mAttacher.addData(mark);
			mHavePlacedMarker = true;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_localize);

		mMapId = getIntent().getExtras().getLong(Utils.Constants.MAP_ID_EXTRA);
		mImg = (ImageView) findViewById(R.id.image_map);

		// Get image URI
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

		// Setup PhotoViewAttacher
		mImg.setImageURI(img);
		mRelative = (RelativeLayout) findViewById(R.id.image_map_container);
		mAttacher = new PhotoViewAttacher(mImg, Utils.getImageSize(img));

		mCachedMapData = Utils.gatherLocalizationData(getContentResolver(),
				mMapId);
		mAttacher.addData(Utils.generateMarkers(mCachedMapData,
				getApplicationContext(), mRelative));

		mAlgo = new LocalizationEuclideanDistance();
		mAlgo.setup(mCachedMapData);

		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		registerReceiver(mReceiver, filter);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mReceiver);
	}

	public void localizeNow(View _) {
		mWifiManager.startScan();
	}
}
