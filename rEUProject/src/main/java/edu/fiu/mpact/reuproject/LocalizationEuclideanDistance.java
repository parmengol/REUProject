package edu.fiu.mpact.reuproject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.net.wifi.ScanResult;
import android.util.Log;

import edu.fiu.mpact.reuproject.Utils.APValue;
import edu.fiu.mpact.reuproject.Utils.TrainLocation;

public class LocalizationEuclideanDistance {
    double oldDistance;
	protected boolean mIsReady = false;
	protected Map<TrainLocation, ArrayList<APValue>> mData = null;

	public float[] localize(List<ScanResult> results) throws IllegalStateException {
		if (!isReadyToLocalize())
			return null;

		TrainLocation best = new TrainLocation(0, 0);
		double minimumEuclideanDistance = -1;
		double distance;
		int i = 0;
		Log.d("PRINTING DATA", "----------------------------");
		Log.d("SIZE", mData.size() + "");
		for (TrainLocation loc : mData.keySet()) { // for each element in the se
			ArrayList<APValue> aps = mData.get(loc); // return the value of the key thats mapped (an array)
			Set<String> bssids = new HashSet<String>(aps.size());
			for (APValue ap : aps)
				bssids.add(ap.mBssid);

			if( i == 0) {
				Log.d("Printing Macs", "");
				for (int j = 0; j < aps.size(); j++) {
					Log.d("in the set: ", aps.get(j).mBssid + "");
					Log.d("size: ", aps.size()+ "");
				}

			}
			i++;

            distance = 0;
			for (final ScanResult result : results) {
				//Log.d("in the loop ", result.BSSID);

				if (bssids.contains(result.BSSID)) {
					for (APValue reading : aps) {
						if (reading.mBssid.equals(result.BSSID)){
							oldDistance = distance;
							distance = distance +
									Math.pow(result.level - reading.mRssi, 2);
							//Log.d("old distance", " " + oldDistance);
							//Log.d("adding", " " + 	Math.pow(result.level - reading.mRssi, 2));
							//Log.d("new distance", " " + distance);
							Log.d("results.level: ", "" + result.level);
							Log.d("mRssi", " " + reading.mRssi );

							break;
						}

					}
				}
			}
				// Technically, we should do sqrt here to get real euclidean
			// distance. If we just care about the ordering and not the actual
			// value, we can skip.


			if (distance < minimumEuclideanDistance
					|| minimumEuclideanDistance == -1) {
				best = loc;
				minimumEuclideanDistance = distance;
				Log.d("min distance", " " + minimumEuclideanDistance);

			}


		}
		//Log.d("min distance", minimumEuclideanDistance + "");
		return new float[] { best.mX, best.mY };
	}

	public boolean isReadyToLocalize() {
		return mIsReady;
	}

	public boolean setup(Map<TrainLocation, ArrayList<APValue>> data) {
		mData = data;
		mIsReady = true;

		return true;
	}
}
