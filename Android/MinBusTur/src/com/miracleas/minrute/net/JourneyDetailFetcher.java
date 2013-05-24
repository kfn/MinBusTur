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
import com.miracleas.minrute.provider.JourneyDetailMetaData;
import com.miracleas.minrute.provider.JourneyDetailNoteMetaData;
import com.miracleas.minrute.provider.JourneyDetailStopImagesMetaData;
import com.miracleas.minrute.provider.JourneyDetailStopMetaData;

public class JourneyDetailFetcher extends BaseFetcher
{
	public static final String URL = "URL";
	public static final String tag = JourneyDetailFetcher.class.getName();
	private String mUrl;
	private List<String> mIds = null;
	private String mTripId;
	private String mLegId;
	private static final String[] PROJECTION = { JourneyDetailMetaData.TableMetaData._ID };
	private static final String[] PROJECTION_IMG = { JourneyDetailStopImagesMetaData.TableMetaData._ID };
	private final String mOriginName;
	private final String mDestName;
	private boolean mFoundOriginName = false;
	private boolean mFoundDestName = false;
	private List<MyImage> mUrls;

	public JourneyDetailFetcher(Context c, Intent intent, String url, String tripId, String legId, String originName, String destName)
	{
		super(c, intent);
		mIds = new ArrayList<String>();
		mUrl = url;
		mTripId = tripId;
		mLegId = legId;
		mOriginName = originName;
		mDestName = destName;
		mUrls = new ArrayList<MyImage>();
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
			String[] selectionArgs = { mUrl };
			cursor = mContentResolver.query(JourneyDetailMetaData.TableMetaData.CONTENT_URI, PROJECTION, selection, selectionArgs, JourneyDetailMetaData.TableMetaData._ID + " LIMIT 1");
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
		HttpURLConnection urlConnection = initHttpURLConnection(mUrl);
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
				ContentProviderResult[] results = saveData(JourneyDetailStopMetaData.AUTHORITY);
				mDbOperations.clear();
				int imgCount = 0;
				for (int i = 0; i < results.length; i++)
				{
					Uri uri = results[i].uri;
					if (uri != null)
					{
						if (results[i].uri.toString().contains(JourneyDetailStopMetaData.TABLE_NAME))
						{
							String id = results[i].uri.getLastPathSegment();
							if (!TextUtils.isEmpty(id))
							{
								//Log.d(tag, "get img in index: "+imgCount);
								MyImage img = mUrls.get(imgCount);
								
								ContentValues values = new ContentValues();
								values.put(JourneyDetailStopImagesMetaData.TableMetaData.URL, img.url);								
								values.put(JourneyDetailStopImagesMetaData.TableMetaData.LAT, img.lat);
								values.put(JourneyDetailStopImagesMetaData.TableMetaData.LNG, img.lng);
								values.put(JourneyDetailStopImagesMetaData.TableMetaData.UPLOADED, "1");
								values.put(JourneyDetailStopImagesMetaData.TableMetaData.IS_UPLOADING, "0");
								values.put(JourneyDetailStopImagesMetaData.TableMetaData.STOP_NAME, img.stopName);
								
								String selection = JourneyDetailStopImagesMetaData.TableMetaData.URL + "=?";
								String[] selectionArgs = {img.url};
								int updates = mContentResolver.update(JourneyDetailStopImagesMetaData.TableMetaData.CONTENT_URI, values, selection, selectionArgs);
								if(updates==0)
								{
									ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(JourneyDetailStopImagesMetaData.TableMetaData.CONTENT_URI);
									b.withValues(values);
									mDbOperations.add(b.build());
								}
							}
							imgCount++;
						}
					}

				}
				if (!mDbOperations.isEmpty())
				{
					saveData(JourneyDetailStopImagesMetaData.AUTHORITY);
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
			ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(JourneyDetailStopMetaData.TableMetaData.CONTENT_URI);

			if (!mFoundOriginName)
			{
				mFoundOriginName = name.equals(mOriginName);
			}

			if (mFoundOriginName && !mFoundDestName)
			{
				mFoundDestName = name.equals(mDestName);
				if (mFoundDestName)
				{
					b.withValue(JourneyDetailStopMetaData.TableMetaData.IS_PART_OF_USER_ROUTE, 1);
				}
			}

			if ((mFoundOriginName && !mFoundDestName))
			{
				b.withValue(JourneyDetailStopMetaData.TableMetaData.IS_PART_OF_USER_ROUTE, 1);
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

			b.withValue(JourneyDetailStopMetaData.TableMetaData.LONGITUDE, x);
			b.withValue(JourneyDetailStopMetaData.TableMetaData.LATITUDE, y);
			b.withValue(JourneyDetailStopMetaData.TableMetaData.DEP_DATE, depDate);
			b.withValue(JourneyDetailStopMetaData.TableMetaData.ROUTE_ID_X, routeIdx);
			b.withValue(JourneyDetailStopMetaData.TableMetaData.ARR_DATE, arrDate);
			b.withValue(JourneyDetailStopMetaData.TableMetaData.TRACK, track);
			b.withValue(JourneyDetailStopMetaData.TableMetaData.NAME, name);
			b.withValue(JourneyDetailStopMetaData.TableMetaData.JOURNEY_DETAIL_ID, jouney.id);
			b.withValue(JourneyDetailStopMetaData.TableMetaData.LEG_ID, mLegId);
			b.withValue(JourneyDetailStopMetaData.TableMetaData.TRIP_ID, mTripId);
			b.withValue(JourneyDetailStopMetaData.TableMetaData.DEP_TIME, depTime);
			b.withValue(JourneyDetailStopMetaData.TableMetaData.ARR_TIME, arrTime);
			mDbOperations.add(b.build());
		}

	}

	private void saveNote(XmlPullParser xpp, JourneyDetail jouney)
	{
		ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(JourneyDetailNoteMetaData.TableMetaData.CONTENT_URI);
		int attrCount = xpp.getAttributeCount();
		for (int i = 0; i < attrCount; i++)
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
		b.withValue(JourneyDetailMetaData.TableMetaData.LEG_ID, mLegId);
		b.withValue(JourneyDetailMetaData.TableMetaData.COUNT_OF_STOPS, t.countOfStops);
		mDbOperations.add(b.build());
	}

	@Override
	protected void onFatalError()
	{
		for (String id : mIds) // optimeres hvis der kommer flere..
		{
			Uri uri = Uri.withAppendedPath(JourneyDetailMetaData.TableMetaData.CONTENT_URI, id);
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

}
