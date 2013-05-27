package com.miracleas.minrute;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.miracleas.minrute.service.LocationService;
import com.miracleas.minrute.service.UpdateVoiceTripService;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;


public abstract class MinRuteBaseActivity extends SherlockFragmentActivity
{
	protected UpdateVoiceTripService mServiceVoice = null;
	protected boolean mBoundVoice = false;
	
	protected LocationService mServiceLocation = null;
	protected boolean mBoundLocation = false;
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		connectToVoiceService();
		connectToLocationService();
	}
	

	private void connectToVoiceService()
	{
		Intent intent = new Intent(this, UpdateVoiceTripService.class);
		bindService(intent, mConnectionVoice, Context.BIND_AUTO_CREATE);
	}
	private void connectToLocationService()
	{
		Intent intent = new Intent(this, LocationService.class);
		bindService(intent, mConnectionLocation, Context.BIND_AUTO_CREATE);
	}
	
	private ServiceConnection mConnectionLocation = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
			mServiceLocation = binder.getService();
			mBoundLocation = true;
			onConnectedServiceLocation();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0)
		{
			mBoundLocation = false;
		}
	};
	
	
	private ServiceConnection mConnectionVoice = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			UpdateVoiceTripService.LocalBinder binder = (UpdateVoiceTripService.LocalBinder) service;
			mServiceVoice = binder.getService();
			mBoundVoice = true;
			onConnectedServiceVoice();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0)
		{
			mBoundVoice = false;
		}
	};
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		if (mBoundVoice)
		{
			unbindService(mConnectionVoice);
			mBoundVoice = false;
		}
		if (mBoundLocation)
		{
			unbindService(mConnectionLocation);
			mBoundLocation = false;
		}
	}
	
	public abstract void onConnectedServiceVoice();
	public abstract void onConnectedServiceLocation();
	
}
