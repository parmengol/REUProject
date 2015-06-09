package edu.fiu.mpact.reuproject;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class DataProvider extends ContentProvider {

	/**
	 * This is the base string that all URIs must start as.
	 */
	public static final String AUTHORITY = "edu.fiu.mpact.reuproject.DataProvider";

	/**
	 * To get a Map, one would query the MAPS_URI
	 */
	public static final Uri MAPS_URI = Uri.parse("content://" + AUTHORITY + "/"
			+ Database.Maps.TABLE_NAME);
	public static final Uri SESSIONS_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + Database.Sessions.TABLE_NAME);
	public static final Uri READINGS_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + Database.Readings.TABLE_NAME);
	public static final Uri SCALE_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + Database.Scale.TABLE_NAME);

	/**
	 * Constant for all Maps.
	 */
	public static final int MAPS = 1;
	public static final String MAPS_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/" + Database.Maps.TABLE_NAME;
	/**
	 * Constant for a single map by ID.
	 */
	public static final int MAPS_ID = 2;
	public static final String MAPS_ID_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/" + Database.Maps.TABLE_NAME;

	/**
	 * Constant for all Sessions.
	 */
	public static final int SESSIONS = 3;
	public static final String SESSIONS_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/" + Database.Sessions.TABLE_NAME;
	/**
	 * Constant for a single session by ID.
	 */
	public static final int SESSIONS_ID = 4;
	public static final String SESSIONS_ID_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/" + Database.Sessions.TABLE_NAME;

	public static final int READINGS = 5;
	public static final String READINGS_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/" + Database.Readings.TABLE_NAME;
	public static final int READINGS_ID = 6;
	public static final String READINGS_ID_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/" + Database.Readings.TABLE_NAME;

	public static final int SCALE = 7;
	public static final String SCALE_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/" + Database.Scale.TABLE_NAME;
	public static final int SCALE_ID = 8;
	public static final String SCALE_ID_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/" + Database.Scale.TABLE_NAME;

	/**
	 * Setup the UriMatcher to actually use our URIs and constants.
	 */
	private static final UriMatcher mMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);
	static {
		mMatcher.addURI(AUTHORITY, Database.Maps.TABLE_NAME, MAPS);
		mMatcher.addURI(AUTHORITY, Database.Maps.TABLE_NAME + "/#", MAPS_ID);
		mMatcher.addURI(AUTHORITY, Database.Sessions.TABLE_NAME, SESSIONS);
		mMatcher.addURI(AUTHORITY, Database.Sessions.TABLE_NAME + "/#",
				SESSIONS_ID);
		mMatcher.addURI(AUTHORITY, Database.Readings.TABLE_NAME, READINGS);
		mMatcher.addURI(AUTHORITY, Database.Readings.TABLE_NAME + "/#",
				READINGS_ID);
		mMatcher.addURI(AUTHORITY, Database.Scale.TABLE_NAME, SCALE);
		mMatcher.addURI(AUTHORITY, Database.Scale.TABLE_NAME + "/#", SCALE_ID);
	}

	private Database mDb;

	@Override
	public boolean onCreate() {
		mDb = new Database(getContext());
		return true;
	}

	// ***********************************************************************

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		final SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		switch (mMatcher.match(uri)) {
		case MAPS:
			queryBuilder.setTables(Database.Maps.TABLE_NAME);
			break;
		case MAPS_ID:
			queryBuilder.setTables(Database.Maps.TABLE_NAME);
			queryBuilder.appendWhere(Database.Maps.ID + "="
					+ uri.getLastPathSegment());
			break;
		case SESSIONS:
			queryBuilder.setTables(Database.Sessions.TABLE_NAME);
			break;
		case SESSIONS_ID:
			queryBuilder.setTables(Database.Sessions.TABLE_NAME);
			queryBuilder.appendWhere(Database.Sessions.ID + "="
					+ uri.getLastPathSegment());
			break;
		case READINGS:
			queryBuilder.setTables(Database.Readings.TABLE_NAME);
			break;
		case READINGS_ID:
			queryBuilder.setTables(Database.Readings.TABLE_NAME);
			queryBuilder.appendWhere(Database.Readings.ID + "="
					+ uri.getLastPathSegment());
			break;
		case SCALE:
			queryBuilder.setTables(Database.Scale.TABLE_NAME);
			break;
		case SCALE_ID:
			queryBuilder.setTables(Database.Scale.TABLE_NAME);
			queryBuilder.appendWhere(Database.Scale.ID + "="
					+ uri.getLastPathSegment());
			break;
		default:
			throw new IllegalArgumentException("Unmatchable URI " + uri);
		}

		final Cursor cursor = queryBuilder.query(mDb.getReadableDatabase(),
				projection, selection, selectionArgs, null, null, sortOrder);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);

		return cursor;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int numRows = 0;
		String id = null;

		switch (mMatcher.match(uri)) {
		case MAPS:
			numRows = mDb.getWritableDatabase().delete(
					Database.Maps.TABLE_NAME, selection, selectionArgs);
			break;
		case MAPS_ID:
			id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection))
				numRows = mDb.getWritableDatabase().delete(
						Database.Maps.TABLE_NAME, Database.Maps.ID + "=" + id,
						null);
			else
				numRows = mDb.getWritableDatabase().delete(
						Database.Maps.TABLE_NAME,
						selection + " and " + Database.Maps.ID + "=" + id,
						selectionArgs);
			break;
		case SESSIONS:
			numRows = mDb.getWritableDatabase().delete(
					Database.Sessions.TABLE_NAME, selection, selectionArgs);
			break;
		case SESSIONS_ID:
			id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection))
				numRows = mDb.getWritableDatabase().delete(
						Database.Sessions.TABLE_NAME,
						Database.Sessions.ID + "=" + id, null);
			else
				numRows = mDb.getWritableDatabase().delete(
						Database.Sessions.TABLE_NAME,
						selection + " and " + Database.Sessions.ID + "=" + id,
						selectionArgs);
			break;
		case READINGS:
			numRows = mDb.getWritableDatabase().delete(
					Database.Readings.TABLE_NAME, selection, selectionArgs);
			break;
		case READINGS_ID:
			id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection))
				numRows = mDb.getWritableDatabase().delete(
						Database.Readings.TABLE_NAME,
						Database.Readings.ID + "=" + id, null);
			else
				numRows = mDb.getWritableDatabase().delete(
						Database.Readings.TABLE_NAME,
						selection + " and " + Database.Readings.ID + "=" + id,
						selectionArgs);
			break;
		case SCALE:
			numRows = mDb.getWritableDatabase().delete(
					Database.Scale.TABLE_NAME, selection, selectionArgs);
			break;
		case SCALE_ID:
			id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection))
				numRows = mDb.getWritableDatabase().delete(
						Database.Scale.TABLE_NAME,
						Database.Scale.ID + "=" + id, null);
			else
				numRows = mDb.getWritableDatabase().delete(
						Database.Scale.TABLE_NAME,
						selection + " and " + Database.Scale.ID + "=" + id,
						selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unmatchable URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return numRows;
	}

	@Override
	public String getType(Uri uri) {
		switch (mMatcher.match(uri)) {
		case MAPS:
			return MAPS_TYPE;
		case MAPS_ID:
			return MAPS_ID_TYPE;
		case SESSIONS:
			return SESSIONS_TYPE;
		case SESSIONS_ID:
			return SESSIONS_ID_TYPE;
		case READINGS:
			return READINGS_TYPE;
		case READINGS_ID:
			return READINGS_ID_TYPE;
		case SCALE:
			return SCALE_TYPE;
		case SCALE_ID:
			return SCALE_ID_TYPE;
		default:
			return null;
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Uri ret = null;
		long rowId;

		switch (mMatcher.match(uri)) {
		case MAPS:
		case MAPS_ID:
			rowId = mDb.getWritableDatabase().insert(Database.Maps.TABLE_NAME,
					null, values);
			uri = ContentUris.withAppendedId(MAPS_URI, rowId);
			break;
		case SESSIONS:
		case SESSIONS_ID:
			rowId = mDb.getWritableDatabase().insert(
					Database.Sessions.TABLE_NAME, null, values);
			uri = ContentUris.withAppendedId(SESSIONS_URI, rowId);
			break;
		case READINGS:
		case READINGS_ID:
			rowId = mDb.getWritableDatabase().insert(
					Database.Readings.TABLE_NAME, null, values);
			uri = ContentUris.withAppendedId(READINGS_URI, rowId);
			break;
		case SCALE:
		case SCALE_ID:
			rowId = mDb.getWritableDatabase().insert(Database.Scale.TABLE_NAME,
					null, values);
			uri = ContentUris.withAppendedId(SCALE_URI, rowId);
			break;
		default:
			throw new IllegalArgumentException("Unmatchable URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return ret;
	}

	@Override
	public int bulkInsert(Uri uri, ContentValues[] values) {
		switch (mMatcher.match(uri)) {
		case READINGS:
			int rows = 0;
			final SQLiteDatabase db = mDb.getWritableDatabase();

			db.beginTransaction();
			try {
				for (ContentValues value : values) {
					final long rowId = db.insertOrThrow(
							Database.Readings.TABLE_NAME, null, value);
					if (rowId != -1)
						rows++;
				}

				db.setTransactionSuccessful();
			} catch (SQLException e) {
			} finally {
				db.endTransaction();
			}

			getContext().getContentResolver().notifyChange(uri, null);
			return rows;
		default:
			return super.bulkInsert(uri, values);
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int numRows = 0;
		String id = null;

		switch (mMatcher.match(uri)) {
		case MAPS:
			numRows = mDb.getWritableDatabase().update(
					Database.Maps.TABLE_NAME, values, selection, selectionArgs);
			break;
		case MAPS_ID:
			id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection))
				numRows = mDb.getWritableDatabase().update(
						Database.Maps.TABLE_NAME, values,
						Database.Maps.ID + "=" + id, null);
			else
				numRows = mDb.getWritableDatabase().update(
						Database.Maps.TABLE_NAME, values,
						Database.Maps.ID + "=" + id + " and " + selection,
						selectionArgs);
			break;
		case SESSIONS:
			numRows = mDb.getWritableDatabase().update(
					Database.Sessions.TABLE_NAME, values, selection,
					selectionArgs);
			break;
		case SESSIONS_ID:
			id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection))
				numRows = mDb.getWritableDatabase().update(
						Database.Sessions.TABLE_NAME, values,
						Database.Sessions.ID + "=" + id, null);
			else
				numRows = mDb.getWritableDatabase().update(
						Database.Sessions.TABLE_NAME, values,
						Database.Sessions.ID + "=" + id + " and " + selection,
						selectionArgs);
			break;
		case READINGS:
			numRows = mDb.getWritableDatabase().update(
					Database.Readings.TABLE_NAME, values, selection,
					selectionArgs);
			break;
		case READINGS_ID:
			id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection))
				numRows = mDb.getWritableDatabase().update(
						Database.Readings.TABLE_NAME, values,
						Database.Readings.ID + "=" + id, null);
			else
				numRows = mDb.getWritableDatabase().update(
						Database.Readings.TABLE_NAME, values,
						Database.Readings.ID + "=" + id + " and " + selection,
						selectionArgs);
			break;
		case SCALE:
			numRows = mDb.getWritableDatabase()
					.update(Database.Scale.TABLE_NAME, values, selection,
							selectionArgs);
			break;
		case SCALE_ID:
			id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection))
				numRows = mDb.getWritableDatabase().update(
						Database.Scale.TABLE_NAME, values,
						Database.Scale.ID + "=" + id, null);
			else
				numRows = mDb.getWritableDatabase().update(
						Database.Scale.TABLE_NAME, values,
						Database.Scale.ID + "=" + id + " and " + selection,
						selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unmatchable URI " + uri);
		}

		return numRows;
	}
}
