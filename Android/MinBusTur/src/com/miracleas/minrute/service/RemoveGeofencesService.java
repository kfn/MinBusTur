package com.miracleas.minrute.service;

import android.app.IntentService;
import android.content.Intent;

public class RemoveGeofencesService extends IntentService
{
	public RemoveGeofencesService()
	{
		super(RemoveGeofencesService.class.getName());
	}
	public RemoveGeofencesService(String name)
	{
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		stopSelf();
	}
}
