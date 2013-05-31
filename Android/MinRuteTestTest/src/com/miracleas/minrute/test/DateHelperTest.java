package com.miracleas.minrute.test;

import java.text.ParseException;
import java.util.Calendar;

import android.content.Context;
import android.text.format.DateUtils;
import com.miracleas.minrute.R;
import com.miracleas.minrute.utils.DateHelper;


public class DateHelperTest extends android.test.AndroidTestCase
{
	private DateHelper mDateHelper = null;
	private Context c = null;
	
	@Override
	public void setUp()
	{
		mDateHelper = new DateHelper(getContext());
		c = getContext();
	}
	
	@Override
	public void tearDown()
	{
		
	}
	
	public void testGetDurationLabel5Minutes() throws ParseException
	{
		long duration = DateUtils.MINUTE_IN_MILLIS * 5;
		String actual = mDateHelper.getDurationLabel(duration, true);
		String expected = "5 "+getContext().getString(R.string.voice_minutes_more);
		assertEquals(expected, actual);
	}
	
	public void testGetDurationLabel5minutes10seconds() throws ParseException
	{
		long duration = (DateUtils.MINUTE_IN_MILLIS * 5) + (DateUtils.SECOND_IN_MILLIS * 10);
		String actual = mDateHelper.getDurationLabel(duration, true);
		String expected = "5 "+c.getString(R.string.voice_minutes_more)+ " "+c.getString(R.string.voice_and) + " 10 "+c.getString(R.string.voice_seconds_more);
		assertEquals(expected, actual);
	}
	
	public void testGetDurationLabel10seconds() throws ParseException
	{
		long duration = (DateUtils.SECOND_IN_MILLIS * 10);
		String actual = mDateHelper.getDurationLabel(duration, true);
		String expected = "10 "+getContext().getString(R.string.voice_seconds_more);
		assertEquals(expected, actual);
	}
}


