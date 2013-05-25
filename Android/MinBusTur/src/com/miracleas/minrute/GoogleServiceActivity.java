package com.miracleas.minrute;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.miracleas.minrute.service.ReceiveTransitionsIntentService;

public class GoogleServiceActivity extends MinRuteBaseActivity implements OnConnectionFailedListener
{
	private static final String tag = GoogleServiceActivity.class.getName();
	// Global constants
	/*
	 * Define a request code to send to Google Play services This code is
	 * returned in Activity.onActivityResult
	 */
	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	
	@Override
	protected void onStart()
	{
		super.onStart();
		if (servicesConnected())
		{
			
		}

	}

	protected boolean servicesConnected()
	{
		// Check that Google Play services is available
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode)
		{
			// In debug mode, log the status
			Log.d(tag, "Google Play services is available.");
			// Continue
			return true;
			// Google Play services was not available for some reason
		} 
		else
		{
			Log.e(tag, "Google Play services is not available.");
			return false;
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult)
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
				servicesConnected();
				break;
			}

		}

	}
	
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

	@Override
	public void onConnectedService()
	{
		// TODO Auto-generated method stub
		
	}

}
