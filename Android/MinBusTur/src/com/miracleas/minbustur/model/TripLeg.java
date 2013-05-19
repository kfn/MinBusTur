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
	public TripLocation origin;
	public TripLocation dest;
	public String updated;
	private long duration = 0;
	private StringBuilder b;
	private Calendar start;
	private Calendar end;
	
	public long getDuration()
	{
		if(duration==0 && origin!=null && dest!=null)
		{
			if(!TextUtils.isEmpty(origin.time) && !TextUtils.isEmpty(dest.time))
			{
				try
				{
					start = DateHelper.parseToCalendar(origin.date+" "+origin.time, DateHelper.formatter);
					end = DateHelper.parseToCalendar(dest.date+" "+dest.time, DateHelper.formatter);
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
	
	public long getDeparturesIn()
	{
		long s = 0l;
		if(start!=null)
		{
			s = start.getTimeInMillis() - System.currentTimeMillis();
		}
		return s;
	}
	
	public long getArrivesIn()
	{
		long s = 0l;
		if(end!=null)
		{
			s = end.getTimeInMillis() - System.currentTimeMillis();
		}
		return s;
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
