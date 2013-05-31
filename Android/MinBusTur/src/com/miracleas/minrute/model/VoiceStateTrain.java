package com.miracleas.minrute.model;

import com.miracleas.minrute.R;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;

public class VoiceStateTrain extends VoiceState
{
	public static final String tag = VoiceStateTrain.class.getName();
	public VoiceStateTrain(Context context, TripLeg leg)
	{
		super(context, leg);
	}

	@Override
	public String departuresIn()
	{		
		long departue = mLeg.departureTime - System.currentTimeMillis();
		String strDuration = mDateHelper.getDurationLabel(departue, true);
		String text = String.format(mContext.getString(R.string.voice_departure_train), strDuration);
		return text;		
	}

	@Override
	public String startUsingTransport()
	{
		long duration = mLeg.getDuration();
		String destName = mLeg.destName;
		String strDuration = mDateHelper.getDurationLabel(duration, false);
		String text = String.format(mContext.getString(R.string.voice_start_using_transport_train), mLeg.name, strDuration);
		return text;			
	}

	@Override
	public String startUsingNextTransportIn()
	{
		long departue = mLeg.departureTime - System.currentTimeMillis();
		String strDuration = mDateHelper.getDurationLabel(departue, true);
		String transportName = mLeg.name;		
		String text = String.format(mContext.getString(R.string.voice_start_using_next_transport_in_train), transportName, strDuration, mLeg.destName);
		return text;	
	}
	
	@Override
	public String leaveTransportIn(String nameOfLocBeforeDest)
	{
		String s = mContext.getString(R.string.voice_leave_next_stop_train);
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
			text = String.format(mContext.getString(R.string.reached_destination), mLeg.originName.split(",")[0]);
		}
		return text;
	}

}
