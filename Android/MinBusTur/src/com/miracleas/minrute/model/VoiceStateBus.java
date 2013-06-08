package com.miracleas.minrute.model;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;

import com.miracleas.minrute.R;

public class VoiceStateBus extends VoiceState
{
	public static final String tag = VoiceStateBus.class.getName();
	public VoiceStateBus(Context context, TripLeg leg, Resources defaultResources, boolean isDanish)
	{
		super(context, leg, defaultResources, isDanish);
	}

	@Override
	public String departuresIn()
	{		
		long departue = mLeg.departureTime - System.currentTimeMillis();
		String strDuration = mDateHelper.getDurationLabel(departue, true);
		String text = String.format(mDefaultResources.getString(R.string.voice_departure_bus), getVoiceFriendlyBusName(mLeg.name), strDuration);
		return text;		
	}
	
	private String getVoiceFriendlyBusName(String busName)
	{
		StringBuilder b = new StringBuilder();
		String[] temp = busName.split(" ");
		if(temp.length>1)
		{
			String number = temp[1];
			if(TextUtils.isDigitsOnly(number))
			{
				b.append(busName);
			}
			else
			{		
				b.append(temp[0]).append(" ");
				for(int i = 0; i < number.length(); i++)
				{
					char c = number.charAt(i);
					if(Character.isDigit(c))
					{
						b.append(c);
					}
					else
					{
						b.append(". ").append(c).append(". ");
					}					
				}
			}
		}
		return b.toString();
	}

	@Override
	public String startUsingTransport()
	{
		String text = "";
		if(!mLeg.isDestiation)
		{
			long duration = mLeg.getDuration();
			String strDuration = mDateHelper.getDurationLabel(duration, false);
			text = String.format(mDefaultResources.getString(R.string.voice_start_using_transport_bus), getVoiceFriendlyBusName(mLeg.name), strDuration);
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
		String text = String.format(mDefaultResources.getString(R.string.voice_start_using_next_transport_in_bus), transportName, strDuration, destName);
		return text;	
	}

	@Override
	public String leaveTransportIn(String nameOfLocBeforeDest)
	{
		String s = mDefaultResources.getString(R.string.voice_leave_next_stop_bus);//String.format(mContext.getString(R.string.voice_leave_next_stop_bus), nameOfLocBeforeDest, mLeg.destName);
		return s;
	}

	@Override
	public boolean startDepartureHandler()
	{
		// TODO Auto-generated method stub
		return !mLeg.isDestiation;
	}

}
