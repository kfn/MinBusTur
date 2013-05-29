package com.miracleas.minrute.service;

import java.util.ArrayList;
import java.util.List;

import com.miracleas.minrute.model.TripLeg;
import com.miracleas.minrute.net.JourneyDetailFetcher;
import com.miracleas.minrute.provider.TripLegMetaData;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
/**
 * if user wants full support, we need to download all details for each tripleg
 * @author kfn
 *
 */
public class FetchAllTripsStopLocationsService extends IntentService
{
	public static final String tag = FetchAllTripsStopLocationsService.class.getName();
	
	public static final String TRIP_ID = "tripId";
	private ContentResolver mCr = null;
	private ArrayList<ContentProviderOperation> mDbOperations;
	
	private static final String[] PROJECTION_LEGS = {
		TripLegMetaData.TableMetaData._ID,
		TripLegMetaData.TableMetaData.ORIGIN_NAME,
		TripLegMetaData.TableMetaData.DEST_NAME,
		TripLegMetaData.TableMetaData.REF
		};
	
	public FetchAllTripsStopLocationsService()
	{
		super(FetchAllTripsStopLocationsService.class.getName());
	}
	public FetchAllTripsStopLocationsService(String name)
	{
		super(name);
	}
	@Override
	public void onCreate()
	{
		super.onCreate();
		mCr = getContentResolver();
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		String tripId = intent.getStringExtra(TRIP_ID);
		getAllJourneyDetails(tripId, intent);
		findStopBeforeDestinationTripLeg();				
	}
	/**
	 * saves all the trip legs stop that appear before destination
	 */
	private void findStopBeforeDestinationTripLeg()
	{
		
	}
	
	private void getAllJourneyDetails(String tripId, Intent intent)
	{
		if(!TextUtils.isEmpty(tripId))
		{
			mDbOperations = new ArrayList<ContentProviderOperation>();
			List<TripLeg> legs = fetchLegs(tripId);
			for(TripLeg leg : legs)
			{
				JourneyDetailFetcher f = new JourneyDetailFetcher(this, intent, leg.ref, leg.tripId, leg.id+"", leg.originName, leg.destName);
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
				do{
					TripLeg leg = new TripLeg();
					leg.ref = c.getString(iRef);
					leg.originName = c.getString(iOriginName);
					leg.destName = c.getString(iDestName);
					leg.id = c.getInt(iId);
					leg.tripId = tripId;
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
}
