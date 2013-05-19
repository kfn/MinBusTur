package com.miracleas.minbustur.model;


import java.text.ParseException;
import java.util.Calendar;

import com.miracleas.minbustur.utils.DateHelper;

import android.support.v4.util.TimeUtils;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

public class TripLeg
{
	public String tripId;
	public String name;
	public String type;
	public String notes;
	public String ref;
	public String originName;
	public String originDate;
	public String originRouteId;
	public String originTime;
	public String originType;
	public String destName;
	public String destDate;
	public String destRouteId;
	public String destTime;
	public String destType;
	public String updated;
	private long duration = 0;
	private StringBuilder b;
	
	public long getDuration()
	{
		if(duration==0)
		{
			if(!TextUtils.isEmpty(originTime) && !TextUtils.isEmpty(destTime))
			{
				try
				{
					Calendar start = DateHelper.parseToCalendar(originDate+" "+originTime, DateHelper.formatter);
					Calendar end = DateHelper.parseToCalendar(destDate+" "+destTime, DateHelper.formatter);
					duration = end.getTimeInMillis() - start.getTimeInMillis();
				} catch (ParseException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return duration;
	}
	
	public String getFormattedDuration()
	{
		b = new StringBuilder();
		TimeUtils.formatDuration(duration, b);
		Log.d("TripLeg", b.toString());
		return b.toString();
	}
	
	private Time getTime(String text)
	{
		int pos = text.charAt(':');
		int hour = Integer.parseInt(text.substring(0, pos));
		int minute = Integer.parseInt(text.substring(pos+1, text.length()-1));
		Time t = new Time();
		t.second = 0;
		t.hour = hour;
		t.minute = minute;
		return t;
	}
}
