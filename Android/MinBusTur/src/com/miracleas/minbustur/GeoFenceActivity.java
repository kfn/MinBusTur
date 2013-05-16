package com.miracleas.minbustur;

import java.util.ArrayList;
import java.util.List;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.android.gms.location.Geofence;
import com.miracleas.minbustur.model.SimpleGeofence;
import com.miracleas.minbustur.model.SimpleGeofenceStore;
import com.miracleas.minbustur.service.ReceiveTransitionsIntentService;

public class GeoFenceActivity extends SherlockFragmentActivity
{
	/*
	 * Use to set an expiration time for a geofence. After this amount of time
	 * Location Services will stop tracking the geofence.
	 */
	private static final long SECONDS_PER_HOUR = 60;
	private static final long MILLISECONDS_PER_SECOND = 1000;
	private static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;
	private static final long GEOFENCE_EXPIRATION_TIME = GEOFENCE_EXPIRATION_IN_HOURS * SECONDS_PER_HOUR * MILLISECONDS_PER_SECOND;

	/*
	 * Handles to UI views containing geofence data
	 */
	// Handle to geofence 1 latitude in the UI
	private EditText mLatitude1;
	// Handle to geofence 1 longitude in the UI
	private EditText mLongitude1;
	// Handle to geofence 1 radius in the UI
	private EditText mRadius1;
	// Handle to geofence 2 latitude in the UI
	private EditText mLatitude2;
	// Handle to geofence 2 longitude in the UI
	private EditText mLongitude2;
	// Handle to geofence 2 radius in the UI
	private EditText mRadius2;
	/*
	 * Internal geofence objects for geofence 1 and 2
	 */
	private SimpleGeofence mUIGeofence1;
	private SimpleGeofence mUIGeofence2;

	// Internal List of Geofence objects
	List<Geofence> mGeofenceList;
	//current List of geofences
	List<Geofence> mCurrentGeofences;
	// Persistent storage for geofences
	private SimpleGeofenceStore mGeofenceStorage;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Instantiate a new geofence storage area
		mGeofenceStorage = new SimpleGeofenceStore(this);

		// Instantiate the current List of geofences
		mCurrentGeofences = new ArrayList<Geofence>();
	}

	/**
	 * Get the geofence parameters for each geofence from the UI and add them to
	 * a List.
	 */
	public void createGeofences()
	{
		/*
		 * Create an internal object to store the data. Set its ID to "1". This
		 * is a "flattened" object that contains a set of strings
		 */
		mUIGeofence1 = new SimpleGeofence("1", Double.valueOf(mLatitude1.getText().toString()), Double.valueOf(mLongitude1.getText().toString()), Float.valueOf(mRadius1.getText().toString()), GEOFENCE_EXPIRATION_TIME,
		// This geofence records only entry transitions
				Geofence.GEOFENCE_TRANSITION_ENTER);
		// Store this flat version
		mGeofenceStorage.setGeofence("1", mUIGeofence1);
		// Create another internal object. Set its ID to "2"
		mUIGeofence2 = new SimpleGeofence("2", Double.valueOf(mLatitude2.getText().toString()), Double.valueOf(mLongitude2.getText().toString()), Float.valueOf(mRadius2.getText().toString()), GEOFENCE_EXPIRATION_TIME,
		// This geofence records both entry and exit transitions
				Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT);
		// Store this flat version
		mGeofenceStorage.setGeofence("2", mUIGeofence2);
		mGeofenceList.add(mUIGeofence1.toGeofence());
		mGeofenceList.add(mUIGeofence2.toGeofence());
	}
	
	/*
     * Create a PendingIntent that triggers an IntentService in your
     * app when a geofence transition occurs.
     */
    private PendingIntent getTransitionPendingIntent() {
        // Create an explicit Intent
        Intent intent = new Intent(this,
                ReceiveTransitionsIntentService.class);
        /*
         * Return the PendingIntent
         */
        return PendingIntent.getService(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }


}
