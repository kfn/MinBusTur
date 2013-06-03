package com.miracleas.minrute.net;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import android.util.Log;

import com.miracleas.minrute.R;
import com.miracleas.minrute.model.TripLeg;
import com.miracleas.minrute.provider.AddressProviderMetaData;
import com.miracleas.minrute.provider.DirectionLegsMetaData;

import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;

public class DirectionsFetcher extends BaseFetcher
{
	public static final String tag = DirectionsFetcher.class.getName();
	private static final String[] PROJECTION = {DirectionLegsMetaData.TableMetaData._ID};
	private String sort = null;
	private String selection;
	public static final String URL = "https://maps.googleapis.com/maps/api/directions/json?sensor=true&mode=walking&";
	public static final int MAX = 40;

	private TripLeg mTripLeg = null;
	private long mUpdated = 0;

	public DirectionsFetcher(Context c, TripLeg leg)
	{
		super(c, null);
        mTripLeg = leg;
		sort = DirectionLegsMetaData.TableMetaData._ID +" LIMIT 1";
		selection = DirectionLegsMetaData.TableMetaData.END_ADDRESS + "=? AND "+ DirectionLegsMetaData.TableMetaData.START_ADDRESS + ">=?";
		mUpdated = System.currentTimeMillis();
	}
	
	@Override
	void doWork() throws Exception
	{
        boolean hasCachedResult = false;
        Cursor cursor = null;
        try
        {
            long updated = System.currentTimeMillis() - DateUtils.WEEK_IN_MILLIS;
            String[] selectionArgs = {mTripLeg.originName, mTripLeg.destName};
            cursor = mContentResolver.query(DirectionLegsMetaData.TableMetaData.CONTENT_URI, PROJECTION, selection, selectionArgs, sort);
            hasCachedResult = cursor.getCount()>0;
        }
        finally
        {
            if(cursor!=null && !cursor.isClosed())
            {
                cursor.close();
            }
        }
        if(!hasCachedResult)
        {
            fetchDirections();
        }
	}
		


	private void fetchDirections() throws Exception
	{
        StringBuilder b = new StringBuilder(URL);
        b.append("origin=").append(URLEncoder.encode(mTripLeg.originName, HTTP.ISO_8859_1)).append("&").append("destination=").append(URLEncoder.encode(mTripLeg.destName, HTTP.ISO_8859_1));
		HttpURLConnection urlConnection = initHttpURLConnection(b.toString());
		try
		{
			int repsonseCode = urlConnection.getResponseCode();
			if (repsonseCode == HttpURLConnection.HTTP_OK)
			{
				mUpdated = System.currentTimeMillis();								
				InputStream input = urlConnection.getInputStream();
				parse(input);
				if (!mDbOperations.isEmpty())
				{					
					saveData(DirectionLegsMetaData.AUTHORITY);
				}

			} else if (repsonseCode == 404)
			{
				throw new Exception(mContext.getString(R.string.search_failed_404));
			} else if (repsonseCode == 500)
			{
				throw new Exception(mContext.getString(R.string.search_failed_500));
			} else if (repsonseCode == 503)
			{
				throw new Exception(mContext.getString(R.string.search_failed_503));
			} else
			{
				Log.e(tag, "server response: " + repsonseCode);
				throw new Exception("error"); 
			}
		} finally
		{
			urlConnection.disconnect();
		}

	}
	
	private void parse(InputStream in) throws XmlPullParserException, IOException, JSONException
    {
		String input = Utils.convertStreamToString(in, HTTP.ISO_8859_1);
        JSONObject root = new JSONObject(input);
        JSONArray routes = root.getJSONArray("routes");
        if(routes.length()>0)
        {
            JSONObject route = routes.getJSONObject(0);
            JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
            String polyline = overviewPolyline.getString("points");
            saveLocation(polyline);
        }
	}
	
	private void saveLocation(String polyline)
	{
		ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(DirectionLegsMetaData.TableMetaData.CONTENT_URI);
		b.withValue(DirectionLegsMetaData.TableMetaData.END_ADDRESS, mTripLeg.destName);
        b.withValue(DirectionLegsMetaData.TableMetaData.OVERVIEW_POLYLINE, polyline);
        b.withValue(DirectionLegsMetaData.TableMetaData.START_ADDRESS, mTripLeg.originName);
        b.withValue(DirectionLegsMetaData.TableMetaData.TRIP_LEG_ID, mTripLeg.id);
		mDbOperations.add(b.build());
	}
}
