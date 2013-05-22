package com.miracleas.minbustur.service;

import com.miracleas.minbustur.net.JourneyDetailFetcher;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;

public class JourneyDetailsService extends IntentService
{
	public static final String ADDRESS_ORIGIN = "origin";
	public static final String ADDRESS_DEST = "dest";
	public static final String URL = "url";
	public static final String LEG = "legId";
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
		String address1 = intent.getStringExtra(ADDRESS_ORIGIN);
		String address2 = intent.getStringExtra(ADDRESS_DEST);
		String url1 = intent.getStringExtra(URL);
		String legId1 = intent.getStringExtra(LEG);
		String tripId = intent.getStringExtra(TRIP_ID);
		
		if(!TextUtils.isEmpty(tripId))
		{
			if(!TextUtils.isEmpty(url1) && !TextUtils.isEmpty(legId1) && !TextUtils.isEmpty(address1) && !TextUtils.isEmpty(address2))
			{
				JourneyDetailFetcher fetcher1 = new JourneyDetailFetcher(this, intent, url1, tripId, legId1, address1, address2);
				fetcher1.startFetch();
				fetcher1.save();
			}
		}
		stopSelf();
	}
}
