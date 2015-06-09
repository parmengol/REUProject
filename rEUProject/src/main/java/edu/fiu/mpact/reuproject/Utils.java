package edu.fiu.mpact.reuproject;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import uk.co.senab.photoview.PhotoMarker;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class Utils {
	public final class Constants {
		public String map = "";
		public static final int IMPORT_PHOTO_ACT = 1;
		public static final int SELECT_MAP_ACT = 2;
		// Arbitrary constant for tracking activity launching
		public static final int IMPORT_ACT = 1;
		// Fully qualified project name for use in intent data passing
		private static final String PKG = "edu.fiu.mpact.reuproject";
		public static final String MAP_NAME_EXTRA = PKG + ".map_name";
		public static final String MAP_URI_EXTRA = PKG + ".map_data";
		public static final String MAP_ID_EXTRA = PKG + ".map_id";
		public static final String INTERNAL_MAP_ID_EXTRA = PKG + "._map_id";

		// Time in ms between automatic scans
		public static final int SCAN_INTERVAL = 1000 * 30;
	}

	// ***********************************************************************

	public static class Coord {
		public float mX, mY;
		public int mRssi = -1;

		public Coord(float x, float y, int rssi) {
			this(x, y);
			mRssi = rssi;
		}

		public Coord(float x, float y) {
			mX = x;
			mY = y;
		}
	}

	public static class TrainLocation extends Coord {
		public TrainLocation(float x, float y) {
			super(x, y);
		}
		public boolean equals(Object obj){
			if (!(obj instanceof TrainLocation))
				return false;
			if (obj == this)
				return true;

			TrainLocation t = (TrainLocation) obj;

			return this.mX == t.mX && this.mY == t.mY;
		}
		public int hashCode(){
			int hash = 3;

			hash = 7 * hash + (int)this.mX;
			return hash;
		}
	}

	public static class APValue {
		public String mBssid = "";
		public int mRssi = -1; //received signal strength indicator

		public APValue(String bssid, int rssi) {
			mBssid = bssid;
			mRssi = rssi;
		}
	}

	// ************************************************************************

	public static int[] getImageSize(Uri uri) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		Bitmap b = BitmapFactory.decodeFile(uri.getPath(), options);

		//Bitmap b = BitmapFactory.decodeResource(Resources.getSystem(), R.drawable.ec_1, options);

		Log.d("decoding", "" + b);

		return new int[] { options.outWidth, options.outHeight };
	}

	/**
	 * Creates a new ImageView instance associated as a child of layout.
	 * 
	 * @param context
	 *            which context to associate with ImageView...can just use
	 *            `getApplicationContext()` or `this`
	 * @param layout
	 *            parent layout
	 * @return view with image resource set to a drawable
	 */
	private static ImageView createNewMarker(Context context,
			RelativeLayout layout, int resId) {
		final ImageView ret = new ImageView(context);
		final int markerSize = context.getResources().getInteger(
				R.integer.map_marker_size);

		ret.setImageResource(resId);
		layout.addView(ret, new LayoutParams(markerSize, markerSize));

		return ret;
	}

	public static PhotoMarker createNewMarker(Context context,
			RelativeLayout wrapper, float x, float y, int resId) {
		return new PhotoMarker(
				createNewMarker(context, wrapper, resId),
				x,
				y,
				context.getResources().getInteger(R.integer.map_marker_size) / 2);
	}

	public static PhotoMarker createNewMarker(Context context,
			RelativeLayout wrapper, float x, float y) {
		return createNewMarker(context, wrapper, x, y, R.drawable.x);
	}

	public static Deque<PhotoMarker> generateMarkers(Deque<Coord> coordsToDraw,
			Context context, RelativeLayout wrapper) {
		final Deque<PhotoMarker> data = new LinkedList<PhotoMarker>();
		final Set<Coord> points = new HashSet<Coord>();

		for (Coord tmpCoord : coordsToDraw) {
			if (!points.contains(tmpCoord)) {
				points.add(tmpCoord);
				PhotoMarker toAdd = createNewMarker(context, wrapper,
						tmpCoord.mX, tmpCoord.mY);
				data.add(toAdd);
			}
		}

		return data;
	}

	/**
	 * Gather (x,y) points we have samples for in this map.
	 */
	public static Deque<PhotoMarker> gatherSamples(ContentResolver cr,
			Context context, RelativeLayout wrapper, long mapId) {
		// Get cursor into readings table
		// FIXME find out if possible to do a SQL DISTINCT query...
		final Cursor cursor = cr.query(DataProvider.READINGS_URI, new String[] {
				Database.Readings.MAP_X, Database.Readings.MAP_Y },
				Database.Readings.MAP_ID + "=?",
				new String[] { Long.toString(mapId) }, null);

		// For readability, store these as local constants
		final int xColumn = cursor.getColumnIndex(Database.Readings.MAP_X);
		final int yColumn = cursor.getColumnIndex(Database.Readings.MAP_Y);

		// Load all the APs into our storage set
		final Deque<Coord> coordsToDraw = new LinkedList<Coord>();
		while (cursor.moveToNext()) {
			if (cursor.isNull(xColumn) || cursor.isNull(yColumn))
				continue;
			
			coordsToDraw.add(new Coord(cursor.getFloat(xColumn), cursor
					.getFloat(yColumn)));
		}
		cursor.close();

		return generateMarkers(coordsToDraw, context, wrapper);
	}

	public static Deque<PhotoMarker> generateMarkers(
			Map<TrainLocation, ArrayList<APValue>> coordsToDraw,
			Context context, RelativeLayout wrapper) {
		return generateMarkers(new LinkedList<Coord>(coordsToDraw.keySet()),
				context, wrapper);
	}

	public static Map<TrainLocation, ArrayList<APValue>> gatherLocalizationData(
			ContentResolver cr, long mapId) {
		final Cursor cursor = cr.query(DataProvider.READINGS_URI, new String[] {
				Database.Readings.MAP_X, Database.Readings.MAP_Y,
				Database.Readings.MAC, Database.Readings.SIGNAL_STRENGTH },
				Database.Readings.MAP_ID + "=?",
				new String[] { Long.toString(mapId) }, null);

		// For readability, store these as local constants
		final int xColumn = cursor.getColumnIndex(Database.Readings.MAP_X);
		final int yColumn = cursor.getColumnIndex(Database.Readings.MAP_Y);
		final int bssidColumn = cursor.getColumnIndex(Database.Readings.MAC);
		final int rssiColumn = cursor
				.getColumnIndex(Database.Readings.SIGNAL_STRENGTH);

		// Cache results of cursor result in a more convenient data structure
		final Map<TrainLocation, ArrayList<APValue>> data = new HashMap<TrainLocation, ArrayList<APValue>>();
		int j = 0;
		int i = 0;
		while (cursor.moveToNext()) {
			if (cursor.isNull(xColumn) || cursor.isNull(yColumn))
				continue;


			TrainLocation loc = new TrainLocation(cursor.getFloat(xColumn),
					cursor.getFloat(yColumn));
			Log.d("x and y", cursor.getFloat(xColumn) + " " + cursor.getFloat(yColumn));

			APValue ap = new APValue(cursor.getString(bssidColumn),
					cursor.getInt(rssiColumn));
			Log.d("x and y of AP", cursor.getString(bssidColumn) + " " + cursor.getFloat(rssiColumn));


			if (data.containsKey(loc)) {
				data.get(loc).add(ap);
				Log.d("IN THE IF!!!", "IN THE IF!!");
				j++;
			} else {
				ArrayList<APValue> new_ = new ArrayList<APValue>();
				new_.add(ap);
				data.put(loc, new_);
			}

			Log.d("CURSOR", i + "");
			i++;
		}
		cursor.close();
		Log.d("TOTAL IN IF: ", " " + j);

		return data;
	}




}
