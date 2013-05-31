package com.miracleas.minrute.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;

import com.miracleas.minrute.model.StopDeparture;
import com.miracleas.minrute.provider.TripLegDetailStopDeparturesMetaData;

public class DepartureBoardFetcher extends BaseFetcher
{
	public static final String tag = DepartureBoardFetcher.class.getName();
	private static final String[] PROJECTION = {TripLegDetailStopDeparturesMetaData.TableMetaData._ID};
	public static final String URL = BASE_URL + "departureBoard?id=";
	private String mSearchId;
	private long mUpdated = 0;
	private String mStopId = null;
	
	public DepartureBoardFetcher(Context c, String stopSearchId, String stopId)
	{
		super(c, null);
		mSearchId = stopSearchId;
		mUpdated = System.currentTimeMillis();
		mStopId = stopId;
	}
	
	@Override
	void doWork() throws Exception
	{
		boolean hasCachedResult = false;
		Cursor cursor = null;
		try
		{	
			long cacheTime = System.currentTimeMillis() - DateUtils.HOUR_IN_MILLIS;
			String selection = TripLegDetailStopDeparturesMetaData.TableMetaData.STOP_ID + "=? AND "+TripLegDetailStopDeparturesMetaData.TableMetaData.UPDATED+">=?";
			String[] selectionArgs = {mSearchId, cacheTime+""};
			cursor = mContentResolver.query(TripLegDetailStopDeparturesMetaData.TableMetaData.CONTENT_URI, PROJECTION, selection, selectionArgs, TripLegDetailStopDeparturesMetaData.TableMetaData._ID+" LIMIT 1");
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
			fetchSearchIdForCurrentStop();				
		}
	}
		

	private void fetchSearchIdForCurrentStop() throws Exception
	{
		StringBuilder b = new StringBuilder(URL);
		b.append(mSearchId);
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
					saveData(TripLegDetailStopDeparturesMetaData.AUTHORITY);
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
			if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("DepartureBoard"))
			{
				eventType = xpp.next();
				while (!(eventType == XmlPullParser.END_TAG && xpp.getName().equals("DepartureBoard")))
				{
					if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("Departure"))
					{				
						StopDeparture dep = new StopDeparture();
						dep.name = xpp.getAttributeValue(null, "name");
						dep.type = xpp.getAttributeValue(null, "type");
						dep.stop = xpp.getAttributeValue(null, "stop");
						dep.time = xpp.getAttributeValue(null, "time");
						dep.date = xpp.getAttributeValue(null, "date");
						dep.direction = xpp.getAttributeValue(null, "direction");											
						eventType = xpp.next();	
						while (!(eventType == XmlPullParser.END_TAG && xpp.getName().equals("Departure")))
						{
							if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("JourneyDetailRef"))
							{						
								dep.ref = xpp.getAttributeValue(null, "ref");
							}
							eventType = xpp.next();	
						}
						insertStopLocation(dep);
					}					
					eventType = xpp.next();				
				}				
			}
			eventType = xpp.next();		
		}
	}
	
	private void insertStopLocation(StopDeparture dep)
	{
		ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(TripLegDetailStopDeparturesMetaData.TableMetaData.CONTENT_URI);	
		b.withValue(TripLegDetailStopDeparturesMetaData.TableMetaData.DATE, dep.date)
		.withValue(TripLegDetailStopDeparturesMetaData.TableMetaData.DIRECTION, dep.direction)
		.withValue(TripLegDetailStopDeparturesMetaData.TableMetaData.NAME, dep.name)
		.withValue(TripLegDetailStopDeparturesMetaData.TableMetaData.REF, dep.ref)
		.withValue(TripLegDetailStopDeparturesMetaData.TableMetaData.STOP, dep.stop)
		.withValue(TripLegDetailStopDeparturesMetaData.TableMetaData.TIME, dep.time)
		.withValue(TripLegDetailStopDeparturesMetaData.TableMetaData.STOP_SEARCH_ID, mSearchId)
		.withValue(TripLegDetailStopDeparturesMetaData.TableMetaData.STOP_ID, mStopId)
		.withValue(TripLegDetailStopDeparturesMetaData.TableMetaData.TYPE, dep.type)
		.withValue(TripLegDetailStopDeparturesMetaData.TableMetaData.UPDATED, mUpdated);
		mDbOperations.add(b.build());
	}


}
