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
import com.miracleas.minrute.provider.AddressGPSMetaData;
import com.miracleas.minrute.provider.AddressProviderMetaData;
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
				if (!mDbOperations.isEmpty())
				{					
					saveData(AddressGPSMetaData.AUTHORITY);
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
		ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(AddressGPSMetaData.TableMetaData.CONTENT_URI);	
		b.withValue(AddressGPSMetaData.TableMetaData.type_int, type);
		b.withValue(AddressGPSMetaData.TableMetaData.updated, mUpdated);
		b.withValue(AddressGPSMetaData.TableMetaData.LATITUDE_Y, xpp.getAttributeValue(null, "y"));
		b.withValue(AddressGPSMetaData.TableMetaData.LONGITUDE_X, xpp.getAttributeValue(null, "x"));
		b.withValue(AddressGPSMetaData.TableMetaData.ADDRESS, mSearchTerm);
		mDbOperations.add(b.build());
	}


}
