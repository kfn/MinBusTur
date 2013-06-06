package com.miracleas.minrute.service;

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.miracleas.minrute.R;
import com.miracleas.minrute.model.GeofenceHelper;
import com.miracleas.minrute.provider.GeofenceMetaData;
import com.miracleas.minrute.provider.TripLegDetailNoteMetaData;
import com.miracleas.minrute.provider.StopImagesMetaData;
import com.miracleas.minrute.provider.TripLegMetaData;
import com.miracleas.minrute.provider.GeofenceTransitionMetaData;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.text.TextUtils;
import android.text.format.DateUtils;
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
		
		List<String> validGeofences = getGeofencesFromGeofenceDb();
		
		for (int i = 0; i < triggerList.size(); i++)
		{
			Geofence geofence = triggerList.get(i);
			String geofenceId = geofence.getRequestId();
			Log.d(tag, "legid: "+geofenceId);
			
			if(validGeofences.contains(geofenceId))
			{
				String legId = null;
				if(geofenceId.contains(GeofenceHelper.LEG_ID_WITH_STOP_ID))
				{
					String[] temp = geofenceId.split(GeofenceHelper.DELIMITER);
					legId = temp[1];
					//temp[2] == stop id
				}
				else if(geofenceId.contains(GeofenceHelper.LEG_ID))
				{
					String[] temp = geofenceId.split(GeofenceHelper.DELIMITER);
					legId = temp[1];
				}
				
				
				if(!TextUtils.isEmpty(legId))
				{
					Uri uri = Uri.withAppendedPath(TripLegMetaData.TableMetaData.CONTENT_URI, legId);
					ContentProviderOperation.Builder b = ContentProviderOperation.newUpdate(uri);	
					b.withValue(TripLegMetaData.TableMetaData.GEOFENCE_EVENT_ID, transitionType);
					b.withValue(TripLegMetaData.TableMetaData.updated, now);
					mDbOperations.add(b.build());
					
					String selection = GeofenceMetaData.TableMetaData.geofence_id + "=?";
					String[] selectionArgs = {geofenceId};			
					b = ContentProviderOperation.newUpdate(GeofenceMetaData.TableMetaData.CONTENT_URI).withSelection(selection, selectionArgs);	
					b.withValue(GeofenceMetaData.TableMetaData.CURRENT_TRANSITION_STATE, transitionType);
					mDbOperations.add(b.build());
					
					b = ContentProviderOperation.newInsert(GeofenceTransitionMetaData.TableMetaData.CONTENT_URI);	
					b.withValue(GeofenceTransitionMetaData.TableMetaData.GEOFENCE_ID, geofenceId);
					b.withValue(GeofenceTransitionMetaData.TableMetaData.GEOFENCE_TRANSITION_TYPE, transitionType);
					b.withValue(GeofenceTransitionMetaData.TableMetaData.updated, now);
					mDbOperations.add(b.build());
				}
			}
			else
			{
				Log.e(tag, geofenceId+ " is not a valid geofence id");
			}
			
			
			
		}
		if(!mDbOperations.isEmpty())
		{					
			try
			{
				int count = getContentResolver().applyBatch(TripLegMetaData.AUTHORITY, mDbOperations).length;
				Log.d(tag, "applyBatch: "+count);
				Uri uri = Uri.withAppendedPath(TripLegMetaData.TableMetaData.CONTENT_URI, StopImagesMetaData.TABLE_NAME);
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
	
	private List<String> getGeofencesFromGeofenceDb()
	{
		String[] projection = {GeofenceMetaData.TableMetaData.geofence_id};
		
		List<String> geofences = new ArrayList<String>();
		ContentResolver cr = getContentResolver();
		Cursor c = null;
		try
		{
			c = cr.query(GeofenceMetaData.TableMetaData.CONTENT_URI, projection, null, null, null);
			if(c.moveToFirst())
			{
				int iId = c.getColumnIndex(GeofenceMetaData.TableMetaData.geofence_id);	
				do
				{
					String geofenceId = c.getString(iId);
					geofences.add(geofenceId);
				} while (c.moveToNext());
			}
		}
		finally
		{
			if(c!=null && !c.isClosed())
			{
				c.close();
			}
		}
		return geofences;
	}
}
