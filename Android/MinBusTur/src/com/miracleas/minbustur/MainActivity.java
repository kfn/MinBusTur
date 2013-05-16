package com.miracleas.minbustur;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationClient.OnAddGeofencesResultListener;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationStatusCodes;
import com.miracleas.minbustur.model.SimpleGeofence;
import com.miracleas.minbustur.model.SimpleGeofenceStore;
import com.miracleas.minbustur.service.ReceiveTransitionsIntentService;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends SherlockFragmentActivity implements ActionBar.TabListener, GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationListener, OnAddGeofencesResultListener
{
	/**
	 * Note: If your app accesses the network or does other long-running work
	 * after receiving a location update, adjust the fastest interval to a
	 * slower value. This prevents your app from receiving updates it can't use.
	 * Once the long-running work is done, set the fastest interval back to a
	 * fast value.
	 */
	// Milliseconds per second
	private static final int MILLISECONDS_PER_SECOND = 1000;
	// Update frequency in seconds
	public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
	// Update frequency in milliseconds
	private static final long UPDATE_INTERVAL = MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
	// The fastest update frequency, in seconds
	private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
	// A fast frequency ceiling in milliseconds
	private static final long FASTEST_INTERVAL = MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;

	// Define an object that holds accuracy and frequency parameters
	LocationRequest mLocationRequest;

	private SectionsPagerAdapter mSectionsPagerAdapter;
	private LocationClient mLocationClient = null;
	private Location mCurrentLocation;
	private boolean mUpdatesRequested;
	private SharedPreferences mPrefs;
	private SharedPreferences.Editor mEditor;
	private TextView mTextViewAddress = null;
	private GetAddressTask mGetAddressTask = null;

	// Stores the PendingIntent used to request geofence monitoring
	private PendingIntent mGeofenceRequestIntent;
	private PendingIntent mTransitionPendingIntent;

	// Defines the allowable request types.
	public enum REQUEST_TYPE
	{
		ADD
	};

	private REQUEST_TYPE mRequestType;
	// Flag that indicates if a request is underway.
	private boolean mInProgress;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	private ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Set up the action bar.
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener()
		{
			@Override
			public void onPageSelected(int position)
			{
				actionBar.setSelectedNavigationItem(position);
			}
		});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++)
		{
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
			actionBar.addTab(actionBar.newTab().setText(mSectionsPagerAdapter.getPageTitle(i)).setTabListener(this));
		}
		mTextViewAddress = (TextView) findViewById(R.id.textViewAddress);
		/*
		 * Create a new location client, using the enclosing class to handle
		 * callbacks.
		 */
		mLocationClient = new LocationClient(this, this, this);
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
		mInProgress = false;

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onPause()
	{
		// Save the current setting for updates
		mEditor.putBoolean("KEY_UPDATES_ON", mUpdatesRequested);
		mEditor.commit();
		super.onPause();
	}

	@Override
	protected void onResume()
	{

		/*
		 * Get any previous setting for location updates Gets "false" if an
		 * error occurs
		 */
		if (mPrefs.contains("KEY_UPDATES_ON"))
		{
			mUpdatesRequested = mPrefs.getBoolean("KEY_UPDATES_ON", false);

			// Otherwise, turn off location updates
		} else
		{
			mEditor.putBoolean("KEY_UPDATES_ON", false);
			mEditor.commit();
		}
		super.onResume();
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		if (servicesConnected())
		{
			// Connect the client.
			mLocationClient.connect();
		}

	}

	/*
	 * Called when the Activity is no longer visible at all. Stop updates and
	 * disconnect.
	 */
	@Override
	protected void onStop()
	{
		// If the client is connected
		if (mLocationClient.isConnected())
		{
			stopPeriodicUpdates();
		}
		/*
		 * After disconnect() is called, the client is considered "dead".
		 */
		mLocationClient.disconnect();
		super.onStop();
	}

	private void stopPeriodicUpdates()
	{

	}

	/*
	 * Handle results returned to the FragmentActivity by Google Play services
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		// Decide what to do based on the original request code
		switch (requestCode)
		{

		case CONNECTION_FAILURE_RESOLUTION_REQUEST:
			/*
			 * If the result code is Activity.RESULT_OK, try to connect again
			 */
			switch (resultCode)
			{
			case Activity.RESULT_OK:
				/*
				 * Try the request again
				 */

				break;
			}

		}
	}

	private boolean servicesConnected()
	{
		// Check that Google Play services is available
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode)
		{
			// In debug mode, log the status
			Log.d("Location Updates", "Google Play services is available.");
			// Continue
			return true;
			// Google Play services was not available for some reason
		} else
		{
			showErrorDialog(resultCode);
			return false;
		}
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

	@Override
	public void onConnected(Bundle dataBundle)
	{
		// Display the connection status
		Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
		mCurrentLocation = mLocationClient.getLastLocation();

		switch (mRequestType)
		{
		case ADD:
			// Get the PendingIntent for the request
			mTransitionPendingIntent = getTransitionPendingIntent();
			// Send a request to add the current geofences
			mLocationClient.addGeofences(mCurrentGeofences, mTransitionPendingIntent, this);

		}

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
		// Display the connection status
		Toast.makeText(this, "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
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
		} else
		{
			// If adding the geofences failed
			/*
			 * Report errors here. You can log the error using Log.e() or update
			 * the UI.
			 */
		}
		// Turn off the in progress flag and disconnect the client
		mInProgress = false;
		// mLocationClient.disconnect();
	}

	@SuppressLint("NewApi")
	@Override
	public void onLocationChanged(Location location)
	{
		// Report to the UI that the location was updated
		String msg = "Updated Location: " + Double.toString(location.getLatitude()) + "," + Double.toString(location.getLongitude());
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

		// Ensure that a Geocoder services is available
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD && Geocoder.isPresent())
		{
			if (mGetAddressTask == null || mGetAddressTask.getStatus() == AsyncTask.Status.FINISHED)
			{
				/*
				 * Reverse geocoding is long-running and synchronous. Run it on
				 * a background thread. Pass the current location to the
				 * background task. When the task finishes, onPostExecute()
				 * displays the address.
				 */
				mGetAddressTask = new GetAddressTask(this);
				mGetAddressTask.execute(location);
			}

		}

	}

	public void onClick(View v)
	{
		if (v.getId() == R.id.btnStart)
		{
			mLocationClient.requestLocationUpdates(mLocationRequest, this);
		} else if (v.getId() == R.id.btnStop)
		{
			mLocationClient.removeLocationUpdates(this);
		}
	}

	private void showErrorDialog(int errorCode)
	{
		// Get the error code

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
			errorFragment.show(getSupportFragmentManager(), "Location Updates");
		}
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction)
	{
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction)
	{
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction)
	{
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter
	{

		public SectionsPagerAdapter(FragmentManager fm)
		{
			super(fm);
		}

		@Override
		public Fragment getItem(int position)
		{
			// getItem is called to instantiate the fragment for the given page.
			// Return a DummySectionFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			Fragment fragment = new DummySectionFragment();
			Bundle args = new Bundle();
			args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position + 1);
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public int getCount()
		{
			// Show 3 total pages.
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position)
		{
			Locale l = Locale.getDefault();
			switch (position)
			{
			case 0:
				return getString(R.string.title_section1).toUpperCase(l);
			case 1:
				return getString(R.string.title_section2).toUpperCase(l);
			case 2:
				return getString(R.string.title_section3).toUpperCase(l);
			}
			return null;
		}
	}

	/**
	 * A dummy fragment representing a section of the app, but that simply
	 * displays dummy text.
	 */
	public static class DummySectionFragment extends Fragment
	{
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		public static final String ARG_SECTION_NUMBER = "section_number";

		public DummySectionFragment()
		{
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			View rootView = inflater.inflate(R.layout.fragment_main_dummy, container, false);
			TextView dummyTextView = (TextView) rootView.findViewById(R.id.section_label);
			dummyTextView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
			return rootView;
		}
	}

	// Global constants
	/*
	 * Define a request code to send to Google Play services This code is
	 * returned in Activity.onActivityResult
	 */
	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

	// Define a DialogFragment that displays the error dialog
	public static class ErrorDialogFragment extends DialogFragment
	{
		// Global field to contain the error dialog
		private Dialog mDialog;

		// Default constructor. Sets the dialog field to null
		public ErrorDialogFragment()
		{
			super();
			mDialog = null;
		}

		// Set the dialog to display
		public void setDialog(Dialog dialog)
		{
			mDialog = dialog;
		}

		// Return a Dialog to the DialogFragment.
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState)
		{
			return mDialog;
		}
	}

	/**
	 * A subclass of AsyncTask that calls getFromLocation() in the background.
	 * The class definition has these generic types: Location - A Location
	 * object containing the current location. Void - indicates that progress
	 * units are not used String - An address passed to onPostExecute()
	 */
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
				String addressText = String.format("%s, %s, %s",
				// If there's a street address, add it
						address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) : "",
						// Locality is usually a city
						address.getLocality(),
						// The country of the address
						address.getCountryName());
				// Return the text
				return addressText;
			} else
			{
				return "No address found";
			}
		}

		@Override
		protected void onPostExecute(String address)
		{

			mTextViewAddress.setText(address);

		}

	}

	/*
	 * Use to set an expiration time for a geofence. After this amount of time
	 * Location Services will stop tracking the geofence.
	 */
	private static final long SECONDS_PER_HOUR = 60;
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
	// current List of geofences
	List<Geofence> mCurrentGeofences;
	// Persistent storage for geofences
	private SimpleGeofenceStore mGeofenceStorage;

	/**
	 * Start a request for geofence monitoring by calling
	 * LocationClient.connect().
	 */
	public void addGeofences()
	{
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
	 * Create a PendingIntent that triggers an IntentService in your app when a
	 * geofence transition occurs.
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

}
