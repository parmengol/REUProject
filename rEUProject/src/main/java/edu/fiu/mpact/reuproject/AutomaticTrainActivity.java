package edu.fiu.mpact.reuproject;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * FIXME make TrainActivity and AutomaticTrainActivity extend from a common
 * class
 * 
 * @author oychang
 *
 */
public class AutomaticTrainActivity extends Activity {

	protected boolean mIsGathering = false;
	protected Button mButton;
	protected TextView mCountView;
	protected int mCount = 0;

	protected Handler mHandler;
	private Runnable mAutoScanner = new Runnable() {
		@Override
		public void run() {
			mWifiManager.startScan();
			mHandler.postDelayed(mAutoScanner, Utils.Constants.SCAN_INTERVAL);
		}
	};

	protected long mMapId;
	private Deque<ContentValues> mCachedResults = new LinkedList<ContentValues>();

	private WifiManager mWifiManager;
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final List<ScanResult> results = mWifiManager.getScanResults();
			for (ScanResult result : results) {
				ContentValues values = new ContentValues();
				values.put(Database.Readings.DATETIME,
						System.currentTimeMillis());
				values.put(Database.Readings.SIGNAL_STRENGTH, result.level);
				values.put(Database.Readings.AP_NAME, result.SSID);
				values.put(Database.Readings.MAC, result.BSSID);
				values.put(Database.Readings.MAP_ID, mMapId);
				mCachedResults.add(values);
			}

			mCountView.setText(Integer.toString(++mCount));
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_automatic_train);

		mMapId = getIntent().getExtras().getLong(Utils.Constants.MAP_ID_EXTRA);
		mButton = (Button) findViewById(R.id.btn_toggle_training);
		mCountView = (TextView) findViewById(R.id.text_readings_count);
		mHandler = new Handler();
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
		mHandler.removeCallbacks(mAutoScanner);
	}

	public void toggleTraining(View _) {
		if (mIsGathering) {
			mButton.setText(R.string.btn_start_automatic_train);
			mHandler.removeCallbacks(mAutoScanner);
		} else {
			mButton.setText(R.string.btn_end_automatic_train);
			mAutoScanner.run();
		}

		mIsGathering = !mIsGathering;
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
