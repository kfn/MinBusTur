package com.miracleas.minbustur.net;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

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
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.location.Address;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.miracleas.minbustur.CreateRouteFragment;
import com.miracleas.minbustur.R;
import com.miracleas.minbustur.provider.AddressProviderMetaData;

public class AddressFetcher extends BaseFetcher
{
	public static final String tag = AddressFetcher.class.getName();
	private static final String[] PROJECTION = {AddressProviderMetaData.TableMetaData._ID};
	private String sort = null;
	private String selection;
	public static final String URL = "http://<baseurl>/location?input=user%20input";
	
	public AddressFetcher(Context c)
	{
		super(c, null);
		sort = AddressProviderMetaData.TableMetaData._ID +" LIMIT 1";
		selection = AddressProviderMetaData.TableMetaData.searchTerm + "=?";
	}

	@Override
	boolean start()
	{
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	void end()
	{
		
		
	}
	@Override
	void fetchHelper() throws Exception
	{
		
	}
	
	private void fetchFromWebService(String userInput) throws Exception
	{
		broadcastLoading();
		HttpURLConnection urlConnection = initHttpURLConnection(URL+userInput);
		try
		{
			int repsonseCode = urlConnection.getResponseCode();
			if (repsonseCode == HttpURLConnection.HTTP_OK)
			{
				long currentTime = System.currentTimeMillis();								
				InputStream input = urlConnection.getInputStream(); //fetchFromAssets();		
				parse(input, currentTime);
				if (!mDbOperations.isEmpty())
				{
					
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
	
	protected void parse(InputStream in, long currentTime) throws XmlPullParserException, IOException
	{
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setNamespaceAware(true);
		XmlPullParser xpp = factory.newPullParser();

		xpp.setInput(in, null);
		int eventType = xpp.getEventType();
		String buildDate = "";
		boolean buildDateSet = false;
		while (eventType != XmlPullParser.END_DOCUMENT)
		{
			
			if (!buildDateSet && eventType == XmlPullParser.START_TAG && xpp.getName().equals("lastBuildDate"))
			{
				buildDate = safeNextText(xpp);
				buildDateSet = true;
			}	
			if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("koncerter"))
			{
				while (!(eventType == XmlPullParser.END_TAG && xpp.getName().equals("koncerter")))
				{
					
					eventType = xpp.next();
				}
			}						
			eventType = xpp.next();
		}
	}
	
	public synchronized void performGeocode(String locationName, Uri dataUri) throws Exception
	{
		boolean hasCachedResult = false;
		Cursor cursor = null;
		try
		{	
			String[] selectionArgs = {locationName};
			cursor = mContentResolver.query(AddressProviderMetaData.TableMetaData.CONTENT_URI, PROJECTION, selection, selectionArgs, sort);
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
			JSONObject obj = getLocationInfo(locationName);
			try
			{
				getAddresses(obj, locationName);
				saveData(dataUri);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
				throw new Exception(mContext.getString(R.string.geocoding_service_not_available));
			}
			
		}
	}
	
	private void saveData(Uri uri) throws RemoteException, OperationApplicationException
	{
		if(!mDbOperations.isEmpty())
		{
			int count = mContentResolver.applyBatch(AddressProviderMetaData.AUTHORITY, mDbOperations).length;
			Log.d(tag, "applyBatch: "+count);
			mContentResolver.notifyChange(uri, null);
		}
	}

	private JSONObject getLocationInfo(String address) throws Exception
	{
		JSONObject jsonObject = null;
		address = URLEncoder.encode(address, HTTP.UTF_8);

		URL url = new URL("http://maps.google.com/maps/api/geocode/json?address=" + address + "&sensor=false&language=da&oe=utf-8&components=country:DK");
		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
		urlConnection.addRequestProperty("Accept", "application/json");
		urlConnection.addRequestProperty("Accept-Charset", HTTP.UTF_8);
		InputStream in = new BufferedInputStream(urlConnection.getInputStream());
		int repsonseCode = urlConnection.getResponseCode();
		if (repsonseCode == 200)
		{
			String input = Utils.convertStreamToString(in, HTTP.UTF_8);
			jsonObject = new JSONObject(input);
		}
		return jsonObject;
	}

	private void getAddresses(JSONObject jsonObject, String searchTerm) throws JSONException
	{
		Log.d(tag, "getAddresses: "+searchTerm);
		JSONArray resultObj = jsonObject.getJSONArray("results");
		List<Address> addresses = new ArrayList<Address>(resultObj.length());
		long updated = System.currentTimeMillis();
		int count = resultObj.length();
		int countOfEntries = 0;
		String startsWith = searchTerm.substring(0, 2).toLowerCase();
		for (int i = 0; i < count && countOfEntries < 10; i++)
		{
			Address address = new Address(Locale.getDefault());
			JSONObject addressObj = resultObj.getJSONObject(i);
			String formatted_address = addressObj.getString("formatted_address");
			StringBuilder bodyBuilder = new StringBuilder();
			if (!TextUtils.isEmpty(formatted_address) && !formatted_address.equals("null"))
			{
				String[] formattedAddress = formatted_address.split(", ");
				JSONObject geometry = addressObj.getJSONObject("geometry");
				JSONObject location = geometry.getJSONObject("location");

				for (int x = 0; x < formattedAddress.length; x++)
				{
					address.setAddressLine(x, formattedAddress[x]);
					bodyBuilder.append(formattedAddress[x]);
					if (x + 1 < formattedAddress.length)
					{
						bodyBuilder.append(",");
					}
				}

				address.setLatitude(location.getDouble("lat"));
				address.setLongitude(location.getDouble("lng"));
				addresses.add(address);

				ContentValues cv = new ContentValues();
				String strAddress = bodyBuilder.toString();
				if(strAddress.toLowerCase().startsWith(startsWith))
				{
					countOfEntries++;
					cv.put(AddressProviderMetaData.TableMetaData.address, strAddress);
					cv.put(AddressProviderMetaData.TableMetaData.lat, location.getDouble("lat"));
					cv.put(AddressProviderMetaData.TableMetaData.lng, location.getDouble("lng"));
					cv.put(AddressProviderMetaData.TableMetaData.searchTerm, searchTerm);
					cv.put(AddressProviderMetaData.TableMetaData.updated, updated);
					ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(AddressProviderMetaData.TableMetaData.CONTENT_URI);
					mDbOperations.add(b.withValues(cv).build());
					Log.d(tag, "add: "+strAddress);
				}
				else
				{
					Log.d(tag, "baah: "+strAddress);
				}
			}

		}
		/*long now = updated - DateUtils.WEEK_IN_MILLIS;
		String[] selectionArgs = { now + "" };
		int count = cr.delete(AddressSearchProviderMetaData.TableMetaData.CONTENT_URI, AddressSearchProviderMetaData.TableMetaData.UPDATED + "<?", selectionArgs);
		//Log.d(CLASSTAG, "deleted: " + count + " addresses");*/
	}


}
