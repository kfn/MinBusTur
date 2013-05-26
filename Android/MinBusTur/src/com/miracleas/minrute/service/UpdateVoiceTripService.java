package com.miracleas.minrute.service;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import com.miracleas.minrute.R;
import com.miracleas.minrute.provider.TripMetaData;
import com.miracleas.minrute.provider.TripVoiceMetaData;
import com.miracleas.minrute.utils.DateHelper;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.text.format.DateUtils;
import android.util.Log;

public class UpdateVoiceTripService extends Service implements android.speech.tts.TextToSpeech.OnInitListener
{
	public static final String tag = UpdateVoiceTripService.class.getName();
	private Handler handler;
	private final IBinder mBinder = new LocalBinder();
	private DateHelper mDateHelper = null;
	private String mTripId = null;
	private long mDeparturesIn = 0;
	private TextToSpeech mTts;
	private String mTextToSpeak;
	
	private static final long FOURTY_FIVE_MINUTES = DateUtils.MINUTE_IN_MILLIS * 45;
	private static final long ONE_HOUR_FOURTY_FIVE_MINUTES = DateUtils.HOUR_IN_MILLIS + FOURTY_FIVE_MINUTES;
	private static final long TEN_MINUTES = DateUtils.MINUTE_IN_MILLIS * 10;
	private static final long FIFTEEN_MINUTES = DateUtils.MINUTE_IN_MILLIS * 15;
	private static final long FIVE_MINUTES = DateUtils.MINUTE_IN_MILLIS * 5;
	private static final long ONE_MINUTE = DateUtils.MINUTE_IN_MILLIS;
	private static final long TWO_MINUTE = DateUtils.MINUTE_IN_MILLIS * 2;
	private static final long TWO_HOURS = DateUtils.HOUR_IN_MILLIS * 2;
	private static final long TEN_SECONDS = DateUtils.SECOND_IN_MILLIS * 10;
	private static final long TEN_SECONDS_MINUS = TEN_SECONDS * -1;

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
		Log.d(tag, "onDestroy");

	}
	
	public void startTextToSpeech()
	{
		// success, create the TTS instance
		mTts = new TextToSpeech(this, this);
		
	}
	
	@Override
	public void onInit(int status)
	{
		if(status==TextToSpeech.SUCCESS)
		{
			Locale locale = Locale.getDefault();
			if(mTts.isLanguageAvailable(locale)==android.speech.tts.TextToSpeech.LANG_AVAILABLE)
			{
				//mTts.setLanguage(locale);
				mTts.setLanguage(Locale.US);
			}
			else
			{
				mTts.setLanguage(Locale.US);
			}
			
			startDepartureTimer();
		}
		
		
	}
	
	public void playVoice(String voice)
	{
		if(voice!=null && isVoiceInitialized())
		{
			mTts.speak(voice, TextToSpeech.QUEUE_ADD, null);
			mTextToSpeak = null;
		}
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

	private Runnable runnable = new Runnable()
	{
		@Override
		public void run()
		{
			if (mTripId != null)
			{
				updateDepartureVoice();
				long tick = getTickTime();
				if(tick!=Long.MIN_VALUE)
				{
					handler.postDelayed(runnable, tick);
				}
				else
				{
					stopDepartureHandler();
				}
			}
		}
	};

	private void startDepartureTimer()
	{
		if (mTripId != null)
		{
			if (handler == null)
			{
				handler = new Handler();
				long tick = getTickTime();
				if(tick!=Long.MIN_VALUE)
				{
					handler.postDelayed(runnable, tick);
				}		
				else
				{
					stopDepartureHandler();
				}
			}			
		}
	}
	
	private long getTickTime()
	{
		long departures =  mDeparturesIn - System.currentTimeMillis();
		long tick = 0;
		if(departures >= DateUtils.DAY_IN_MILLIS )
		{
			tick = Long.MIN_VALUE;
		}
		else if(departures >= TWO_HOURS)
		{
			tick = ONE_HOUR_FOURTY_FIVE_MINUTES;
		}
		else if(departures >= DateUtils.HOUR_IN_MILLIS)
		{
			tick = FOURTY_FIVE_MINUTES;
		}
		else if(departures >= FIFTEEN_MINUTES)
		{
			tick = FIVE_MINUTES;
		}
		else if(departures > TEN_MINUTES)
		{
			tick = TWO_MINUTE;
		}
		else if(departures> DateUtils.MINUTE_IN_MILLIS)
		{
			tick = ONE_MINUTE;
		}
		else if(departures> TEN_SECONDS)
		{
			tick = TEN_SECONDS;
		}
		else if(departures> 0)
		{
			tick = DateUtils.SECOND_IN_MILLIS;
		}
		else if(departures > TEN_SECONDS_MINUS)
		{
			tick = TEN_SECONDS;
		}
		else
		{
			tick = Long.MIN_VALUE;
		}
		return tick;
	}

	public void stopDepartureHandler()
	{
		if (handler == null)
		{
			handler.removeCallbacks(runnable);
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
	private UpdateDepartueTask mUpdateDepartueTask = null;
	
	private void updateDepartureVoice()
	{
		long departures =  mDeparturesIn - System.currentTimeMillis();
		String str = String.format(getString(R.string.voice_departure), mDateHelper.getDurationLabel(departures, true));
		playVoice(str);
		/*if(mUpdateDepartueTask==null || mUpdateDepartueTask.getStatus()==AsyncTask.Status.FINISHED)
		{
			mUpdateDepartueTask = new UpdateDepartueTask();
			mUpdateDepartueTask.execute(null,null,null);
		}*/		
	}

	private class UpdateDepartueTask extends AsyncTask<Void, Void, String>
	{
		@Override
		protected String doInBackground(Void... params)
		{
			long departures =  mDeparturesIn - System.currentTimeMillis();
			String str = mDateHelper.getDurationLabel(departures, true);

			String where = TripVoiceMetaData.TableMetaData.TRIP_ID + "=?";
			String[] selectionArgs = { mTripId };
			ContentValues values = new ContentValues();
			values.put(TripVoiceMetaData.TableMetaData.DEPARTURES_IN, str);
			ContentResolver cr = getContentResolver();
			cr.update(TripVoiceMetaData.TableMetaData.CONTENT_URI, values, where, selectionArgs);
			return str;
		}
		@Override
		public void onPostExecute(String result)
		{
			
		}

	}
	
	private LoadDepartueTask mLoadDepartueTask = null;
	private OnVoiceServiceReadyListener mOnVoiceServiceReadyListener = null;
	
	public void initVoiceService(String tripId, OnVoiceServiceReadyListener listener)
	{
		mOnVoiceServiceReadyListener = listener;
		mTripId = tripId;
		if(mLoadDepartueTask==null)
		{
			mLoadDepartueTask = new LoadDepartueTask();
			mLoadDepartueTask.execute(null, null, null);
		}
		
	}
	
	private class LoadDepartueTask extends AsyncTask<Void, Void, Long>
	{
		@Override
		protected Long doInBackground(Void... params)
		{
			ContentResolver cr = getContentResolver();
			Cursor c = null;
			long departures = 0;
			try
			{
				String[] projection = { TripMetaData.TableMetaData.DEPATURES_TIME_LONG };
				Uri uri = Uri.withAppendedPath(TripMetaData.TableMetaData.CONTENT_URI, mTripId);
				c = cr.query(uri, projection, null, null, null);
				if (c.moveToFirst())
				{
					int i = c.getColumnIndex(TripMetaData.TableMetaData.DEPATURES_TIME_LONG);
					departures = c.getLong(i);
				}

			} finally
			{
				if (c != null)
				{
					c.close();
				}
			}

			return departures;
		}

		@Override
		public void onPostExecute(Long result)
		{
			mDeparturesIn = result;
			mOnVoiceServiceReadyListener.onVoiceServiceReady();
			mOnVoiceServiceReadyListener = null;
		}
	}
	
	public static interface OnVoiceServiceReadyListener
	{
		public void onVoiceServiceReady();
	}

}