package com.miracleas.minrute.service;

import java.util.ArrayList;
import java.util.List;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.miracleas.minrute.model.TripRequest;
import com.miracleas.minrute.net.AddressToGPSFetcher;
import com.miracleas.minrute.provider.AddressGPSMetaData;
import com.miracleas.minrute.provider.JourneyDetailStopImagesMetaData;
import com.miracleas.minrute.provider.TripLegMetaData;
import com.miracleas.minrute.provider.TripMetaData;

public class FetchGpsOnMissingAddressesService extends IntentService
{
	public static final String tag = FetchGpsOnMissingAddressesService.class.getName();
	public static final String TRIP_ID = "trip_id";
	private static final String[] PROJECTION_LEGS = { TripLegMetaData.TableMetaData.ORIGIN_NAME, TripLegMetaData.TableMetaData._ID};
	private static final String[] PROJECTION_ADDRESS = {AddressGPSMetaData.TableMetaData.ADDRESS};
	private boolean mHasStartLocation = false;
	private boolean mHasStopLocation = false;
	private ArrayList<ContentProviderOperation> mDbOperations;
	
	private ContentResolver mContentResolver;
	
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
		mContentResolver = getContentResolver();
		mDbOperations = new ArrayList<ContentProviderOperation>();
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{	
		TripRequest tripRequest = intent.getParcelableExtra(TripRequest.tag);
		insertStartAndEndLocationsForTrip(tripRequest);
		
		fetchGpsOnMissingAddresses(intent.getStringExtra(TRIP_ID));	
		stopSelf();
	}
	
	private void fetchGpsOnMissingAddresses(String tripId)
	{
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
		else
		{
			Log.e(tag, "tripid is null");
		}
	}
	
	private void insertStartAndEndLocationsForTrip(TripRequest tripRequest)
	{
		mHasStartLocation = insertAddress(tripRequest.originCoordNameNotEncoded, tripRequest.getOriginCoordY(), tripRequest.getOriginCoordX());
		mHasStopLocation = insertAddress(tripRequest.destCoordNameNotEncoded, tripRequest.getDestCoordY(), tripRequest.getDestCoordX());
	}
	
	private boolean insertAddress(String address, String latY, String lngX)
	{
		boolean hasAddressCached = false;
		if(!(TextUtils.isEmpty(address) && TextUtils.isEmpty(latY) && TextUtils.isEmpty(lngX)))
		{
			ContentResolver cr = getContentResolver();
			ContentValues values = new ContentValues();
			values.put(AddressGPSMetaData.TableMetaData.ADDRESS, address);
			values.put(AddressGPSMetaData.TableMetaData.LATITUDE_Y, latY);
			values.put(AddressGPSMetaData.TableMetaData.LONGITUDE_X, lngX);
			
			String where = AddressGPSMetaData.TableMetaData.ADDRESS + "=?";
			String[] selectionArgs = {address};
			hasAddressCached = cr.update(AddressGPSMetaData.TableMetaData.CONTENT_URI, values, where, selectionArgs) > 0;
			if(!hasAddressCached)
			{
				hasAddressCached = cr.insert(AddressGPSMetaData.TableMetaData.CONTENT_URI, values)!=null;				
			}
			saveGoogleStreetViewImage(latY, lngX, address);
		}
		return hasAddressCached;
		
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
	
	
	private void markAllGpsAddressesIsLoaded(String tripId)
	{
		Uri uri = Uri.withAppendedPath(TripMetaData.TableMetaData.CONTENT_URI, tripId);
		String selection = TripMetaData.TableMetaData.HAS_ALL_ADDRESS_GPSES + "=?";
		String[] selectionArgs = {"0"};
		ContentValues values = new ContentValues();
		values.put(TripMetaData.TableMetaData.HAS_ALL_ADDRESS_GPSES, "1");
		mContentResolver.update(uri, values, selection, selectionArgs);
	}
	
	private List<String> fetchStopLocationNames(String tripId)
	{
		
		String selection = TripLegMetaData.TableMetaData.TRIP_ID + "=?";
		String[] selectionArgs = { tripId};
		
		List<String> addresses = new ArrayList<String>(0);
		Cursor c = null;
		try
		{
			c = mContentResolver.query(TripLegMetaData.TableMetaData.CONTENT_URI, PROJECTION_LEGS, selection, selectionArgs, null);
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
			mDbOperations.addAll(fetcher.getDbOpersions());
		}
		try
		{
			saveData(AddressGPSMetaData.AUTHORITY);
		} 
		catch (RemoteException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (OperationApplicationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected ContentProviderResult[] saveData(String authority) throws RemoteException, OperationApplicationException
	{
		ContentProviderResult[] results = null;
		if(!mDbOperations.isEmpty())
		{
			results  = mContentResolver.applyBatch(authority, mDbOperations);			
			Log.d(tag, "applyBatch: "+results.length);		
			mDbOperations.clear();
		}
		return results;
	}
	
	private List<String> getRequestedAddesses(Cursor c)
	{
		List<String> addresses = new ArrayList<String>(c.getCount());
		if(c.moveToFirst())
		{

			int i = c.getColumnIndex(TripLegMetaData.TableMetaData.ORIGIN_NAME);
			do{	
				if(!((c.isFirst() && mHasStartLocation) || (c.isLast() && mHasStopLocation)))
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
			c = mContentResolver.query(AddressGPSMetaData.TableMetaData.CONTENT_URI, PROJECTION_ADDRESS, selection, selectionArgs, null);
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
