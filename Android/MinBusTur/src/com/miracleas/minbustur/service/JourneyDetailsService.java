package com.miracleas.minbustur.service;

import com.miracleas.minbustur.net.JourneyDetailFetcher;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;

public class JourneyDetailsService extends IntentService
{
	public static final String URL = "url";
	public static final String URL1 = "url1";
	public static final String URL2 = "url2";
	public static final String LEG = "legId";
	public static final String LEG_ID1 = "legId1";
	public static final String LEG_ID2 = "legId2";
	public static final String TRIP_ID = "trip_id";
	public JourneyDetailsService()
	{
		super(JourneyDetailsService.class.getName());
	}
	public JourneyDetailsService(String name)
	{
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		String url1 = intent.getStringExtra(URL1);
		String url2 = intent.getStringExtra(URL2);
		String legId1 = intent.getStringExtra(LEG_ID1);
		String legId2 = intent.getStringExtra(LEG_ID2);
		String tripId = intent.getStringExtra(TRIP_ID);
		if(!TextUtils.isEmpty(tripId))
		{
			if(!TextUtils.isEmpty(url1) && !TextUtils.isEmpty(legId1))
			{
				JourneyDetailFetcher fetcher = new JourneyDetailFetcher(this, intent, url1, tripId, legId1);
				fetcher.startFetch();
			}
			if(!TextUtils.isEmpty(url2) && !TextUtils.isEmpty(legId2))
			{
				JourneyDetailFetcher fetcher = new JourneyDetailFetcher(this, intent, url2, tripId, legId2);
				fetcher.startFetch();
			}
		}
		
		
		
		stopSelf();
	}
}
