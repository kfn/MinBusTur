package com.miracleas.minrute.service;

import com.miracleas.minrute.model.TripLeg;
import com.miracleas.minrute.net.JourneyDetailFetcher;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;

public class JourneyDetailsService extends IntentService
{
	
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
		TripLeg leg = intent.getParcelableExtra(TripLeg.tag);
		if(!TextUtils.isEmpty(leg.tripId))
		{
			JourneyDetailFetcher fetcher1 = new JourneyDetailFetcher(this, intent, leg);
			fetcher1.startFetch();
			fetcher1.save();
		}
		stopSelf();
	}
}
