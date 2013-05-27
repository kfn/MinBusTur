package com.miracleas.minrute.service;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.miracleas.minrute.service.UpdateVoiceTripService.LocalBinder;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.DateUtils;
import android.util.Log;

public class LocationService extends Service implements LocationListener, GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener
{
	public static final String tag = LocationService.class.getName();
	private final IBinder mBinder = new LocalBinder();
	// LOCATION
	// Update frequency in seconds
	public static final int UPDATE_INTERVAL_IN_SECONDS = 10;
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

	public void startLocationListening()
	{
		if(!mIsLocationStarted)
		{
			mLocationClient.connect();
			mIsLocationStarted = true;
		}
		
	}

	public void stopLocationListening()
	{
		if(mIsLocationStarted)
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
		if(loc!=null)
		{
			Log.d(tag, loc.getLatitude()+","+loc.getLongitude());
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

}
