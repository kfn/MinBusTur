package com.miracleas.minrute.service;

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.miracleas.minrute.R;
import com.miracleas.minrute.provider.JourneyDetailNoteMetaData;
import com.miracleas.minrute.provider.JourneyDetailStopImagesMetaData;
import com.miracleas.minrute.provider.TripLegMetaData;
import com.miracleas.minrute.provider.GeofenceTransitionMetaData;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;

public class ReceiveTransitionsIntentService extends IntentService
{
	public static final String tag = ReceiveTransitionsIntentService.class.getName();

	public ReceiveTransitionsIntentService(String name)
	{
		super(name);
	}

	/**
	 * Sets an identifier for the service
	 */
	public ReceiveTransitionsIntentService()
	{
		super("ReceiveTransitionsIntentService");
	}

	/**
	 * Handles incoming intents
	 * 
	 * @param intent
	 *            The Intent sent by Location Services. This Intent is provided
	 *            to Location Services (inside a PendingIntent) when you call
	 *            addGeofences()
	 */
	@Override
	protected void onHandleIntent(Intent intent)
	{
		Log.d(tag, "onHandleIntent");
		// First check for errors
		if (LocationClient.hasError(intent))
		{
			// Get the error code with a static method
			int errorCode = LocationClient.getErrorCode(intent);
			// Log the error
			Log.e(tag, "Location Services error: " + Integer.toString(errorCode));
			/*
			 * You can also send the error code to an Activity or Fragment with
			 * a broadcast Intent
			 */
			/*
			 * If there's no error, get the transition type and the IDs of the
			 * geofence or geofences that triggered the transition
			 */
		} else
		{
			// Get the type of transition (entry or exit)
			int transitionType = LocationClient.getGeofenceTransition(intent);
			// Test that a valid transition was reported
			if ((transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) || (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT))
			{
				if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER)
				{
					Log.d(tag, "Enter");
					saveTransistion(transitionType, intent);
				} 
				else if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT)
				{
					Log.d(tag, "Exit");
				}			
			} else
			{
				// An invalid transition was reported
				Log.e(tag, "Geofence transition error: " + Integer.toString(transitionType));
			}

		}
	}
	
	private void saveTransistion(int transitionType, Intent intent)
	{
		List<Geofence> triggerList = LocationClient.getTriggeringGeofences(intent);
		ArrayList<ContentProviderOperation> mDbOperations = new ArrayList<ContentProviderOperation>(triggerList.size());
		long now = System.currentTimeMillis();
		for (int i = 0; i < triggerList.size(); i++)
		{
			Geofence geofence = triggerList.get(i);
			String legId = geofence.getRequestId();
			Log.d(tag, "legid: "+legId);
			Uri uri = Uri.withAppendedPath(TripLegMetaData.TableMetaData.CONTENT_URI, legId);
			ContentProviderOperation.Builder b = ContentProviderOperation.newUpdate(uri);	
			b.withValue(TripLegMetaData.TableMetaData.GEOFENCE_EVENT_ID, transitionType);
			b.withValue(TripLegMetaData.TableMetaData.updated, now);
			mDbOperations.add(b.build());
			
			b = ContentProviderOperation.newInsert(GeofenceTransitionMetaData.TableMetaData.CONTENT_URI);	
			b.withValue(GeofenceTransitionMetaData.TableMetaData.TRIP_LEG_ID, legId);
			b.withValue(GeofenceTransitionMetaData.TableMetaData.GEOFENCE_TRANSITION_TYPE, transitionType);
			b.withValue(GeofenceTransitionMetaData.TableMetaData.updated, System.currentTimeMillis());
			mDbOperations.add(b.build());
		}
		if(!mDbOperations.isEmpty())
		{					
			try
			{
				int count = getContentResolver().applyBatch(TripLegMetaData.AUTHORITY, mDbOperations).length;
				Log.d(tag, "applyBatch: "+count);
				Uri uri = Uri.withAppendedPath(TripLegMetaData.TableMetaData.CONTENT_URI, JourneyDetailStopImagesMetaData.TABLE_NAME);
				getContentResolver().notifyChange(uri, null);
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
}
