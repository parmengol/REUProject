package edu.fiu.mpact.TrainingReuProject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import android.net.wifi.ScanResult;
import android.util.Log;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.apache.http.Header;
import edu.fiu.mpact.TrainingReuProject.Utils.EncTrainDistPair;
import edu.fiu.mpact.TrainingReuProject.Utils.TrainDistPair;
import edu.fiu.mpact.TrainingReuProject.Utils.APValue;
import edu.fiu.mpact.TrainingReuProject.Utils.TrainLocation;

public class LocalizationEuclideanDistance {
    double oldDistance;
	protected boolean mIsReady = false;
	protected Map<TrainLocation, ArrayList<APValue>> mData = null;
	private LocalizeActivity mLocAct;

	public void remotePrivLocalize(List<ScanResult> results, long mMapId, final PrivateKey sk, PublicKey pk) throws IllegalStateException {

		AsyncHttpClient client = new AsyncHttpClient();
		RequestParams params = new RequestParams();
		final Gson gson = new Gson();
		ArrayList<APValue> resultAPVs = new ArrayList<>();
		HashSet<String> bssids = Utils.gatherMetaMacs(mLocAct.getContentResolver());

		// SELECT mac, COUNT(mac) totalCount FROM testtable GROUP BY mac HAVING COUNT(mac) = ( SELECT COUNT(mac) totalCount FROM testtable GROUP BY mac ORDER BY totalCount DESC LIMIT 1 )

		// local sums
		ArrayList<String> matches = new ArrayList<>();
		long sum3 = 0;
		ArrayList<BigInteger> sum2comp = new ArrayList<>();
		for (ScanResult res : results)
		{
			if (bssids.contains(res.BSSID))
			{
				matches.add(res.BSSID);
				sum3 += res.level;
				sum2comp.add(Paillier.encrypt(BigInteger.valueOf((long) res.level * -2),pk));
			}
		}
		params.put("matches", gson.toJson(matches));
		params.put("sum2comp", gson.toJson(sum2comp));
		params.put("sum3", Paillier.encrypt(BigInteger.valueOf(sum3), pk).toString());
		params.put("publicKey", gson.toJson(pk));
		System.out.println(params.toString());
		// 10.109.185.244
		// eic15.eng.fiu.edu
		client.addHeader("Content-Type","application/json");
		client.setResponseTimeout(60000);
		client.post("http://10.109.185.244:8080/wifiloc/localize/doprivlocalize", params, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(int i, Header[] headers, byte[] bytes) {
				System.out.println(new String(bytes) + " " + i);
				ArrayList<EncTrainDistPair> resultList;
				ArrayList<TrainDistPair> plainResultList = new ArrayList<TrainDistPair>();
				try {
					resultList = gson.fromJson(new String(bytes), new TypeToken<ArrayList<EncTrainDistPair>>() {
					}.getType());
				} catch (Exception e) {
					Toast.makeText(mLocAct, e.getMessage(), Toast.LENGTH_LONG).show();
					return;
				}

				// decrypt
				for (EncTrainDistPair res : resultList)
				{
					plainResultList.add(new TrainDistPair(res.trainLocation,Paillier.decrypt(res.dist,sk).doubleValue()));
				}

				// draw
				mLocAct.drawMarkers(sortAndWeight(plainResultList));
			}

			@Override
			public void onFailure(int statusCode, Header[] headers, byte[] bytes, Throwable throwable) {
				if (statusCode == 404) {
					Toast.makeText(mLocAct.getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
				}
				// When Http response code is '500'
				else if (statusCode == 500) {
					Toast.makeText(mLocAct.getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
				}
				// When Http response code other than 404, 500
				else {
					Toast.makeText(mLocAct.getApplicationContext(), "Unexpected Error occcured! [Most common Error: Device might not be connected to Internet or remote server is not up and running]" + statusCode, Toast.LENGTH_LONG).show();
				}
				//System.out.println(new String(bytes) + " " + i);
			}
		});
	}

	public void remoteLocalize(List<ScanResult> results, long mMapId) throws IllegalStateException {
		AsyncHttpClient client = new AsyncHttpClient();
		RequestParams params = new RequestParams();
		final Gson gson = new Gson();
		ArrayList<APValue> resultAPVs = new ArrayList<>();
		for (ScanResult res : results)
		{
			resultAPVs.add(new APValue(res.BSSID,res.level));
		}
		String jsondata = gson.toJson(resultAPVs);
		System.out.println(jsondata);
		//String jsonmap = gson.toJson(mMapId);
		//System.out.println(jsonmap);
		//params.put("mapId", jsonmap);
		params.put("scanData", jsondata);
		System.out.println(params.toString());
		// 10.109.185.244
		// eic15.eng.fiu.edu
		client.get("http://eic15.eng.fiu.edu:8080/wifiloc/localize/dolocalize", params, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(int i, Header[] headers, byte[] bytes) {
				System.out.println(new String(bytes) + " " + i);
				ArrayList<TrainDistPair> resultList;
				try {
					resultList = gson.fromJson(new String(bytes), new TypeToken<ArrayList<TrainDistPair>>() {
					}.getType());
				} catch (Exception e) {
					Toast.makeText(mLocAct, e.getMessage(), Toast.LENGTH_LONG).show();
					return;
				}
				// decrypt
				mLocAct.drawMarkers(sortAndWeight(resultList));
			}

			@Override
			public void onFailure(int statusCode, Header[] headers, byte[] bytes, Throwable throwable) {
				if (statusCode == 404) {
					Toast.makeText(mLocAct.getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
				}
				// When Http response code is '500'
				else if (statusCode == 500) {
					Toast.makeText(mLocAct.getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
				}
				// When Http response code other than 404, 500
				else {
					Toast.makeText(mLocAct.getApplicationContext(), "Unexpected Error occcured! [Most common Error: Device might not be connected to Internet or remote server is not up and running]" + statusCode, Toast.LENGTH_LONG).show();
				}
				//System.out.println(new String(bytes) + " " + i);
			}
		});
	}

	public void localize(List<ScanResult> results) throws IllegalStateException {
		if (!isReadyToLocalize())
			return;

		ArrayList<TrainDistPair> resultList = new ArrayList<>();

		for (TrainLocation loc : mData.keySet()) { // for each element in the set
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

			resultList.add(new TrainDistPair(loc, distance));
			Log.d("my log", loc + " " + distance);
//			if (distance < minimumEuclideanDistance) {
//				best = loc;
//				minimumEuclideanDistance = distance;
//				Log.d("min distance", " " + minimumEuclideanDistance);
//
//			}
		}
		mLocAct.drawMarkers(sortAndWeight(resultList));
//		Collections.sort(resultList);
//		double tot = resultList.get(0).dist + resultList.get(1).dist + resultList.get(2).dist;
//		double w0 = (1-(resultList.get(0).dist/tot))/2.0;
//		double w1 = (1-(resultList.get(1).dist/tot))/2.0;
//		double w2 = (1-(resultList.get(2).dist/tot))/2.0;
//		Log.d("weight", "w0 = " + w0 + " w1 = " + w1 + " w2 = " + w2);
//		return new float[] {resultList.get(0).trainLocation.mX, resultList.get(0).trainLocation.mY,
//		resultList.get(1).trainLocation.mX, resultList.get(1).trainLocation.mY, resultList.get(2).trainLocation.mX,
//		resultList.get(2).trainLocation.mY, (float)w0, (float)w1, (float)w2};
		//Log.d("min distance", minimumEuclideanDistance + "");
		//return new float[] { best.mX, best.mY };
	}

	public boolean isReadyToLocalize() {
		return mIsReady;
	}

	public boolean setup(Map<TrainLocation, ArrayList<APValue>> data, LocalizeActivity locact) {
		mData = data;
		mLocAct = locact;
		mIsReady = true;

		return true;
	}

	private float[] sortAndWeight(ArrayList<TrainDistPair> resultList)
	{
		Collections.sort(resultList);
		System.out.println("result0 = " + resultList.get(0).dist);
		System.out.println("result1 = " + resultList.get(1).dist);
		System.out.println("result2 = " + resultList.get(2).dist);
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