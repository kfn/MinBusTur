package com.miracleas.minrute.net;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import android.util.Log;

import com.miracleas.minrute.R;
import com.miracleas.minrute.model.TripLeg;
import com.miracleas.minrute.provider.AddressGPSMetaData;
import com.miracleas.minrute.provider.DirectionMetaData;

import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class DirectionsFetcher extends BaseFetcher
{
	public static final String tag = DirectionsFetcher.class.getName();
	private static final String[] PROJECTION = {DirectionMetaData.TableMetaData.updated};
	private String sort = null;
	
	public static final String URL = "https://maps.googleapis.com/maps/api/directions/json?sensor=true&";
	public static final int MAX = 40;

	private TripLeg mTripLeg = null;
	private long mUpdated = 0;
    private double originLat;
    private double originLng;
    private double destLat;
    private double destLng;

	public DirectionsFetcher(Context c, TripLeg leg)
	{
		super(c, null);
        mTripLeg = leg;
		sort = DirectionMetaData.TableMetaData._ID +" LIMIT 1";
		
		mUpdated = System.currentTimeMillis();
	}
	
	@Override
	void doWork() throws Exception
	{
        boolean hasCachedResult = false;
        Cursor cursor = null;
        try
        {
        	String selection = DirectionMetaData.TableMetaData.END_ADDRESS + "=? AND "+ DirectionMetaData.TableMetaData.START_ADDRESS + ">=? AND "
        			+DirectionMetaData.TableMetaData.DIRECTION_MODE + "=? AND "+DirectionMetaData.TableMetaData.updated + ">=?";
            long updated = System.currentTimeMillis() - DateUtils.WEEK_IN_MILLIS * 4;
            String[] selectionArgs = {mTripLeg.originName, mTripLeg.destName, getMode(mTripLeg), updated + ""};
            cursor = mContentResolver.query(DirectionMetaData.TableMetaData.CONTENT_URI, PROJECTION, selection, selectionArgs, sort);
            hasCachedResult = cursor.getCount()>0;
            if(!hasCachedResult)
            {
                mContentResolver.delete(DirectionMetaData.TableMetaData.CONTENT_URI, selection, selectionArgs);
            }
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
            fetchGPSchoords();
            if(originLat!=0d && destLat!=0d)
            {
                fetchDirections();
            }

        }
	}
		
	public static String getMode(TripLeg leg)
	{
		if(leg.isWalk())
		{
			return "walking";
		}
		else
		{
			return "transit";
		}
	}

	private void fetchDirections() throws Exception
	{
        StringBuilder b = new StringBuilder(URL);
        b.append("origin=").append(originLat).append(",").append(originLng).append("&").append("destination=").append(destLat).append(",").append(destLng);
        b.append("&mode=").append(getMode(mTripLeg));
        String url = b.toString();
        Log.d(tag, url);
		HttpURLConnection urlConnection = initHttpURLConnection(url);
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
					saveData(DirectionMetaData.AUTHORITY);
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
            saveDirection(polyline);
        }
	}
	
	private void saveDirection(String polyline)
	{
		ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(DirectionMetaData.TableMetaData.CONTENT_URI);
		b.withValue(DirectionMetaData.TableMetaData.END_ADDRESS, mTripLeg.destName);
        b.withValue(DirectionMetaData.TableMetaData.OVERVIEW_POLYLINE, polyline);
        b.withValue(DirectionMetaData.TableMetaData.START_ADDRESS, mTripLeg.originName);
        b.withValue(DirectionMetaData.TableMetaData.TRIP_LEG_ID, mTripLeg.id);
        b.withValue(DirectionMetaData.TableMetaData.DIRECTION_MODE, getMode(mTripLeg));
		mDbOperations.add(b.build());
	}

    private void fetchGPSchoords()
    {
        Cursor c = null;
        try
        {
            String[] projection = {AddressGPSMetaData.TableMetaData.LATITUDE_Y, AddressGPSMetaData.TableMetaData.LONGITUDE_X, AddressGPSMetaData.TableMetaData.ADDRESS};
            String selection = AddressGPSMetaData.TableMetaData.ADDRESS + "=? OR "+AddressGPSMetaData.TableMetaData.ADDRESS + "=?";
            String[] selectionArgs = {mTripLeg.originName, mTripLeg.destName};
            c = mContentResolver.query(AddressGPSMetaData.TableMetaData.CONTENT_URI, projection, selection, selectionArgs, AddressGPSMetaData.TableMetaData.ADDRESS + " LIMIT 2");
            if(c.moveToFirst())
            {
                int iLat = c.getColumnIndex(AddressGPSMetaData.TableMetaData.LATITUDE_Y);
                int iLng = c.getColumnIndex(AddressGPSMetaData.TableMetaData.LONGITUDE_X);
                int iAddress = c.getColumnIndex(AddressGPSMetaData.TableMetaData.ADDRESS);
                do
                {
                    String address = c.getString(iAddress);
                    double lat = (double)(c.getInt(iLat) / 1000000d);
                    double lng = (double)(c.getInt(iLng) / 1000000d);
                    if(isOrigin(address))
                    {
                        originLat = lat;
                        originLng = lng;
                    }
                    else
                    {
                        destLat = lat;
                        destLng = lng;
                    }
                }
                while(c.moveToNext());
            }
        }
        finally
        {
            if(c!=null)
            {
                c.close();
            }
        }
    }

    private boolean isOrigin(String address)
    {
        return address.equals(mTripLeg.originName);
    }
}
