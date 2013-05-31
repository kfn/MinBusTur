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
	public String leaveTransportIn(String nameOfLocBeforeDest)
	{
		// TODO Auto-generated method stub
		return "";
	}
	
	@Override
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
			long temp = (mLeg.departureTime - (DateUtils.SECOND_IN_MILLIS * 30)) - currentTime;
			tick = temp;
		}
		else if(departures>= DateUtils.SECOND_IN_MILLIS * 30)
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
	
	/*public long getTickTime()
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
		else if(departures >= THREE_MINUTE)
		{
			tick = TWO_MINUTE;
		}
		else if(departures>= (DateUtils.MINUTE_IN_MILLIS + TWENTY_SECONDS))
		{
			tick = ONE_MINUTE;
		}
		else if(departures> TWENTY_SECONDS)
		{
			long temp = (departures - TEN_SECONDS) - System.currentTimeMillis();
			tick = TWENTY_SECONDS;
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
	}*/
	
	@Override
	public String nameOfDestination()
	{
		return mLeg.originName.split(",")[0];
	}

}
