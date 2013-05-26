package com.miracleas.minrute.service;

import java.util.ArrayList;
import java.util.List;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.miracleas.minrute.net.AddressToGPSFetcher;
import com.miracleas.minrute.provider.AddressGPSMetaData;
import com.miracleas.minrute.provider.TripLegMetaData;
import com.miracleas.minrute.provider.TripMetaData;

public class FetchGpsOnMissingAddressesService extends IntentService
{
	public static final String TRIP_ID = "trip_id";
	private static final String[] PROJECTION_LEGS = { TripLegMetaData.TableMetaData.DEST_NAME, TripLegMetaData.TableMetaData._ID};
	private static final String[] PROJECTION_ADDRESS = {AddressGPSMetaData.TableMetaData.ADDRESS};
	
	private ContentResolver cr;
	
	public FetchGpsOnMissingAddressesService()
	{
		super(FetchGpsOnMissingAddressesService.class.getName());
	}
	public FetchGpsOnMissingAddressesService(String name)
	{
		super(name);
	}
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		cr = getContentResolver();
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{	
		fetchGpsOnMissingAddresses(intent.getStringExtra(TRIP_ID));	
		stopSelf();
	}
	
	private void fetchGpsOnMissingAddresses(String tripId)
	{
		//TO-DO Fetch first and last GPS from Db!!!
		if(!TextUtils.isEmpty(tripId))
		{
			List<String> addresses = fetchStopLocationNames(tripId);			
			if(!addresses.isEmpty())
			{
				String inClause = createStopLocationNamesQuery(addresses);
				List<String> addressesCached = getCachedAddresses(inClause);
				addresses.removeAll(addressesCached);
				if(!addresses.isEmpty())
				{
					fetchGPSForAddresses(addresses);
				}
			}		
			markAllGpsAddressesIsLoaded(tripId);			
		}
	}
	
	private void markAllGpsAddressesIsLoaded(String tripId)
	{
		Uri uri = Uri.withAppendedPath(TripMetaData.TableMetaData.CONTENT_URI, tripId);
		String selection = TripMetaData.TableMetaData.HAS_ALL_ADDRESS_GPSES + "=?";
		String[] selectionArgs = {"0"};
		ContentValues values = new ContentValues();
		values.put(TripMetaData.TableMetaData.HAS_ALL_ADDRESS_GPSES, "1");
		cr.update(uri, values, selection, selectionArgs);
	}
	
	private List<String> fetchStopLocationNames(String tripId)
	{
		ContentResolver cr = getContentResolver();
		String selection = TripLegMetaData.TableMetaData.TRIP_ID + "=?";
		String[] selectionArgs = { tripId};
		
		List<String> addresses = new ArrayList<String>(0);
		Cursor c = null;
		try
		{
			c = cr.query(TripLegMetaData.TableMetaData.CONTENT_URI, PROJECTION_LEGS, selection, selectionArgs, null);
			addresses = getRequestedAddesses(c);
		}
		finally
		{
			if(c!=null)
			{
				c.close();
			}
		}
		return addresses;		
	}
	
	private void fetchGPSForAddresses(List<String> addresses)
	{
		for(String address : addresses)
		{
			AddressToGPSFetcher fetcher = new AddressToGPSFetcher(this, address);
			fetcher.startFetch();
		}
	}
	
	private List<String> getRequestedAddesses(Cursor c)
	{
		List<String> addresses = new ArrayList<String>(c.getCount());
		if(c.moveToFirst())
		{

			int i = c.getColumnIndex(TripLegMetaData.TableMetaData.DEST_NAME);
			do{	
				if(!(c.isFirst() || c.isLast()))
				{
					addresses.add(c.getString(i));
				}
				
			}while(c.moveToNext());
		}
		return addresses;
	}
	
	private List<String> getCachedAddresses(String inClause)
	{
		List<String> addresses = new ArrayList<String>(0);
		Cursor c = null;
		try
		{
			String selection = inClause;
			String[] selectionArgs = null;
			//MAN KUNNE OGSAA SOEGE I SUGGESTION ADDRESS TABELLEN
			c = cr.query(AddressGPSMetaData.TableMetaData.CONTENT_URI, PROJECTION_ADDRESS, selection, selectionArgs, null);
			if(c.moveToFirst())
			{
				addresses = new ArrayList<String>(c.getCount());
				int i = c.getColumnIndex(AddressGPSMetaData.TableMetaData.ADDRESS);
				do{
					addresses.add(c.getString(i));
				}while(c.moveToNext());
			}
		}
		finally
		{
			if(c!=null)
			{
				c.close();
			}
		}
		return addresses;
	}
	/**
	 * returns a IN query clause without the first and last location in the trip
	 * @param addresses
	 * @return
	 */
	private String createStopLocationNamesQuery(List<String> addresses)
	{
		StringBuilder b = new StringBuilder();
		if(!addresses.isEmpty())
		{
			b.append(AddressGPSMetaData.TableMetaData.ADDRESS).append(" IN (");
		}
		int size = addresses.size();
		for(int i = 0; i < size; i++)
		{
			b.append("'")
			.append(addresses.get(i))
			.append("'");
			if(i+1<size)
			{
				b.append(",");
			}
		}
		
		if(!addresses.isEmpty())
		{
			b.append(") ");
		}
		
		return b.toString();
	}
}
