package com.miracleas.minbustur.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.miracleas.minbustur.R;
import com.miracleas.minbustur.model.AddressSearch;
import com.miracleas.minbustur.model.TripRequest;
import com.miracleas.minbustur.provider.AddressProviderMetaData;

public class TripFetcher extends BaseFetcher
{
	public static final String tag = TripFetcher.class.getName();
	private static final String[] PROJECTION = {AddressProviderMetaData.TableMetaData._ID};
	private String selection;
	public static final String URL = BASE_URL + "/bin/ajax-getstop.exe/trip?";
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

	
	//originId=8600626&destCoordX=<xInteger>&destCoordY=<yInteger>&destCoordName=<NameOfDestination>&date=19.09.10&time=07:02&useBus=0"
	public void tripSearch(TripRequest tripRequest) throws Exception
	{
		b = new StringBuilder();
		addRequest("originId", tripRequest.getOriginId());
		addRequest("originCoordX", tripRequest.getOriginCoordX());
		addRequest("originCoordY", tripRequest.getOriginCoordY());
		addRequest("originCoordName", tripRequest.getOriginCoordName());
		addRequest("destId", tripRequest.getDestId());
		addRequest("destCoordX", tripRequest.getDestCoordX());
		addRequest("destCoordY", tripRequest.getDestCoordY());
		addRequest("destCoordName", tripRequest.getDestCoordName());
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
				String s = Utils.convertStreamToString(input, HTTP.ISO_8859_1).replace("SLs.sls=", "");
				//parse(s);
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
	
	private void parse(String in) throws IOException, JSONException
	{
		JSONObject obj = new JSONObject(in);
		JSONArray suggestions = obj.getJSONArray("suggestions");
		for(int i = 0; i < suggestions.length(); i++)
		{
			AddressSearch current = new AddressSearch();
			JSONObject s = suggestions.getJSONObject(i);
			current.address = s.getString("value");
			current.latitude = ((double)s.getInt("xcoord") / 1000000d)+ "";
			current.longitude = ((double)s.getInt("ycoord") / 1000000d)+ "";
			current.type = s.getString("type");
			current.typeStr = s.getString("typeStr");
			saveAddress(current);
		}
	}
	
	private void saveAddress(AddressSearch s)
	{
		ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(AddressProviderMetaData.TableMetaData.CONTENT_URI);
		ContentProviderOperation operation = 
		b.withValue(AddressProviderMetaData.TableMetaData.address, s.address).
		withValue(AddressProviderMetaData.TableMetaData.lat, s.latitude).
		withValue(AddressProviderMetaData.TableMetaData.lng, s.longitude).
		withValue(AddressProviderMetaData.TableMetaData.type, s.type).
		withValue(AddressProviderMetaData.TableMetaData.updated, mUpdated).
		build();
		mDbOperations.add(operation);
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
