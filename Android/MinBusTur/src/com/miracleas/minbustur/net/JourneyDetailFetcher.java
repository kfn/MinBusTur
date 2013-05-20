package com.miracleas.minbustur.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.protocol.HTTP;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.miracleas.minbustur.R;
import com.miracleas.minbustur.model.AddressSearch;
import com.miracleas.minbustur.model.JourneyDetail;
import com.miracleas.minbustur.model.Trip;
import com.miracleas.minbustur.model.TripLocation;
import com.miracleas.minbustur.provider.AddressProviderMetaData;
import com.miracleas.minbustur.provider.JourneyDetailMetaData;
import com.miracleas.minbustur.provider.JourneyDetailNoteMetaData;
import com.miracleas.minbustur.provider.JourneyDetailStopMetaData;
import com.miracleas.minbustur.provider.TripMetaData;

public class JourneyDetailFetcher extends BaseFetcher
{
	public static final String URL = "URL";
	public static final String tag = JourneyDetailFetcher.class.getName();
	private String mUrl;
	private List<String> mIds = null;
	private String mTripId;
	private String mLegId;
	private static final String[] PROJECTION = {JourneyDetailMetaData.TableMetaData._ID};
	
	public JourneyDetailFetcher(Context c, Intent intent, String url, String tripId, String legId)
	{
		super(c, intent);
		mIds = new ArrayList<String>();
		mUrl = url;
		mTripId = tripId;
		mLegId = legId;
	}
	@Override
	protected boolean start()
	{
		return !TextUtils.isEmpty(mUrl);
	}
	
	@Override
	void doWork() throws Exception
	{
		boolean hasCachedResult = false;
		Cursor cursor = null;
		try
		{	
			String selection = JourneyDetailMetaData.TableMetaData.REF + "=?";
			String[] selectionArgs = {mUrl};
			cursor = mContentResolver.query(JourneyDetailMetaData.TableMetaData.CONTENT_URI, PROJECTION, selection, selectionArgs, JourneyDetailMetaData.TableMetaData._ID +" LIMIT 1");
			hasCachedResult = cursor.getCount()>0;
		}
		finally
		{
			if(cursor!=null)
			{
				cursor.close();
			}
		}
		if(!hasCachedResult)
		{
			fetchJourneyDetails();
		}
		
	}
		
	private void fetchJourneyDetails() throws Exception
	{
		HttpURLConnection urlConnection = initHttpURLConnection(mUrl);		
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
				com.miracleas.minbustur.utils.Utils.copyDbToSdCard(com.miracleas.minbustur.provider.MinBusTurProvider.DATABASE_NAME);

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
	
	private void parse(InputStream in) throws XmlPullParserException, IOException
	{
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setNamespaceAware(true);
		XmlPullParser xpp = factory.newPullParser();

		xpp.setInput(in, null);
		int eventType = xpp.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT)
		{
			if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("JourneyDetail"))
			{
				JourneyDetail journey = createNewJourneyDetail();
				eventType = xpp.next();
				int countOfStops = 0;
				while (!(eventType == XmlPullParser.END_TAG && xpp.getName().equals("JourneyDetail")))
				{
					if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("Stop"))
					{						
						saveStop(xpp, journey);
						countOfStops++;
					}
					else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("JourneyName"))
					{						
						saveJourneyName(xpp, journey);
					}
					else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("JourneyType"))
					{						
						saveJourneyType(xpp, journey);
					}
					else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("Note"))
					{						
						saveNote(xpp, journey);
					}
					eventType = xpp.next();				
				}	
				journey.countOfStops = countOfStops;
				updateJourneyDetail(journey);
			}
			eventType = xpp.next();		
		}
	}
	
	private void saveStop(XmlPullParser xpp, JourneyDetail jouney)
	{
		ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(JourneyDetailStopMetaData.TableMetaData.CONTENT_URI);	
		int attrCount = xpp.getAttributeCount();
		for(int i = 0; i < attrCount; i++)
		{
			b.withValue(xpp.getAttributeName(i), xpp.getAttributeValue(i));
		}
		b.withValue(JourneyDetailStopMetaData.TableMetaData.JOURNEY_DETAIL_ID, jouney.id);
		b.withValue(JourneyDetailStopMetaData.TableMetaData.LEG_ID, mLegId);
		b.withValue(JourneyDetailStopMetaData.TableMetaData.TRIP_ID, mTripId);
		mDbOperations.add(b.build());
	}
	
	private void saveNote(XmlPullParser xpp, JourneyDetail jouney)
	{
		ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(JourneyDetailNoteMetaData.TableMetaData.CONTENT_URI);	
		int attrCount = xpp.getAttributeCount();
		for(int i = 0; i < attrCount; i++)
		{
			b.withValue(xpp.getAttributeName(i), xpp.getAttributeValue(i));
		}
		b.withValue(JourneyDetailNoteMetaData.TableMetaData.JOURNEY_DETAIL_ID, jouney.id);
		mDbOperations.add(b.build());
	}
	
	private void saveJourneyName(XmlPullParser xpp, JourneyDetail jouney)
	{
		jouney.name = xpp.getAttributeValue(null, "name");
		jouney.nameRouteIdxFrom = xpp.getAttributeValue(null, "routeIdxFrom");
		jouney.nameRouteIdxTo = xpp.getAttributeValue(null, "routeIdxTo");		
	}
	
	private void saveJourneyType(XmlPullParser xpp, JourneyDetail jouney)
	{
		jouney.type = xpp.getAttributeValue(null, "type");
		jouney.nameRouteIdxFrom = xpp.getAttributeValue(null, "routeIdxFrom");
		jouney.nameRouteIdxTo = xpp.getAttributeValue(null, "routeIdxTo");		
	}
	
	private JourneyDetail createNewJourneyDetail()
	{
		ContentValues values = new ContentValues();
		values.put(JourneyDetailMetaData.TableMetaData.NAME, "");
		Uri uri = mContentResolver.insert(JourneyDetailMetaData.TableMetaData.CONTENT_URI, values);
		JourneyDetail t = new JourneyDetail();
		t.id = uri.getLastPathSegment();
		mIds.add(t.id);
		return t;
	}
	
	private void updateJourneyDetail(JourneyDetail t)
	{
		Uri uri = Uri.withAppendedPath(JourneyDetailMetaData.TableMetaData.CONTENT_URI, t.id);
		ContentProviderOperation.Builder b = ContentProviderOperation.newUpdate(uri);
		b.withValue(JourneyDetailMetaData.TableMetaData.NAME, t.name);
		b.withValue(JourneyDetailMetaData.TableMetaData.NAME_ROUTE_ID_X_FROM, t.nameRouteIdxFrom);
		b.withValue(JourneyDetailMetaData.TableMetaData.NAME_ROUTE_ID_X_TO, t.nameRouteIdxTo);
		b.withValue(JourneyDetailMetaData.TableMetaData.TYPE, t.type);
		b.withValue(JourneyDetailMetaData.TableMetaData.TYPE_ROUTE_ID_X_FROM, t.typeRouteIdxFrom);
		b.withValue(JourneyDetailMetaData.TableMetaData.TYPE_ROUTE_ID_X_TO, t.typeRouteIdxTo);
		b.withValue(JourneyDetailMetaData.TableMetaData.REF, mUrl);
		b.withValue(JourneyDetailMetaData.TableMetaData.TRIP_ID, mTripId);
		b.withValue(JourneyDetailMetaData.TableMetaData.COUNT_OF_STOPS, t.countOfStops);
		mDbOperations.add(b.build());
	}
	@Override
	protected void onFatalError()
	{
		for(String id : mIds) // optimeres hvis der kommer flere..
		{
			Uri uri = Uri.withAppendedPath(JourneyDetailMetaData.TableMetaData.CONTENT_URI, id);
			mContentResolver.delete(uri, null, null);
		}
	}

}
