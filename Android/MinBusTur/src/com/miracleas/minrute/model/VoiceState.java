package com.miracleas.minrute.model;

import android.content.Context;
import android.text.format.DateUtils;

import com.miracleas.minrute.utils.DateHelper;

public abstract class VoiceState
{
	protected static final long FOURTY_FIVE_MINUTES = DateUtils.MINUTE_IN_MILLIS * 45;
	protected static final long ONE_HOUR_FOURTY_FIVE_MINUTES = DateUtils.HOUR_IN_MILLIS + FOURTY_FIVE_MINUTES;
	protected static final long TEN_MINUTES = DateUtils.MINUTE_IN_MILLIS * 10;
	protected static final long FIFTEEN_MINUTES = DateUtils.MINUTE_IN_MILLIS * 15;
	protected static final long FIVE_MINUTES = DateUtils.MINUTE_IN_MILLIS * 5;
	protected static final long ONE_MINUTE = DateUtils.MINUTE_IN_MILLIS;
	protected static final long TWO_MINUTE = DateUtils.MINUTE_IN_MILLIS * 2;
	protected static final long TWO_HOURS = DateUtils.HOUR_IN_MILLIS * 2;
	protected static final long TEN_SECONDS = DateUtils.SECOND_IN_MILLIS * 10;
	protected static final long TEN_SECONDS_MINUS = TEN_SECONDS * -1;
	
	protected TripLeg mLeg = null;
	protected DateHelper mDateHelper = null;
	protected Context mContext = null;
	
	public VoiceState(Context context, TripLeg leg)
	{
		mLeg = leg;
		mDateHelper = new DateHelper(context);
		mDateHelper.setVoice(true);
		mContext = context;
	}
	public abstract long getTickTime();
	public abstract String departuresIn();
	public abstract String startUsingTransport();
	public abstract String startUsingNextTransportIn();
	public abstract String leaveTransportIn();
	public abstract String nameOfDestination();
}
