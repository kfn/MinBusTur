package com.miracleas.minrute.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;

import org.apache.http.protocol.HTTP;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.miracleas.minrute.R;
import com.miracleas.minrute.model.Trip;
import com.miracleas.minrute.model.TripLeg;
import com.miracleas.minrute.model.TripLocation;
import com.miracleas.minrute.model.TripRequest;
import com.miracleas.minrute.provider.StopImagesMetaData;
import com.miracleas.minrute.provider.TripLegMetaData;
import com.miracleas.minrute.provider.TripMetaData;
import com.miracleas.minrute.provider.GeofenceTransitionMetaData;
import com.miracleas.minrute.utils.DateHelper;

public class TripFetcher extends BaseFetcher
{
	public static final String tag = TripFetcher.class.getName();
	public static final String URL = BASE_URL + "trip?";
	private long mUpdated = 0;
	private StringBuilder b = new StringBuilder();
	private DateHelper mDateHelper = null;
	private TripRequest mTripRequest = null;
	public static final String TRIP_REQUEST = "TRIP_REQUEST";
	
	private boolean mFoundDestination = false;
	
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
				mDateHelper = new DateHelper(mContext);
				InputStream input = urlConnection.getInputStream();
				parse(input);
				if (!mDbOperations.isEmpty())
				{					
					saveData(TripLegMetaData.AUTHORITY);
					exportDatabase();
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
										leg.origin = getTripLegValues(xpp, false);				
									}
									else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("Destination"))
									{
										leg.dest = getTripLegValues(xpp, true);	
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
									
									if(mFoundDestination)
									{
										TripLeg legDestination = leg;
										legDestination.origin = leg.dest;
										saveLeg(legDestination, trip.getLegCount()+1);
									}
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
	
	private TripLocation getTripLegValues(XmlPullParser xpp, boolean isDestination) throws UnsupportedEncodingException
	{
		TripLocation leg = new TripLocation();
		leg.name = xpp.getAttributeValue(null, "name");
		leg.date = xpp.getAttributeValue(null, "date");
		leg.routeId = xpp.getAttributeValue(null, "routeIdx");
		leg.time = xpp.getAttributeValue(null, "time");
		leg.type = xpp.getAttributeValue(null, "type");
		leg.track = xpp.getAttributeValue(null, "track");
		leg.rtTrack = xpp.getAttributeValue(null, "rtTrack");
		
		if(isDestination && mTripRequest.destCoordNameNotEncoded.equals(leg.name))
		{
			mFoundDestination = true;
		}
		else
		{
			mFoundDestination = false;
		}
		
		//saveGoogleStreetViewImage(leg.name);
		return leg;
	}
	
	private void saveGoogleStreetViewImage(String locationName) throws UnsupportedEncodingException
	{
		if(!TextUtils.isEmpty(locationName))
		{
			String url = "http://maps.googleapis.com/maps/api/streetview?size=600x300&heading=151.78&pitch=-0.76&sensor=false&location="+URLEncoder.encode(locationName, HTTP.UTF_8);
			ContentValues values = new ContentValues();
			values.put(StopImagesMetaData.TableMetaData.STOP_NAME, locationName);
			
			String selection = StopImagesMetaData.TableMetaData.STOP_NAME + "=?";
			String[] selectionArgs = {locationName};
			int updates = mContentResolver.update(StopImagesMetaData.TableMetaData.CONTENT_URI, values, selection, selectionArgs);
			if(updates==0)
			{
				values.put(StopImagesMetaData.TableMetaData.URL, url);								
				values.put(StopImagesMetaData.TableMetaData.UPLOADED, "1");
				values.put(StopImagesMetaData.TableMetaData.STOP_NAME, locationName);
				values.put(StopImagesMetaData.TableMetaData.IS_GOOGLE_STREET_NAME_SEARCH, "1");
				ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(StopImagesMetaData.TableMetaData.CONTENT_URI);
				b.withValues(values);
				mDbOperations.add(b.build());
			}
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
	
	private void updateTrip(Trip t)
	{
		Uri uri = Uri.withAppendedPath(TripMetaData.TableMetaData.CONTENT_URI, t.id);
		ContentProviderOperation.Builder b = ContentProviderOperation.newUpdate(uri);
		long duration = t.getTotalDuration();
		long departures = t.getDepatureTimeLong();
		b.withValue(TripMetaData.TableMetaData.DURATION, duration);
		b.withValue(TripMetaData.TableMetaData.DURATION_LABEL, mDateHelper.getDurationLabel(duration, false));
		b.withValue(TripMetaData.TableMetaData.LEG_COUNT, t.getLegCount());
		b.withValue(TripMetaData.TableMetaData.LEG_NAMES, t.getNames());
		b.withValue(TripMetaData.TableMetaData.LEG_TYPES, t.getTypes());
		b.withValue(TripMetaData.TableMetaData.DEPATURE_TIME, t.getDepatureTime());
		b.withValue(TripMetaData.TableMetaData.DEPATURES_TIME_LONG, departures);
		b.withValue(TripMetaData.TableMetaData.ARRIVAL_TIME, t.getArrivalTime());
		b.withValue(TripMetaData.TableMetaData.TRANSPORT_CHANGES, t.getTransportChanges());
		//b.withValue(TripMetaData.TableMetaData.DURATION_BUS, mDateHelper.getDurationLabel(t.getDurationBus(), false));
		//b.withValue(TripMetaData.TableMetaData.DURATION_TRAIN, mDateHelper.getDurationLabel(t.getDurationTrain(), false));
		//b.withValue(TripMetaData.TableMetaData.DURATION_WALK, mDateHelper.getDurationLabel(t.getDurationWalk(), false));
		mDbOperations.add(b.build());
				
		//IF VOICE ON	
		/*mDateHelper.setVoice(true);
		departures = departures - System.currentTimeMillis();
		b = ContentProviderOperation.newInsert(TripVoiceMetaData.TableMetaData.CONTENT_URI);
		b.withValue(TripVoiceMetaData.TableMetaData.TRIP_ID, t.id);
		b.withValue(TripVoiceMetaData.TableMetaData.DEPARTURES_IN, mDateHelper.getDurationLabel(departures, true));
		mDbOperations.add(b.build());
		mDateHelper.setVoice(false);*/
	}
	
	private void saveLeg(TripLeg leg, int stepNumber)
	{
		ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(TripLegMetaData.TableMetaData.CONTENT_URI);	
		b.withValue(TripLegMetaData.TableMetaData.DEST_DATE, leg.dest.date)
		.withValue(TripLegMetaData.TableMetaData.DEST_NAME, leg.dest.name)
		.withValue(TripLegMetaData.TableMetaData.DEST_ROUTE_ID, leg.dest.routeId)
		.withValue(TripLegMetaData.TableMetaData.DEST_TIME, leg.dest.time)
		.withValue(TripLegMetaData.TableMetaData.DEST_TYPE, leg.dest.type)
		.withValue(TripLegMetaData.TableMetaData.DEPARTURES_IN_TIME_LONG, leg.getCalculatedDepartures())
		.withValue(TripLegMetaData.TableMetaData.NAME, leg.name)
		.withValue(TripLegMetaData.TableMetaData.TYPE, leg.type)
		.withValue(TripLegMetaData.TableMetaData.NOTES, leg.notes)
		.withValue(TripLegMetaData.TableMetaData.ORIGIN_DATE, leg.origin.date)
		.withValue(TripLegMetaData.TableMetaData.ORIGIN_NAME, leg.origin.name)
		.withValue(TripLegMetaData.TableMetaData.ORIGIN_ROUTE_ID, leg.origin.routeId)
		.withValue(TripLegMetaData.TableMetaData.ORIGIN_TIME, leg.origin.time)
		.withValue(TripLegMetaData.TableMetaData.ORIGIN_TYPE, leg.origin.type)
		.withValue(TripLegMetaData.TableMetaData.TRIP_ID, leg.tripId)
		.withValue(TripLegMetaData.TableMetaData.DURATION, leg.getCalculatedDuration())
		.withValue(TripLegMetaData.TableMetaData.DURATION_FORMATTED, leg.getFormattedDuration())
		.withValue(TripLegMetaData.TableMetaData.STEP_NUMBER, stepNumber)
		.withValue(TripLegMetaData.TableMetaData.updated, mUpdated)
		.withValue(TripLegMetaData.TableMetaData.REF, leg.ref)
		.withValue(TripLegMetaData.TableMetaData.ORIGIN_TRACK, leg.origin.track)
		.withValue(TripLegMetaData.TableMetaData.DEST_TRACK, leg.dest.track)
		.withValue(TripLegMetaData.TableMetaData.ORIGIN_RT_TRACK, leg.origin.rtTrack)
		.withValue(TripLegMetaData.TableMetaData.DEST_RT_TRACK, leg.dest.rtTrack);
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
