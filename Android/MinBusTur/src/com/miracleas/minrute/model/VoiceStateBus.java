package com.miracleas.minrute.model;

import com.miracleas.minrute.R;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;

public class VoiceStateBus extends VoiceState
{
	public static final String tag = VoiceStateBus.class.getName();
	public VoiceStateBus(Context context, TripLeg leg)
	{
		super(context, leg);
	}

	@Override
	public String departuresIn()
	{		
		long departue = mLeg.departureTime - System.currentTimeMillis();
		String strDuration = mDateHelper.getDurationLabel(departue, true);
		String text = String.format(mContext.getString(R.string.voice_departure_vechical), strDuration);
		return text;		
	}

	@Override
	public String startUsingTransport()
	{
		long duration = mLeg.getDuration();
		String strDuration = mDateHelper.getDurationLabel(duration, false);
		String text = String.format(mContext.getString(R.string.voice_start_using_transport_bus), mLeg.name, strDuration);
		return text;			
	}

	@Override
	public String startUsingNextTransportIn()
	{
		long departue = mLeg.departureTime - System.currentTimeMillis();
		String strDuration = mDateHelper.getDurationLabel(departue, true);
		String transportName = mLeg.name;		
		String text = String.format(mContext.getString(R.string.voice_start_using_next_transport_in_bus), transportName, strDuration, mLeg.destName);
		return text;	
	}

	@Override
	public String leaveTransportIn()
	{
		return mContext.getString(R.string.voice_leave_next_stop);
	}
	@Override
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
		else if(departures> TEN_SECONDS_MINUS)
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
