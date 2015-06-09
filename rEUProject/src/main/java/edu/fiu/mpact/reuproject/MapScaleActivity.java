package edu.fiu.mpact.reuproject;

import uk.co.senab.photoview.PhotoViewAttacher;
import uk.co.senab.photoview.PhotoViewAttacher.OnPhotoTapListener;
import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class MapScaleActivity extends Activity {

	private long mMapId;
	private int mPointsCaptured = 0;
	private float[][] mPoints = new float[2][];
	private RelativeLayout mRelative;
	private ImageView mImageView;
	private PhotoViewAttacher mAttacher;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map_scale);

		mMapId = getIntent().getExtras().getLong(Utils.Constants.MAP_ID_EXTRA);

		// Get URI for map image.
		final Uri queryUri = ContentUris.withAppendedId(DataProvider.MAPS_URI,
				mMapId);
		final Cursor cursor = getContentResolver().query(queryUri, null, null,
				null, null);
		if (!cursor.moveToFirst()) {
			Toast.makeText(this, getString(R.string.toast_map_id_warning),
					Toast.LENGTH_LONG).show();
			finish();
			cursor.close();
			return;
		}
		final Uri mapUri = Uri.parse(cursor.getString(cursor
				.getColumnIndex(Database.Maps.DATA)));
		cursor.close();
		final int[] imgSize = Utils.getImageSize(mapUri);

		mRelative = (RelativeLayout) findViewById(R.id.image_map_container);

		mImageView = (ImageView) findViewById(R.id.image_map_preview);
		mImageView.setImageURI(mapUri);
		mAttacher = new PhotoViewAttacher(mImageView, imgSize);

		mAttacher.setOnPhotoTapListener(new OnPhotoTapListener() {
			@Override
			public void onPhotoTap(View view, float x, float y) {
				if (mPointsCaptured >= 2)
					return;

				mAttacher.addData(Utils.createNewMarker(
						getApplicationContext(), mRelative, imgSize[0] * x,
						imgSize[1] * y));
				mPoints[mPointsCaptured++] = new float[] { x, y };
			}
		});
	}

	/**
	 * Generate 2D euclidean distance between two points. The scale can be
	 * calculated with further knowledge of the image's size. If we assume this
	 * distance is 1-meter, we can extrapolate image size with image dimensions.
	 * 
	 * @param points
	 *            2x2 matrix of (x,y)
	 * @return non-normalized map scale
	 */
	public static double generateMapScale(float[][] points) {
		final double xDist = Math.pow(points[0][0] - points[1][0], 2);
		final double yDist = Math.pow(points[0][1] - points[1][1], 2);
		return Math.sqrt(xDist + yDist);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.map_scale, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_save:
			// Handle click but don't save because we don't have enough data
			if (mPointsCaptured != 2)
				return true;

			final ContentValues values = new ContentValues();
			values.put(Database.Scale.ID, mMapId);
			values.put(Database.Scale.MAP_SCALE, generateMapScale(mPoints));

			getContentResolver().insert(DataProvider.SCALE_URI, values);
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
