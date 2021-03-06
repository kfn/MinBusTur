package com.miracleas.minrute.net;

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
import android.database.Cursor;
import android.net.Uri;
import android.text.format.DateUtils;
import android.util.Log;

import com.miracleas.minrute.R;
import com.miracleas.minrute.model.AddressSearch;
import com.miracleas.minrute.provider.AddressProviderMetaData;

public class AddressFetcher extends BaseFetcher
{
	public static final String tag = AddressFetcher.class.getName();
	private static final String[] PROJECTION = {AddressProviderMetaData.TableMetaData._ID};
	private String sort = null;
	private String selection;
	public static final String URL = BASE_URL + "location?input=";
	public static final int MAX = 40;
	 
	private String mSearchTerm;
	private long mUpdated = 0;
	
	public AddressFetcher(Context c, Uri notifyUri)
	{
		super(c, null);
		sort = AddressProviderMetaData.TableMetaData._ID +" LIMIT 1";
		selection = AddressProviderMetaData.TableMetaData.searchTerm + "=? AND "+AddressProviderMetaData.TableMetaData.updated + ">=?";
		mUpdated = System.currentTimeMillis();
		mUriNotify = notifyUri;
	}
	
	@Override
	void doWork() throws Exception
	{
		
	}
		
	public synchronized void performGeocode(String locationName) throws Exception
	{
		mSearchTerm = locationName;
		boolean hasCachedResult = false;
		Cursor cursor = null;
		try
		{	
			long updated = System.currentTimeMillis() - DateUtils.WEEK_IN_MILLIS;
			String[] selectionArgs = {mSearchTerm, updated+""};
			cursor = mContentResolver.query(AddressProviderMetaData.TableMetaData.CONTENT_URI, PROJECTION, selection, selectionArgs, sort);
			hasCachedResult = cursor.getCount()>0;
			//TO-DO lav clean up af old data
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
			rejseplanenAddressSearch();				
		}
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
				if (!mDbOperations.isEmpty())
				{					
					saveData(AddressProviderMetaData.AUTHORITY);
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
		int count = 0;
		while (eventType != XmlPullParser.END_DOCUMENT)
		{
			if (count<MAX && eventType == XmlPullParser.START_TAG && xpp.getName().equals("LocationList"))
			{
				eventType = xpp.next();
				while (count<MAX && !(eventType == XmlPullParser.END_TAG && xpp.getName().equals("LocationList")))
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
		ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(AddressProviderMetaData.TableMetaData.CONTENT_URI);	
		b.withValue(AddressProviderMetaData.TableMetaData.type_int, type);
		int attrCount = xpp.getAttributeCount();
		for(int i = 0; i < attrCount; i++)
		{
			b.withValue(xpp.getAttributeName(i), xpp.getAttributeValue(i));
		}
		b.withValue(AddressProviderMetaData.TableMetaData.updated, mUpdated);
		b.withValue(AddressProviderMetaData.TableMetaData.searchTerm, mSearchTerm);
		if(log)
		{
			log(tag, xpp.getAttributeValue(null, "name"));
		}
		mDbOperations.add(b.build());
	}


}
