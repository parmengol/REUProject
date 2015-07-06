package edu.fiu.mpact.TrainingReuProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import uk.co.senab.photoview.PhotoMarker;
import uk.co.senab.photoview.PhotoViewAttacher;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Second activity in normal activity lifecycle. Lists each map with a
 * interactive thumbnail and gives option of adding another training session or
 * the option of starting a localization session.
 * 
 * @author oychang
 *
 */
public class ViewMapActivity extends Activity {
	private long mMapId;
	private RelativeLayout mRelative;
	private ImageView mImageView;
	private PhotoViewAttacher mAttacher;
	public static final String PREFS_NAME = "MyPrefsFile";
	private ImageView selMrk;
	private TextView mTextView;
	private ListView mListView;
	private Map<Utils.TrainLocation, ArrayList<Utils.APValue>> mCachedMapData;
	private ArrayList<Utils.APValue> aparray;
	private Database controller;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view_map);

		mMapId = getIntent().getExtras().getLong(Utils.Constants.MAP_ID_EXTRA);

		// Get URI for map image.
		// The cursor that handles the list of sessions is created in
		// SessionListFragment.java
		final Uri queryUri = ContentUris.withAppendedId(DataProvider.MAPS_URI,
				mMapId);
		final Cursor cursor = getContentResolver().query(queryUri, null, null,
				null, null);
		if (!cursor.moveToFirst()) {
			Toast.makeText(this, getString(R.string.toast_map_id_warning),
					Toast.LENGTH_LONG).show();
			cursor.close();
			finish();
			return;
		}
		final Uri mapUri = Uri.parse(cursor.getString(cursor
				.getColumnIndex(Database.Maps.DATA)));
		cursor.close();

		mRelative = (RelativeLayout) findViewById(R.id.image_map_container);

		mImageView = (ImageView) findViewById(R.id.img_map_preview);
		mImageView.setImageURI(mapUri);
		// mAttacher = new PhotoViewAttacher(mImageView);

		mAttacher = new PhotoViewAttacher(mImageView,
				Utils.getImageSize(mapUri, getApplicationContext()));

		// gathersamples is buggy

		mTextView = (TextView) findViewById(R.id.marker_location_text);
		mListView = (ListView) findViewById(R.id.marker_rss_list);

		aparray = new ArrayList<>();
		updateMarkers();
//		mCachedMapData = Utils.gatherLocalizationData(getContentResolver(),
//				mMapId);
//		Deque<PhotoMarker> mrkrs = Utils.generateMarkers(mCachedMapData,
//				getApplicationContext(), mRelative);
//		for (final PhotoMarker mrk : mrkrs)
//		{
//			mrk.marker.setOnLongClickListener(new View.OnLongClickListener() {
//				@Override
//				public boolean onLongClick(View v) {
//					PopupMenu popup = new PopupMenu(ViewMapActivity.this, mrk.marker);
//					popup.getMenuInflater().inflate(R.menu.marker, popup.getMenu());
//					popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
//						@Override
//						public boolean onMenuItemClick(MenuItem item) {
//							switch (item.getItemId()) {
//								case R.id.action_delete_cmenu:
//									mrk.marker.setVisibility(View.GONE);
//									onDelete(mrk.x, mrk.y);
//									return true;
//								default:
//									return true;
//							}
//						}
//					});
//					popup.show();
//					return true;
//				}
//			});
//		}
//		mAttacher.addData(mrkrs);


		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		boolean dialogShown = settings.getBoolean("dialogShown", false);

		if (!dialogShown) {
			showAlertDialog();

			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("dialogShown", true);
			editor.commit();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.view_map, menu);
		return true;
	}

	// i broke this
	public void exportToCsv() {
		Uri uri = null;
		PrintWriter file = null;

		Uri queryUri;
		Cursor cursor;
		// Use map name for filename
		queryUri = ContentUris.withAppendedId(DataProvider.MAPS_URI, mMapId);
		cursor = getContentResolver().query(queryUri, null, null, null, null);
		if (!cursor.moveToFirst()) {
			Toast.makeText(this, getString(R.string.toast_map_id_warning),
					Toast.LENGTH_LONG).show();
			cursor.close();
			return;
		}
		try {
			final String filename = cursor.getString(cursor
					.getColumnIndex(Database.Maps.NAME));
			cursor.close();
			uri = Uri.fromFile(File.createTempFile(filename, ".csv",
					getExternalCacheDir()));
		} catch (IOException e) {
		}

		try {
			file = new PrintWriter(new File(uri.getPath()));
		} catch (FileNotFoundException e) {
		}

		// Setup database interaction constants
//		final String[] sessionsProjection = { Database.Sessions.TIME,
//				Database.Sessions.SDK_VERSION, Database.Sessions.MANUFACTURER,
//				Database.Sessions.MODEL };
		final String[] readingsProjection = { Database.Readings.DATETIME,
				Database.Readings.MAP_X, Database.Readings.MAP_Y,
				Database.Readings.SIGNAL_STRENGTH, Database.Readings.AP_NAME,
				Database.Readings.MAC };
		final String header = TextUtils.join(",", readingsProjection);
		file.write(header + "\n");

		// For each session
		SortedMap<Long, String> map = new TreeMap<Long, String>();
//		cursor = getContentResolver().query(DataProvider.SESSIONS_URI,
//				sessionsProjection, Database.Sessions.MAP_ID + "=?",
//				new String[] { Long.toString(mMapId) }, null);
//		while (cursor.moveToNext()) {
//			final long time = cursor.getLong(cursor
//					.getColumnIndex(Database.Sessions.TIME));
//			final String value = TextUtils
//					.join(",",
//							new String[] {
//									Long.toString(time),
//									Integer.toString(cursor.getInt(cursor
//											.getColumnIndex(Database.Sessions.SDK_VERSION))),
//									cursor.getString(cursor
//											.getColumnIndex(Database.Sessions.MANUFACTURER)),
//									cursor.getString(cursor
//											.getColumnIndex(Database.Sessions.MODEL)) });
//			map.put(time, value);
//		}
//		cursor.close();

		// For each reading in that session
		//long lower = 0;
//		final String selection = String.format("%s >= ? AND %s < ?",
//				Database.Readings.DATETIME, Database.Readings.DATETIME);

		//for (long upper : map.keySet()) {
		cursor = getContentResolver()
				.query(DataProvider.READINGS_URI,
						readingsProjection,
						Database.Readings.MAP_ID + "=?",
						new String[]{Long.toString(mMapId)}, null);
		while (cursor.moveToNext()) {
			//String row = map.get(upper) + ",";
			String row = TextUtils
					.join(",",
							new String[]{
									Long.toString(cursor.getLong(cursor
											.getColumnIndex(Database.Readings.DATETIME))),
									Float.toString(cursor.getFloat(cursor
											.getColumnIndex(Database.Readings.MAP_X))),
									Float.toString(cursor.getFloat(cursor
											.getColumnIndex(Database.Readings.MAP_Y))),
									Integer.toString(cursor.getInt(cursor
											.getColumnIndex(Database.Readings.SIGNAL_STRENGTH))),
									cursor.getString(cursor
											.getColumnIndex(Database.Readings.AP_NAME)),
									cursor.getString(cursor
											.getColumnIndex(Database.Readings.MAC))});
			file.write(row + "\n");
		}
		//lower = upper;
		cursor.close();


		// Close
		file.close();
		// FIXME externalize string
		Toast.makeText(this, "Done!", Toast.LENGTH_SHORT).show();
	}

	/**
	 * Fork out to either training or localization activity
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;

		switch (item.getItemId()) {
		case R.id.action_new_session:
			intent = new Intent(this, TrainActivity.class);
			intent.putExtra(Utils.Constants.MAP_ID_EXTRA, mMapId);
			startActivityForResult(intent, 1);
			return true;
		case R.id.action_localize:
			intent = new Intent(this, LocalizeActivity.class);
			intent.putExtra(Utils.Constants.MAP_ID_EXTRA, mMapId);
			startActivity(intent);
			return true;
//		case R.id.action_add_map_scale:
//			intent = new Intent(this, MapScaleActivity.class);
//			intent.putExtra(Utils.Constants.MAP_ID_EXTRA, mMapId);
//			startActivity(intent);
//			return true;
		case R.id.action_lazy_train:
			intent = new Intent(this, AutomaticTrainActivity.class);
			intent.putExtra(Utils.Constants.MAP_ID_EXTRA, mMapId);
			startActivity(intent);
			return true;
		case R.id.action_export_csv:
			exportToCsv();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	public void updateMarkers()
	{
		mCachedMapData = Utils.gatherLocalizationData(getContentResolver(),
				mMapId);
		Deque<PhotoMarker> mrkrs = Utils.generateMarkers(mCachedMapData,
				getApplicationContext(), mRelative);
		for (final PhotoMarker mrk : mrkrs)
		{
			mrk.marker.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					PopupMenu popup = new PopupMenu(ViewMapActivity.this, mrk.marker);
					popup.getMenuInflater().inflate(R.menu.marker, popup.getMenu());
					popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							switch (item.getItemId()) {
								case R.id.action_delete_cmenu:
									mrk.marker.setVisibility(View.GONE);
									onDelete(mrk.x, mrk.y);
									deleteRemote(10, 10);
									return true;
								default:
									return true;
							}
						}
					});
					popup.show();
					return true;
				}
			});

			mrk.marker.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mTextView.setText(String.valueOf(mrk.x) + " , " + String.valueOf(mrk.y));
					aparray = mCachedMapData.get(new Utils.TrainLocation(mrk.x, mrk.y));
					Log.d("qwer", "aparray size = " + aparray.size());
					APValueAdapter adapter = new APValueAdapter(ViewMapActivity.this, aparray);

					mListView.setAdapter(adapter);
				}
			});
		}
		mAttacher.replaceData(mrkrs);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK)
		{
			updateMarkers();
		}
	}

	private void onDelete(float x, float y)
	{
		String[] mSelectionArgs = {String.valueOf(x-0.0001), String.valueOf(x+0.0001), String.valueOf(y-0.0001), String.valueOf(y+0.0001)};
		getContentResolver().delete(DataProvider.READINGS_URI,
				"mapx>? and mapx<? and mapy>? and mapy<?", mSelectionArgs);
	}

	private void showAlertDialog() {
		new AlertDialog.Builder(this)
				.setTitle("Instructions")
				.setMessage("Select the plus icon at the top to start your train session.")
								.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
									}
								})
								.setIcon(R.drawable.ic_launcher).show();
	}

	public static double roundToSignificantFigures(double num, int n) {
		if(num == 0) {
			return 0;
		}

		final double d = Math.ceil(Math.log10(num < 0 ? -num: num));
		final int power = n - (int) d;

		final double magnitude = Math.pow(10, power);
		final long shifted = Math.round(num * magnitude);
		return shifted/magnitude;
	}

	public void deleteRemote(final float x, final float y){
		Log.d("my log", "in delete");
		AsyncHttpClient client = new AsyncHttpClient();
		final RequestParams params = new RequestParams();
		String jsondata = makeJSON(x, y);

		params.put("deleteJSON", jsondata);
		client.post("http://eic15.eng.fiu.edu:80/wifiloc/delete.php", params, new AsyncHttpResponseHandler() {

			@Override
			public void onSuccess(int i, Header[] headers, byte[] bytes) {
				onSuccess(new String(bytes));
			}

			@Override
			public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {

			}

			public void onSuccess(String response) {
				Log.d("my log", "in success");
				params.put("mapx", x);
				params.put("mapy", y);

			}
		});
	}

	public String makeJSON(float mapx, float mapy){
		ArrayList<ContentValues> wordList;
		wordList = new ArrayList<ContentValues>();

				ContentValues cv = new ContentValues();

				cv.put("mapx", mapx);
				cv.put("mapy", mapy);

				wordList.add(cv);
		
		Gson gson = new GsonBuilder().create();
		//Use GSON to serialize Array List to JSON
		Log.d("my log", gson.toJson(wordList));
		return gson.toJson(wordList);
	}

}





	


