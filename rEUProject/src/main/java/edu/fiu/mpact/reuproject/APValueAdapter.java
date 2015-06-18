package edu.fiu.mpact.reuproject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by aquallut on 6/18/15.
 */
public class APValueAdapter extends ArrayAdapter<Utils.APValue>{
    public APValueAdapter(Context context, ArrayList<Utils.APValue> aps) {
        super(context, 0, aps);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Utils.APValue ap = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.marker_list_item, parent, false);
        }
        // Lookup view for data population
        TextView libssid = (TextView) convertView.findViewById(R.id.li_marker_bssid);
        TextView lirssi = (TextView) convertView.findViewById(R.id.li_marker_rssi);
        // Populate the data into the template view using the data object
        libssid.setText(ap.mBssid);
        lirssi.setText(String.valueOf(ap.mRssi));
        // Return the completed view to render on screen
        return convertView;
    }

}
