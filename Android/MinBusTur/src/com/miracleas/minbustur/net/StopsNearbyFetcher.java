package com.miracleas.minbustur.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;

import org.apache.http.protocol.HTTP;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.DateUtils;
import android.util.Log;

import com.miracleas.minbustur.R;
import com.miracleas.minbustur.model.AddressSearch;
import com.miracleas.minbustur.model.NearbyLocationRequest;
import com.miracleas.minbustur.provider.AddressProviderMetaData;
import com.miracleas.minbustur.provider.JourneyDetailStopMetaData;

public class StopsNearbyFetcher extends BaseFetcher
{
	public static final String tag = StopsNearbyFetcher.class.getName();
	private static final String[] PROJECTION = {JourneyDetailStopMetaData.TableMetaData.SEARCH_ID};
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
			String selection = JourneyDetailStopMetaData.TableMetaData._ID + "=? AND "+JourneyDetailStopMetaData.TableMetaData.SEARCH_ID+" NOT NULL";
			String[] selectionArgs = {mRequest.stopId};
			cursor = mContentResolver.query(JourneyDetailStopMetaData.TableMetaData.CONTENT_URI, PROJECTION, selection, selectionArgs, JourneyDetailStopMetaData.TableMetaData._ID+" LIMIT 1");
			if(cursor.moveToFirst())
			{
				mSearchId = cursor.getString(cursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.SEARCH_ID));
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
					saveData(JourneyDetailStopMetaData.AUTHORITY);
				}
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
		Uri uri = Uri.withAppendedPath(JourneyDetailStopMetaData.TableMetaData.CONTENT_URI, mRequest.stopId);
		ContentProviderOperation.Builder b = ContentProviderOperation.newUpdate(uri);	
		String searchId =  xpp.getAttributeValue(null, "id");
		b.withValue(JourneyDetailStopMetaData.TableMetaData.SEARCH_ID, searchId);
		mSearchId = searchId;
		mDbOperations.add(b.build());
	}
	
	public String getSearchId()
	{
		return mSearchId;
	}


}
