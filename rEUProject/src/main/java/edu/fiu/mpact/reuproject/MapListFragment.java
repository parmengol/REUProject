package edu.fiu.mpact.reuproject;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.content.ContextWrapper;


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

	@Override
	public void onActivityCreated(Bundle savedState) {
		super.onActivityCreated(savedState);

		registerForContextMenu(getListView());
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
									ContextMenu.ContextMenuInfo menuInfo) {
		getActivity().getMenuInflater().inflate(R.menu.main_cmenu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

		switch (item.getItemId()) {
			case R.id.action_selectMap_cmenu:
				final Intent nextIntent = new Intent(getActivity(),
						ViewMapActivity.class);
				nextIntent.putExtra(Utils.Constants.MAP_ID_EXTRA, info.id); // what is map_id_extra
				startActivity(nextIntent);
				return true;
			case R.id.action_delete_cmenu:
				String[] mSelectionArgs = {info.id + ""};
				getActivity().getApplicationContext().getContentResolver().delete(DataProvider.MAPS_URI,
						"_id = ?", mSelectionArgs);
				getActivity().getApplicationContext().getContentResolver().delete(DataProvider.READINGS_URI,
						"map = ?", mSelectionArgs);
				return true;
			default:
				return super.onContextItemSelected(item);
		}

	}


}

