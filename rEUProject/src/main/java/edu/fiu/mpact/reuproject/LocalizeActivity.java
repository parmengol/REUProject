package edu.fiu.mpact.reuproject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import uk.co.senab.photoview.PhotoMarker;
import uk.co.senab.photoview.PhotoViewAttacher;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
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
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

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
	private Runnable runnable;
	private Handler mHandler;
	private boolean auto = false;

	protected Map<TrainLocation, ArrayList<APValue>> mCachedMapData;
	protected LocalizationEuclideanDistance mAlgo = null;
	public static final String PREFS_NAME = "MyPrefsFile3";

	private WifiManager mWifiManager;
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("LocalizeActivity", "onReceive start");
			final List<ScanResult> results = mWifiManager.getScanResults();
			if (auto == true)
				mWifiManager.startScan();
			float[] bestGuess = mAlgo.localize(results);

			float cx = (bestGuess[0] + bestGuess[2] + bestGuess[4])/3;
			float cy = (bestGuess[1] + bestGuess[3] + bestGuess[5])/3;

			final PhotoMarker mark = Utils.createNewMarker(
					getApplicationContext(), mRelative, cx,
					cy, R.drawable.o);

			final PhotoMarker bestguess = Utils.createNewMarker(
					getApplicationContext(), mRelative, bestGuess[0],
					bestGuess[1], R.drawable.red_x);

			final PhotoMarker secondguess = Utils.createNewMarker(
					getApplicationContext(), mRelative, bestGuess[2],
					bestGuess[3], R.drawable.bluegreen_x);

			final PhotoMarker thirdguess = Utils.createNewMarker(
					getApplicationContext(), mRelative, bestGuess[4],
					bestGuess[5], R.drawable.bluegreen_x);

//			final PhotoMarker mark = Utils.createNewMarker(
//					getApplicationContext(), mRelative, bestGuess[0],
//					bestGuess[1], R.drawable.o);

			if (mHavePlacedMarker)
				for (int i = 0; i < 4; i++)
					mAttacher.removeLastMarkerAdded();
			mAttacher.addData(mark);
			mAttacher.addData(bestguess);
			mAttacher.addData(secondguess);
			mAttacher.addData(thirdguess);
			mHavePlacedMarker = true;
			Log.d("LocalizeActivity", "onReceive end");
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
		mAttacher = new PhotoViewAttacher(mImg, Utils.getImageSize(img, getApplicationContext()));

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

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		boolean dialogShown = settings.getBoolean("dialogShown3", false);

		if (!dialogShown) {
			showAlertDialog();

			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("dialogShown3", true);
			editor.commit();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mReceiver);
	}

	public void onToggleClicked(View view)
	{
		// Is the toggle on?
		boolean on = ((ToggleButton) view).isChecked();

		if (on) {
			auto = true;
			mWifiManager.startScan();
		} else {
			auto = false;
		}
	}

	public void localizeNow(View _)
	{
		//Log.d("LocalizeActivity", "localizenow");
		mWifiManager.startScan();
	}

	private void showAlertDialog() {
		new AlertDialog.Builder(this)
				.setTitle("Instructions")
				.setMessage("Find your current location by clicking Localize. Automatically" +
						" find your location by turning on Auto-Localize. With this, you can move to different" +
								" locations and the red dot will follow your movement.")
								.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				})
				.setIcon(R.drawable.ic_launcher)
				.show();
	}
}
