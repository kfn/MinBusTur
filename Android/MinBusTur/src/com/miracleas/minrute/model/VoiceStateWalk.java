package com.miracleas.minrute.model;

import com.miracleas.minrute.R;

import android.content.Context;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;

public class VoiceStateWalk extends VoiceState
{
	public static final String tag = VoiceStateWalk.class.getName();
	
	public VoiceStateWalk(Context context, TripLeg leg)
	{
		super(context, leg);
	}

	@Override
	public String departuresIn()
	{		
		long departue = mLeg.departureTime - System.currentTimeMillis();
		String strDuration = mDateHelper.getDurationLabel(departue, true);
		String text = String.format(mContext.getString(R.string.voice_departure_walk), strDuration);
		return text;		
	}

	@Override
	public String startUsingTransport()
	{
		long duration = mLeg.getDuration();
		String destName = mLeg.destName;
		String strDuration = mDateHelper.getDurationLabel(duration, false);
		String text = String.format(mContext.getString(R.string.voice_start_using_transport_walk), strDuration, destName);
		return text;			
	}

	@Override
	public String startUsingNextTransportIn()
	{
		return "";
	}

	@Override
	public String leaveTransportIn()
	{
		// TODO Auto-generated method stub
		return "";
	}
	
	
	public long getTickTime()
	{
		long departures =  mLeg.departureTime - System.currentTimeMillis();
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
