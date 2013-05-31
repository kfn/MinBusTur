package com.miracleas.minrute.model;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;

import com.miracleas.minrute.utils.DateHelper;

public abstract class VoiceState
{
	public static final String tag = VoiceState.class.getName();
	private static final long TEN_MINUTES = DateUtils.MINUTE_IN_MILLIS * 10;
	private static final long FIFTEEN_MINUTES = DateUtils.MINUTE_IN_MILLIS * 15;	
	private static final long ONE_MINUTE = DateUtils.MINUTE_IN_MILLIS;
	private static final long TEN_SECONDS = DateUtils.SECOND_IN_MILLIS * 10;	
	public static final long THIRTY_SECONDS = DateUtils.SECOND_IN_MILLIS * 30;
	public static final long ONE_MINUTE_MINUS = ONE_MINUTE * -1;
	
	private static final long ONE_MINUTE_FIFTY_SEC = DateUtils.MINUTE_IN_MILLIS  + (DateUtils.SECOND_IN_MILLIS * 50);
	private static final long FIFTY_SEC = (DateUtils.SECOND_IN_MILLIS * 50);
	public static final long TWENTY_SECONDS = DateUtils.SECOND_IN_MILLIS * 20;
	
	
	protected TripLeg mLeg = null;
	protected DateHelper mDateHelper = null;
	protected Context mContext = null;
	
	public VoiceState(Context context, TripLeg leg)
	{
		mLeg = leg;
		mDateHelper = new DateHelper(context);
		mDateHelper.setVoice(true);
		mContext = context;
	}
	
	public abstract String departuresIn();
	public abstract String startUsingTransport();
	public abstract String startUsingNextTransportIn();
	public abstract String leaveTransportIn(String nameOfLocBeforeDest);
	public abstract String nameOfDestination();
	
	public long getTickTime(long currentTime)
	{
		long departures =  mLeg.departureTime - currentTime;
		Log.d(tag, "departures in: "+departures / DateUtils.SECOND_IN_MILLIS+" seks.");
		long tick = 0;
		if(departures >= DateUtils.DAY_IN_MILLIS )
		{
			tick = Long.MIN_VALUE;
		}
		
		else if(departures>= FIFTEEN_MINUTES)
		{
			long temp = (mLeg.departureTime - TEN_MINUTES) - currentTime;
			tick = temp;
		}
		else if(departures>= ONE_MINUTE_FIFTY_SEC)
		{
			long temp = DateUtils.MINUTE_IN_MILLIS;
			tick = temp;
		}
		else if(departures>= FIFTY_SEC)
		{
			long temp = (mLeg.departureTime - THIRTY_SECONDS) - currentTime;
			tick = temp;
		}
		else if(departures>= TWENTY_SECONDS)
		{		
			long temp = (mLeg.departureTime - TEN_SECONDS) - currentTime;
			tick = temp;
		}
		else if(departures>= 0)
		{
			tick = 0;
		}
		else
		{
			tick = Long.MIN_VALUE;
		}
		
		Log.d(tag, "wait: "+tick / DateUtils.SECOND_IN_MILLIS+" seks.");
		return tick;
	}
}
