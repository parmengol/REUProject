package uk.co.senab.photoview;

import android.widget.ImageView;

public class PhotoMarker {
	public ImageView marker;
	public float x;
	public float y;
	public int margin;

	public PhotoMarker(ImageView marker, float x, float y, int margin) {
		this.marker = marker;
		this.x = x;
		this.y = y;
		this.margin = margin;
	}
}
