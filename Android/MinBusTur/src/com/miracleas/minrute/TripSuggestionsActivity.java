package com.miracleas.minrute;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import com.miracleas.minrute.model.TripRequest;
import com.miracleas.minrute.provider.TripLegMetaData;
import com.miracleas.minrute.provider.TripMetaData;

public class TripSuggestionsActivity extends SherlockFragmentActivity implements TripSuggestionsFragment.Callbacks
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_trip_suggestions);
		// Set up the action bar.
		final ActionBar actionBar = getSupportActionBar();
		// Show the Up button in the action bar.
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		if (savedInstanceState == null)
		{
			Intent intent = getIntent();	
			TripRequest tripRequest = intent.getParcelableExtra(TripRequest.tag);
			// Create the detail fragment and add it to the activity
			// using a fragment transaction.
			TripSuggestionsFragment fragment = TripSuggestionsFragment.createInstance(tripRequest);
			getSupportFragmentManager().beginTransaction().add(R.id.fragmentTripSuggestionsContainer, fragment).commit();
		}

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
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
			NavUtils.navigateUpFromSameTask(this);
			return true;
		
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	@Override
	public void onTripSuggestionSelected(String id, int stepCount, TripRequest tripRequest)
	{
		Intent intent = new Intent(this, TripGuideActivity.class);
		intent.putExtra(TripMetaData.TableMetaData._ID, id);
		intent.putExtra(TripLegMetaData.TableMetaData.STEP_NUMBER, stepCount);	
		intent.putExtra(TripRequest.tag, tripRequest);
		startActivity(intent);
	}
}
