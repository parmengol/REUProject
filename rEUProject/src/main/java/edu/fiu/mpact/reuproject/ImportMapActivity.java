package edu.fiu.mpact.reuproject;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class ImportMapActivity extends Activity {
	private Uri outputFile = null;
	private ImageView imageView = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_import_map);

		this.imageView = (ImageView) findViewById(R.id.img_map);
	}

	public void startPhotoPicker(View _) {
		// http://stackoverflow.com/a/25666698
		// Use this approach rather than starting an activity via `new
		// Intent("com.android.camera.action.CROP")`
		final Intent intent = new Intent(Intent.ACTION_PICK,
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		intent.setType("image/*");
		intent.putExtra("crop", "true");

		try {
			// Should create a uniquely identifiable file in the cache dir.
			// Because we use the ECD, these images are copied from their
			// original location and will continue to exist if they've been
			// deleted in their original location.
			// The image might not be a JPEG, but we don't mind.
			this.outputFile = Uri.fromFile(File.createTempFile("croppedMap",
					".jpg", getExternalCacheDir()));
			intent.putExtra("output", this.outputFile);
			startActivityForResult(intent, Utils.Constants.IMPORT_PHOTO_ACT);
		} catch (IOException e) {
			Log.w("startPhotoPicker", "could not open file");
		}
	}

	public void saveMap(View _) {
		final String mapName = ((EditText) findViewById(R.id.map_name))
				.getText().toString();

		if (mapName.isEmpty()) {
			Toast.makeText(this, getText(R.string.toast_name_warning),
					Toast.LENGTH_LONG).show();
			return;
		} else if (this.outputFile == null) {
			Toast.makeText(this, getText(R.string.toast_map_warning),
					Toast.LENGTH_LONG).show();
			return;
		}

		final Intent data = new Intent();
		data.putExtra(Utils.Constants.MAP_NAME_EXTRA, mapName);
		data.putExtra(Utils.Constants.MAP_URI_EXTRA, this.outputFile.toString());

		setResult(RESULT_OK, data);
		finish();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case Utils.Constants.IMPORT_PHOTO_ACT:
			if (resultCode != RESULT_OK)
				return;

			findViewById(R.id.btn_choose_map_image).setVisibility(View.GONE);
			this.imageView.setVisibility(View.VISIBLE);
			this.imageView.setImageURI(this.outputFile);

			return;
		default:
			super.onActivityResult(requestCode, resultCode, data);
			return;
		}

	}
}
