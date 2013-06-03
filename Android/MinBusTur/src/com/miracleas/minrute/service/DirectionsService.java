package com.miracleas.minrute.service;

import android.app.IntentService;
import android.content.Intent;

import com.miracleas.minrute.model.TripLeg;
import com.miracleas.minrute.net.DirectionsFetcher;
import com.miracleas.minrute.net.TripFetcher;

public class DirectionsService extends IntentService
{
	public DirectionsService()
	{
		super(DirectionsService.class.getName());
	}
	public DirectionsService(String name)
	{
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
        TripLeg leg = intent.getParcelableExtra(TripLeg.tag);
        DirectionsFetcher fetcher = new DirectionsFetcher(this, leg);
        fetcher.startFetch();
		stopSelf();
	}
}
