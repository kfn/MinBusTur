package com.miracleas.minrute;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.miracleas.minrute.service.UpdateVoiceTripService;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;


public abstract class MinRuteBaseActivity extends SherlockFragmentActivity
{
	protected UpdateVoiceTripService mService = null;
	protected boolean mBound = false;
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		connectToService();
	}
	
	/**
	 * connects to the Android service that communicates with the container
	 */
	private void connectToService()
	{
		Intent intent = new Intent(this, UpdateVoiceTripService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}
	private ServiceConnection mConnection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			UpdateVoiceTripService.LocalBinder binder = (UpdateVoiceTripService.LocalBinder) service;
			mService = binder.getService();
			mBound = true;
			onConnectedService();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0)
		{
			mBound = false;
		}
	};
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		if (mBound)
		{
			unbindService(mConnection);
			mBound = false;
		}		
	}
	
	public abstract void onConnectedService();
	
}
