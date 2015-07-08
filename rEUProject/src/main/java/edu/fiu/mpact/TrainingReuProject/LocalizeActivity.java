package edu.fiu.mpact.TrainingReuProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

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
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;

import edu.fiu.mpact.TrainingReuProject.Utils.APValue;
import edu.fiu.mpact.TrainingReuProject.Utils.TrainLocation;

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
	private boolean remote = false;
	private RadioButton cb1, cb2, cb3, cb4;
	private int opt = 1;
	private PrivateKey sk;
	private PublicKey pk;

	protected Map<TrainLocation, ArrayList<APValue>> mCachedMapData = null;
	protected Map<TrainLocation, ArrayList<APValue>> mFileData = null;
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
			switch (opt) {
				case 1:
					mAlgo.setup(mCachedMapData, LocalizeActivity.this);
					mAlgo.localize(results);
					break;
				case 2:
					mAlgo.remoteLocalize(results, mMapId);
					break;
				case 3:
					mAlgo.remoteLocalize2(results, mMapId);
					break;
				case 4:
					mAlgo.remoteLocalize3(results, mMapId);
					break;
				case 5:
					mAlgo.remotePrivLocalize(results, mMapId, sk, pk);
					break;
				case 7:
					mAlgo.setup(mFileData, LocalizeActivity.this);
					mAlgo.localize(results);
					break;
				case 6:
					mAlgo.remotePrivLocalize2(results, mMapId, sk, pk);
					break;
				default:
					break;
			}
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

		//mAttacher.addData(Utils.generateMarkers(mCachedMapData,
				//getApplicationContext(), mRelative));

		mAlgo = new LocalizationEuclideanDistance();

		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		registerReceiver(mReceiver, filter);



		sk = new PrivateKey(512);
		pk = new PublicKey();
		Paillier.keyGen(sk, pk);

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		boolean dialogShown = settings.getBoolean("dialogShown3", false);

		if (!dialogShown) {
			showAlertDialog();

			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("dialogShown3", true);
			editor.commit();
		}

		try {
			mFileData = loadFileData(mMapId);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mReceiver);
	}

	public void onClickedCheckBox(View view)
	{
		switch (view.getId()) {
			case R.id.checkBoxLocal:
				opt = 1;
				break;
			case R.id.checkBoxRemote:
				opt = 2;
//				cb1.setChecked(false);
//				cb3.setChecked(false);
//				cb4.setChecked(false);

				break;
			case R.id.checkBoxRemote2:
				opt = 3;
				break;
			case R.id.checkBoxRemote3:
				opt = 4;
				break;
			case R.id.checkBoxPrivate:
				opt = 5;
				break;
			case R.id.checkBoxPrivate2:
				opt = 6;
				break;
			case R.id.checkBoxFile:
				opt = 7;
				break;
		}
	}

	public void onToggleClickedAuto(View view)
	{
		// Is the toggle on?
		boolean on = ((ToggleButton) view).isChecked();

		if (on) {
			auto = true;
			localizeNow();
		} else {
			auto = false;
		}
	}

	public void localizeNow()
	{
		if (opt == 1 && mCachedMapData.size() < 3 || opt == 7 && mFileData.size() < 3) {
			Toast.makeText(LocalizeActivity.this,
					getResources().getText(R.string.toast_not_enough_data),
					Toast.LENGTH_LONG).show();
			return;
		}
		mWifiManager.startScan();
	}

	public void localizeNow(View _)
	{
		localizeNow();
	}

	public void drawMarkers(float[] markerlocs)
	{
		float cx = (float)(markerlocs[0]*markerlocs[6] + markerlocs[2]*markerlocs[7] + markerlocs[4]*markerlocs[8]);
		float cy = (float)(markerlocs[1]*markerlocs[6] + markerlocs[3]*markerlocs[7] + markerlocs[5]*markerlocs[8]);

		final PhotoMarker mark = Utils.createNewMarker(
				getApplicationContext(), mRelative, cx,
				cy, R.drawable.o);


		final PhotoMarker bestguess = Utils.createNewMarker(
				getApplicationContext(), mRelative, markerlocs[0],
				markerlocs[1], R.drawable.red_x);
		bestguess.marker.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				PopupMenu popup = new PopupMenu(LocalizeActivity.this,bestguess.marker);
				popup.getMenuInflater().inflate(R.menu.marker,popup.getMenu());
				popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						switch (item.getItemId()) {
							case R.id.action_delete_cmenu:
								bestguess.marker.setVisibility(View.GONE);
								onDelete(bestguess.x, bestguess.y);
								// remove from file data
								if(mFileData.size() > 0)
								mFileData.remove(new TrainLocation(bestguess.x, bestguess.y));
								return true;
							default:
								return true;
						}
					}
				});
				popup.show();
				return true;
			}});

		final PhotoMarker secondguess = Utils.createNewMarker(
				getApplicationContext(), mRelative, markerlocs[2],
				markerlocs[3], R.drawable.bluegreen_x);
		secondguess.marker.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				PopupMenu popup = new PopupMenu(LocalizeActivity.this,secondguess.marker);
				popup.getMenuInflater().inflate(R.menu.marker,popup.getMenu());
				popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						switch (item.getItemId()) {
							case R.id.action_delete_cmenu:
								secondguess.marker.setVisibility(View.GONE);
								onDelete(secondguess.x, secondguess.y);
								// remove from file data
								if(mFileData.size() > 0)
								mFileData.remove(new TrainLocation(secondguess.x, secondguess.y));
								return true;
							default:
								return true;
						}
					}
				});
				popup.show();
				return true;
			}});


		final PhotoMarker thirdguess = Utils.createNewMarker(
				getApplicationContext(), mRelative, markerlocs[4],
				markerlocs[5], R.drawable.bluegreen_x);
		thirdguess.marker.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				PopupMenu popup = new PopupMenu(LocalizeActivity.this,thirdguess.marker);
				popup.getMenuInflater().inflate(R.menu.marker,popup.getMenu());
				popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						switch (item.getItemId()) {
							case R.id.action_delete_cmenu:
								thirdguess.marker.setVisibility(View.GONE);
								onDelete(thirdguess.x, thirdguess.y);
								// remove from file data
								if(mFileData.size() > 0)
								mFileData.remove(new TrainLocation(thirdguess.x, thirdguess.y));
								return true;
							default:
								return true;
						}
					}
				});
				popup.show();
				return true;
			}});

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
	}

	private void onDelete(float x, float y) {
		AsyncHttpClient client = new AsyncHttpClient();
		RequestParams params = new RequestParams();

		System.out.println("trying to delete " + x + "," + y);

		params.put("x", x);
		params.put("y", y);
		client.post("http://eic15.eng.fiu.edu:80/wifiloc/deletereading.php", params, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(int i, Header[] headers, byte[] bytes) {
				System.out.println(new String(bytes));
				Toast.makeText(getApplicationContext(), "message = " + new String(bytes), Toast.LENGTH_LONG).show();
			}

			@Override
			public void onFailure(int statusCode, Header[] headers, byte[] bytes, Throwable throwable) {
				if(statusCode == 404){
					Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
				}else if(statusCode == 500){
					Toast.makeText(getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
				}else{
					Toast.makeText(getApplicationContext(), "Unexpected Error occcured! [Most common Error: Device might not be connected to Internet]", Toast.LENGTH_LONG).show();
				}
			}
		});
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

	private  Map<TrainLocation, ArrayList<APValue>>  loadFileData(long mapId) throws IOException {
        Log.d("my log", "loading file");
		ArrayList<String[]> data = new ArrayList<String[]>();

		// set up file reading
		InputStream inStream = getApplicationContext().getResources().openRawResource(R.raw.readings);
		InputStreamReader is = new InputStreamReader(inStream);
		BufferedReader reader = new BufferedReader(is);


		int i = 0;
		final Map<TrainLocation, ArrayList<APValue>> fileData = new HashMap<TrainLocation, ArrayList<APValue>>();
		String line = reader.readLine();  //read first line

		while (line != null) {             //continue until no more lines
			String[] lineList = line.split("\\|"); // put line tokens in array

			if (Long.parseLong(lineList[8]) == mapId) { //get from correct map
				data.add(lineList);        // add to array of data

				// create training location
				TrainLocation loc = new TrainLocation(Float.valueOf(data.get(i)[3]), Float.valueOf(data.get(i)[4]));
				// create AP
				APValue ap = (new APValue(data.get(i)[7], Integer.parseInt(data.get(i)[5])));

				if (fileData.containsKey(loc)) {
					fileData.get(loc).add(ap);
				} else {
					ArrayList<APValue> new_ = new ArrayList<APValue>();
					new_.add(ap);
					fileData.put(loc, new_);
				}


				i++;
			}

			line = reader.readLine();

		}
		return fileData;
	}

}
