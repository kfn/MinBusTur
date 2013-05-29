package com.miracleas.minrute.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.protocol.HTTP;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import com.miracleas.minrute.R;
import com.miracleas.minrute.model.AddressSearch;
import com.miracleas.minrute.provider.AddressGPSMetaData;
import com.miracleas.minrute.provider.AddressProviderMetaData;
import com.miracleas.minrute.provider.JourneyDetailStopImagesMetaData;
/**
 * searches for a Locations GPS values - uses first result
 * @author kfn
 *
 */
public class AddressToGPSFetcher extends BaseFetcher
{
	public static final String tag = AddressToGPSFetcher.class.getName();
	private static final String URL = BASE_URL + "location?input=";	 
	private String mSearchTerm;
	private long mUpdated = 0;
	
	public AddressToGPSFetcher(Context c, String address)
	{
		super(c, null);
		mUpdated = System.currentTimeMillis();
		mSearchTerm = address;
	}
	
	@Override
	void doWork() throws Exception
	{
		rejseplanenAddressSearch();
	}

	private void rejseplanenAddressSearch() throws Exception
	{
		HttpURLConnection urlConnection = initHttpURLConnection(URL+URLEncoder.encode(mSearchTerm, HTTP.ISO_8859_1));	
		try
		{
			int repsonseCode = urlConnection.getResponseCode();
			if (repsonseCode == HttpURLConnection.HTTP_OK)
			{
				mUpdated = System.currentTimeMillis();								
				InputStream input = urlConnection.getInputStream();
				parse(input);
				/*if (!mDbOperations.isEmpty())
				{					
					saveData(AddressGPSMetaData.AUTHORITY);
				}*/

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
		int count = 0;
		while (eventType != XmlPullParser.END_DOCUMENT)
		{
			if (count<1 && eventType == XmlPullParser.START_TAG && xpp.getName().equals("LocationList"))
			{
				eventType = xpp.next();
				while (count<1 && !(eventType == XmlPullParser.END_TAG && xpp.getName().equals("LocationList")))
				{
					if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("StopLocation"))
					{						
						saveLocation(xpp, AddressSearch.TYPE_STATION_STOP);
						count++;
					}
					else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("CoordLocation"))
					{						
						saveLocation(xpp, AddressSearch.TYPE_ADRESSE);
						count++;
					}
					eventType = xpp.next();				
				}				
			}
			eventType = xpp.next();		
		}
	}
	
	private void saveLocation(XmlPullParser xpp, int type)
	{
		String lat = xpp.getAttributeValue(null, "y");
		String lng = xpp.getAttributeValue(null, "x");
		String locationName = mSearchTerm;
		ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(AddressGPSMetaData.TableMetaData.CONTENT_URI);	
		b.withValue(AddressGPSMetaData.TableMetaData.type_int, type);
		b.withValue(AddressGPSMetaData.TableMetaData.updated, mUpdated);
		b.withValue(AddressGPSMetaData.TableMetaData.LATITUDE_Y, lat);
		b.withValue(AddressGPSMetaData.TableMetaData.LONGITUDE_X, lng);
		b.withValue(AddressGPSMetaData.TableMetaData.ADDRESS, locationName);
		mDbOperations.add(b.build());
		
		saveGoogleStreetViewImage(lat, lng, locationName);
	}
	
	private void saveGoogleStreetViewImage(String lat, String lng, String locationName)
	{		
		double lat1 = (double) (Integer.parseInt(lat) / 1000000d);
		double lng1 = (double) (Integer.parseInt(lng) / 1000000d);
		
		
		ContentValues values = new ContentValues();
		values.put(JourneyDetailStopImagesMetaData.TableMetaData.STOP_NAME, locationName);
		
		String selection = JourneyDetailStopImagesMetaData.TableMetaData.STOP_NAME + "=?";
		String[] selectionArgs = {locationName};
		int updates = mContentResolver.update(JourneyDetailStopImagesMetaData.TableMetaData.CONTENT_URI, values, selection, selectionArgs);
		if(updates==0)
		{
			StringBuilder b1 = new StringBuilder();
			b1.append("http://maps.googleapis.com/maps/api/streetview?size=600x300&heading=151.78&pitch=-0.76&sensor=false&location=")
			.append(lat1).append(",").append(lng1);
			
			values.put(JourneyDetailStopImagesMetaData.TableMetaData.URL, b1.toString());								
			values.put(JourneyDetailStopImagesMetaData.TableMetaData.UPLOADED, "1");
			values.put(JourneyDetailStopImagesMetaData.TableMetaData.STOP_NAME, locationName);
			values.put(JourneyDetailStopImagesMetaData.TableMetaData.IS_GOOGLE_STREET_LAT_LNG, "1");
			values.put(JourneyDetailStopImagesMetaData.TableMetaData.LAT, lat1);
			values.put(JourneyDetailStopImagesMetaData.TableMetaData.LNG, lng1);
			ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(JourneyDetailStopImagesMetaData.TableMetaData.CONTENT_URI);
			b.withValues(values);
			mDbOperations.add(b.build());
		}		
	}
	public ArrayList<ContentProviderOperation> getDbOpersions()
	{
		return mDbOperations;
	}

}
