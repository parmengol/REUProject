package edu.fiu.mpact.reuproject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.net.wifi.ScanResult;
import android.support.v4.util.ArrayMap;
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
		for (TrainLocation loc : mData.keySet()) { // for each element in the se
			ArrayList<APValue> aps = mData.get(loc); // return the value of the key thats mapped (an array)
			Set<String> bssids = new HashSet<String>(aps.size());
			for (APValue ap : aps)
				bssids.add(ap.mBssid);

			int count = 0;
            double distance = 0;
			for (final ScanResult result : results) {
				if (bssids.contains(result.BSSID)) {
					count++;
					for (APValue reading : aps) {
						if (reading.mBssid.equals(result.BSSID)){
							distance += Math.pow(result.level - reading.mRssi, 2);
							break;
						}

					}
				}
			}
			if (count != 0)
				distance = distance / (float)count;
			Log.d("euc", "result match " + count + " out of " + results.size());
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
