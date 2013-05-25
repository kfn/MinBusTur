package com.miracleas.minrute;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.text.TextUtils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import com.miracleas.minrute.model.TripRequest;
import com.miracleas.minrute.provider.JourneyDetailMetaData;
import com.miracleas.minrute.provider.TripLegMetaData;
import com.miracleas.minrute.provider.TripMetaData;

public class TripGuideActivity extends GeofenceActivity implements TripGuideFragment.Callbacks
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_trip_guide);

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
			String tripId = intent.getStringExtra(TripMetaData.TableMetaData._ID);
			int stepCount = intent.getIntExtra(TripLegMetaData.TableMetaData.STEP_NUMBER, 1);			
			TripRequest tripRequest = intent.getParcelableExtra(TripRequest.tag);
			// Create the detail fragment and add it to the activity
			// using a fragment transaction.
			TripGuideFragment fragment = TripGuideFragment.createInstance(tripId, stepCount, tripRequest);
			getSupportFragmentManager().beginTransaction().add(R.id.fragmentTripGuideContainer, fragment).commit();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		//getSupportMenuInflater().inflate(R.menu.create_route, menu);
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
			//finish();
			NavUtils.navigateUpFromSameTask(this);

			return true;
		
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragmentTripGuideContainer);
		if(f!=null)
		{
			f.onActivityResult(requestCode, resultCode, data);
		}
		
	}

	@Override
	public void onTripLegSelected(String tripId, String legId, String ref, String transportType)
	{
		if(!TextUtils.isEmpty(ref))
		{
			Intent activity = new Intent(this, TripLegDetailsActivity.class);
			activity.putExtra(JourneyDetailMetaData.TableMetaData.TRIP_ID, tripId);
			activity.putExtra(JourneyDetailMetaData.TableMetaData.LEG_ID, legId);
			activity.putExtra(JourneyDetailMetaData.TableMetaData.REF, ref);
			activity.putExtra(TripLegMetaData.TableMetaData.TYPE, transportType);
			startActivity(activity);
		}		
	}
	
	@Override
	public void onConnectedService()
	{		
		TripGuideFragment f = (TripGuideFragment)getSupportFragmentManager().findFragmentById(R.id.fragmentTripGuideContainer);
		if(f!=null)
		{
			f.onConnectedService();
		}
	}
}
