package com.miracleas.minbustur.service;

import com.miracleas.minbustur.model.NearbyLocationRequest;
import com.miracleas.minbustur.net.DepartureBoardFetcher;
import com.miracleas.minbustur.net.StopsNearbyFetcher;
import com.miracleas.minbustur.net.TripFetcher;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;

public class DepartureBoardsService extends IntentService
{
	public static final String REQUEST = "REQUEST";
	public static final String STOP_ID = "STOP_ID";
	
	public DepartureBoardsService()
	{
		super(DepartureBoardsService.class.getName());
	}
	public DepartureBoardsService(String name)
	{
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{		
		NearbyLocationRequest request = intent.getParcelableExtra(REQUEST);
		
		if(request!=null)
		{
			String stopId = request.stopId;
			StopsNearbyFetcher nearbyFetcher = new StopsNearbyFetcher(this, intent, request);
			nearbyFetcher.startFetch();
			String searchId = nearbyFetcher.getSearchId();
			if(!TextUtils.isEmpty(searchId) && !TextUtils.isEmpty(stopId))
			{
				DepartureBoardFetcher tripFetcher = new DepartureBoardFetcher(this, searchId, stopId);
				tripFetcher.startFetch();
			}
		}
		
		
		stopSelf();
	}
}
