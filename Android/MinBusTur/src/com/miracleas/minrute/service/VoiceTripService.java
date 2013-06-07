package com.miracleas.minrute.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

import com.google.android.gms.location.Geofence;
import com.miracleas.minrute.R;
import com.miracleas.minrute.SettingsActivity;
import com.miracleas.minrute.model.GeofenceHelper;
import com.miracleas.minrute.model.TripLeg;
import com.miracleas.minrute.model.VoiceState;
import com.miracleas.minrute.model.VoiceStateBus;
import com.miracleas.minrute.model.VoiceStateTrain;
import com.miracleas.minrute.model.VoiceStateWalk;
import com.miracleas.minrute.provider.TripLegDetailStopMetaData;
import com.miracleas.minrute.provider.TripLegMetaData;
import com.miracleas.minrute.provider.TripMetaData;
import com.miracleas.minrute.provider.GeofenceTransitionMetaData;
import com.miracleas.minrute.utils.DateHelper;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;

public class VoiceTripService extends Service implements android.speech.tts.TextToSpeech.OnInitListener, OnSharedPreferenceChangeListener
{
	public static final String tag = VoiceTripService.class.getName();
	private Handler handler;
	private final IBinder mBinder = new LocalBinder();
	private DateHelper mDateHelper = null;
	private String mTripId = null;
	private TextToSpeech mTts;
	private String mTextToSpeak;
	private TransistionObserver mTransistionObserver = null;
	private List<TripLeg> mLegs = null;
	private int mCurrentStep = -1;
	private VoiceState mCurrentVoiceState = null;
	private WakeLock wl;
	private Resources mDefaultResources;
	
	private boolean mIsVoiceOn = true;
	private boolean mIsVoiceDepartureOn = true;
	private boolean mIsVoiceStopBeforeDestOn = true;
	
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
		public VoiceTripService getService()
		{
			// Return this instance of LocalService so clients can call public
			// methods
			return VoiceTripService.this;
		}
	}
	
	private void startWakelock()
	{
		if(wl==null)
		{
			PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
		    wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK , tag);
		    wl.acquire();
		}
		
	}
	
	private void stopWakeLock()
	{
		if(wl!=null)
		{
			wl.release();
			wl = null;
		}
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		initLanguage();
		getVoiceSettings();
		mDateHelper = new DateHelper(this, mDefaultResources);
		mDateHelper.setVoice(true);
		mTransistionObserver = new TransistionObserver(handler);
		//startWakelock();
		Log.d(tag, "onCreate");
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		stopWakeLock();
		stopDepartureHandler();
		mLoadDepartueTask = null;
		if(mTts!=null)
		{
			mTts.shutdown();
			mTts = null;
		}
		mOnVoiceServiceReadyListener = null;
		
		getContentResolver().unregisterContentObserver(
				mTransistionObserver);
		
		Log.d(tag, "onDestroy");

	}
	
	private void initLanguage()
	{
		String lan = SettingsActivity.getSelectedLanguage(this);
		Locale locale = new Locale(lan);
	    
		Resources standardResources = getResources();
		AssetManager assets = standardResources.getAssets();
		DisplayMetrics metrics = standardResources.getDisplayMetrics();
		Configuration config = new Configuration(standardResources.getConfiguration());
		config.locale = locale;
		mDefaultResources = new Resources(assets, metrics, config);
		mDefaultResources.updateConfiguration(config, null);
		//mDefaultResources.getString(R.string.voice_start_using_next_transport_in_bus);
		
		
		//mDefaultResources = ResourceBundle.getBundle("com.miracleas.minrute", Locale.ENGLISH);
		
	}
	/**
	 * initializes the speech service
	 */
	public void startTextToSpeech()
	{
		getVoiceSettings();
		
		
		Log.d(tag, "startTextToSpeech");
		if(mTts==null && mIsVoiceOn)
		{
			// success, create the TTS instance
			mTts = new TextToSpeech(this, this);
		}		
	}
	
	private void getVoiceSettings()
	{
		mIsVoiceDepartureOn = SettingsActivity.isVoiceDepartureOn(this);
		mIsVoiceStopBeforeDestOn = SettingsActivity.isVoiceStopBeforeArrivalOn(this);
		mIsVoiceOn = SettingsActivity.isVoiceOn(this);
	}
	
	@Override
	public void onInit(int status)
	{
		Log.d(tag, "onInit");
		if(status==TextToSpeech.SUCCESS)
		{
			String da = getString(R.string.pref_voice_language_danish_key);
			String lan = SettingsActivity.getSelectedLanguage(this);
			boolean isDanish = da.equals(lan);
			
			Locale locale = new Locale(lan);
			if(isDanish && mTts.isLanguageAvailable(locale)==android.speech.tts.TextToSpeech.LANG_AVAILABLE)
			{
				mTts.setLanguage(locale);				
				mTts.setSpeechRate(0.3f);
				//((mTts.setLanguage(Locale.US);
			}
			else
			{
				mTts.setLanguage(Locale.US);
			}
			
			mTts.speak(mDefaultResources.getString(R.string.voice_is_on), TextToSpeech.QUEUE_ADD, null);	
		}
	}
	/**
	 * sets the id for the current trip that needs voice support
	 * invokes OnVoiceServiceReadyListener.onVoiceServiceReady()
	 * when completed
	 * @param tripId
	 * @param listener
	 */
	public void LoadTripIdForVoiceService(String tripId, OnVoiceServiceReadyListener listener)
	{
		Log.d(tag, "LoadTripIdForVoiceService");
		mOnVoiceServiceReadyListener = listener;
		mTripId = tripId;
		if(mLoadDepartueTask==null || mLoadDepartueTask.getStatus()==AsyncTask.Status.FINISHED)
		{
			Log.d(tag, "LoadDepartueTask");
			mLoadDepartueTask = new LoadTripLegsTask();
			mLoadDepartueTask.execute(null, null, null);
		}		
	}
	
	public void playVoice(String voice)
	{
		if(mIsVoiceOn)
		{
			Log.d(tag, "playVoice: "+voice);
			if(voice!=null && isVoiceInitialized())
			{
				
				mTts.speak(voice, TextToSpeech.QUEUE_ADD, null);
				mTextToSpeak = null;
			}
			else
			{
				Log.e(tag, "Voice is not ready");
				mTextToSpeak = voice;
				//startTextToSpeech();
			}
		}
		
	}
	
	public void stopVoices()
	{
		stopWakeLock();
		Log.d(tag, "stopVoices");
		stopDepartureHandler();
	}
	
	

	public String getTextToSpeak()
	{
		return mTextToSpeak;
	}

	public void setTextToSpeak(String textToSpeak)
	{
		this.mTextToSpeak = textToSpeak;
	}

	public boolean isVoiceInitialized()
	{
		return mTts!=null;
	}
	

	private void startDepartureTimer()
	{
		getVoiceSettings();
		
		Log.d(tag, "startDepartureTimer");
		if ((mIsVoiceOn && mIsVoiceDepartureOn) && mTripId != null)
		{
			if (handler == null)
			{
				startWakelock();
				long departues = departues();
				if(departues<=VoiceState.THIRTY_SECONDS && departues >= VoiceState.ONE_MINUTE_MINUS)
				{
					Log.d(tag, "startDepartureTimer canceled, startUsingTransport");
					playVoice(mCurrentVoiceState.startUsingTransport());
				}
				else if(departues>VoiceState.THIRTY_SECONDS)
				{
					long delay = startDepartureHandlerin();
					if(delay >= VoiceState.THIRTY_SECONDS)
					{
						Log.d(tag, "playVoice without delay; departuresIn");
						playVoice(mCurrentVoiceState.departuresIn());
						delay = delay + VoiceState.THIRTY_SECONDS;
					}
					Log.d(tag, "playVoice delayed with: "+(delay / DateUtils.SECOND_IN_MILLIS));
					handler = new Handler();
					handler.postDelayed(new Runnable()
					{
						
						@Override
						public void run()
						{
							if(handler!=null && (mIsVoiceOn && mIsVoiceDepartureOn))
								playDepartureVoice();
							
						}
					}, delay);
				}
			}			
		}
		else
		{
			Log.d(tag, "startDepartureTimer: did not start");
		}
	}
	
	private void playDepartureVoice()
	{
		getVoiceSettings();
		if(mIsVoiceOn && mIsVoiceDepartureOn)
		{
			long departues = departues();
			if(departues<=VoiceState.THIRTY_SECONDS && departues >= VoiceState.ONE_MINUTE_MINUS)
			{
				Log.d(tag, "playDepartureVoice because it is 30 secs or less");
				playVoice(mCurrentVoiceState.startUsingTransport());
			}
			else if(departues > VoiceState.ONE_MINUTE_MINUS)
			{
				long tick = mCurrentVoiceState.getTickTime(Calendar.getInstance().getTimeInMillis());
				if(tick<=0)
				{
					playStartTripLegNow();
					stopDepartureHandler();
					
				}				
				else if(tick!=Long.MIN_VALUE)
				{
					Log.d(tag, "tick: playDepartureVoice");
					playVoice(mCurrentVoiceState.departuresIn());	
					Log.d(tag, "start in "+(tick/DateUtils.SECOND_IN_MILLIS)+" secs");
					if(handler==null)
					{
						Log.d(tag, "handler is null. stop.");					
					}
					else
					{
						Log.d(tag, "postDelayed");
						handler.postDelayed(runnableDeparture, tick);
					}
				}		
				else
				{
					stopDepartureHandler();
				}
			}
		}
		
		
	}
	
	private long startDepartureHandlerin()
	{
		Calendar c = Calendar.getInstance();
		int milliseconds = c.get(Calendar.MILLISECOND);
		long seconds = 10 - (c.get(Calendar.SECOND) / 10);
		Log.d(tag, "milliseconds: "+milliseconds);
		Log.d(tag, "seconds1: "+seconds);
		
		//seconds = (59 - seconds) * DateUtils.SECOND_IN_MILLIS;
		Log.d(tag, "seconds2: "+seconds/DateUtils.SECOND_IN_MILLIS);
		return (seconds - milliseconds);
	}

	private Runnable runnableDeparture = new Runnable()
	{
		@Override
		public void run()
		{
			playDepartureVoice();
		}
	};
	
	
	private void changeState(int index)
	{
		Log.d(tag, "changeState: "+index);
		stopDepartureHandler();
		mCurrentStep = index;
		if(mCurrentStep>-1 && mCurrentStep<mLegs.size())
		{
			TripLeg leg = mLegs.get(mCurrentStep);
			mCurrentVoiceState = null;
			if(leg.isWalk())
			{
				mCurrentVoiceState = new VoiceStateWalk(VoiceTripService.this, leg, mDefaultResources);
			}
			else if(leg.isTrain())
			{
				mCurrentVoiceState = new VoiceStateTrain(VoiceTripService.this, leg, mDefaultResources);
			}
			else if(leg.isBus())
			{
				mCurrentVoiceState = new VoiceStateBus(VoiceTripService.this, leg, mDefaultResources);
			}
			if(mCurrentVoiceState!=null)
			{
				playVoice(mCurrentVoiceState.nameOfDestination());
				if(mCurrentVoiceState.startDepartureHandler())
				{
					Log.d(tag, "startDepartureHandler: true");
					startDepartureTimer();
				}
				else
				{
					Log.d(tag, "startDepartureHandler: false");
					playVoice(mCurrentVoiceState.startUsingTransport());
					
				}
				
			}			
		}
		else if(mCurrentStep>=mLegs.size())
		{
			stopVoices();
		}
	}
	
	private long departues()
	{
		return mLegs.get(mCurrentStep).departureTime;
	}

	
	private void playStartTripLegNow()
	{
		if(mCurrentVoiceState!=null)
		{
			Log.d(tag, "playStartTripLegNow");
			playVoice(mCurrentVoiceState.startUsingTransport());
		}
	}

	public void stopDepartureHandler()
	{
		
		if (handler != null)
		{
			Log.d(tag, "stopDepartureHandler");
			handler.removeCallbacks(runnableDeparture);
			handler = null;
		}
	}

	public String getTripId()
	{
		return mTripId;
	}

	public void setTripId(String tripId)
	{
		this.mTripId = tripId;
	}

	private LoadTripLegsTask mLoadDepartueTask = null;
	private OnVoiceServiceReadyListener mOnVoiceServiceReadyListener = null;
	

	
	private class LoadTripLegsTask extends AsyncTask<Void, Void, ArrayList<TripLeg>>
	{
		@Override
		protected ArrayList<TripLeg> doInBackground(Void... params)
		{
			ArrayList<TripLeg> legs = new ArrayList<TripLeg>(0);
			ContentResolver cr = getContentResolver();
			Cursor c = null;
			try
			{
				String[] projection = {TripLegMetaData.TableMetaData._ID, TripLegMetaData.TableMetaData.NAME,TripLegMetaData.TableMetaData.ORIGIN_NAME, TripLegMetaData.TableMetaData.DEST_NAME, TripLegMetaData.TableMetaData.DEPARTURES_IN_TIME_LONG, TripLegMetaData.TableMetaData.TYPE, TripLegMetaData.TableMetaData.DURATION };
				String selection = TripLegMetaData.TableMetaData.TRIP_ID + "=?";
				String[] selectionArgs = {mTripId};
				
				c = cr.query(TripLegMetaData.TableMetaData.CONTENT_URI, projection, selection, selectionArgs, TripLegMetaData.TableMetaData.STEP_NUMBER);
				legs = new ArrayList<TripLeg>(c.getCount());
				int step = 0;
				if (c.moveToFirst())
				{
					int iDestName = c.getColumnIndex(TripLegMetaData.TableMetaData.DEST_NAME);
					int iOrgName = c.getColumnIndex(TripLegMetaData.TableMetaData.ORIGIN_NAME);
					int iType = c.getColumnIndex(TripLegMetaData.TableMetaData.TYPE);
					int iDuration = c.getColumnIndex(TripLegMetaData.TableMetaData.DURATION);
					int iDepartures = c.getColumnIndex(TripLegMetaData.TableMetaData.DEPARTURES_IN_TIME_LONG);
					int iTransportName = c.getColumnIndex(TripLegMetaData.TableMetaData.NAME);
					int iId = c.getColumnIndex(TripLegMetaData.TableMetaData._ID);
					do{
						TripLeg leg = new TripLeg();
						leg.destName = c.getString(iDestName);
						leg.originName = c.getString(iOrgName);
						leg.type = c.getString(iType);
						leg.setDuration(c.getLong(iDuration));
						leg.departureTime = c.getLong(iDepartures);
						leg.name = c.getString(iTransportName);
						leg.id = c.getInt(iId);
						leg.step = step;
						leg.isDestiation = c.isLast();
						leg.isOrigin = c.isFirst();
						legs.add(leg);
						step++;
					}while(c.moveToNext());
				}

			} finally
			{
				if (c != null)
				{
					c.close();
				}
			}

			return legs;
		}

		@Override
		public void onPostExecute(ArrayList<TripLeg> result)
		{
			mLegs = result;
			if(result.size()>0)
			{
				changeState(0);
				mOnVoiceServiceReadyListener.onVoiceServiceReady();
				mOnVoiceServiceReadyListener = null;
				
				getContentResolver().registerContentObserver(
						GeofenceTransitionMetaData.TableMetaData.CONTENT_URI, true,
						mTransistionObserver);
			}
			
			
		}
	}
	
	public static interface OnVoiceServiceReadyListener
	{
		public void onVoiceServiceReady();
	}
	
	class TransistionObserver extends ContentObserver {
	    public TransistionObserver(Handler h) {
	        super(h);
	    }

	    @Override
	    public boolean deliverSelfNotifications() {
	        return false;
	    }

	    @Override
	    public void onChange(boolean selfChange) {
	        Log.d(tag, "onChange");
	        super.onChange(selfChange);
	        if(!selfChange)
	        {
	        	Log.d(tag, "loadTransition");
	        	loadTransition();
	        }
	        

	    }
	}
	private void loadTransition()
	{
		if(!TextUtils.isEmpty(mTripId) && mLoadNewTransitionTask==null || mLoadNewTransitionTask.getStatus()==AsyncTask.Status.FINISHED)
		{
			mLoadNewTransitionTask = new LoadNewTransitionTask();
			mLoadNewTransitionTask.execute(null,null,null);
		}
	}
	private LoadNewTransitionTask mLoadNewTransitionTask = null;
	
	private class LoadNewTransitionTask extends AsyncTask<Void, Void, Integer>
	{
		private int mStopId = -1;
		private String mNameOfLocationBeforeDest;
		@Override
		protected Integer doInBackground(Void... params)
		{		
			int tripLegId = getTripLegIdTransition();
			if(mStopId!=-1)
			{
				mNameOfLocationBeforeDest = getNameOfStopBeforeDestination(mStopId);
			}
			return findTripLegIndex(tripLegId);
		}
		
		private int getTripLegIdTransition()
		{
			Log.d(tag, "getTripLegIdTransition()");
			int legId = -1;
			Cursor c = null;
			try
			{
				String[] projection = {GeofenceTransitionMetaData.TableMetaData.GEOFENCE_ID};
				c = getContentResolver().query(GeofenceTransitionMetaData.TableMetaData.CONTENT_URI, projection, null, null, GeofenceTransitionMetaData.TableMetaData.updated+" DESC LIMIT 1");
				if(c.moveToFirst())
				{
					String geofenceId = c.getString(c.getColumnIndex(GeofenceTransitionMetaData.TableMetaData.GEOFENCE_ID));
					Log.d(tag, "getTripLegIdTransition: "+geofenceId);
					if(geofenceId.contains(GeofenceHelper.LEG_ID))
					{						
						String[] temp = geofenceId.split(GeofenceHelper.DELIMITER);
						legId = Integer.parseInt(temp[1]);
					}
					else if(geofenceId.contains(GeofenceHelper.LEG_ID_WITH_STOP_ID))
					{
						String[] temp = geofenceId.split(GeofenceHelper.DELIMITER);
						legId = Integer.parseInt(temp[1]);
						mStopId = Integer.parseInt(temp[2]);
					}
				}		
			}
			finally
			{
				if(c!=null && !c.isClosed())
				{
					c.close();
				}
			}
			return legId;
		}
		
		private String getNameOfStopBeforeDestination(int stopId)
		{
			String name = "";
			if(stopId == GeofenceHelper.FAKE_STOP_ID)
			{
				return mDefaultResources.getString(R.string.fake_stop_name);	
			}

			Cursor c = null;
			try
			{
				String[] projection = {TripLegDetailStopMetaData.TableMetaData.NAME};
				Uri uri = Uri.withAppendedPath(TripLegDetailStopMetaData.TableMetaData.CONTENT_URI, stopId+"");
				c = getContentResolver().query(uri, projection, null, null, TripLegDetailStopMetaData.TableMetaData._ID+" DESC LIMIT 1");
				if(c.moveToFirst())
				{
					name = c.getString(c.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.NAME));
					Log.d(tag, "getNameOfStopBeforeDestination: "+name);
				}
                else
                {
                    Log.e(tag, "getNameOfStopBeforeDestination could not find stop with id: "+stopId);
                }
			}
			finally
			{
				if(c!=null && !c.isClosed())
				{
					c.close();
				}
			}
			return name;
		}
		
		private int findTripLegIndex(int legId)
		{
			Log.d(tag, "findTripLegIndex: "+legId);
			int index = -1;
			boolean found = false;
			for(int i = 0; i < mLegs.size() && !found; i++)
			{
				Log.d(tag, mLegs.get(i).id+"=="+legId);
				if(mLegs.get(i).id==legId)
				{
					found = true;
					index = i;
				}
			}
			Log.d(tag, "new state index is: "+index);
			return index;
		}
		

		@Override
		public void onPostExecute(Integer newIndex)
		{
			if(!TextUtils.isEmpty(mNameOfLocationBeforeDest) && mIsVoiceStopBeforeDestOn)
			{
				stopDepartureHandler();
				playVoice(mCurrentVoiceState.leaveTransportIn(mNameOfLocationBeforeDest));
			}
			
			if(newIndex!=-1 && mCurrentStep!=newIndex && newIndex>mCurrentStep)
			{
				changeState(newIndex);
			}	

		}
	}
	/**
	 * virker ikke
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		if(key.equals(getString(R.string.pref_voice_on_key)))
		{
			mIsVoiceOn = sharedPreferences.getBoolean(key, true);
		}
		else if(key.equals(getString(R.string.pref_voice_arrival_before_last_stop_key)))
		{
			mIsVoiceStopBeforeDestOn = sharedPreferences.getBoolean(key, true);
		}
		else if(key.equals(getString(R.string.pref_voice_departure_key)))
		{
			mIsVoiceDepartureOn = sharedPreferences.getBoolean(key, true);
		}		
	}
}
