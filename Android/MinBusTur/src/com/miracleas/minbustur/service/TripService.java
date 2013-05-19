package com.miracleas.minbustur.service;

import com.miracleas.minbustur.net.TripFetcher;

import android.app.IntentService;
import android.content.Intent;

public class TripService extends IntentService
{
	public TripService()
	{
		super(TripService.class.getName());
	}
	public TripService(String name)
	{
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		TripFetcher tripFetcher = new TripFetcher(this, intent, null);
		tripFetcher.startFetch();
		stopSelf();
	}
}
