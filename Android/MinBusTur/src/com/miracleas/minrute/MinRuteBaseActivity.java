package com.miracleas.minrute;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.miracleas.minrute.service.LocationService;
import com.miracleas.minrute.service.UpdateVoiceTripService;
import com.miracleas.minrute.utils.App;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;


public abstract class MinRuteBaseActivity extends SherlockFragmentActivity
{
	protected UpdateVoiceTripService mServiceVoice = null;
	protected boolean mBoundVoice = false;
	protected boolean mSupportVoice = false;
	
	protected LocationService mServiceLocation = null;
	protected boolean mBoundLocation = false;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		connectToLocationService();
		connectToVoiceService();
	}
	
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putBoolean("mSupportVoice", mSupportVoice);
	}
	
	public void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
		mSupportVoice = savedInstanceState.getBoolean("mSupportVoice");
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
	
	public void onConnectedServiceVoice()
	{
		startIfsupportsTextSpeach();
	}
	public abstract void onConnectedServiceLocation();
	
	private void onVoiceSupportCheckFinished(boolean voiceSupport){
		if(voiceSupport)
		{
			if(mBoundVoice)
			{
				mServiceVoice.startTextToSpeech();
			}
		}
		
	}
	
	private int MY_DATA_CHECK_CODE = 554;

	public void startIfsupportsTextSpeach()
	{
		Intent checkIntent = new Intent();
		checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (App.SUPPORTS_JELLY_BEAN || requestCode == MY_DATA_CHECK_CODE)
		{
			if (App.SUPPORTS_JELLY_BEAN || resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS)
			{
				mSupportVoice = true;
				onVoiceSupportCheckFinished(true);
				
			} else
			{
				onVoiceSupportCheckFinished(false);
				// missing data, install it
				Intent installIntent = new Intent();
				installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(installIntent);
			}
		}
	}
	
}
