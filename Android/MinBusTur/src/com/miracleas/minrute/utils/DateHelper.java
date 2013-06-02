package com.miracleas.minrute.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.miracleas.minrute.R;

import android.content.Context;
import android.text.format.DateUtils;

public class DateHelper
{
	public static SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault());
	public static SimpleDateFormat formatterInternational = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
	public static SimpleDateFormat formatterTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
	public static SimpleDateFormat formatterDateRejseplanen = new SimpleDateFormat("dd.MM.yy", Locale.getDefault());

	private Context c = null;
	private boolean mVoice = false;

	public DateHelper(Context c)
	{
		this.c = c;
	}

	public static Calendar parseToCalendar(String date, SimpleDateFormat format) throws java.text.ParseException
	{
		Date d = format.parse(date);
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		return cal;

	}
	
	

	public boolean isVoice()
	{
		return mVoice;
	}

	public void setVoice(boolean voice)
	{
		this.mVoice = voice;
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
	
	public void setVoices(String days, String hours, String minutes, String seconds)
	{
		
	}
	
	
	public String getDurationLabel(long time, boolean isTripTime)
	{		
		StringBuilder b = new StringBuilder();
		boolean inFuture = time >=0;
		if(!inFuture)
		{
			return "";//(" "+ c.getString(R.string.voice_departured));
		}

		if(time>0)
		{
			int tempDays = (int)(time / 1000 / 60 / 60 / 24);
			int tempHours = (int)(time / 1000 / 60 / 60) - (tempDays * 24);
			int tempMinutes = (int)(time / 1000 / 60) - (tempHours * 60);
			int tempSecs = (int)(time / 1000 ) - (tempMinutes * 60);
			if(tempMinutes >= 0 && tempSecs>0 && tempSecs>40)
			{
				tempMinutes++;;
				tempSecs = 0;
			}
			else if(tempMinutes >= 1  && tempSecs>0 && tempSecs<20)
			{
				tempSecs = 0;
			}
			
			if(tempSecs>20 && tempSecs < 40)
			{
				tempSecs = 30;
			}
			
			if(tempMinutes==60)
			{
				tempMinutes = 0;
				tempHours = tempHours + 1;
			}
			
			if(tempDays>0)
			{
				String days = getDay(tempDays);
				b.append(tempDays).append(" ").append(days);
			}
			else
			{
				if(tempHours>0)
				{
					String hours = getHour(tempHours);											
					b.append(tempHours).append(" ").append(hours);
				}
				if(tempMinutes>0)
				{
					if(tempHours>0)
					{
						b.append(" ").append(c.getString(R.string.voice_and)).append(" ");
					}
					String minutes = getMintues(tempMinutes);
					b.append(tempMinutes).append(" ").append(minutes);
				}
				
				if(tempSecs>0)
				{
					if(tempMinutes>0 || tempHours>0)
					{
						b.append(" ").append(c.getString(R.string.voice_and)).append(" ");
					}
					String seconds = getSeconds(tempSecs);
					b.append(tempSecs).append(" ").append(seconds);
				}
			}
		}
		else if(time==0)
		{
			if(isTripTime)
			{
				if(mVoice)
				{
					b.append(c.getString(R.string.voice_departure_now));
				}
				else
				{
					b.append(c.getString(R.string.voice_departure));
				}
				
			}
			else
			{
				b.append("0");
			}
			
		}	
		
		return b.toString();
	}
	
	private String getDay(int day)
	{
		String s = null;
		if(day==1)
		{
			if(mVoice)
			{
				s = c.getString(R.string.voice_days_one);
			}
			else
			{
				s = c.getString(R.string.days_one);
			}
		}
		else
		{
			if(mVoice)
			{
				s = c.getString(R.string.voice_days_more);
			}
			else
			{
				s = c.getString(R.string.days);
			}
		}
		return s;
	}
	private String getHour(int hour)
	{
		String s = null;
		if(hour==1)
		{
			if(mVoice)
			{
				s = c.getString(R.string.voice_hours_one);
			}
			else
			{
				s = c.getString(R.string.hours_one);
			}
		}
		else
		{
			if(mVoice)
			{
				s = c.getString(R.string.voice_hours_more);
			}
			else
			{
				s = c.getString(R.string.hours);
			}
		}
		return s;
	}
	
	private String getMintues(int min)
	{
		String s = null;
		if(min==1)
		{
			if(mVoice)
			{
				s = c.getString(R.string.voice_minutes_one);
			}
			else
			{
				s = c.getString(R.string.minutes_one);
			}
		}
		else
		{
			if(mVoice)
			{
				s = c.getString(R.string.voice_minutes_more);
			}
			else
			{
				s = c.getString(R.string.minutes);
			}
		}
		return s;
	}
	
	private String getSeconds(int min)
	{
		String s = null;
		if(min==1)
		{
			if(mVoice)
			{
				s = c.getString(R.string.voice_seconds_one);
			}
			else
			{
				s = c.getString(R.string.seconds_one);
			}
		}
		else
		{
			if(mVoice)
			{
				s = c.getString(R.string.voice_seconds_more);
			}
			else
			{
				s = c.getString(R.string.seconds);
			}
		}
		return s;
	}

}