package com.miracleas.minrute;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationClient.OnAddGeofencesResultListener;
import com.google.android.gms.location.LocationClient.OnRemoveGeofencesResultListener;
import com.google.android.gms.location.LocationStatusCodes;
import com.miracleas.minrute.provider.GeofenceMetaData;
import com.miracleas.minrute.service.ReceiveTransitionsIntentService;
import com.miracleas.minrute.service.RemoveGeofencesService;

public abstract class GeofenceActivity extends GoogleServiceActivity implements GooglePlayServicesClient.ConnectionCallbacks, OnAddGeofencesResultListener, OnRemoveGeofencesResultListener
{
	public static final String tag = GeofenceActivity.class.getName();
	public int CONNECTION_FAILURE_RESOLUTION_REQUEST = 251;
	// Holds the location client
	private LocationClient mLocationClient;
	// Stores the PendingIntent used to request geofence monitoring
	private PendingIntent mGeofenceRequestIntent;
	private SaveGeofences mSaveGeofences = null;
	// Defines the allowable request types.
	// Enum type for controlling the type of removal requested
	public enum REQUEST_TYPE
	{
		ADD, REMOVE_INTENT, REMOVE_LIST
	};

	protected REQUEST_TYPE mRequestType;
	// Flag that indicates if a request is underway.
	private boolean mInProgress;
	private List<Geofence> mCurrentGeofences;
	// Store the list of geofence Ids to remove
	private List<String> mGeofencesToRemove;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		// Start with the request flag set to false
		mInProgress = false;
		mCurrentGeofences = new ArrayList<Geofence>();

	}

	/*
	 * Provide the implementation of ConnectionCallbacks.onConnected() Once the
	 * connection is available, send a request to add the Geofences
	 */
	@Override
	public void onConnected(Bundle dataBundle)
	{
		switch (mRequestType)
		{
		case ADD:
			// Get the PendingIntent for the request
			mGeofenceRequestIntent = getTransitionPendingIntent();
			// Send a request to add the current geofences
			if (mCurrentGeofences != null && !mCurrentGeofences.isEmpty())
			{
				mLocationClient.addGeofences(mCurrentGeofences, mGeofenceRequestIntent, this);
			}
			break;
		case REMOVE_INTENT:
			mGeofenceRequestIntent = getRemoveGeofencesPendingIntent();
			mLocationClient.removeGeofences(mGeofenceRequestIntent, this);
			break;
		// If removeGeofencesById was called
		case REMOVE_LIST:
			mLocationClient.removeGeofences(mGeofencesToRemove, this);
			break;

		}
	}

	/**
	 * Create a PendingIntent that triggers an IntentService in your app when a
	 * geofence transition occurs.
	 * 
	 * @return
	 */
	private PendingIntent getTransitionPendingIntent()
	{
		// Create an explicit Intent
		Intent intent = new Intent(this, ReceiveTransitionsIntentService.class);
		/*
		 * Return the PendingIntent
		 */
		return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	/*
	 * Create a PendingIntent
	 */
	private PendingIntent getRemoveGeofencesPendingIntent()
	{
		// Create an explicit Intent
		Intent intent = new Intent(this, RemoveGeofencesService.class);
		/*
		 * Return the PendingIntent
		 */
		return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	/**
	 * Start a request for geofence monitoring by calling
	 * LocationClient.connect().
	 */
	public void addGeofences(List<Geofence> geofences)
	{
		mCurrentGeofences = geofences;
		// Start a request to add geofences
		mRequestType = REQUEST_TYPE.ADD;
		/*
		 * Test for Google Play services after setting the request type. If
		 * Google Play services isn't present, the proper request can be
		 * restarted.
		 */
		if (!servicesConnected())
		{
			return;
		}
		/*
		 * Create a new location client object. Since the current activity class
		 * implements ConnectionCallbacks and OnConnectionFailedListener, pass
		 * the current activity object as the listener for both parameters
		 */
		mLocationClient = new LocationClient(this, this, this);
		// If a request is not already underway
		if (!mInProgress)
		{
			// Indicate that a request is underway
			mInProgress = true;
			// Request a connection from the client to Location Services
			mLocationClient.connect();
		} else
		{
			/*
			 * A request is already underway. You can handle this situation by
			 * disconnecting the client, re-setting the flag, and then re-trying
			 * the request.
			 */
		}
	}

	/**
	 * Start a request to remove geofences by calling LocationClient.connect()
	 */
	public void removeAllGeofences()
	{
		// Record the type of removal request
		mRequestType = REQUEST_TYPE.REMOVE_INTENT;
		/*
		 * Test for Google Play services after setting the request type. If
		 * Google Play services isn't present, the request can be restarted.
		 */
		if (!servicesConnected())
		{
			return;
		}

		/*
		 * Create a new location client object. Since the current activity class
		 * implements ConnectionCallbacks and OnConnectionFailedListener, pass
		 * the current activity object as the listener for both parameters
		 */
		mLocationClient = new LocationClient(this, this, this);
		// If a request is not already underway
		if (!mInProgress)
		{
			// Indicate that a request is underway
			mInProgress = true;
			// Request a connection from the client to Location Services
			mLocationClient.connect();
		} else
		{
			/*
			 * A request is already underway. You can handle this situation by
			 * disconnecting the client, re-setting the flag, and then re-trying
			 * the request.
			 */
		}
	}

	/**
	 * Start a request to remove monitoring by calling LocationClient.connect()
	 * 
	 */
	private void removeGeofences(List<String> geofenceIds)
	{
		Log.d(tag, "removeGeofences request");
		// If Google Play services is unavailable, exit
		// Record the type of removal request
		mRequestType = REQUEST_TYPE.REMOVE_LIST;
		/*
		 * Test for Google Play services after setting the request type. If
		 * Google Play services isn't present, the request can be restarted.
		 */
		if (!servicesConnected())
		{
			return;
		}
		// Store the list of geofences to remove
		mGeofencesToRemove = geofenceIds;
		/*
		 * Create a new location client object. Since the current activity class
		 * implements ConnectionCallbacks and OnConnectionFailedListener, pass
		 * the current activity object as the listener for both parameters
		 */
		mLocationClient = new LocationClient(this, this, this);
		// If a request is not already underway
		if (!mInProgress)
		{
			// Indicate that a request is underway
			mInProgress = true;
			// Request a connection from the client to Location Services
			mLocationClient.connect();
		} else
		{
			/*
			 * A request is already underway. You can handle this situation by
			 * disconnecting the client, re-setting the flag, and then re-trying
			 * the request.
			 */
		}
	}

	/*
	 * Provide the implementation of
	 * OnAddGeofencesResultListener.onAddGeofencesResult. Handle the result of
	 * adding the geofences
	 */
	@Override
	public void onAddGeofencesResult(int statusCode, String[] geofenceRequestIds)
	{
		// If adding the geofences was successful
		if (LocationStatusCodes.SUCCESS == statusCode)
		{
			/*
			 * Handle successful addition of geofences here. You can send out a
			 * broadcast intent or update the UI. geofences into the Intent's
			 * extended data.
			 */
			saveGeofences(true);
			Log.d(tag, "added geofences!");
		} else
		{
			// If adding the geofences failed
			/*
			 * Report errors here. You can log the error using Log.e() or update
			 * the UI.
			 */
			Log.e(tag, "adding the geofences failed");
			Toast.makeText(this, "adding the geofences failed", Toast.LENGTH_SHORT).show();
		}
		// Turn off the in progress flag and disconnect the client
		mInProgress = false;
		mLocationClient.disconnect();
	}

	/*
	 * Implement ConnectionCallbacks.onDisconnected() Called by Location
	 * Services once the location client is disconnected.
	 */
	@Override
	public void onDisconnected()
	{
		// Turn off the request flag
		mInProgress = false;
		// Destroy the current location client
		mLocationClient = null;
	}

	// Implementation of OnConnectionFailedListener.onConnectionFailed
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult)
	{
		// Turn off the request flag
		mInProgress = false;
		/*
		 * If the error has a resolution, start a Google Play services activity
		 * to resolve it.
		 */
		if (connectionResult.hasResolution())
		{
			try
			{
				connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
			} catch (SendIntentException e)
			{
				// Log the error
				e.printStackTrace();
			}
			// If no resolution is available, display an error dialog
		} else
		{
			// Get the error code
			int errorCode = connectionResult.getErrorCode();
			// Get the error dialog from Google Play services
			Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
			// If Google Play services can provide an error dialog
			if (errorDialog != null)
			{
				// Create a new DialogFragment for the error dialog
				ErrorDialogFragment errorFragment = new ErrorDialogFragment();
				// Set the dialog in the DialogFragment
				errorFragment.setDialog(errorDialog);
				// Show the error dialog in the DialogFragment
				errorFragment.show(getSupportFragmentManager(), "Geofence Detection");
			}
		}
	}

	/**
	 * When the request to remove geofences by PendingIntent returns, handle the
	 * result.
	 * 
	 * @param statusCode
	 *            the code returned by Location Services
	 * @param requestIntent
	 *            The Intent used to request the removal.
	 */
	@Override
	public void onRemoveGeofencesByPendingIntentResult(int statusCode, PendingIntent requestIntent)
	{
		// If removing the geofences was successful
		if (statusCode == LocationStatusCodes.SUCCESS)
		{
			/*
			 * Handle successful removal of geofences here. You can send out a
			 * broadcast intent or update the UI. geofences into the Intent's
			 * extended data.
			 */
			Log.d(tag, "removed geofences!");
			saveGeofences(false);
		} else
		{
			// If adding the geocodes failed
			/*
			 * Report errors here. You can log the error using Log.e() or update
			 * the UI.
			 */
		}
		/*
		 * Disconnect the location client regardless of the request status, and
		 * indicate that a request is no longer in progress
		 */
		mInProgress = false;
		mLocationClient.disconnect();
	}

	@Override
	public void onRemoveGeofencesByRequestIdsResult(int statusCode, String[] geofenceRequestIds)
	{
		if (statusCode == LocationStatusCodes.SUCCESS)
		{
			Log.d(tag, "onRemoveGeofencesByRequestIdsResult! removed: "+geofenceRequestIds.length);
		}
		else
		{
			Log.e(tag, "onRemoveGeofencesByRequestIdsResult! failed");
		}
	}

	private void saveGeofences(boolean save)
	{
		if (mCurrentGeofences != null && !mCurrentGeofences.isEmpty() && (mSaveGeofences == null || mSaveGeofences.getStatus() == AsyncTask.Status.FINISHED))
		{
			mSaveGeofences = new SaveGeofences();
			mSaveGeofences.execute(save, null, null);
		}
	}

	private class SaveGeofences extends AsyncTask<Boolean, Void, Void>
	{
		@Override
		protected Void doInBackground(Boolean... params)
		{
			ContentResolver cr = getContentResolver();
			if (params[0])
			{
				for (Geofence g : mCurrentGeofences)
				{
					ContentValues values = new ContentValues();
					values.put(GeofenceMetaData.TableMetaData.geofence_id, g.getRequestId());
					cr.insert(GeofenceMetaData.TableMetaData.CONTENT_URI, values);
				}
			} 
			else
			{
				cr.delete(GeofenceMetaData.TableMetaData.CONTENT_URI, null, null);		
			}
			return null;
		}

		protected void onPostExecute(Void result)
		{

		}
	}
	
	public void removeSavedGeofences()
	{
		LoadGeofences();
	}
	
	private void LoadGeofences()
	{
		if ((mLoadGeofences == null || mLoadGeofences.getStatus() == AsyncTask.Status.FINISHED))
		{
			mLoadGeofences = new LoadGeofences();
			mLoadGeofences.execute(null, null, null);
		}
	}
	private LoadGeofences mLoadGeofences = null;
	
	private class LoadGeofences extends AsyncTask<Boolean, Void, List<String>>
	{
		@Override
		protected List<String> doInBackground(Boolean... params)
		{
			Cursor c = null;
			List<String> geofenceIds = new ArrayList<String>();
			try
			{
				c = getContentResolver().query(GeofenceMetaData.TableMetaData.CONTENT_URI, null, null, null, null);
				if(c.moveToFirst())
				{
					int i = c.getColumnIndex(GeofenceMetaData.TableMetaData.geofence_id);
					do{
						geofenceIds.add(c.getString(i));
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
			return geofenceIds;
		}
		@Override
		protected void onPostExecute(List<String> geofenceIds)
		{
			if(!geofenceIds.isEmpty())
			{
				removeGeofences(geofenceIds);					
			}
		}
	}

}
