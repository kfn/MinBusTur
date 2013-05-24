package com.miracleas.minrute;

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import com.miracleas.minrute.model.TripRequest;
import com.miracleas.minrute.provider.JourneyDetailMetaData;
import com.miracleas.minrute.provider.JourneyDetailStopImagesMetaData;
import com.miracleas.minrute.provider.JourneyDetailStopMetaData;
import com.miracleas.minrute.provider.TripLegMetaData;

public class TripLegMapActivity extends GeofenceActivity implements TripLegMapFragment.Callbacks
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_trip_leg_map);

		// Set up the action bar.
		final ActionBar actionBar = getSupportActionBar();
		// Show the Up button in the action bar.
		actionBar.setDisplayHomeAsUpEnabled(true);
		//actionBar.setTitle(null);
		// actionBar.setDisplayShowHomeEnabled(false);
		
		// savedInstanceState is non-null when there is fragment state
		// saved from previous configurations of this activity
		// (e.g. when rotating the screen from portrait to landscape).
		// In this case, the fragment will automatically be re-added
		// to its container so we don't need to manually add it.
		// For more information, see the Fragments API guide at:
		//
		// http://developer.android.com/guide/components/fragments.html
		//

		if (savedInstanceState == null)
		{
			Intent intent = getIntent();
			long journeyId = intent.getLongExtra(JourneyDetailMetaData.TableMetaData._ID, -1);
			String legId = intent.getStringExtra(JourneyDetailMetaData.TableMetaData.LEG_ID);
			String transportType = intent.getStringExtra(TripLegMetaData.TableMetaData.TYPE);
			TripLegMapFragment fragment = TripLegMapFragment.createInstance(journeyId+"", legId, transportType);
			getSupportFragmentManager().beginTransaction().add(R.id.fragmentTripLegMapContainer, fragment).commit();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		//getSupportMenuInflater().inflate(R.menu.activity_trip_leg_details, menu);
		return super.onCreateOptionsMenu(menu);
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{

		switch (item.getItemId())
		{
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			finish();
			return true;
		
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onStopSelected(String stopId, String lat, String lng, String transportType, String stopName)
	{
		Intent activity = new Intent(this, TripStopDetailsActivity.class);
		activity.putExtra(JourneyDetailStopMetaData.TableMetaData.LATITUDE, lat);
		activity.putExtra(JourneyDetailStopMetaData.TableMetaData.LONGITUDE, lng);
		activity.putExtra(JourneyDetailStopMetaData.TableMetaData._ID, stopId);
		activity.putExtra(JourneyDetailMetaData.TableMetaData.TYPE, transportType);
		activity.putExtra(JourneyDetailStopImagesMetaData.TableMetaData.STOP_NAME, stopName);
		startActivity(activity);
	}





}
