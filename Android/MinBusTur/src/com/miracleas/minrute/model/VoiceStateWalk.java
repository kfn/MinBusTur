package com.miracleas.minrute.model;

import java.util.ResourceBundle;

import com.miracleas.minrute.R;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;

public class VoiceStateWalk extends VoiceState
{
	public static final String tag = VoiceStateWalk.class.getName();
	
	public VoiceStateWalk(Context context, TripLeg leg, Resources defaultResources, boolean isDanish)
	{
		super(context, leg, defaultResources, isDanish);
	}
	

	@Override
	public String departuresIn()
	{		
		String text = "";
		if(!mLeg.isDestiation)
		{
			if(!mLeg.isOrigin)
			{
				text = startUsingTransport();
			}
			else
			{
				long departue = mLeg.departureTime - System.currentTimeMillis();
				String strDuration = mDateHelper.getDurationLabel(departue, true);
				
				if(mLeg.isOrigin)
				{
					text = String.format(mDefaultResources.getString(R.string.voice_departure_walk), strDuration);
				}
				else
				{
					String destName = null;
					if(mIsDanish)
					{
						destName = mLeg.destName;
					}
					else
					{
						destName = mDefaultResources.getString(R.string.voice_the_next_stop);
					}
					text = String.format(mDefaultResources.getString(R.string.voice_start_using_transport_walk), strDuration, destName);
				}
			}
		}
		return text;		
	}

	@Override
	public String startUsingTransport()
	{
		String text = "";
		if(!mLeg.isDestiation)
		{
			long duration = mLeg.getDuration();
			String destName = null;
			if(mIsDanish)
			{
				destName = mLeg.destName;
			}
			else
			{
				destName = mDefaultResources.getString(R.string.voice_the_next_stop);
			}
			
			String strDuration = mDateHelper.getDurationLabel(duration, false);
			text = String.format(mDefaultResources.getString(R.string.voice_start_using_transport_walk), strDuration, destName);
		}
		
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
	public boolean startDepartureHandler()
	{
		return mLeg.isOrigin;
	}

}
