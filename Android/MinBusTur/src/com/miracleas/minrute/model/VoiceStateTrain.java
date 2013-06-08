package com.miracleas.minrute.model;

import java.util.ResourceBundle;

import com.miracleas.minrute.R;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateUtils;
import android.util.Log;

public class VoiceStateTrain extends VoiceState
{
	public static final String tag = VoiceStateTrain.class.getName();
	public VoiceStateTrain(Context context, TripLeg leg, Resources defaultResources, boolean isDanish)
	{
		super(context, leg, defaultResources, isDanish);
	}

	@Override
	public String departuresIn()
	{		
		long departue = mLeg.departureTime - System.currentTimeMillis();
		String strDuration = mDateHelper.getDurationLabel(departue, true);
		String text = String.format(mDefaultResources.getString(R.string.voice_departure_train), strDuration);
		return text;		
	}

	@Override
	public String startUsingTransport()
	{
		String text = "";
		if(!mLeg.isDestiation)
		{
			long duration = mLeg.getDuration();
			String destName = mLeg.destName;
			String strDuration = mDateHelper.getDurationLabel(duration, false);
			text = String.format(mDefaultResources.getString(R.string.voice_start_using_transport_train), mLeg.name, strDuration);
		}
		
		return text;			
	}

	@Override
	public String startUsingNextTransportIn()
	{
		long departue = mLeg.departureTime - System.currentTimeMillis();
		String strDuration = mDateHelper.getDurationLabel(departue, true);
		String transportName = mLeg.name;	
		String destName = null;
		if(mIsDanish)
		{
			destName = mLeg.destName;
		}
		else
		{
			destName = mDefaultResources.getString(R.string.voice_the_next_stop);
		}
		String text = String.format(mDefaultResources.getString(R.string.voice_start_using_next_transport_in_train), transportName, strDuration, destName);
		return text;	
	}
	
	@Override
	public String leaveTransportIn(String nameOfLocBeforeDest)
	{
		String s = mDefaultResources.getString(R.string.voice_leave_next_stop_train);
		return s;
	}

	@Override
	public boolean startDepartureHandler()
	{
		// TODO Auto-generated method stub
		return !mLeg.isDestiation;
	}

}
