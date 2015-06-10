package edu.fiu.mpact.reuproject;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class MapListFragment extends ListFragment implements
		LoaderManager.LoaderCallbacks<Cursor> {

	private static final int LOADER_ID = 1;
	private static final String[] FROM = {Database.Maps.DATA,
			Database.Maps.NAME};
	private static final String[] CURSOR_COLUMNS = {Database.Maps.ID,
			Database.Maps.DATA, Database.Maps.NAME};
	private static final int[] TO = {R.id.li_map_image, R.id.li_map_name};

	private SimpleCursorAdapter mAdapter;

	// FIXME isn't this unnecessary?
	public MapListFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// FIXME reverse the order so the newest sessions are at the top
		mAdapter = new SimpleCursorAdapter(getActivity(),
				R.layout.map_list_item, null, FROM, TO, 0);
		mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {


			@Override
			public boolean setViewValue(View view, Cursor cursor,
										int columnIndex) {
				if (view.getId() == R.id.li_map_image) {
					((ImageView) view).setImageURI(Uri.parse(cursor
							.getString(columnIndex)));
					return true;
				}
				return false;
			}

		});
		setListAdapter(mAdapter);

		getLoaderManager().initLoader(LOADER_ID, null, this);
	}

	@Override
	public void onListItemClick(ListView list, View v, int position, long id) {
		final Intent nextIntent = new Intent(getActivity(),
				ViewMapActivity.class);
		nextIntent.putExtra(Utils.Constants.MAP_ID_EXTRA, id); // what is map_id_extra
		Log.d("My Log", id + " ");
		startActivity(nextIntent);

	}


	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), DataProvider.MAPS_URI,
				CURSOR_COLUMNS, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (loader.getId() == LOADER_ID)
			mAdapter.swapCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	public void deleteMap(View v) {
		ImageButton btn_image = (ImageButton) v.findViewById(R.id.delete);
		btn_image.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				View parentItem = (View) view.getParent();
			}
		});
	}

	@Override
	public void onActivityCreated(Bundle savedState) {
		super.onActivityCreated(savedState);

		getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
										   int arg2, long arg3) {
				Toast.makeText(getActivity(), "On long click listener", Toast.LENGTH_LONG).show();
				return true;
			}
		});


	}
}

