package edu.fiu.mpact.reuproject;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
//import com.parse.Parse;
//import com.parse.ParseObject;


/**
 * Activity Flow:
 * 
 * (1) MainActivity -> (2) ViewMapActivity -> (3a) TrainActivity -or- (3b)
 * LocalizeActivity
 * 
 * @author oychang
 *
 */
public class MainActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// Enable Local Datastore.
		//Parse.enableLocalDatastore(this);
		//Parse.initialize(this, "NqGKf2aqzDof3utFsKsOXZ3my4W0PuO70Yli7qjJ", "9M1DrCJ9PzZ8nei4JXtdkHbTycDW3F6JzwPyaTGA");

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_add:
			final Intent addIntent = new Intent(this, ImportMapActivity.class);
			startActivityForResult(addIntent, Utils.Constants.IMPORT_ACT);
			return true;
		case R.id.action_dbm:
			final Intent dbmIntent = new Intent(this, AndroidDatabaseManager.class);
			startActivity(dbmIntent);
			return true;
		case R.id.action_selectMap:
				Intent myIntent = new Intent(this, SelectMap.class);
				startActivityForResult(myIntent, Utils.Constants.SELECT_MAP_ACT);
				return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		final ContentValues values = new ContentValues();

		switch (requestCode) {
		case Utils.Constants.IMPORT_ACT:
			if (resultCode == RESULT_OK) {
				values.put(Database.Maps.NAME,
						data.getStringExtra(Utils.Constants.MAP_NAME_EXTRA));
				values.put(Database.Maps.DATA,
						data.getStringExtra(Utils.Constants.MAP_URI_EXTRA));
				values.put(Database.Maps.DATE_ADDED, System.currentTimeMillis());
				getContentResolver().insert(DataProvider.MAPS_URI, values);

			}
			break;
			case Utils.Constants.SELECT_MAP_ACT:

				if(resultCode == RESULT_OK){
					values.put(Database.Maps.NAME, data.getStringExtra(Utils.Constants.MAP_NAME_EXTRA));
					values.put(Database.Maps.DATA, data.getStringExtra(Utils.Constants.MAP_URI_EXTRA));
					values.put(Database.Maps.DATE_ADDED, System.currentTimeMillis());
					getContentResolver().insert(DataProvider.MAPS_URI, values);

				}

			default:
			super.onActivityResult(requestCode, resultCode, data);
			break;
		}
	}






}
