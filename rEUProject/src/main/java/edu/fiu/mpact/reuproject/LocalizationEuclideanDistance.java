package edu.fiu.mpact.reuproject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import android.app.ProgressDialog;
import android.net.wifi.ScanResult;
import android.support.v4.util.ArrayMap;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;

import edu.fiu.mpact.reuproject.Utils.TrainDistPair;
import edu.fiu.mpact.reuproject.Utils.APValue;
import edu.fiu.mpact.reuproject.Utils.TrainLocation;

public class LocalizationEuclideanDistance {
    double oldDistance;
	protected boolean mIsReady = false;
	protected Map<TrainLocation, ArrayList<APValue>> mData = null;
	private ArrayList<TrainDistPair> resultList;
	private int status = 0;

	public float[] remoteLocalize(List<ScanResult> results) throws IllegalStateException {
		AsyncHttpClient client = new AsyncHttpClient();
		RequestParams params = new RequestParams();
		final Gson gson = new Gson();
		String jsondata = gson.toJson(results);
		System.out.println(jsondata);
		params.put("scanData", jsondata);
		client.post("http://eic15.eng.fiu.edu:80/wifiloc/dolocalize", params, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(int i, Header[] headers, byte[] bytes) {
				resultList = gson.fromJson(new String(bytes), new TypeToken<ArrayList<TrainDistPair>>(){}.getType());
				// decrypt
				status = 0;
			}

			@Override
			public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
				System.out.println(new String(bytes) + " " + i);
				status = 1;
			}
		});
		return sortAndWeight();
	}

	public float[] localize(List<ScanResult> results) throws IllegalStateException {
		if (!isReadyToLocalize())
			return null;

		TrainLocation best = new TrainLocation(0, 0);
		ArrayList<Utils.TrainDistPair> bestList= new ArrayList<Utils.TrainDistPair>();
		double minimumEuclideanDistance = 1000.0;
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
				distance = distance / (double)count;
			else
				continue;
			Log.d("euc", "result match " + count + " out of " + results.size());
				// Technically, we should do sqrt here to get real euclidean
			// distance. If we just care about the ordering and not the actual
			// value, we can skip.

			bestList.add(new Utils.TrainDistPair(loc, distance));
//			if (distance < minimumEuclideanDistance) {
//				best = loc;
//				minimumEuclideanDistance = distance;
//				Log.d("min distance", " " + minimumEuclideanDistance);
//
//			}
		}

		Collections.sort(bestList);
		double tot = bestList.get(0).dist + bestList.get(1).dist + bestList.get(2).dist;
		double w0 = (1-(bestList.get(0).dist/tot))/2.0;
		double w1 = (1-(bestList.get(1).dist/tot))/2.0;
		double w2 = (1-(bestList.get(2).dist/tot))/2.0;
		Log.d("weight", "w0 = " + w0 + " w1 = " + w1 + " w2 = " + w2);
		return new float[] {bestList.get(0).trainLocation.mX, bestList.get(0).trainLocation.mY,
		bestList.get(1).trainLocation.mX, bestList.get(1).trainLocation.mY, bestList.get(2).trainLocation.mX,
		bestList.get(2).trainLocation.mY, (float)w0, (float)w1, (float)w2};
		//Log.d("min distance", minimumEuclideanDistance + "");
		//return new float[] { best.mX, best.mY };
	}

	public boolean isReadyToLocalize() {
		return mIsReady;
	}

	public boolean setup(Map<TrainLocation, ArrayList<APValue>> data) {
		mData = data;
		mIsReady = true;

		return true;
	}

	private float[] sortAndWeight()
	{
		Collections.sort(resultList);
		double tot = resultList.get(0).dist + resultList.get(1).dist + resultList.get(2).dist;
		double w0 = (1-(resultList.get(0).dist/tot))/2.0;
		double w1 = (1-(resultList.get(1).dist/tot))/2.0;
		double w2 = (1-(resultList.get(2).dist/tot))/2.0;
		Log.d("weight", "w0 = " + w0 + " w1 = " + w1 + " w2 = " + w2);
		return new float[] {resultList.get(0).trainLocation.mX, resultList.get(0).trainLocation.mY,
				resultList.get(1).trainLocation.mX, resultList.get(1).trainLocation.mY, resultList.get(2).trainLocation.mX,
				resultList.get(2).trainLocation.mY, (float)w0, (float)w1, (float)w2};
	}
}