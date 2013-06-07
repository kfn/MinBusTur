package com.miracleas.minrute.model;

import java.util.ResourceBundle;

import com.miracleas.minrute.R;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateUtils;
import android.util.Log;

public class VoiceStateBus extends VoiceState
{
	public static final String tag = VoiceStateBus.class.getName();
	public VoiceStateBus(Context context, TripLeg leg, Resources defaultResources)
	{
		super(context, leg, defaultResources);
	}

	@Override
	public String departuresIn()
	{		
		long departue = mLeg.departureTime - System.currentTimeMillis();
		String strDuration = mDateHelper.getDurationLabel(departue, true);
		String text = String.format(mDefaultResources.getString(R.string.voice_departure_bus), strDuration);
		return text;		
	}

	@Override
	public String startUsingTransport()
	{
		String text = "";
		if(!mLeg.isDestiation)
		{
			long duration = mLeg.getDuration();
			String strDuration = mDateHelper.getDurationLabel(duration, false);
			text = String.format(mDefaultResources.getString(R.string.voice_start_using_transport_bus), mLeg.name, strDuration);
		}
		
		return text;			
	}

	@Override
	public String startUsingNextTransportIn()
	{
		long departue = mLeg.departureTime - System.currentTimeMillis();
		String strDuration = mDateHelper.getDurationLabel(departue, true);
		String transportName = mLeg.name;		
		String text = String.format(mDefaultResources.getString(R.string.voice_start_using_next_transport_in_bus), transportName, strDuration, mLeg.destName);
		return text;	
	}

	@Override
	public String leaveTransportIn(String nameOfLocBeforeDest)
	{
		String s = mDefaultResources.getString(R.string.voice_leave_next_stop_bus);//String.format(mContext.getString(R.string.voice_leave_next_stop_bus), nameOfLocBeforeDest, mLeg.destName);
		return s;
	}


	@Override
	public String nameOfDestination()
	{
		String text = "";
		if(!mLeg.isDestiation)
		{
			text = mLeg.originName.split(",")[0];
		}
		else
		{
			text = String.format(mDefaultResources.getString(R.string.reached_destination), mLeg.originName.split(",")[0]);
		}
		return text;
	}

	@Override
	public boolean startDepartureHandler()
	{
		// TODO Auto-generated method stub
		return !mLeg.isDestiation;
	}

}
