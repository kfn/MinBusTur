package com.miracleas.minrute.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.miracleas.minrute.model.JourneyDetail;
import com.miracleas.minrute.model.TripLeg;
import com.miracleas.minrute.provider.TripLegDetailMetaData;
import com.miracleas.minrute.provider.TripLegDetailNoteMetaData;
import com.miracleas.minrute.provider.StopImagesMetaData;
import com.miracleas.minrute.provider.TripLegDetailStopMetaData;
import com.miracleas.minrute.provider.TripLegMetaData;

public class JourneyDetailFetcher extends BaseFetcher
{
	public static final String URL = "URL";
	public static final String tag = JourneyDetailFetcher.class.getName();
	private List<String> mIds = null;
	private static final String[] PROJECTION = { TripLegDetailMetaData.TableMetaData._ID };
	private static final String[] PROJECTION_IMG = { StopImagesMetaData.TableMetaData._ID };
	
	private boolean mFoundOriginName = false;
	private boolean mFoundDestName = false;
	private List<MyImage> mUrls;
	private TripLeg mLeg = null;

	public JourneyDetailFetcher(Context c, Intent intent, TripLeg leg)
	{
		super(c, intent);
		mIds = new ArrayList<String>();
		mLeg = leg;
		mUrls = new ArrayList<MyImage>();
	}

	@Override
	protected boolean start()
	{
		return !TextUtils.isEmpty(mLeg.ref);
	}

	@Override
	void doWork() throws Exception
	{
		boolean hasCachedResult = false;
		Cursor cursor = null;
		try
		{
			String selection = TripLegDetailMetaData.TableMetaData.REF + "=?";
			String[] selectionArgs = { mLeg.ref };
			cursor = mContentResolver.query(TripLegDetailMetaData.TableMetaData.CONTENT_URI, PROJECTION, selection, selectionArgs, TripLegDetailMetaData.TableMetaData._ID + " LIMIT 1");
			hasCachedResult = cursor.getCount() > 0;
		} finally
		{
			if (cursor != null)
			{
				cursor.close();
			}
		}
		if (!hasCachedResult)
		{
			fetchJourneyDetails();
		}

	}

	private void fetchJourneyDetails() throws Exception
	{
		HttpURLConnection urlConnection = initHttpURLConnection(mLeg.ref);
		try
		{
			int repsonseCode = urlConnection.getResponseCode();
			if (repsonseCode == HttpURLConnection.HTTP_OK)
			{
				InputStream input = urlConnection.getInputStream();
				parse(input);
			} 
		} finally
		{
			urlConnection.disconnect();
		}

	}

	public void save()
	{
		if (!mDbOperations.isEmpty())
		{
			try
			{
				ContentProviderResult[] results = saveData(TripLegDetailStopMetaData.AUTHORITY);
				mDbOperations.clear();
				int imgCount = 0;
				for (int i = 0; i < results.length; i++)
				{
					Uri uri = results[i].uri;
					if (uri != null)
					{
						if (results[i].uri.toString().contains(TripLegDetailStopMetaData.TABLE_NAME))
						{
							String id = results[i].uri.getLastPathSegment();
							if (!TextUtils.isEmpty(id))
							{
								//Log.d(tag, "get img in index: "+imgCount);
								MyImage img = mUrls.get(imgCount);
								
								ContentValues values = new ContentValues();
								values.put(StopImagesMetaData.TableMetaData.URL, img.url);								
								String selection = StopImagesMetaData.TableMetaData.URL + "=?";
								String[] selectionArgs = {img.url};
								int updates = mContentResolver.update(StopImagesMetaData.TableMetaData.CONTENT_URI, values, selection, selectionArgs);
								if(updates==0)
								{
									values.put(StopImagesMetaData.TableMetaData.LAT, img.lat);
									values.put(StopImagesMetaData.TableMetaData.LNG, img.lng);
									values.put(StopImagesMetaData.TableMetaData.UPLOADED, "1");
									values.put(StopImagesMetaData.TableMetaData.IS_UPLOADING, "0");
									values.put(StopImagesMetaData.TableMetaData.STOP_NAME, img.stopName);
									values.put(StopImagesMetaData.TableMetaData.IS_GOOGLE_STREET_LAT_LNG, "1");
									ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(StopImagesMetaData.TableMetaData.CONTENT_URI);
									b.withValues(values);
									mDbOperations.add(b.build());
									
									selection = StopImagesMetaData.TableMetaData.IS_GOOGLE_STREET_NAME_SEARCH + "=? AND "
									+StopImagesMetaData.TableMetaData.STOP_NAME + "=?";
									String[] selectionArgs1 = {"1", img.stopName};
									b = ContentProviderOperation.newDelete(StopImagesMetaData.TableMetaData.CONTENT_URI).withSelection(selection, selectionArgs1);
									mDbOperations.add(b.build());
								}
							}
							imgCount++;
						}
					}

				}
				if (!mDbOperations.isEmpty())
				{
					saveData(StopImagesMetaData.AUTHORITY);
				}

				exportDatabase();
			} catch (RemoteException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OperationApplicationException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public ArrayList<ContentProviderOperation> getDbOperations()
	{
		return mDbOperations;
	}

	public void addContentProviderOperations(ArrayList<ContentProviderOperation> list)
	{
		mDbOperations.addAll(list);
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
					} else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("JourneyName"))
					{
						saveJourneyName(xpp, journey);
					} else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("JourneyType"))
					{
						saveJourneyType(xpp, journey);
					} else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("Note"))
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
		String depTime = xpp.getAttributeValue(null, "depTime");
		String arrTime = xpp.getAttributeValue(null, "arrTime");
		String name = xpp.getAttributeValue(null, "name");

		boolean isValid = !TextUtils.isEmpty(depTime) || !TextUtils.isEmpty(arrTime);
		if (isValid)
		{
			ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(TripLegDetailStopMetaData.TableMetaData.CONTENT_URI);

			if (!mFoundOriginName)
			{
				mFoundOriginName = name.equals(mLeg.originName);
			}

			if (mFoundOriginName && !mFoundDestName)
			{
				mFoundDestName = name.equals(mLeg.destName);
				if (mFoundDestName)
				{
					b.withValue(TripLegDetailStopMetaData.TableMetaData.IS_PART_OF_USER_ROUTE, 1);
				}
			}

			if ((mFoundOriginName && !mFoundDestName))
			{
				b.withValue(TripLegDetailStopMetaData.TableMetaData.IS_PART_OF_USER_ROUTE, 1);
			}
			String x = xpp.getAttributeValue(null, "x");
			String y = xpp.getAttributeValue(null, "y");
			String routeIdx = xpp.getAttributeValue(null, "routeIdx");
			String arrDate = xpp.getAttributeValue(null, "arrDate");
			String depDate = xpp.getAttributeValue(null, "depDate");
			String track = xpp.getAttributeValue(null, "track");

			double lat = (double) (Integer.parseInt(y) / 1000000d);
			double lng = (double) (Integer.parseInt(x) / 1000000d);
			StringBuilder url = new StringBuilder();
			url.append("http://maps.googleapis.com/maps/api/streetview?size=600x300&heading=151.78&pitch=-0.76&sensor=false&location=").append(lat).append(",").append(lng);
			
			
			MyImage img = new MyImage(y, x, url.toString(), name);
			mUrls.add(img);

			
			b.withValue(TripLegDetailStopMetaData.TableMetaData.LONGITUDE, x);
			b.withValue(TripLegDetailStopMetaData.TableMetaData.LATITUDE, y);
			b.withValue(TripLegDetailStopMetaData.TableMetaData.DEP_DATE, depDate);
			b.withValue(TripLegDetailStopMetaData.TableMetaData.ROUTE_ID_X, routeIdx);
			b.withValue(TripLegDetailStopMetaData.TableMetaData.ARR_DATE, arrDate);
			b.withValue(TripLegDetailStopMetaData.TableMetaData.TRACK, track);
			b.withValue(TripLegDetailStopMetaData.TableMetaData.NAME, name);
			b.withValue(TripLegDetailStopMetaData.TableMetaData.JOURNEY_DETAIL_ID, jouney.id);
			b.withValue(TripLegDetailStopMetaData.TableMetaData.LEG_ID, mLeg.id);
			b.withValue(TripLegDetailStopMetaData.TableMetaData.TRIP_ID, mLeg.tripId);
			b.withValue(TripLegDetailStopMetaData.TableMetaData.DEP_TIME, depTime);
			b.withValue(TripLegDetailStopMetaData.TableMetaData.ARR_TIME, arrTime);
			mDbOperations.add(b.build());
		}

	}

	private void saveNote(XmlPullParser xpp, JourneyDetail jouney)
	{
		ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(TripLegDetailNoteMetaData.TableMetaData.CONTENT_URI);
		int attrCount = xpp.getAttributeCount();
		for (int i = 0; i < attrCount; i++)
		{
			b.withValue(xpp.getAttributeName(i), xpp.getAttributeValue(i));
		}
		b.withValue(TripLegDetailNoteMetaData.TableMetaData.JOURNEY_DETAIL_ID, jouney.id);
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
		values.put(TripLegDetailMetaData.TableMetaData.NAME, "");
		Uri uri = mContentResolver.insert(TripLegDetailMetaData.TableMetaData.CONTENT_URI, values);
		JourneyDetail t = new JourneyDetail();
		t.id = uri.getLastPathSegment();
		mIds.add(t.id);
		return t;
	}

	private void updateJourneyDetail(JourneyDetail t)
	{
		Uri uri = Uri.withAppendedPath(TripLegDetailMetaData.TableMetaData.CONTENT_URI, t.id);
		ContentProviderOperation.Builder b = ContentProviderOperation.newUpdate(uri);
		b.withValue(TripLegDetailMetaData.TableMetaData.NAME, t.name);
		b.withValue(TripLegDetailMetaData.TableMetaData.NAME_ROUTE_ID_X_FROM, t.nameRouteIdxFrom);
		b.withValue(TripLegDetailMetaData.TableMetaData.NAME_ROUTE_ID_X_TO, t.nameRouteIdxTo);
		b.withValue(TripLegDetailMetaData.TableMetaData.TYPE, t.type);
		b.withValue(TripLegDetailMetaData.TableMetaData.TYPE_ROUTE_ID_X_FROM, t.typeRouteIdxFrom);
		b.withValue(TripLegDetailMetaData.TableMetaData.TYPE_ROUTE_ID_X_TO, t.typeRouteIdxTo);
		b.withValue(TripLegDetailMetaData.TableMetaData.REF, mLeg.ref);
		b.withValue(TripLegDetailMetaData.TableMetaData.TRIP_ID, mLeg.tripId);
		b.withValue(TripLegDetailMetaData.TableMetaData.LEG_ID, mLeg.id);
		b.withValue(TripLegDetailMetaData.TableMetaData.COUNT_OF_STOPS, t.countOfStops);
		mDbOperations.add(b.build());
	}

	@Override
	protected void onFatalError()
	{
		for (String id : mIds) // optimeres hvis der kommer flere..
		{
			Uri uri = Uri.withAppendedPath(TripLegDetailMetaData.TableMetaData.CONTENT_URI, id);
			mContentResolver.delete(uri, null, null);
		}
	}
	
	private class MyImage
	{
		public String lat;
		public String lng;
		public String url;
		public String stopName;
		public MyImage(String lat, String lng, String url, String stopName)
		{
			super();
			this.lat = lat;
			this.lng = lng;
			this.url = url;
			this.stopName = stopName;
		}
		
	}

	public void clearDbOpretions()
	{
		mDbOperations.clear();		
	}

}
