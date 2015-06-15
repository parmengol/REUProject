package edu.fiu.mpact.reuproject;


import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import uk.co.senab.photoview.PhotoMarker;
import uk.co.senab.photoview.PhotoViewAttacher;
import uk.co.senab.photoview.PhotoViewAttacher.OnPhotoTapListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.Image;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class TrainActivity extends Activity {
	private long mMapId;

	private boolean markerPlaced = false;
	private LinkedList<ContentValues> mCachedResults = new LinkedList<ContentValues>();

	private ImageView mImg;
	private float[] mImgLocation = new float[2];
	private PhotoViewAttacher mAttacher;
	private RelativeLayout mRelative;

	private AlertDialog mDialog;
	private WifiManager mWifiManager;
	public static final String PREFS_NAME = "MyPrefsFile2";
	private static int sessionNum = 0;


	private PhotoMarker mrk;
	private ImageView selMrk;

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mDialog.hide();

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
				values.put(Database.Readings.SESSION_ID, sessionNum + 1);

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
		if (mCachedResults.isEmpty())
			return;
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

       session.size();
//		Toast.makeText(getApplicationContext(), "Thanks for training!",
//				Toast.LENGTH_LONG).show();
		sessionNum++;


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
				.setMessage("Find where you are on the map and click on your location. When you are done, click the " +
						"\"Lock \" button. You can train multiple spots, one after another after locking. Make" +
						" sure to \"Save\" at the end. You can remove a trained location by holding down the X.")
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				})
				.setIcon(R.drawable.ic_launcher)
				.show();
	}



}

