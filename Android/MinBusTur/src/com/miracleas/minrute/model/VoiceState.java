package com.miracleas.minrute.model;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;

import com.miracleas.minrute.utils.DateHelper;

public abstract class VoiceState
{
	public static final String tag = VoiceState.class.getName();
	protected static final long FOURTY_FIVE_MINUTES = DateUtils.MINUTE_IN_MILLIS * 45;
	protected static final long ONE_HOUR_FOURTY_FIVE_MINUTES = DateUtils.HOUR_IN_MILLIS + FOURTY_FIVE_MINUTES;
	protected static final long TEN_MINUTES = DateUtils.MINUTE_IN_MILLIS * 10;
	protected static final long FIFTEEN_MINUTES = DateUtils.MINUTE_IN_MILLIS * 15;
	protected static final long FIVE_MINUTES = DateUtils.MINUTE_IN_MILLIS * 5;
	protected static final long ONE_MINUTE = DateUtils.MINUTE_IN_MILLIS;
	protected static final long TWO_MINUTE = DateUtils.MINUTE_IN_MILLIS * 2;
	protected static final long THREE_MINUTE = DateUtils.MINUTE_IN_MILLIS * 3;
	protected static final long TWO_HOURS = DateUtils.HOUR_IN_MILLIS * 2;
	protected static final long TEN_SECONDS = DateUtils.SECOND_IN_MILLIS * 10;
	public static final long TWENTY_SECONDS = DateUtils.SECOND_IN_MILLIS * 20;
	public static final long THIRTY_SECONDS = DateUtils.SECOND_IN_MILLIS * 30;
	protected static final long TEN_SECONDS_MINUS = TEN_SECONDS * -1;
	
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
		long tick = 0;
		if(departures >= DateUtils.DAY_IN_MILLIS )
		{
			tick = Long.MIN_VALUE;
		}
		else if(departures >= TWO_HOURS)
		{
			long temp = (mLeg.departureTime - DateUtils.HOUR_IN_MILLIS) - currentTime;
			tick = temp;
		}
		else if(departures >= DateUtils.HOUR_IN_MILLIS)
		{
			long temp = (mLeg.departureTime - (DateUtils.MINUTE_IN_MILLIS * 30)) - currentTime;
			tick = temp;
		}
		else if(departures >= DateUtils.MINUTE_IN_MILLIS * 30)
		{
			long temp = (mLeg.departureTime - FIFTEEN_MINUTES) - currentTime;
			tick = temp;
		}
		else if(departures >= FIFTEEN_MINUTES)
		{
			long temp = (mLeg.departureTime - TEN_MINUTES) - currentTime;
			tick = temp;
		}
		else if(departures >= TEN_MINUTES)
		{
			long temp = (mLeg.departureTime - FIVE_MINUTES) - currentTime;
			tick = temp;
		}
		else if(departures>= FIVE_MINUTES)
		{
			long temp = (mLeg.departureTime - THREE_MINUTE) - currentTime;
			tick = temp;
		}
		else if(departures>= THREE_MINUTE)
		{
			long temp = (mLeg.departureTime - TWO_MINUTE) - currentTime;
			tick = temp;
		}
		else if(departures>= TWO_MINUTE)
		{
			long temp = (mLeg.departureTime - ONE_MINUTE) - currentTime;
			tick = temp;
		}
		else if(departures>= ONE_MINUTE)
		{
			long temp = (mLeg.departureTime - THIRTY_SECONDS) - currentTime;
			tick = temp;
		}
		else if(departures>= THIRTY_SECONDS)
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
