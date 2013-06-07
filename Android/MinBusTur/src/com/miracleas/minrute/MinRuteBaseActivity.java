package com.miracleas.minrute;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.miracleas.minrute.net.BaseFetcher;
import com.miracleas.minrute.service.LocationService;
import com.miracleas.minrute.service.VoiceTripService;
import com.miracleas.minrute.utils.App;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.widget.Toast;


public abstract class MinRuteBaseActivity extends SherlockFragmentActivity implements ConfirmDialogFragment.Callbacks
{
	protected VoiceTripService mServiceVoice = null;
	protected boolean mBoundVoice = false;
	protected boolean mSupportVoice = false;
	
	protected LocationService mServiceLocation = null;
	protected boolean mBoundLocation = false;
	
	public static final int INSTALL_VOICE_SUPPORT = 9922;
	
	
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
		Intent intent = new Intent(this, VoiceTripService.class);
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
			VoiceTripService.LocalBinder binder = (VoiceTripService.LocalBinder) service;
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
		if (requestCode == MY_DATA_CHECK_CODE)
		{
			if (App.SUPPORTS_JELLY_BEAN || resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS)
			{
				mSupportVoice = true;
				onVoiceSupportCheckFinished(true);
				
			} else
			{
				showDialogInstallVoiceSupport();
			}
		}
		else if(requestCode == INSTALL_VOICE_SUPPORT)
		{
			if(resultCode == Activity.RESULT_OK)
			{
				Toast.makeText(this, R.string.installed_voice, Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	public void onStart()
	{
		super.onStart();
		registerBroadcast();
	}
	
	public void onStop()
	{
		super.onStop();
		unregisterBroadcast();
	}
	
	private void registerBroadcast()
	{
		LocalBroadcastManager r = LocalBroadcastManager.getInstance(this);
		IntentFilter filter = new IntentFilter();
		filter.addAction(BaseFetcher.BROADCAST_SERVER_RESPONSE_ACTION);
		r.registerReceiver(mServerReceiver, filter);
	}
	
	private void unregisterBroadcast()
	{
		LocalBroadcastManager r = LocalBroadcastManager.getInstance(this);
		r.unregisterReceiver(mServerReceiver);
	}
	
	private BroadcastReceiver mServerReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();
			if(action.equals(BaseFetcher.BROADCAST_SERVER_RESPONSE_ACTION))
			{
				String msg = intent.getStringExtra(BaseFetcher.BROADCAST_MSG);
				boolean success = intent.getBooleanExtra(BaseFetcher.BROADCAST_MSG_SUCCESS_BOOLEAN, false);
				onServerResponse(success);
				if(!TextUtils.isEmpty(msg))
				{
					Toast.makeText(MinRuteBaseActivity.this,msg, Toast.LENGTH_SHORT).show();
				}
			}
			
		}};
	protected abstract void onServerResponse(boolean success);
	/**
	 * default implementation does nothing.
	 */
	protected void showDialogInstallVoiceSupport()
	{
		
	}
	/**
	 * redirects user to Google Play Voice support download
	 */
	protected void showDialogInstallVoiceSupportHelper()
	{
		
		//Toast.makeText(this, R.string.voice_is_not_installed, Toast.LENGTH_SHORT).show();
		onVoiceSupportCheckFinished(false);
		// missing data, install it
		Intent installIntent = new Intent();
		installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
		startActivityForResult(installIntent, INSTALL_VOICE_SUPPORT);
	}
	
	@Override
    public void doPositiveClick()
    {
    }

    @Override
    public void doNegativeClick()
    {
    }
}
