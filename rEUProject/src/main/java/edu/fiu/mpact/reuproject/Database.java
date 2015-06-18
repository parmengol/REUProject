package edu.fiu.mpact.reuproject;

import java.util.ArrayList;
import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class Database extends SQLiteOpenHelper {

	protected static final String DB_NAME = "LocalizationData.db";
	protected static final int DB_VERSION = 4;

	public static final String[] TABLES = { Maps.TABLE_NAME, Readings.TABLE_NAME };
	private static final String[] SCHEMAS = { Maps.SCHEMA, Readings.SCHEMA };

	public static class Maps {
		public static final String TABLE_NAME = "Maps";
		public static final String ID = "_id";
		public static final String NAME = "name";
		public static final String DATE_ADDED = "date_added";
		public static final String DATA = "data";

		private static final String ID_COLUMN = ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT";
		private static final String NAME_COLUMN = NAME + " TEXT NOT NULL";
		private static final String DATE_ADDED_COLUMN = DATE_ADDED
				+ " INTEGER NOT NULL";
		private static final String DATA_COLUMN = DATA + " TEXT NOT NULL";

		private static final String SCHEMA = generateSchema(TABLE_NAME,
				ID_COLUMN, NAME_COLUMN, DATE_ADDED_COLUMN, DATA_COLUMN);
	}

	public static class Scale {
		public static final String TABLE_NAME = "Scale";
		public static final String ID = "_id";
		public static final String MAP_SCALE = "map_scale";

		private static final String ID_COLUMN = ID + " INTEGER PRIMARY KEY";
		private static final String MAP_SCALE_COLUMN = MAP_SCALE + " FLOAT";
		private static final String ID_FOREIGN_COLUMN = generateForeignKeyColumn(
				ID, Maps.TABLE_NAME, Maps.ID);

		private static final String SCHEMA = generateSchema(TABLE_NAME,
				ID_COLUMN, MAP_SCALE_COLUMN, ID_FOREIGN_COLUMN);
	}

	public static class Readings {
		public static final String TABLE_NAME = "Readings";
		public static final String ID = "_id";
		public static final String DATETIME = "datetime";
		public static final String MAP_X = "mapx";
		public static final String MAP_Y = "mapy";
		public static final String SIGNAL_STRENGTH = "rss";
		public static final String AP_NAME = "ap_name";
		public static final String MAC = "mac";
		public static final String MAP_ID = "map";
//		public static final String SESSION_ID = "session";

		private static final String ID_COLUMN = ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT";
		private static final String DATETIME_COLUMN = DATETIME
				+ " INTEGER NOT NULL";
		private static final String MAP_X_COLUMN = MAP_X + " FLOAT";
		private static final String MAP_Y_COLUMN = MAP_Y + " FLOAT";
		private static final String SIGNAL_STRENGTH_COLUMN = SIGNAL_STRENGTH
				+ " INTEGER NOT NULL";
		private static final String AP_NAME_COLUMN = AP_NAME + " TEXT NOT NULL";
		private static final String MAC_COLUMN = MAC + " TEXT NOT NULL";
		private static final String MAP_ID_COLUMN = MAP_ID
				+ " INTEGER NOT NULL";
//		private static final String SESSION_ID_COLUMN = SESSION_ID
//				+ " INTEGER";
		private static final String MAP_ID_FOREIGN_COLUMN = generateForeignKeyColumn(
				MAP_ID, Maps.TABLE_NAME, Maps.ID);

		private static final String SCHEMA = generateSchema(TABLE_NAME,
				ID_COLUMN, DATETIME_COLUMN, MAP_X_COLUMN, MAP_Y_COLUMN,
				SIGNAL_STRENGTH_COLUMN, AP_NAME_COLUMN, MAC_COLUMN,
				MAP_ID_COLUMN, MAP_ID_FOREIGN_COLUMN);
	}

//	public static class Sessions {
//		public static final String TABLE_NAME = "Sessions";
//		public static final String ID = "_id";
//		public static final String TIME = "session_datetime";
//		public static final String MAP_ID = "map_id";
//		public static final String SDK_VERSION = "sdk";
//		public static final String MANUFACTURER = "maker";
//		public static final String MODEL = "model";
//
//		private static final String ID_COLUMN = ID
//				+ " INTEGER PRIMARY KEY AUTOINCREMENT";
//		private static final String TIME_COLUMN = TIME + " INTEGER NOT NULL";
//		private static final String SDK_VERSION_COLUMN = SDK_VERSION
//				+ " INTEGER NOT NULL";
//		private static final String MANUFACTURER_COLUMN = MANUFACTURER
//				+ " TEXT NOT NULL";
//		private static final String MODEL_COLUMN = MODEL + " TEXT NOT NULL";
//		private static final String MAP_ID_COLUMN = MAP_ID
//				+ " INTEGER NOT NULL";
//		private static final String MAP_ID_FOREIGN_COLUMN = generateForeignKeyColumn(
//				MAP_ID, Maps.TABLE_NAME, Maps.ID);
//
//		private static final String SCHEMA = generateSchema(TABLE_NAME,
//				ID_COLUMN, TIME_COLUMN, SDK_VERSION_COLUMN,
//				MANUFACTURER_COLUMN, MODEL_COLUMN, MAP_ID_COLUMN,
//				MAP_ID_FOREIGN_COLUMN);
//	}

	// ***********************************************************************

	public Database(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.i("Database.onCreate", "*** Creating Tables ***");

		for (final String schema : SCHEMAS) {
			Log.d("Database.onCreate", "Command = " + schema);
			db.execSQL(schema);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i("Database.onUpgrade", "Upgrading database from " + oldVersion
				+ " to " + newVersion);

		Log.w("Database.onUpgrade", "Dropping and recreating tables!");
		for (final String tableName : TABLES)
			db.execSQL("DROP TABLE IF EXISTS " + tableName);

		onCreate(db);
	}

	protected static String generateForeignKeyColumn(String fk,
			String refTable, String refColumn) {
		final String format = "FOREIGN KEY(%s) REFERENCES %s(%s)";
		return String.format(Locale.US, format, fk, refTable, refColumn);
	}

	protected static String generateSchema(String tableName,
			String... columnDefs) {
		final StringBuilder ret = new StringBuilder();
		// Build beginning of CREATE statement
		ret.append("CREATE TABLE IF NOT EXISTS ");
		ret.append(tableName);
		ret.append('(');

		// Build columns of table
		for (int i = 0; i < columnDefs.length - 1; i++) {
			ret.append(columnDefs[i]);
			ret.append(',');
		}
		if (columnDefs.length > 0)
			ret.append(columnDefs[columnDefs.length - 1]);

		// Build end
		ret.append(')');
		ret.append(';');
		return ret.toString();
	}

	public ArrayList<Cursor> getData(String Query){
		//get writable database
		SQLiteDatabase sqlDB = this.getWritableDatabase();
		String[] columns = new String[] { "mesage" };
		//an array list of cursor to save three cursors ec_1 has results from the query
		//other cursor stores error message if any errors are triggered
		ArrayList<Cursor> alc = new ArrayList<Cursor>(2);
		MatrixCursor Cursor2= new MatrixCursor(columns);
		alc.add(null);
		alc.add(null);




		try{
			String maxQuery = Query ;
			//execute the query results will be save in Cursor c
			Cursor c = sqlDB.rawQuery(maxQuery, null);


			//add value to cursor2
			Cursor2.addRow(new Object[] { "Success" });

			alc.set(1,Cursor2);
			if (null != c && c.getCount() > 0) {


				alc.set(0,c);
				c.moveToFirst();

				return alc ;
			}
			return alc;
		} catch(SQLException sqlEx){
			Log.d("printing exception", sqlEx.getMessage());
			//if any exceptions are triggered save the error message to cursor an return the arraylist
			Cursor2.addRow(new Object[] { ""+sqlEx.getMessage() });
			alc.set(1,Cursor2);
			return alc;
		} catch(Exception ex){

			Log.d("printing exception", ex.getMessage());

			//if any exceptions are triggered save the error message to cursor an return the arraylist
			Cursor2.addRow(new Object[] { ""+ex.getMessage() });
			alc.set(1,Cursor2);
			return alc;
		}


	}

}
