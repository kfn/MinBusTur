package com.miracleas.minrute.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import com.google.android.gms.location.Geofence;
import com.miracleas.minrute.R;
import com.miracleas.minrute.model.TripLeg;
import com.miracleas.minrute.model.VoiceState;
import com.miracleas.minrute.model.VoiceStateBus;
import com.miracleas.minrute.model.VoiceStateTrain;
import com.miracleas.minrute.model.VoiceStateWalk;
import com.miracleas.minrute.provider.TripLegMetaData;
import com.miracleas.minrute.provider.TripMetaData;
import com.miracleas.minrute.provider.GeofenceTransitionMetaData;
import com.miracleas.minrute.utils.DateHelper;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

public class UpdateVoiceTripService extends Service implements android.speech.tts.TextToSpeech.OnInitListener
{
	public static final String tag = UpdateVoiceTripService.class.getName();
	private Handler handler;
	private final IBinder mBinder = new LocalBinder();
	private DateHelper mDateHelper = null;
	private String mTripId = null;
	private TextToSpeech mTts;
	private String mTextToSpeak;
	private TransistionObserver mTransistionObserver = null;
	private List<TripLeg> mLegs = null;
	private int mCurrentStep = 0;
	private VoiceState mCurrentVoiceState = null;
	
	private static String[] projectionTripLegTrans = {TripLegMetaData.TableMetaData._ID, TripLegMetaData.TableMetaData.ORIGIN_NAME,TripLegMetaData.TableMetaData.GEOFENCE_EVENT_ID,  TripLegMetaData.TableMetaData.TYPE, TripLegMetaData.TableMetaData.STEP_NUMBER, TripLegMetaData.TableMetaData.updated};

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
		public UpdateVoiceTripService getService()
		{
			// Return this instance of LocalService so clients can call public
			// methods
			return UpdateVoiceTripService.this;
		}
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		mDateHelper = new DateHelper(this);
		mDateHelper.setVoice(true);
		mTransistionObserver = new TransistionObserver(handler);
				
		Log.d(tag, "onCreate");
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
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
	/**
	 * initializes the speech service
	 */
	public void startTextToSpeech()
	{
		Log.d(tag, "startTextToSpeech");
		if(mTts==null)
		{
			// success, create the TTS instance
			mTts = new TextToSpeech(this, this);
		}		
	}
	
	@Override
	public void onInit(int status)
	{
		Log.d(tag, "onInit");
		if(status==TextToSpeech.SUCCESS)
		{
			Locale locale = Locale.getDefault();
			if(mTts.isLanguageAvailable(locale)==android.speech.tts.TextToSpeech.LANG_AVAILABLE)
			{
				mTts.setLanguage(locale);
				mTts.setSpeechRate(0.5f);
				//((mTts.setLanguage(Locale.US);
			}
			else
			{
				mTts.setLanguage(Locale.US);
			}
			
			mTts.speak(getString(R.string.voice_is_on), TextToSpeech.QUEUE_ADD, null);	
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
	
	public void stopVoices()
	{
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

	private Runnable runnableDeparture = new Runnable()
	{
		@Override
		public void run()
		{
			if (mTripId != null)
			{
				updateDepartureVoice();
				long tick = mCurrentVoiceState.getTickTime();
				if(tick==0)
				{
					playStartTripLegNow();
					stopDepartureHandler();
				}	
				else if(tick!=Long.MIN_VALUE)
				{
					handler.postDelayed(runnableDeparture, tick);
				}
				else
				{
					stopDepartureHandler();
				}
			}
		}
	};
	
	
	private void changeState(int index)
	{
		stopDepartureHandler();
		mCurrentStep = index;
		if(mCurrentStep>-1 && mCurrentStep<mLegs.size())
		{
			TripLeg leg = mLegs.get(mCurrentStep);
			mCurrentVoiceState = null;
			if(leg.isWalk())
			{
				mCurrentVoiceState = new VoiceStateWalk(UpdateVoiceTripService.this, leg);
			}
			else if(leg.isTrain())
			{
				mCurrentVoiceState = new VoiceStateTrain(UpdateVoiceTripService.this, leg);
			}
			else if(leg.isBus())
			{
				mCurrentVoiceState = new VoiceStateBus(UpdateVoiceTripService.this, leg);
			}
			if(mCurrentVoiceState!=null)
			{
				playVoice(mCurrentVoiceState.nameOfDestination());
				startDepartureTimer();
			}			
		}
	}

	private void startDepartureTimer()
	{
		if (mTripId != null)
		{
			if (handler == null)
			{
				handler = new Handler();
				long tick = mCurrentVoiceState.getTickTime();
				if(tick==0)
				{
					playStartTripLegNow();
					stopDepartureHandler();
					
				}				
				else if(tick!=Long.MIN_VALUE)
				{
					Calendar c = Calendar.getInstance();
					int milliseconds = c.get(Calendar.MILLISECOND);
					long seconds = c.get(Calendar.SECOND);
					seconds = (60 - seconds) * DateUtils.SECOND_IN_MILLIS;
					
					tick = (seconds-(DateUtils.SECOND_IN_MILLIS*2)) - milliseconds;
					Log.d(tag, "start in "+(tick/DateUtils.SECOND_IN_MILLIS)+" secs");
					handler.postDelayed(runnableDeparture, tick);
				}		
				else
				{
					stopDepartureHandler();
				}
			}			
		}
	}
	
	private void playStartTripLegNow()
	{
		if(mCurrentVoiceState!=null)
		{
			playVoice(mCurrentVoiceState.startUsingTransport());
		}
	}

	public void stopDepartureHandler()
	{
		if (handler != null)
		{
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
	
	
	private void updateDepartureVoice()
	{
		//long departures =  mLegs.get(mCurrentStep).departureTime - System.currentTimeMillis();
		//String str = String.format(getString(R.string.voice_departure), mDateHelper.getDurationLabel(departures, true));
		if(mCurrentVoiceState!=null)
		{
			playVoice(mCurrentVoiceState.departuresIn());	
		}	
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
						legs.add(leg);
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
		
		@Override
		protected Integer doInBackground(Void... params)
		{		
			int tripLegId = getTripLegIdTransition();	
			return findTripLegIndex(tripLegId);
		}
		
		private int getTripLegIdTransition()
		{
			int legId = -1;
			Cursor c = null;
			try
			{
				String[] projection = {GeofenceTransitionMetaData.TableMetaData.TRIP_LEG_ID};
				c = getContentResolver().query(GeofenceTransitionMetaData.TableMetaData.CONTENT_URI, projection, null, null, GeofenceTransitionMetaData.TableMetaData.updated+" DESC LIMIT 1");
				if(c.moveToFirst())
				{
					legId = c.getInt(c.getColumnIndex(GeofenceTransitionMetaData.TableMetaData.TRIP_LEG_ID));
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
		
		private int findTripLegIndex(int legId)
		{
			int index = -1;
			boolean found = false;
			for(int i = 0; i < mLegs.size() && !found; i++)
			{
				if(mLegs.get(i).id==legId)
				{
					found = true;
					index = i;
				}
			}
			return index;
		}
		

		@Override
		public void onPostExecute(Integer newIndex)
		{
			if(newIndex!=-1)
			{
				mCurrentVoiceState.leaveTransportIn();
				changeState(newIndex);
			}		
		}
	}
}
