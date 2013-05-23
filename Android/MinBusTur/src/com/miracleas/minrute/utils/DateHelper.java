package com.miracleas.minrute.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateHelper
{
	public static SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault());
	public static SimpleDateFormat formatterInternational = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
	
	private String mDays;
	private String mHours;
	private String mMinutes;
	private String mSeconds;
	

	public DateHelper(String days, String hours, String minutes, String seconds)
	{
		this.mDays = days;
		this.mHours = hours;
		this.mMinutes = minutes;
		this.mSeconds = seconds;
	}

	public static Calendar parseToCalendar(String date, SimpleDateFormat format) throws java.text.ParseException
	{
		Date d = format.parse(date);
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		return cal;

	}

	public static String convertDateToString(Calendar date, SimpleDateFormat format)
	{
		return format.format(date.getTime());
	}

	public static String convertCalendarToString(Calendar date, SimpleDateFormat format)
	{
		return format.format(date.getTime());
	}

	public static Calendar parseISO8601(String date) throws java.text.ParseException
	{
		Date d = formatterInternational.parse(date);
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		return cal;

	}
	
	public String getDurationLabel(long time, boolean isTripTime)
	{		
		StringBuilder b = new StringBuilder();
		boolean inFuture = time >=0;
		if(!inFuture)
		{
			time = time * -1;
		}
		else if(isTripTime)
		{
			b.append("om ");
		}
		if(time>0)
		{
			int tempDays = (int)(time / 1000 / 60 / 60 / 24);
			int tempHours = (int)(time / 1000 / 60 / 60) - (tempDays * 24);
			int tempMinutes = (int)(time / 1000 / 60) - (tempHours * 60);
			//tempMinutes = (int) (Math.ceil(tempMinutes / 5d) * 5);
			if(tempMinutes==60)
			{
				tempMinutes = 0;
				tempHours = tempHours + 1;
			}
			
			if(tempDays>0)
			{
				String days = mDays;
				if(tempDays==1)
				{
					days = mDays.substring(0, mDays.length()-1);
				}
				b.append(tempDays).append(" ").append(days);
			}
			else
			{
				if(tempHours>0)
				{
					String hours = mHours;
					if(tempHours==1)
					{
						hours = mHours.substring(0, mHours.length()-1);
					}
						
					b.append(tempHours).append(" ").append(hours);
				}
				if(tempMinutes>0)
				{
					if(tempHours>0)
					{
						b.append(" ");
					}
					
					b.append(tempMinutes).append(" ").append(mMinutes);
				}
				if(b.length()==0)
				{
					b.append(mSeconds);
				}
			}
		}
		else if(time==0)
		{
			if(isTripTime)
			{
				b.append("Lige nu");
			}
			else
			{
				b.append("0");
			}
			
		}	
		if(!inFuture)
		{
			b.append(" siden");
		}
		return b.toString();
	}

}