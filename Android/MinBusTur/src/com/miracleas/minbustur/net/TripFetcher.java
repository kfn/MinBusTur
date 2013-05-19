package com.miracleas.minbustur.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.miracleas.minbustur.R;
import com.miracleas.minbustur.model.AddressSearch;
import com.miracleas.minbustur.model.Trip;
import com.miracleas.minbustur.model.TripLeg;
import com.miracleas.minbustur.model.TripRequest;
import com.miracleas.minbustur.provider.AddressProviderMetaData;
import com.miracleas.minbustur.provider.TripLegMetaData;
import com.miracleas.minbustur.provider.TripMetaData;

public class TripFetcher extends BaseFetcher
{
	public static final String tag = TripFetcher.class.getName();
	private static final String[] PROJECTION = {AddressProviderMetaData.TableMetaData._ID};
	private String selection;
	public static final String URL = BASE_URL + "trip?";
	private long mUpdated = 0;
	private Uri mUriNotify = null;
	private TripRequest mTripRequest = null;
	private StringBuilder b = new StringBuilder();

	
	public TripFetcher(Context c, Intent intent, Uri notifyUri)
	{
		super(c, intent);
		mUpdated = System.currentTimeMillis();
		mUriNotify = notifyUri;
	}
	@Override
	void fetchHelper() throws Exception
	{
		
	}

	public void tripSearch(TripRequest tripRequest) throws Exception
	{
		b = new StringBuilder();
		if(TextUtils.isEmpty(tripRequest.getOriginId()))
		{
			addRequest("originCoordX", tripRequest.getOriginCoordX());
			addRequest("originCoordY", tripRequest.getOriginCoordY());
			addRequest("originCoordName", tripRequest.getOriginCoordName());
		}
		else
		{
			addRequest("originId", tripRequest.getOriginId());
		}
		if(TextUtils.isEmpty(tripRequest.getDestId()))
		{
			addRequest("destCoordX", tripRequest.getDestCoordX());
			addRequest("destCoordY", tripRequest.getDestCoordY());
			addRequest("destCoordName", tripRequest.getDestCoordName());
		}
		else
		{
			addRequest("destId", tripRequest.getDestId());
		}
		addRequest("viaId", tripRequest.getViaId());
		addRequest("date", tripRequest.getDate());
		addRequest("time", tripRequest.getTime());
		addTransportRequest("useBus", tripRequest.getUseBus());
		addTransportRequest("useMetro", tripRequest.getUseMetro());
		addTransportRequest("useTog", tripRequest.getUseTog());
		addArrivalRequest("searchForArrival", tripRequest.getSearchForArrival());

		HttpURLConnection urlConnection = initHttpURLConnection(URL+b.toString());		
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
					saveData(TripLegMetaData.AUTHORITY);
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
				throw new Exception("error"); // status.getReasonPhrase()
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
			if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("TripList"))
			{
				eventType = xpp.next();
				while (!(eventType == XmlPullParser.END_TAG && xpp.getName().equals("TripList")))
				{
					if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("Trip"))
					{
						Trip trip = createNewTrip();
						while (!(eventType == XmlPullParser.END_TAG && xpp.getName().equals("Trip")))
						{							
							if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("Leg"))
							{				
								trip.incrementLegCount();
								TripLeg leg = new TripLeg();
								leg.tripId = trip.id;
								leg.name = xpp.getAttributeValue(null, "name");
								leg.type = xpp.getAttributeValue(null, "date");	
								
								eventType = xpp.next();
								while (!(eventType == XmlPullParser.END_TAG && xpp.getName().equals("Leg")))
								{	
									if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("Origin"))
									{									
										leg.originName = xpp.getAttributeValue(null, "name");
										leg.originDate = xpp.getAttributeValue(null, "date");
										leg.originRouteId = xpp.getAttributeValue(null, "routeIdx");
										leg.originTime = xpp.getAttributeValue(null, "time");
										leg.originType = xpp.getAttributeValue(null, "type");				
									}
									else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("Destination"))
									{
										leg.destName = xpp.getAttributeValue(null, "name");
										leg.destDate = xpp.getAttributeValue(null, "date");
										leg.destRouteId = xpp.getAttributeValue(null, "routeIdx");
										leg.destTime = xpp.getAttributeValue(null, "time");
										leg.destType = xpp.getAttributeValue(null, "type");
									}
									else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("Notes"))
									{
										leg.notes = xpp.getAttributeValue(null, "text");
									}
									eventType = xpp.next();	
								}
								if(!TextUtils.isEmpty(leg.tripId))
								{
									saveLeg(leg);
									trip.duration = trip.duration + leg.getDuration();
								}							
							}
							eventType = xpp.next();			
						}							
						updateTrip(trip);
					}				
					eventType = xpp.next();				
				}				
			}
			eventType = xpp.next();		
		}
	}
	
	private Trip createNewTrip()
	{
		ContentValues values = new ContentValues();
		values.put(TripMetaData.TableMetaData.DURATION, 0l);
		Uri uri = mContentResolver.insert(TripMetaData.TableMetaData.CONTENT_URI, values);
		Trip t = new Trip();
		t.id = uri.getLastPathSegment();
		return t;
	}
	
	private String updateTrip(Trip t)
	{
		ContentValues values = new ContentValues();
		values.put(TripMetaData.TableMetaData.DURATION, t.duration);
		values.put(TripMetaData.TableMetaData.LEG_COUNT, t.legCount);
		values.put(TripMetaData.TableMetaData.LEG_NAMES, t.getNames());
		values.put(TripMetaData.TableMetaData.LEG_TYPES, t.getTypes());
		Uri uri = Uri.withAppendedPath(TripMetaData.TableMetaData.CONTENT_URI, t.id);
		mContentResolver.update(uri, values, null, null);
		return uri.getLastPathSegment();
	}
	
	private void saveLeg(TripLeg leg)
	{
		ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(TripLegMetaData.TableMetaData.CONTENT_URI);	
		b.withValue(TripLegMetaData.TableMetaData.DEST_DATE, leg.destDate)
		.withValue(TripLegMetaData.TableMetaData.DEST_NAME, leg.destName)
		.withValue(TripLegMetaData.TableMetaData.DEST_ROUTE_ID, leg.destRouteId)
		.withValue(TripLegMetaData.TableMetaData.DEST_TIME, leg.destTime)
		.withValue(TripLegMetaData.TableMetaData.DEST_TYPE, leg.destType)
		.withValue(TripLegMetaData.TableMetaData.NAME, leg.name)
		.withValue(TripLegMetaData.TableMetaData.NOTES, leg.notes)
		.withValue(TripLegMetaData.TableMetaData.ORIGIN_DATE, leg.originName)
		.withValue(TripLegMetaData.TableMetaData.ORIGIN_NAME, leg.originName)
		.withValue(TripLegMetaData.TableMetaData.ORIGIN_ROUTE_ID, leg.originRouteId)
		.withValue(TripLegMetaData.TableMetaData.ORIGIN_TIME, leg.originTime)
		.withValue(TripLegMetaData.TableMetaData.ORIGIN_TYPE, leg.originType)
		.withValue(TripLegMetaData.TableMetaData.TRIP_ID, leg.tripId)
		.withValue(TripLegMetaData.TableMetaData.DURATION, leg.getDuration())
		.withValue(TripLegMetaData.TableMetaData.DURATION_FORMATTED, leg.getFormattedDuration())
		.withValue(TripLegMetaData.TableMetaData.updated, mUpdated);
		mDbOperations.add(b.build());
	}


	
	private void addRequest(String key, String value)
	{
		if(!TextUtils.isEmpty(value))
		{
			if(b.length()>0)
			{
				b.append("&");
			}
			b.append(key).append("=").append(value);
		}
	}
	private void addTransportRequest(String key, int value)
	{
		if(value==0)
		{
			if(b.length()>0)
			{
				b.append("&");
			}
			b.append(key).append("=").append(value);
		}
	}
	private void addArrivalRequest(String key, int value)
	{
		if(value==1)
		{
			if(b.length()>0)
			{
				b.append("&");
			}
			b.append(key).append("=").append(value);
		}
	}

}
