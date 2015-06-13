package edu.fiu.mpact.reuproject;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class SessionListFragment extends ListFragment implements
		LoaderManager.LoaderCallbacks<Cursor> {
	private static final int LOADER_ID = 2;
	// Data for cursor projections
	private static final String[] FROM = { Database.Sessions.TIME,
			Database.Sessions.MANUFACTURER, Database.Sessions.MODEL };
	private static final String[] CURSOR_COLUMNS = { Database.Sessions.ID,
			Database.Sessions.TIME, Database.Sessions.MANUFACTURER,
			Database.Sessions.MODEL };
	private static final int[] TO = { R.id.li_session_date,
			R.id.li_session_maker, R.id.li_session_model };

	private SimpleCursorAdapter mAdapter;
	private long mMapId;

	public SessionListFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mMapId = getArguments().getLong(Utils.Constants.INTERNAL_MAP_ID_EXTRA, -1);

		mAdapter = new SimpleCursorAdapter(getActivity(),
				R.layout.session_list_item, null, FROM, TO, 0);
		mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			@Override
			public boolean setViewValue(View view, Cursor cursor,
					int columnIndex) {
				switch (view.getId()) {
				case R.id.li_session_date:
					long unixTime = cursor.getLong(columnIndex);
					((TextView) view).setText(DateFormat.format(
							"MM/dd/yyyy hh:mm", unixTime));
					return true;
				default:
					return false;
				}
			}
		});
		setListAdapter(mAdapter);

		getLoaderManager().initLoader(LOADER_ID, null, this);
	}

	// These loader methods are to provide "easy" automatic reloading of the
	// cursor whenever we create a new session.

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), DataProvider.SESSIONS_URI,
				CURSOR_COLUMNS, Database.Sessions.MAP_ID + "=?",
				new String[] { Long.toString(mMapId) }, null);
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
		getActivity().getMenuInflater().inflate(R.menu.session_cmenu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

		switch (item.getItemId()) {
			case R.id.action_selectSession_cmenu:
				return true;
			case R.id.action_deleteSession_cmenu:
				String[] mSelectionArgs = {info.id + ""};
				getActivity().getApplicationContext().getContentResolver().delete(DataProvider.SESSIONS_URI,
						"_id = ?", mSelectionArgs);
				((ViewMapActivity)getActivity()).updateMarkers();
				return true;
			default:
				return super.onContextItemSelected(item);
		}

	}


}
