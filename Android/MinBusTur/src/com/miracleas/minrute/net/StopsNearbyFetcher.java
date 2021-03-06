package com.miracleas.minrute.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.miracleas.minrute.R;
import com.miracleas.minrute.model.NearbyLocationRequest;
import com.miracleas.minrute.provider.TripLegDetailStopMetaData;

public class StopsNearbyFetcher extends BaseFetcher
{
	public static final String tag = StopsNearbyFetcher.class.getName();
	private static final String[] PROJECTION = {TripLegDetailStopMetaData.TableMetaData.SEARCH_ID};
	//http://xmlopen.rejseplanen.dk/bin/rest.exe/stopsNearby?coordX=10119067&coordY=56138605&maxRadius=10&maxNumber=1
	public static final String URL = BASE_URL + "stopsNearby?";
	private NearbyLocationRequest mRequest;
	private String mSearchId = null;
	
	public StopsNearbyFetcher(Context c, Intent intent, NearbyLocationRequest request)
	{
		super(c, intent);
		mRequest = request;
	}
	
	@Override
	void doWork() throws Exception
	{
		boolean hasCachedResult = false;
		Cursor cursor = null;
		try
		{	
			String selection = TripLegDetailStopMetaData.TableMetaData._ID + "=? AND "+TripLegDetailStopMetaData.TableMetaData.SEARCH_ID+" NOT NULL";
			String[] selectionArgs = {mRequest.stopId};
			cursor = mContentResolver.query(TripLegDetailStopMetaData.TableMetaData.CONTENT_URI, PROJECTION, selection, selectionArgs, TripLegDetailStopMetaData.TableMetaData._ID+" LIMIT 1");
			if(cursor.moveToFirst())
			{
				mSearchId = cursor.getString(cursor.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.SEARCH_ID));
				hasCachedResult = true;
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
			fetchSearchIdForCurrentStop();				
		}
	}
		

	private void fetchSearchIdForCurrentStop() throws Exception
	{
		StringBuilder b = new StringBuilder(URL);
		b.append("coordX=").append(mRequest.coordX).append("&coordY=").append(mRequest.coordY).append("&maxRadius=10&maxNumber=1");
		HttpURLConnection urlConnection = initHttpURLConnection(b.toString());	
		try
		{
			int repsonseCode = urlConnection.getResponseCode();
			if (repsonseCode == HttpURLConnection.HTTP_OK)
			{							
				InputStream input = urlConnection.getInputStream();
				parse(input);
				if (!mDbOperations.isEmpty())
				{		
					saveData(TripLegDetailStopMetaData.AUTHORITY);
				}
			}
            else if (repsonseCode == 404)
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
	
	private void parse(InputStream in) throws XmlPullParserException, IOException
	{
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setNamespaceAware(true);
		XmlPullParser xpp = factory.newPullParser();

		xpp.setInput(in, null);
		int eventType = xpp.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT)
		{
			if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("LocationList"))
			{
				eventType = xpp.next();
				while (!(eventType == XmlPullParser.END_TAG && xpp.getName().equals("LocationList")))
				{
					if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("StopLocation"))
					{						
						updateStopLocation(xpp);
					}
					eventType = xpp.next();				
				}				
			}
			eventType = xpp.next();		
		}
	}
	
	private void updateStopLocation(XmlPullParser xpp)
	{
		Uri uri = Uri.withAppendedPath(TripLegDetailStopMetaData.TableMetaData.CONTENT_URI, mRequest.stopId);
		ContentProviderOperation.Builder b = ContentProviderOperation.newUpdate(uri);	
		String searchId =  xpp.getAttributeValue(null, "id");
		b.withValue(TripLegDetailStopMetaData.TableMetaData.SEARCH_ID, searchId);
		mSearchId = searchId;
		mDbOperations.add(b.build());
	}
	
	public String getSearchId()
	{
		return mSearchId;
	}


}
