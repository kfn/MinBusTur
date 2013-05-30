package com.miracleas.minrute.service;

import java.util.ArrayList;
import java.util.List;

import com.miracleas.minrute.model.GeofenceHelper;
import com.miracleas.minrute.model.TripLeg;
import com.miracleas.minrute.model.TripLegStop;
import com.miracleas.minrute.model.TripRequest;
import com.miracleas.minrute.net.JourneyDetailFetcher;
import com.miracleas.minrute.provider.AddressGPSMetaData;
import com.miracleas.minrute.provider.GeofenceMetaData;
import com.miracleas.minrute.provider.JourneyDetailStopMetaData;
import com.miracleas.minrute.provider.TripLegMetaData;
import com.miracleas.minrute.provider.TripMetaData;

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
/**
 * if user wants full support, we need to download all details for each tripleg
 * @author kfn
 *
 */
public class FetchGpsOnMissingTripLegStopAddressesService extends IntentService
{
	public static final String tag = FetchGpsOnMissingTripLegStopAddressesService.class.getName();
	
	public static final String TRIP_ID = "tripId";
	private ContentResolver mCr = null;
	private ArrayList<ContentProviderOperation> mDbOperations;
	private List<TripLeg> mLegs = null;
	private List<TripLegStop> mAllStops = null;
	
	private static final String[] PROJECTION_LEGS = {
		TripLegMetaData.TableMetaData._ID,
		TripLegMetaData.TableMetaData.ORIGIN_NAME,
		TripLegMetaData.TableMetaData.DEST_NAME,
		TripLegMetaData.TableMetaData.REF,
		TripLegMetaData.TableMetaData.TYPE
		};
	
	private static final String[] PROJECTION_STOP_DETAILS = {
		JourneyDetailStopMetaData.TableMetaData.LATITUDE,
		JourneyDetailStopMetaData.TableMetaData.LONGITUDE,
		JourneyDetailStopMetaData.TableMetaData.NAME,
		JourneyDetailStopMetaData.TableMetaData._ID
		};
	private static final String[] PROJECTION_ADDRESS = {AddressGPSMetaData.TableMetaData.ADDRESS};
	
	private long mUpdated = 0;
	
	public FetchGpsOnMissingTripLegStopAddressesService()
	{
		super(FetchGpsOnMissingTripLegStopAddressesService.class.getName());
	}
	public FetchGpsOnMissingTripLegStopAddressesService(String name)
	{
		super(name);
	}
	@Override
	public void onCreate()
	{
		super.onCreate();
		mCr = getContentResolver();
		mUpdated = System.currentTimeMillis();
		mAllStops = new ArrayList<TripLegStop>();
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		String tripId = intent.getStringExtra(TRIP_ID);
		
		if(!TextUtils.isEmpty(tripId))
		{
			getAllJourneyDetails(tripId, intent);
			List<TripLegStop> stops = findStopBeforeDestinationTripLeg();
			addGeofences(stops);			
			fetchGpsOnMissingAddresses(tripId);
			
			try
			{
				saveData(JourneyDetailStopMetaData.AUTHORITY);
			} catch (RemoteException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OperationApplicationException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			TripRequest tripRequest = intent.getParcelableExtra(TripRequest.tag);
			Intent service = new Intent(this, FetchGpsOnMissingAddressesService.class);
			service.putExtra(FetchGpsOnMissingAddressesService.TRIP_ID, tripId);			
			service.putExtra(TripRequest.tag, tripRequest);
			startService(service);
		}
		
		
	}
	
	private void addGeofences(List<TripLegStop> stops)
	{
		for(TripLegStop stop : stops)
		{
			saveGeofence(stop);
		}
	}
	
	private void saveGeofence(TripLegStop stop)
	{
		String geofenceId = null;
		if(stop.isBeforLast)
		{
			geofenceId = GeofenceHelper.LEG_ID_WITH_STOP_ID + GeofenceHelper.DELIMITER + stop.legId + GeofenceHelper.DELIMITER + stop.id;
		}
		
		if(geofenceId!=null)
		{
			ContentValues values = new ContentValues();
			values.put(GeofenceMetaData.TableMetaData.geofence_id, geofenceId);
			
			String where = GeofenceMetaData.TableMetaData.geofence_id + "=?";
			String[] selectionArgs = {geofenceId};
			int updated = mCr.update(GeofenceMetaData.TableMetaData.CONTENT_URI, values, where, selectionArgs);
			
			if(updated==0)
			{
				double lat = (double)Integer.parseInt(stop.lat) / 1000000d;
				double lng = (double)Integer.parseInt(stop.lng) / 1000000d;
				values.put(GeofenceMetaData.TableMetaData.TYPE_OF_TRANSPORT, stop.transportType);
				values.put(GeofenceMetaData.TableMetaData.LAT, lat);
				values.put(GeofenceMetaData.TableMetaData.LNG, lng);
				ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(GeofenceMetaData.TableMetaData.CONTENT_URI);
				b.withValues(values);
				mDbOperations.add(b.build());
			}
		}
		
	}
	
	/**
	 * saves all the trip legs stop that appear before destination
	 */
	private List<TripLegStop> findStopBeforeDestinationTripLeg()
	{
		List<TripLegStop> stops = new ArrayList<TripLegStop>(mLegs.size());
		for(TripLeg leg : mLegs)
		{
			List<TripLegStop> legStops = fetchTripLegStop(leg);
			mAllStops.addAll(legStops);
			TripLegStop first = null;
			TripLegStop last = null;
			TripLegStop beforeLast = null;
			if(legStops.size()>0)
			{
				first = legStops.get(0);
				first.isFirst = true;
			}
			if(legStops.size()==2)
			{
				last = legStops.get(0);
				last.isLast = true;
			}
			if(legStops.size()>2)
			{
				beforeLast = legStops.get(legStops.size()-2);
				last = legStops.get(legStops.size()-1);
				last.isLast = true;
				beforeLast.isBeforLast = true;
			}
			
			if(first!=null)
			{
				stops.add(first);
			}		
			if(last!=null)
			{
				stops.add(last);
			}		
			if(beforeLast!=null)
			{
				stops.add(beforeLast);
			}		
		}
		return stops;
	}
	/**
	 * fetches the stop before destination of TripLeg
	 * @param leg
	 * @return
	 */
	private List<TripLegStop> fetchTripLegStop(TripLeg leg)
	{
		List<TripLegStop> legStops = new ArrayList<TripLegStop>();
		Cursor c = null;
		try
		{
			String selection =  JourneyDetailStopMetaData.TableMetaData.LEG_ID + "=? AND "+JourneyDetailStopMetaData.TableMetaData.IS_PART_OF_USER_ROUTE + "=?";
			String[] selectionArgs = {leg.id + "", "1"};
			c = mCr.query(JourneyDetailStopMetaData.TableMetaData.CONTENT_URI, PROJECTION_STOP_DETAILS, selection, selectionArgs, JourneyDetailStopMetaData.TableMetaData._ID);
			
			if(c.moveToFirst())
			{
				int iLat = c.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.LATITUDE);
				int iLng = c.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.LONGITUDE);
				int iName = c.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.NAME);
				int iId = c.getColumnIndex(JourneyDetailStopMetaData.TableMetaData._ID);
				do
				{
					String lat = c.getString(iLat);
					String lng = c.getString(iLng);
					String name = c.getString(iName);
					int id = c.getInt(iId);
					TripLegStop myStop = new TripLegStop(lat, lng, name, id);
					myStop.legId = leg.id;
					myStop.transportType = leg.type;
					legStops.add(myStop);
				}
				while(c.moveToNext());
			}
		}
		finally
		{
			if(c!=null && !c.isClosed())
			{
				c.close();
			}
		}
		return legStops;
	}
	

	
	private void getAllJourneyDetails(String tripId, Intent intent)
	{
		if(!TextUtils.isEmpty(tripId))
		{
			mDbOperations = new ArrayList<ContentProviderOperation>();
			mLegs = fetchLegs(tripId);
			for(TripLeg leg : mLegs)
			{
				JourneyDetailFetcher f = new JourneyDetailFetcher(this, intent,leg);
				f.startFetch();
				mDbOperations.addAll(f.getDbOperations());
				f.clearDbOpretions();
			}
			try
			{
				saveData(TripLegMetaData.AUTHORITY);
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
	}
	
	private List<TripLeg> fetchLegs(String tripId)
	{
		List<TripLeg> legs = new ArrayList<TripLeg>(0);
		Cursor c = null;
		try
		{
			String selection = TripLegMetaData.TableMetaData.TRIP_ID + "=?";
			String[] selectionArgs = {tripId};
			c = mCr.query(TripLegMetaData.TableMetaData.CONTENT_URI, PROJECTION_LEGS, selection, selectionArgs, TripLegMetaData.TableMetaData.STEP_NUMBER);
			if(c.moveToFirst())
			{
				legs = new ArrayList<TripLeg>(c.getCount());
				int iId = c.getColumnIndex(TripLegMetaData.TableMetaData._ID);
				int iOriginName = c.getColumnIndex(TripLegMetaData.TableMetaData.ORIGIN_NAME);
				int iDestName = c.getColumnIndex(TripLegMetaData.TableMetaData.DEST_NAME);
				int iRef = c.getColumnIndex(TripLegMetaData.TableMetaData.REF);
				int iType = c.getColumnIndex(TripLegMetaData.TableMetaData.TYPE);
				do{
					TripLeg leg = new TripLeg();
					leg.ref = c.getString(iRef);
					leg.originName = c.getString(iOriginName);
					leg.destName = c.getString(iDestName);
					leg.id = c.getInt(iId);
					leg.tripId = tripId;
					leg.type = c.getString(iType);;
					legs.add(leg);
				}while(c.moveToNext());
			}
		}
		finally
		{
			if(c!=null && !c.isClosed())
			{
				c.close();
			}
		}
		return legs;
	}
	
	protected ContentProviderResult[] saveData(String authority) throws RemoteException, OperationApplicationException
	{
		ContentProviderResult[] results = null;
		if(!mDbOperations.isEmpty())
		{
			results  = mCr.applyBatch(authority, mDbOperations);			
			Log.d(tag, "applyBatch: "+results.length);		
			mDbOperations.clear();
		}
		return results;
	}
	
	
	private void fetchGpsOnMissingAddresses(String tripId)
	{
		if(!TextUtils.isEmpty(tripId))
		{
			List<TripLegStop> addresses = mAllStops;			
			if(!addresses.isEmpty())
			{
				String inClause = createStopLocationNamesQuery(addresses);
				List<TripLegStop> addressesCached = getCachedAddresses(inClause);
				addresses.removeAll(addressesCached);
				if(!addresses.isEmpty())
				{
					saveAddresses(addresses);
				}
			}		
				
		}
		else
		{
			Log.e(tag, "tripid is null");
		}
	}
	
	/*private void markAllGpsAddressesIsLoaded(String tripId)
	{
		Uri uri = Uri.withAppendedPath(TripMetaData.TableMetaData.CONTENT_URI, tripId);
		String selection = TripMetaData.TableMetaData.HAS_ALL_ADDRESS_GPSES + "=?";
		String[] selectionArgs = {"0"};
		ContentValues values = new ContentValues();
		values.put(TripMetaData.TableMetaData.HAS_ALL_ADDRESS_GPSES, "1");
		mCr.update(uri, values, selection, selectionArgs);
	}*/
	
	private void saveAddresses(List<TripLegStop> addresses)
	{
		for(TripLegStop stop : addresses)
		{
			saveAddress(stop);
		}
	}
	
	private void saveAddress(TripLegStop stop)
	{
		ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(AddressGPSMetaData.TableMetaData.CONTENT_URI);			
		b.withValue(AddressGPSMetaData.TableMetaData.updated, mUpdated);
		b.withValue(AddressGPSMetaData.TableMetaData.LATITUDE_Y, stop.lat);
		b.withValue(AddressGPSMetaData.TableMetaData.LONGITUDE_X, stop.lng);
		b.withValue(AddressGPSMetaData.TableMetaData.ADDRESS, stop.name);
		mDbOperations.add(b.build());
	}
	
	
	private List<TripLegStop> getCachedAddresses(String inClause)
	{
		List<TripLegStop> addresses = new ArrayList<TripLegStop>(0);
		Cursor c = null;
		try
		{
			String selection = inClause;
			String[] selectionArgs = null;
			//MAN KUNNE OGSAA SOEGE I SUGGESTION ADDRESS TABELLEN
			c = mCr.query(AddressGPSMetaData.TableMetaData.CONTENT_URI, PROJECTION_ADDRESS, selection, selectionArgs, null);
			if(c.moveToFirst())
			{
				addresses = new ArrayList<TripLegStop>(c.getCount());
				int i = c.getColumnIndex(AddressGPSMetaData.TableMetaData.ADDRESS);
				do{
					TripLegStop t = new TripLegStop(null,null, c.getString(i), -1);
					addresses.add(t);
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
	private String createStopLocationNamesQuery(List<TripLegStop> addresses)
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
			.append(addresses.get(i).name)
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
