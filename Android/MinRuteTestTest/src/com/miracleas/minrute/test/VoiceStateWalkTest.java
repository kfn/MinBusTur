package com.miracleas.minrute.test;

import com.miracleas.minrute.R;
import com.miracleas.minrute.model.TripLeg;
import com.miracleas.minrute.model.VoiceState;
import com.miracleas.minrute.model.VoiceStateWalk;

import android.content.Context;
import android.text.format.DateUtils;

public class VoiceStateWalkTest extends android.test.AndroidTestCase
{
	private Context c;
	private long currentTime = 0;
	
	@Override
	public void setUp()
	{
		c = getContext();
		currentTime = System.currentTimeMillis();
	}
	
	private TripLeg getTripLeg(long departureTime)
	{
		TripLeg leg = new TripLeg();
		leg.departureTime = departureTime;
		return leg;
	}
	
	@Override
	public void tearDown()
	{
		
	}
	
	public void testDeparturesIn5Minutes()
	{
		long duration = DateUtils.MINUTE_IN_MILLIS * 5;
		long departureTime = currentTime + duration;
		TripLeg leg = getTripLeg(departureTime);
		VoiceState voice = new VoiceStateWalk(c, leg);
		String actual = voice.departuresIn();
		String expected = String.format(mContext.getString(R.string.voice_departure_walk), "5 "+c.getString(R.string.voice_minutes_more));
		assertEquals(expected, actual);
	}
	
	public void testGetTickTime4Secs()
	{
		long duration = DateUtils.SECOND_IN_MILLIS * 4;
		long departureTime = currentTime + duration;
		TripLeg leg = getTripLeg(departureTime);
		VoiceState voice = new VoiceStateWalk(c, leg);
		long actual = voice.getTickTime(currentTime);
		long expected = 0;
		assertEquals(expected, actual);
	}
	
	public void testGetTickTime9Secs()
	{
		long duration = DateUtils.SECOND_IN_MILLIS * 9;
		long departureTime = currentTime + duration;
		TripLeg leg = getTripLeg(departureTime);
		VoiceState voice = new VoiceStateWalk(c, leg);
		long actual = voice.getTickTime(currentTime);
		long expected = 0;
		assertEquals(expected, actual);
	}
	
	public void testGetTickTime11Secs()
	{
		long duration = DateUtils.SECOND_IN_MILLIS * 11;
		long departureTime = currentTime + duration;
		TripLeg leg = getTripLeg(departureTime);
		VoiceState voice = new VoiceStateWalk(c, leg);
		long actual = voice.getTickTime(currentTime);
		long expected = 0;
		assertEquals(expected, actual);
	}
	
	public void testGetTickTime29Secs()
	{
		long duration = DateUtils.SECOND_IN_MILLIS * 29;
		long departureTime = currentTime + duration;
		TripLeg leg = getTripLeg(departureTime);
		VoiceState voice = new VoiceStateWalk(c, leg);
		long actual = voice.getTickTime(currentTime);
		long expected = 0;
		assertEquals(expected, actual);
	}
	
	public void testGetTickTime30Secs()
	{
		long duration = DateUtils.SECOND_IN_MILLIS * 30;
		long departureTime = currentTime + duration;
		TripLeg leg = getTripLeg(departureTime);
		VoiceState voice = new VoiceStateWalk(c, leg);
		long actual = voice.getTickTime(currentTime);
		long expected = DateUtils.SECOND_IN_MILLIS * 20;
		assertEquals(expected, actual);
	}
	
	public void testGetTickTime31Secs()
	{
		long duration = DateUtils.SECOND_IN_MILLIS * 31;
		long departureTime = currentTime + duration;
		TripLeg leg = getTripLeg(departureTime);
		VoiceState voice = new VoiceStateWalk(c, leg);
		long actual = voice.getTickTime(currentTime);
		long expected =  DateUtils.SECOND_IN_MILLIS * 21;
		assertEquals(expected, actual);
	}
	
	public void testGetTickTime59Secs()
	{
		long duration = DateUtils.SECOND_IN_MILLIS * 59;
		long departureTime = currentTime + duration;
		TripLeg leg = getTripLeg(departureTime);
		VoiceState voice = new VoiceStateWalk(c, leg);
		long actual = voice.getTickTime(currentTime);
		long expected = DateUtils.SECOND_IN_MILLIS * 49;
		assertEquals(expected, actual);
	}
	
	public void testGetTickTime1minute1Secs()
	{
		long duration = DateUtils.MINUTE_IN_MILLIS + (DateUtils.SECOND_IN_MILLIS * 1);
		long departureTime = currentTime + duration;
		TripLeg leg = getTripLeg(departureTime);
		VoiceState voice = new VoiceStateWalk(c, leg);
		long actual = voice.getTickTime(currentTime);
		long expected = DateUtils.SECOND_IN_MILLIS * 31;
		assertEquals(expected, actual);
	}
	
	public void testGetTickTime1minute30Secs()
	{
		long duration = DateUtils.MINUTE_IN_MILLIS + (DateUtils.SECOND_IN_MILLIS * 30);
		long departureTime = currentTime + duration;
		TripLeg leg = getTripLeg(departureTime);
		VoiceState voice = new VoiceStateWalk(c, leg);
		long actual = voice.getTickTime(currentTime);
		long expected = DateUtils.MINUTE_IN_MILLIS;
		assertEquals(expected, actual);
	}
	
	public void testGetTickTime3minute1Secs()
	{
		long duration = (DateUtils.MINUTE_IN_MILLIS * 3) + (DateUtils.SECOND_IN_MILLIS * 1);
		long departureTime = currentTime + duration;
		TripLeg leg = getTripLeg(departureTime);
		VoiceState voice = new VoiceStateWalk(c, leg);
		long actual = voice.getTickTime(currentTime);
		long expected = DateUtils.MINUTE_IN_MILLIS  + (DateUtils.SECOND_IN_MILLIS);
		assertEquals(expected, actual);
	}
	
	public void testGetTickTime4minute59Secs()
	{
		long duration = (DateUtils.MINUTE_IN_MILLIS * 4) + (DateUtils.SECOND_IN_MILLIS * 59);
		long departureTime = currentTime + duration;
		TripLeg leg = getTripLeg(departureTime);
		VoiceState voice = new VoiceStateWalk(c, leg);
		long actual = voice.getTickTime(currentTime);
		long expected = DateUtils.MINUTE_IN_MILLIS * 2 + (DateUtils.SECOND_IN_MILLIS * 59);
		assertEquals(expected, actual);
	}
	
	public void testGetTickTime5minute()
	{
		long duration = (DateUtils.MINUTE_IN_MILLIS * 5);
		long departureTime = currentTime + duration;
		TripLeg leg = getTripLeg(departureTime);
		VoiceState voice = new VoiceStateWalk(c, leg);
		long actual = voice.getTickTime(currentTime);
		long expected = DateUtils.MINUTE_IN_MILLIS * 2;
		assertEquals(expected, actual);
	}
	
	public void testGetTickTime15minute1Secs()
	{
		long duration = (DateUtils.MINUTE_IN_MILLIS * 15) + (DateUtils.SECOND_IN_MILLIS * 1);
		long departureTime = currentTime + duration;
		TripLeg leg = getTripLeg(departureTime);
		VoiceState voice = new VoiceStateWalk(c, leg);
		long actual = voice.getTickTime(currentTime);
		long expected = (DateUtils.MINUTE_IN_MILLIS * 5) + (DateUtils.SECOND_IN_MILLIS * 1);
		assertEquals(expected, actual);
	}
	
	public void testGetTickTime14minute58Secs()
	{
		long duration = (DateUtils.MINUTE_IN_MILLIS * 14) + (DateUtils.SECOND_IN_MILLIS * 58);
		long departureTime = currentTime + duration;
		TripLeg leg = getTripLeg(departureTime);
		VoiceState voice = new VoiceStateWalk(c, leg);
		long actual = voice.getTickTime(currentTime);
		long expected = (DateUtils.MINUTE_IN_MILLIS * 9) + (DateUtils.SECOND_IN_MILLIS * 58);
		assertEquals(expected, actual);
	}
	
	
	
	public void testGetTickTime15minute()
	{
		long duration = (DateUtils.MINUTE_IN_MILLIS * 15);
		long departureTime = currentTime + duration;
		TripLeg leg = getTripLeg(departureTime);
		VoiceState voice = new VoiceStateWalk(c, leg);
		long actual = voice.getTickTime(currentTime);
		long expected = (DateUtils.MINUTE_IN_MILLIS * 5);
		
		assertEquals(expected, actual);
	}
}
