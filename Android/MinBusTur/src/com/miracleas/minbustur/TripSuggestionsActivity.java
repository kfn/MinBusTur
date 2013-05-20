package com.miracleas.minbustur;

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.miracleas.minbustur.provider.TripLegMetaData;

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

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onTripSuggestionSelected(String id, int stepCount)
	{
		Intent intent = new Intent(this, TripGuideActivity.class);
		intent.putExtra(TripLegMetaData.TableMetaData._ID, id);
		intent.putExtra(TripLegMetaData.TableMetaData.STEP_NUMBER, stepCount);	
		startActivity(intent);
	}
}
