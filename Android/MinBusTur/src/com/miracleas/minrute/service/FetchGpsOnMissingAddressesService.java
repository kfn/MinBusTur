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
import android.text.format.DateUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.miracleas.minrute.model.GeofenceHelper;
import com.miracleas.minrute.model.GeofenceMy;
import com.miracleas.minrute.model.TripRequest;
import com.miracleas.minrute.net.AddressToGPSFetcher;
import com.miracleas.minrute.provider.AddressGPSMetaData;
import com.miracleas.minrute.provider.GeofenceMetaData;
import com.miracleas.minrute.provider.StopImagesMetaData;
import com.miracleas.minrute.provider.TripLegMetaData;
import com.miracleas.minrute.provider.TripMetaData;

public class FetchGpsOnMissingAddressesService extends IntentService
{
	public static final String tag = FetchGpsOnMissingAddressesService.class.getName();
	public static final String TRIP_ID = "trip_id";
	private static final String[] PROJECTION_LEGS = { TripLegMetaData.TableMetaData.ORIGIN_NAME, TripLegMetaData.TableMetaData._ID };
	private static final String[] PROJECTION_ADDRESS = { AddressGPSMetaData.TableMetaData.ADDRESS };
	private boolean mHasStartLocation = false;
	private boolean mHasStopLocation = false;
	private ArrayList<ContentProviderOperation> mDbOperations;

	private static final String[] projectionAddress = { TripLegMetaData.TableMetaData._ID, TripLegMetaData.TableMetaData.TYPE, AddressGPSMetaData.TableMetaData.ADDRESS, AddressGPSMetaData.TableMetaData.LATITUDE_Y, AddressGPSMetaData.TableMetaData.LONGITUDE_X };

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
		String tripId = intent.getStringExtra(TRIP_ID);
		TripRequest tripRequest = intent.getParcelableExtra(TripRequest.tag);
		if(!TextUtils.isEmpty(tripId) && tripRequest!=null)
		{			
			saveStartAndEndLocationsForTrip(tripRequest);
			fetchAndSaveGpsOnMissingAddresses(tripId);
			List<GeofenceMy> geofences = getGeofencesFromAddressGPS(tripId);
			saveGeofences(geofences);
			try
			{
				saveData(GeofenceMetaData.AUTHORITY);
				markAllGpsAddressesIsLoaded(tripId);
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
		
		stopSelf();
	}

	/**
	 * fetches all the addresses in the selected Trip TripLegs. if any of the
	 * addresses does not exist in the AddressGPS database, we need to fetch the
	 * GPS for the address from a webservice. We need the GPS'es for geofencing.
	 * The address/gps results are saved to database
	 * 
	 * @param tripId
	 */
	private void fetchAndSaveGpsOnMissingAddresses(String tripId)
	{
		if (!TextUtils.isEmpty(tripId))
		{
			List<String> addresses = fetchStopLocationNames(tripId);
			if (!addresses.isEmpty())
			{
				String inClause = createStopLocationNamesQuery(addresses);
				List<String> addressesCached = getCachedAddresses(inClause);
				addresses.removeAll(addressesCached); // remove the cached
														// addresses
				if (!addresses.isEmpty())
				{
					fetchGPSForAddresses(addresses);
				}
			}
			
		} else
		{
			Log.e(tag, "tripid is null");
		}
	}

	/**
	 * saves the origin and destination address on the AddressGPS database, so
	 * that we can retrieve the GPS choords for geofencing
	 * 
	 * @param tripRequest
	 */
	private void saveStartAndEndLocationsForTrip(TripRequest tripRequest)
	{
		mHasStartLocation = insertAddress(tripRequest.originCoordNameNotEncoded, tripRequest.getOriginCoordY(), tripRequest.getOriginCoordX());
		mHasStopLocation = insertAddress(tripRequest.destCoordNameNotEncoded, tripRequest.getDestCoordY(), tripRequest.getDestCoordX());
	}

	private boolean insertAddress(String address, String latY, String lngX)
	{
		boolean hasAddressCached = false;
		if (!(TextUtils.isEmpty(address) && TextUtils.isEmpty(latY) && TextUtils.isEmpty(lngX)))
		{
			ContentResolver cr = getContentResolver();
			ContentValues values = new ContentValues();
			values.put(AddressGPSMetaData.TableMetaData.ADDRESS, address);
			values.put(AddressGPSMetaData.TableMetaData.LATITUDE_Y, latY);
			values.put(AddressGPSMetaData.TableMetaData.LONGITUDE_X, lngX);

			String where = AddressGPSMetaData.TableMetaData.ADDRESS + "=?";
			String[] selectionArgs = { address };
			hasAddressCached = cr.update(AddressGPSMetaData.TableMetaData.CONTENT_URI, values, where, selectionArgs) > 0;
			if (!hasAddressCached)
			{
				hasAddressCached = cr.insert(AddressGPSMetaData.TableMetaData.CONTENT_URI, values) != null;
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
		values.put(StopImagesMetaData.TableMetaData.STOP_NAME, locationName);

		String selection = StopImagesMetaData.TableMetaData.STOP_NAME + "=?";
		String[] selectionArgs = { locationName };
		int updates = mContentResolver.update(StopImagesMetaData.TableMetaData.CONTENT_URI, values, selection, selectionArgs);
		if (updates == 0)
		{
			StringBuilder b1 = new StringBuilder();
			b1.append("http://maps.googleapis.com/maps/api/streetview?size=600x300&heading=151.78&pitch=-0.76&sensor=false&location=").append(lat1).append(",").append(lng1);

			values.put(StopImagesMetaData.TableMetaData.URL, b1.toString());
			values.put(StopImagesMetaData.TableMetaData.UPLOADED, "1");
			values.put(StopImagesMetaData.TableMetaData.STOP_NAME, locationName);
			values.put(StopImagesMetaData.TableMetaData.IS_GOOGLE_STREET_LAT_LNG, "1");
			values.put(StopImagesMetaData.TableMetaData.LAT, lat1);
			values.put(StopImagesMetaData.TableMetaData.LNG, lng1);
			ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(StopImagesMetaData.TableMetaData.CONTENT_URI);
			b.withValues(values);
			mDbOperations.add(b.build());
		}
	}

	/**
	 * marks the selected Trip with the "Has all GPS choords on addresses". We
	 * can now create the desired geofences for the selected trip.
	 * 
	 * @param tripId
	 */
	private void markAllGpsAddressesIsLoaded(String tripId)
	{
		Uri uri = Uri.withAppendedPath(TripMetaData.TableMetaData.CONTENT_URI, tripId);
		String selection = TripMetaData.TableMetaData.HAS_ALL_ADDRESS_GPSES + "=?";
		String[] selectionArgs = { "0" };
		ContentValues values = new ContentValues();
		values.put(TripMetaData.TableMetaData.HAS_ALL_ADDRESS_GPSES, "1");
		mContentResolver.update(uri, values, selection, selectionArgs);
	}

	/**
	 * fetches all the origin addresses in the selected Trip TripLegs.
	 * 
	 * @param tripId
	 * @return
	 */
	private List<String> fetchStopLocationNames(String tripId)
	{

		String selection = TripLegMetaData.TableMetaData.TRIP_ID + "=?";
		String[] selectionArgs = { tripId };

		List<String> addresses = new ArrayList<String>(0);
		Cursor c = null;
		try
		{
			c = mContentResolver.query(TripLegMetaData.TableMetaData.CONTENT_URI, PROJECTION_LEGS, selection, selectionArgs, null);
			addresses = getRequestedAddesses(c);
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
	 * fetches all the addresses that are not cached from a webservice. We need
	 * the GPS choords for each address. The results are saved to database.
	 * 
	 * @param addresses
	 */
	private void fetchGPSForAddresses(List<String> addresses)
	{
		for (String address : addresses)
		{
			AddressToGPSFetcher fetcher = new AddressToGPSFetcher(this, address);
			fetcher.startFetch();
			mDbOperations.addAll(fetcher.getDbOpersions());
		}
		try
		{
			saveData(AddressGPSMetaData.AUTHORITY);
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

	protected ContentProviderResult[] saveData(String authority) throws RemoteException, OperationApplicationException
	{
		ContentProviderResult[] results = null;
		if (!mDbOperations.isEmpty())
		{
			results = mContentResolver.applyBatch(authority, mDbOperations);
			Log.d(tag, "applyBatch: " + results.length);
			mDbOperations.clear();
		}
		return results;
	}

	private List<String> getRequestedAddesses(Cursor c)
	{
		List<String> addresses = new ArrayList<String>(c.getCount());
		if (c.moveToFirst())
		{

			int i = c.getColumnIndex(TripLegMetaData.TableMetaData.ORIGIN_NAME);
			do
			{
				if (!((c.isFirst() && mHasStartLocation) || (c.isLast() && mHasStopLocation)))
				{
					addresses.add(c.getString(i));
				}

			} while (c.moveToNext());
		}
		return addresses;
	}

	/**
	 * returns a list with all of the addresses that are already cached
	 * 
	 * @param inClause
	 * @return
	 */
	private List<String> getCachedAddresses(String inClause)
	{
		List<String> addresses = new ArrayList<String>(0);
		Cursor c = null;
		try
		{
			String selection = inClause;
			String[] selectionArgs = null;
			// MAN KUNNE OGSAA SOEGE I SUGGESTION ADDRESS TABELLEN
			c = mContentResolver.query(AddressGPSMetaData.TableMetaData.CONTENT_URI, PROJECTION_ADDRESS, selection, selectionArgs, null);
			if (c.moveToFirst())
			{
				addresses = new ArrayList<String>(c.getCount());
				int i = c.getColumnIndex(AddressGPSMetaData.TableMetaData.ADDRESS);
				do
				{
					addresses.add(c.getString(i));
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
	 * returns a IN query clause without origin addresses in the selected trip
	 * 
	 * @param addresses
	 * @return
	 */
	private String createStopLocationNamesQuery(List<String> addresses)
	{
		StringBuilder b = new StringBuilder();
		if (!addresses.isEmpty())
		{
			b.append(AddressGPSMetaData.TableMetaData.ADDRESS).append(" IN (");
		}
		int size = addresses.size();
		for (int i = 0; i < size; i++)
		{
			b.append("'").append(addresses.get(i)).append("'");
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
	/**
	 * returns a list of the selected Trip geofences. Theses addresses does not 
	 * include addresses in TripLegDetailStop (used for finding location before
	 * destination of each TripLeg).
	 * matches the selected Trip TripLegs origin addresses with the addresses saved
	 * in the AddressGPS table. All matches are converted to Geofence objects
	 * @param tripId
	 * @return
	 */
	private List<GeofenceMy> getGeofencesFromAddressGPS(String tripId)
	{
		List<GeofenceMy> geofences = new ArrayList<GeofenceMy>();
		String selection = TripLegMetaData.TableMetaData.TRIP_ID + "=?";
		String[] selectionArgs = { tripId };
		Uri uri = Uri.withAppendedPath(TripLegMetaData.TableMetaData.CONTENT_URI, AddressGPSMetaData.TABLE_NAME);
		ContentResolver cr = getContentResolver();

		Cursor c = null;
		try
		{
			c = cr.query(uri, projectionAddress, selection, selectionArgs, null);
			if (c.moveToFirst())
			{
				int iId = c.getColumnIndex(TripLegMetaData.TableMetaData._ID);
				int iType = c.getColumnIndex(TripLegMetaData.TableMetaData.TYPE);
				int iLat = c.getColumnIndex(AddressGPSMetaData.TableMetaData.LATITUDE_Y);
				int iLng = c.getColumnIndex(AddressGPSMetaData.TableMetaData.LONGITUDE_X);
				int iAddress = c.getColumnIndex(AddressGPSMetaData.TableMetaData.ADDRESS);
				do
				{
					int latd = c.getInt(iLat);
					int lngd = c.getInt(iLng);
					String address = c.getString(iAddress);
					if (latd != 0 && lngd != 0)
					{
						Log.d(tag, address);
						String typeOfTransport = c.getString(iType);
						int radius = GeofenceHelper.getRadius(typeOfTransport);
						double lat = (double) latd / 1000000d;
						double lng = (double) lngd / 1000000d;
						int id = c.getInt(iId);
						String geofenceId = GeofenceHelper.LEG_ID + GeofenceHelper.DELIMITER + id;
						int transition = Geofence.GEOFENCE_TRANSITION_ENTER; // | // Geofence.GEOFENCE_TRANSITION_EXIT
						
						geofences.add( new GeofenceMy(tripId, geofenceId, radius, transition, lat, lng));
						Log.d(tag, "id: " + geofenceId);
					} else
					{
						Log.e(tag, "not valid address");
					}

				} while (c.moveToNext());
			}

		} finally
		{

			if (c != null && !c.isClosed())
			{
				c.close();
			}

		}
		return geofences;
	}
	
	private void saveGeofences(List<GeofenceMy> geofences)
	{
		ContentResolver cr = getContentResolver();
		android.location.Location previous = null;
		for(GeofenceMy geo : geofences)
		{
			GeofenceHelper.saveGeofence(geo, mDbOperations, cr, previous);
			previous = new android.location.Location("");
			previous.setLatitude(geo.lat);
			previous.setLatitude(geo.lng);
		}
	}
}
