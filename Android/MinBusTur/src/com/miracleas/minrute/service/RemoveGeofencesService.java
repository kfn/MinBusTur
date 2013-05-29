package com.miracleas.minrute.service;

import com.miracleas.minrute.provider.GeofenceMetaData;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;

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
