package com.miracleas.minbustur.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateHelper
{
	public static SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault());
	public static SimpleDateFormat formatterInternational = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

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

}