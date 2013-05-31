package com.miracleas.minrute.service;

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.location.Geofence;
import com.miracleas.minrute.model.GeofenceHelper;
import com.miracleas.minrute.model.GeofenceMy;
import com.miracleas.minrute.model.TripLeg;
import com.miracleas.minrute.model.TripLegStop;
import com.miracleas.minrute.model.TripRequest;
import com.miracleas.minrute.net.JourneyDetailFetcher;
import com.miracleas.minrute.provider.AddressGPSMetaData;
import com.miracleas.minrute.provider.GeofenceMetaData;
import com.miracleas.minrute.provider.TripLegDetailStopMetaData;
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
 * then we know when the user have to leave the bus etc. (the stop before
 * destination)
 * 
 * @author kfn
 * 
 */
public class FetchGeofencesForStopBeforeService extends IntentService
{
	public static final String tag = FetchGeofencesForStopBeforeService.class.getName();

	public static final String TRIP_ID = "tripId";
	private ContentResolver mCr = null;
	private ArrayList<ContentProviderOperation> mDbOperations;

	private static final String[] PROJECTION_LEGS = { TripLegMetaData.TableMetaData._ID, TripLegMetaData.TableMetaData.ORIGIN_NAME, TripLegMetaData.TableMetaData.DEST_NAME, TripLegMetaData.TableMetaData.REF, TripLegMetaData.TableMetaData.TYPE };

	private static final String[] PROJECTION_STOP_DETAILS = { TripLegDetailStopMetaData.TableMetaData.LATITUDE, TripLegDetailStopMetaData.TableMetaData.LONGITUDE, TripLegDetailStopMetaData.TableMetaData.NAME, TripLegDetailStopMetaData.TableMetaData._ID };
	private static final String[] PROJECTION_ADDRESS = { AddressGPSMetaData.TableMetaData.ADDRESS };

	private long mUpdated = 0;

	public FetchGeofencesForStopBeforeService()
	{
		super(FetchGeofencesForStopBeforeService.class.getName());
	}

	public FetchGeofencesForStopBeforeService(String name)
	{
		super(name);
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		mCr = getContentResolver();
		mUpdated = System.currentTimeMillis();
		
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		String tripId = intent.getStringExtra(TRIP_ID);
		if (!TextUtils.isEmpty(tripId))
		{
			findTripLegStopsBeforeDestination(tripId, intent);
			findGPSOnTripLegOriginAddresses(tripId, intent);
		}
	}
	
	
	
	/**
	 * fetches all stops in the selected Trip TripLegs that are before a TripLeg
	 * destination. Saves the stops gps in the geofence database.
	 * All the addresses loaded from the TripLeg details are saved in the AddressGPS
	 * database for caching purpose (findGPSOnTripLegOriginAddresses uses this database
	 * to reduce network requests for Address to GPS conversion)
	 * @param tripId
	 * @param intent
	 */
	private boolean findTripLegStopsBeforeDestination(String tripId, Intent intent)
	{
		boolean success = false;
		List<TripLeg> legs = fetchLegs(tripId);
		fetchAllTripLegDetails(legs, tripId, intent);
		List<TripLegStop> stops = findStopBeforeDestinations(legs);
		saveGeofences(stops, tripId);
		fetchGpsOnMissingAddresses(tripId, stops);

		try
		{
			saveData(TripLegDetailStopMetaData.AUTHORITY);
			success = true;
		} catch (RemoteException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OperationApplicationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return success;
	}
	/**
	 * fetches all addresses for the selected Trip TripLegs.
	 * Makes sure that all addresses has a GPS choords, that
	 * can be used for geofencing.
	 * @param tripId
	 * @param intent
	 */
	private void findGPSOnTripLegOriginAddresses(String tripId, Intent intent)
	{
		TripRequest tripRequest = intent.getParcelableExtra(TripRequest.tag);
		Intent service = new Intent(this, FetchGpsOnMissingAddressesService.class);
		service.putExtra(FetchGpsOnMissingAddressesService.TRIP_ID, tripId);
		service.putExtra(TripRequest.tag, tripRequest);
		startService(service);
	}

	private void saveGeofences(List<TripLegStop> stops, String tripId)
	{
		for (TripLegStop stop : stops)
		{
			saveGeofence(stop, tripId);
		}
	}

	private void saveGeofence(TripLegStop stop, String tripId)
	{
		String geofenceId = null;
		if (stop.isBeforLast)
		{
			geofenceId = GeofenceHelper.LEG_ID_WITH_STOP_ID + GeofenceHelper.DELIMITER + stop.legId + GeofenceHelper.DELIMITER + stop.id;
		}

		if (geofenceId != null)
		{
			int radius = GeofenceHelper.getRadius(stop.transportType);
			double lat = (double) Integer.parseInt(stop.lat) / 1000000d;
			double lng = (double) Integer.parseInt(stop.lng) / 1000000d;
			GeofenceMy geo = new GeofenceMy(tripId, geofenceId, radius, Geofence.GEOFENCE_TRANSITION_ENTER, lat, lng);			
			GeofenceHelper.saveGeofence(geo, mDbOperations, mCr);
		}

	}

	/**
	 * returns a list with all the stops that are before the TripLegs
	 * destination
	 * 
	 * @param legs
	 * @return
	 */
	private List<TripLegStop> findStopBeforeDestinations(List<TripLeg> legs)
	{
		List<TripLegStop> stops = new ArrayList<TripLegStop>(legs.size());
		for (TripLeg leg : legs)
		{
			List<TripLegStop> legStops = fetchTripLegStop(leg);
			stops.addAll(legStops);
			// TripLegStop first = null;
			// TripLegStop last = null;
			//TripLegStop beforeLast = null;
			/*
			 * if(legStops.size()>0) { first = legStops.get(0); first.isFirst =
			 * true; } if(legStops.size()==2) { last = legStops.get(0);
			 * last.isLast = true; }
			 */
			/*if (legStops.size() > 2)
			{
				beforeLast = legStops.get(legStops.size() - 2);
				// last = legStops.get(legStops.size()-1);
				// last.isLast = true;
				beforeLast.isBeforLast = true;
				
			}*/

			/*
			 * if(first!=null) { stops.add(first); } if(last!=null) {
			 * stops.add(last); }
			 */
			/*
			 * if(beforeLast!=null) { stops.add(beforeLast); }
			 */
		}
		return stops;
	}

	/**
	 * fetches the stop before destination of TripLeg
	 * 
	 * @param leg
	 * @return
	 */
	private List<TripLegStop> fetchTripLegStop(TripLeg leg)
	{
		List<TripLegStop> legStops = new ArrayList<TripLegStop>();
		Cursor c = null;
		try
		{
			String selection = TripLegDetailStopMetaData.TableMetaData.LEG_ID + "=? AND " + TripLegDetailStopMetaData.TableMetaData.IS_PART_OF_USER_ROUTE + "=?";
			String[] selectionArgs = { leg.id + "", "1" };
			c = mCr.query(TripLegDetailStopMetaData.TableMetaData.CONTENT_URI, PROJECTION_STOP_DETAILS, selection, selectionArgs, TripLegDetailStopMetaData.TableMetaData._ID + " DESC LIMIT 3");

			if (c.getCount()==3 && c.moveToFirst() && c.moveToNext())
			{
				int iLat = c.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.LATITUDE);
				int iLng = c.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.LONGITUDE);
				int iName = c.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.NAME);
				int iId = c.getColumnIndex(TripLegDetailStopMetaData.TableMetaData._ID);
				String lat = c.getString(iLat);
				String lng = c.getString(iLng);
				String name = c.getString(iName);
				int id = c.getInt(iId);
				TripLegStop myStop = new TripLegStop(lat, lng, name, id);
				myStop.legId = leg.id;
				myStop.transportType = leg.type;
				myStop.isBeforLast = true;
				legStops.add(myStop);
			}
		} finally
		{
			if (c != null && !c.isClosed())
			{
				c.close();
			}
		}
		return legStops;
	}

	/**
	 * saves all TripLeg details to database.
	 * 
	 * @param tripLegs
	 * @param tripId
	 * @param intent
	 */
	private void fetchAllTripLegDetails(List<TripLeg> tripLegs, String tripId, Intent intent)
	{
		if (!TextUtils.isEmpty(tripId))
		{
			mDbOperations = new ArrayList<ContentProviderOperation>();

			for (TripLeg leg : tripLegs)
			{
				JourneyDetailFetcher f = new JourneyDetailFetcher(this, intent, leg);
				f.startFetch();
				mDbOperations.addAll(f.getDbOperations());
				f.clearDbOpretions();
			}
			try
			{
				saveData(TripLegMetaData.AUTHORITY);
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

	/**
	 * returns a list from database with the selected Trip TripLegs
	 * 
	 * @param tripId
	 * @return
	 */
	private List<TripLeg> fetchLegs(String tripId)
	{
		List<TripLeg> legs = new ArrayList<TripLeg>(0);
		Cursor c = null;
		try
		{
			String selection = TripLegMetaData.TableMetaData.TRIP_ID + "=?";
			String[] selectionArgs = { tripId };
			c = mCr.query(TripLegMetaData.TableMetaData.CONTENT_URI, PROJECTION_LEGS, selection, selectionArgs, TripLegMetaData.TableMetaData.STEP_NUMBER);
			if (c.moveToFirst())
			{
				legs = new ArrayList<TripLeg>(c.getCount());
				int iId = c.getColumnIndex(TripLegMetaData.TableMetaData._ID);
				int iOriginName = c.getColumnIndex(TripLegMetaData.TableMetaData.ORIGIN_NAME);
				int iDestName = c.getColumnIndex(TripLegMetaData.TableMetaData.DEST_NAME);
				int iRef = c.getColumnIndex(TripLegMetaData.TableMetaData.REF);
				int iType = c.getColumnIndex(TripLegMetaData.TableMetaData.TYPE);
				do
				{
					TripLeg leg = new TripLeg();
					leg.ref = c.getString(iRef);
					leg.originName = c.getString(iOriginName);
					leg.destName = c.getString(iDestName);
					leg.id = c.getInt(iId);
					leg.tripId = tripId;
					leg.type = c.getString(iType);
					;
					legs.add(leg);
				} while (c.moveToNext());
			}
		} finally
		{
			if (c != null && !c.isClosed())
			{
				c.close();
			}
		}
		return legs;
	}

	protected ContentProviderResult[] saveData(String authority) throws RemoteException, OperationApplicationException
	{
		ContentProviderResult[] results = null;
		if (!mDbOperations.isEmpty())
		{
			results = mCr.applyBatch(authority, mDbOperations);
			Log.d(tag, "applyBatch: " + results.length);
			mDbOperations.clear();
		}
		return results;
	}

	private void fetchGpsOnMissingAddresses(String tripId, List<TripLegStop> stops)
	{
		if (!TextUtils.isEmpty(tripId))
		{
			List<TripLegStop> addresses = stops;
			if (!addresses.isEmpty())
			{
				String inClause = createStopLocationNamesQuery(addresses);
				List<TripLegStop> addressesCached = getCachedAddresses(inClause);
				addresses.removeAll(addressesCached);
				if (!addresses.isEmpty())
				{
					saveAddresses(addresses);
				}
			}

		} else
		{
			Log.e(tag, "tripid is null");
		}
	}

	/*
	 * private void markAllGpsAddressesIsLoaded(String tripId) { Uri uri =
	 * Uri.withAppendedPath(TripMetaData.TableMetaData.CONTENT_URI, tripId);
	 * String selection = TripMetaData.TableMetaData.HAS_ALL_ADDRESS_GPSES +
	 * "=?"; String[] selectionArgs = {"0"}; ContentValues values = new
	 * ContentValues();
	 * values.put(TripMetaData.TableMetaData.HAS_ALL_ADDRESS_GPSES, "1");
	 * mCr.update(uri, values, selection, selectionArgs); }
	 */

	private void saveAddresses(List<TripLegStop> addresses)
	{
		for (TripLegStop stop : addresses)
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
			// MAN KUNNE OGSAA SOEGE I SUGGESTION ADDRESS TABELLEN
			c = mCr.query(AddressGPSMetaData.TableMetaData.CONTENT_URI, PROJECTION_ADDRESS, selection, selectionArgs, null);
			if (c.moveToFirst())
			{
				addresses = new ArrayList<TripLegStop>(c.getCount());
				int i = c.getColumnIndex(AddressGPSMetaData.TableMetaData.ADDRESS);
				do
				{
					TripLegStop t = new TripLegStop(null, null, c.getString(i), -1);
					addresses.add(t);
				} while (c.moveToNext());
			}
		} finally
		{
			if (c != null)
			{
				c.close();
			}
		}
		return addresses;
	}

	/**
	 * returns a IN query clause without the first and last location in the trip
	 * 
	 * @param addresses
	 * @return
	 */
	private String createStopLocationNamesQuery(List<TripLegStop> addresses)
	{
		StringBuilder b = new StringBuilder();
		if (!addresses.isEmpty())
		{
			b.append(AddressGPSMetaData.TableMetaData.ADDRESS).append(" IN (");
		}
		int size = addresses.size();
		for (int i = 0; i < size; i++)
		{
			b.append("'").append(addresses.get(i).name).append("'");
			if (i + 1 < size)
			{
				b.append(",");
			}
		}

		if (!addresses.isEmpty())
		{
			b.append(") ");
		}

		return b.toString();
	}
}
