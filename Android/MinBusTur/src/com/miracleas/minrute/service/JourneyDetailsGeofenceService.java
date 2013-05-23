package com.miracleas.minrute.service;

import com.miracleas.minrute.net.JourneyDetailFetcher;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;

public class JourneyDetailsGeofenceService extends IntentService
{
	public static final String ADDRESS = "address";
	public static final String ADDRESS1 = "address1";
	public static final String ADDRESS2 = "address2";
	public static final String ADDRESS3 = "address3";
	public static final String ADDRESS4 = "address4";
	public static final String URL = "url";
	public static final String URL1 = "url1";
	public static final String URL2 = "url2";
	public static final String LEG = "legId";
	public static final String LEG_ID1 = "legId1";
	public static final String LEG_ID2 = "legId2";
	public static final String TRIP_ID = "trip_id";
	public JourneyDetailsGeofenceService()
	{
		super(JourneyDetailsGeofenceService.class.getName());
	}
	public JourneyDetailsGeofenceService(String name)
	{
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		String address1 = intent.getStringExtra(ADDRESS1);
		String address2 = intent.getStringExtra(ADDRESS2);
		String address3 = intent.getStringExtra(ADDRESS3);
		String address4 = intent.getStringExtra(ADDRESS4);
		String url1 = intent.getStringExtra(URL1);
		String url2 = intent.getStringExtra(URL2);
		String legId1 = intent.getStringExtra(LEG_ID1);
		String legId2 = intent.getStringExtra(LEG_ID2);
		String tripId = intent.getStringExtra(TRIP_ID);
		if(!TextUtils.isEmpty(tripId))
		{
			JourneyDetailFetcher fetcher1 = null;
			JourneyDetailFetcher fetcher2 = null;
			if(!TextUtils.isEmpty(url1) && !TextUtils.isEmpty(legId1) && !TextUtils.isEmpty(address1) && !TextUtils.isEmpty(address2))
			{
				fetcher1 = new JourneyDetailFetcher(this, intent, url1, tripId, legId1, address1, address2);
				fetcher1.startFetch();
			}
			if(!TextUtils.isEmpty(url2) && !TextUtils.isEmpty(legId2) && !TextUtils.isEmpty(address3) && !TextUtils.isEmpty(address4))
			{
				fetcher2 = new JourneyDetailFetcher(this, intent, url2, tripId, legId2, address3, address4);
				fetcher2.startFetch();
			}
			if(fetcher1!=null && fetcher2!=null)
			{
				fetcher1.addContentProviderOperations(fetcher2.getDbOperations());
				fetcher1.save();
			}
		}
		
		
		
		stopSelf();
	}
}
