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
import com.miracleas.minbustur.model.TripLocation;
import com.miracleas.minbustur.model.TripRequest;
import com.miracleas.minbustur.provider.AddressProviderMetaData;
import com.miracleas.minbustur.provider.TripLegMetaData;
import com.miracleas.minbustur.provider.TripMetaData;
import com.miracleas.minbustur.utils.DateHelper;

public class TripFetcher extends BaseFetcher
{
	public static final String tag = TripFetcher.class.getName();
	public static final String URL = BASE_URL + "trip?";
	private long mUpdated = 0;
	private StringBuilder b = new StringBuilder();
	private DateHelper mDateHelper = null;
	private TripRequest mTripRequest = null;
	public static final String TRIP_REQUEST = "TRIP_REQUEST";
	
	public TripFetcher(Context c, Intent intent, Uri notifyUri)
	{
		super(c, intent);
		mUpdated = System.currentTimeMillis();
		mUriNotify = notifyUri;
		mTripRequest = intent.getParcelableExtra(TripFetcher.TRIP_REQUEST);
	}
	@Override
	void doWork() throws Exception
	{
		if(mTripRequest!=null)
		{
			tripSearch(mTripRequest);
		}		
	}

	public void tripSearch(TripRequest tripRequest) throws Exception
	{
		deleteOldTrips();
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
				mDateHelper = new DateHelper(mContext.getString(R.string.days), mContext.getString(R.string.hours), mContext.getString(R.string.minutes), mContext.getString(R.string.seconds));
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
								TripLeg leg = new TripLeg();
								leg.tripId = trip.id;
								leg.name = xpp.getAttributeValue(null, "name");
								leg.type = xpp.getAttributeValue(null, "type");	
								
								eventType = xpp.next();
								while (!(eventType == XmlPullParser.END_TAG && xpp.getName().equals("Leg")))
								{	
									if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("Origin"))
									{									
										leg.origin = getTripLegValues(xpp);				
									}
									else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("Destination"))
									{
										leg.dest = getTripLegValues(xpp);	
									}
									else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("Notes"))
									{
										leg.notes = xpp.getAttributeValue(null, "text");
									}
									else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("JourneyDetailRef"))
									{
										leg.ref = xpp.getAttributeValue(null, "ref");
									}
									
									
									eventType = xpp.next();	
								}
								if(!TextUtils.isEmpty(leg.tripId))
								{
									trip.addLeg(leg);
									saveLeg(leg, trip.getLegCount());
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
	
	private TripLocation getTripLegValues(XmlPullParser xpp)
	{
		TripLocation leg = new TripLocation();
		leg.name = xpp.getAttributeValue(null, "name");
		leg.date = xpp.getAttributeValue(null, "date");
		leg.routeId = xpp.getAttributeValue(null, "routeIdx");
		leg.time = xpp.getAttributeValue(null, "time");
		leg.type = xpp.getAttributeValue(null, "type");
		return leg;
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
	
	private void updateTrip(Trip t)
	{
		Uri uri = Uri.withAppendedPath(TripMetaData.TableMetaData.CONTENT_URI, t.id);
		ContentProviderOperation.Builder b = ContentProviderOperation.newUpdate(uri);
		long duration = t.getTotalDuration();
		b.withValue(TripMetaData.TableMetaData.DURATION, duration);
		b.withValue(TripMetaData.TableMetaData.DURATION_LABEL, mDateHelper.getDurationLabel(duration, false));
		b.withValue(TripMetaData.TableMetaData.LEG_COUNT, t.getLegCount());
		b.withValue(TripMetaData.TableMetaData.LEG_NAMES, t.getNames());
		b.withValue(TripMetaData.TableMetaData.LEG_TYPES, t.getTypes());
		b.withValue(TripMetaData.TableMetaData.DEPATURE_TIME, t.getDepatureTime());
		b.withValue(TripMetaData.TableMetaData.ARRIVAL_TIME, t.getArrivalTime());
		b.withValue(TripMetaData.TableMetaData.TRANSPORT_CHANGES, t.getTransportChanges());
		b.withValue(TripMetaData.TableMetaData.DURATION_BUS, mDateHelper.getDurationLabel(t.getDurationBus(), false));
		b.withValue(TripMetaData.TableMetaData.DURATION_TRAIN, mDateHelper.getDurationLabel(t.getDurationTrain(), false));
		b.withValue(TripMetaData.TableMetaData.DURATION_WALK, mDateHelper.getDurationLabel(t.getDurationWalk(), false));
		b.withValue(TripMetaData.TableMetaData.ARRIVES_IN_TIME_LABEL, mDateHelper.getDurationLabel(t.getArrivesInTime(), true));
		long departures = t.getDeparturesInTime();
		b.withValue(TripMetaData.TableMetaData.DEPATURES_IN_TIME_LABEL, mDateHelper.getDurationLabel(departures, true));
		b.withValue(TripMetaData.TableMetaData.DEPATURES_IN_TIME, departures);
		mDbOperations.add(b.build());
	}
	
	private void saveLeg(TripLeg leg, int stepNumber)
	{
		ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(TripLegMetaData.TableMetaData.CONTENT_URI);	
		b.withValue(TripLegMetaData.TableMetaData.DEST_DATE, leg.dest.date)
		.withValue(TripLegMetaData.TableMetaData.DEST_NAME, leg.dest.name)
		.withValue(TripLegMetaData.TableMetaData.DEST_ROUTE_ID, leg.dest.routeId)
		.withValue(TripLegMetaData.TableMetaData.DEST_TIME, leg.dest.time)
		.withValue(TripLegMetaData.TableMetaData.DEST_TYPE, leg.dest.type)
		.withValue(TripLegMetaData.TableMetaData.NAME, leg.name)
		.withValue(TripLegMetaData.TableMetaData.TYPE, leg.type)
		.withValue(TripLegMetaData.TableMetaData.NOTES, leg.notes)
		.withValue(TripLegMetaData.TableMetaData.ORIGIN_DATE, leg.origin.name)
		.withValue(TripLegMetaData.TableMetaData.ORIGIN_NAME, leg.origin.name)
		.withValue(TripLegMetaData.TableMetaData.ORIGIN_ROUTE_ID, leg.origin.routeId)
		.withValue(TripLegMetaData.TableMetaData.ORIGIN_TIME, leg.origin.time)
		.withValue(TripLegMetaData.TableMetaData.ORIGIN_TYPE, leg.origin.type)
		.withValue(TripLegMetaData.TableMetaData.TRIP_ID, leg.tripId)
		.withValue(TripLegMetaData.TableMetaData.DURATION, leg.getDuration())
		.withValue(TripLegMetaData.TableMetaData.DURATION_FORMATTED, leg.getFormattedDuration())
		.withValue(TripLegMetaData.TableMetaData.STEP_NUMBER, stepNumber)
		.withValue(TripLegMetaData.TableMetaData.updated, mUpdated)
		.withValue(TripLegMetaData.TableMetaData.REF, leg.ref);
		mDbOperations.add(b.build());
	}

	private void deleteOldTrips()
	{
		mContentResolver.delete(TripMetaData.TableMetaData.CONTENT_URI, null, null);
		mContentResolver.delete(TripLegMetaData.TableMetaData.CONTENT_URI, null, null);
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
