package com.miracleas.minrute.service;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.DateUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class LocationService extends Service implements LocationListener, GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener
{
	public static final String tag = LocationService.class.getName();
	private final IBinder mBinder = new LocalBinder();
	// LOCATION
	// Update frequency in seconds
	public static final int UPDATE_INTERVAL_IN_SECONDS = 15;
	// Update frequency in milliseconds
	private static final long UPDATE_INTERVAL = DateUtils.SECOND_IN_MILLIS * UPDATE_INTERVAL_IN_SECONDS;
	// The fastest update frequency, in seconds
	private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
	// A fast frequency ceiling in milliseconds
	private static final long FASTEST_INTERVAL = DateUtils.SECOND_IN_MILLIS * FASTEST_INTERVAL_IN_SECONDS;

	// Define an object that holds accuracy and frequency parameters
	private LocationRequest mLocationRequest;
	private LocationClient mLocationClient;
	private boolean mUpdatesRequested;
	private boolean mIsLocationStarted = false;

	private SharedPreferences mPrefs;
	private SharedPreferences.Editor mEditor;

	private OnNewLocationReceivedListener mOnNewLocationReceived = null;
	private GetAddressTask mGetAddressTask = null;

	@Override
	public void onCreate()
	{
		super.onCreate();

		// Create the LocationRequest object
		mLocationRequest = LocationRequest.create();
		// Use high accuracy
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		
		// Set the update interval to 5 seconds
		mLocationRequest.setInterval(UPDATE_INTERVAL);
		// Set the fastest update interval to 1 second
		mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

		// Open the shared preferences
		mPrefs = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
		// Get a SharedPreferences editor
		mEditor = mPrefs.edit();
		/*
		 * Create a new location client, using the enclosing class to handle
		 * callbacks.
		 */
		mLocationClient = new LocationClient(this, this, this);
		// Start with updates turned off
		mUpdatesRequested = false;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Log.i("LocalService", "Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	public void onDestroy()
	{
		super.onDestroy();
		mOnNewLocationReceived = null;
		stopLocationListening();
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		// TODO Auto-generated method stub
		return mBinder;
	}

	/**
	 * Class used for the client Binder. Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder
	{
		public LocationService getService()
		{
			// Return this instance of LocalService so clients can call public
			// methods
			return LocationService.this;
		}
	}
	
	public Location getLastLocation()
	{
		return mLocationClient.getLastLocation();
	}

	public void startLocationListening()
	{
        //mIsLocationStarted = true;
		if (!mIsLocationStarted)
		{
			mLocationClient.connect();
			mIsLocationStarted = true;
		}

	}

	public void stopLocationListening()
	{
		if (mIsLocationStarted)
		{
			// If the client is connected
			if (mLocationClient.isConnected())
			{

			}
			/*
			 * After disconnect() is called, the client is considered "dead".
			 */
			mLocationClient.disconnect();
			mIsLocationStarted = false;
		}

	}

	@Override
	public void onLocationChanged(Location loc)
	{
		if (loc != null)
		{
			//Log.d(tag, loc.getLatitude() + "," + loc.getLongitude());
			if (mOnNewLocationReceived != null)
			{
				mOnNewLocationReceived.onNewLocationReceived(loc);
			}
		}

	}

	@Override
	public void onConnected(Bundle args)
	{
		Log.d(tag, "onConnected");
		mLocationClient.requestLocationUpdates(mLocationRequest, this);
	}

	@Override
	public void onDisconnected()
	{
		Log.d(tag, "onDisconnected");

	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0)
	{
		Log.d(tag, "onConnectionFailed");

	}

	public OnNewLocationReceivedListener getOnNewLocationReceived()
	{
		return mOnNewLocationReceived;
	}

	public void setOnNewLocationReceived(OnNewLocationReceivedListener onNewLocationReceived)
	{
		this.mOnNewLocationReceived = onNewLocationReceived;
	}

	public static interface OnNewLocationReceivedListener
	{
		void onNewLocationReceived(Location loc);
		void onAddressGeocoded(String address);
	}

	public void geocode(Location location)
	{
		if(mGetAddressTask==null || mGetAddressTask.getStatus()==AsyncTask.Status.FINISHED)
		{
			mGetAddressTask = new GetAddressTask(this);
			mGetAddressTask.execute(location);
		}
	}

	
	private class GetAddressTask extends AsyncTask<Location, Void, String>
	{
		Context mContext;

		public GetAddressTask(Context context)
		{
			super();
			mContext = context;
		}

		/**
		 * Get a Geocoder instance, get the latitude and longitude look up the
		 * address, and return it
		 * 
		 * @params params One or more Location objects
		 * @return A string containing the address of the current location, or
		 *         an empty string if no address can be found, or an error
		 *         message
		 */
		@Override
		protected String doInBackground(Location... params)
		{
			Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
			// Get the current location from the input parameter list
			Location loc = params[0];
			// Create a list to contain the result address
			List<Address> addresses = null;
			try
			{
				/*
				 * Return 1 address.
				 */
				addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
			} catch (IOException e1)
			{
				Log.e("LocationSampleActivity", "IO Exception in getFromLocation()");
				e1.printStackTrace();
				return ("IO Exception trying to get address");
			} catch (IllegalArgumentException e2)
			{
				// Error message to post in the log
				String errorString = "Illegal arguments " + Double.toString(loc.getLatitude()) + " , " + Double.toString(loc.getLongitude()) + " passed to address service";
				Log.e("LocationSampleActivity", errorString);
				e2.printStackTrace();
				return errorString;
			}
			// If the reverse geocode returned an address
			if (addresses != null && addresses.size() > 0)
			{
				// Get the first address
				Address address = addresses.get(0);
				/*
				 * Format the first line of address (if available), city, and
				 * country name.
				 */
				String addressText = String.format("%s, %s %s",
				// If there's a street address, add it
						address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) : "",
						// Locality is usually a city
						address.getPostalCode(),
						// The country of the address
						address.getLocality());
				// Return the text
				return addressText;
			} else
			{
				return "No address found";
			}
			
		}
		
		public void onPostExecute(String result)
		{
			if(mOnNewLocationReceived!=null)
			{
				mOnNewLocationReceived.onAddressGeocoded(result);
			}
		}
	}

}
