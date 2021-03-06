package com.miracleas.minrute.model;

import java.util.ResourceBundle;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateUtils;
import android.util.Log;

import com.miracleas.minrute.R;
import com.miracleas.minrute.utils.DateHelper;

public abstract class VoiceState
{
	public static final String tag = VoiceState.class.getName();
	private static final long TEN_MINUTES = DateUtils.MINUTE_IN_MILLIS * 10;
	private static final long FIFTEEN_MINUTES = DateUtils.MINUTE_IN_MILLIS * 15;	
	private static final long TWENTY_MINUTES = DateUtils.MINUTE_IN_MILLIS * 20;
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
	protected Resources mDefaultResources;
	protected boolean mIsDanish = false;
	
	public VoiceState(Context context, TripLeg leg, Resources defaultResources, boolean isDanish)
	{
		mLeg = leg;
		mLeg.originName = shortendAddressName(mLeg.originName);
		mLeg.destName = shortendAddressName(mLeg.destName);
		mDefaultResources = defaultResources;
		mDateHelper = new DateHelper(context, defaultResources);
		mDateHelper.setVoice(true);
		mContext = context;
		mIsDanish = isDanish;
	}
	
	public abstract String departuresIn();
	public abstract String startUsingTransport();
	public abstract String startUsingNextTransportIn();
	public abstract String leaveTransportIn(String nameOfLocBeforeDest);
	
	public abstract boolean startDepartureHandler();
	
	public void setRessource(Resources languageResources)
	{
		mDefaultResources = languageResources;
	}
	
	public void setIsDanish(boolean isDanish)
	{
		mIsDanish = isDanish;
	}
	
	
	public String nameOfDestination()
	{
		String text = "";
		if(mIsDanish)
		{
			if(!mLeg.isDestiation)
			{
				text = mLeg.originName;
			}
			else
			{
				text = String.format(mDefaultResources.getString(R.string.reached_destination), mLeg.originName);
			}
		}
		else if(!mLeg.isDestiation && !mLeg.isOrigin)
		{
			text = mDefaultResources.getString(R.string.voice_arrived_at_stop);
		}
		else if(mLeg.isDestiation)
		{
			text = mDefaultResources.getString(R.string.reached_destination);
		}
		
		return text;
	}
	
	public long getTickTime(long currentTime)
	{
		long departures =  mLeg.departureTime - currentTime;
		Log.d(tag, "departures in: "+departures / DateUtils.SECOND_IN_MILLIS+" seks.");
		long tick = 0;
		if(departures >= DateUtils.DAY_IN_MILLIS )
		{
			tick = Long.MIN_VALUE;
		}
		else if(departures>= TWENTY_MINUTES)
		{
			long temp = (mLeg.departureTime - FIFTEEN_MINUTES) - currentTime;
			tick = temp;
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
	
	protected String shortendAddressName(String address)
	{
		int i = address.indexOf(",");
		if(i!=-1)
		{
			return address.substring(0, i);
		}
		else
		{
			return address;
		}
	}
}
