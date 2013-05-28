package com.miracleas.minrute;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.miracleas.minrute.model.TripRequest;
import com.miracleas.minrute.net.TripFetcher;
import com.miracleas.minrute.service.TripService;

public class ChooseOriginDestActivity extends GeofenceActivity implements ChooseOriginDestFragment.Callbacks, android.app.DatePickerDialog.OnDateSetListener, 
							android.app.TimePickerDialog.OnTimeSetListener, com.miracleas.minrute.service.LocationService.OnNewLocationReceivedListener
{
	
	private MenuItem mLocationItem;
	private boolean mLoading = false;
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_choose_origin_dest);

		// Set up the action bar.
		final ActionBar actionBar = getSupportActionBar();
		// Show the Up button in the action bar.
		//actionBar.setDisplayHomeAsUpEnabled(true);
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

		/*if (savedInstanceState == null)
		{
			Intent intent = getIntent();
			// Create the detail fragment and add it to the activity
			// using a fragment transaction.
			CreateRouteFragment fragment = CreateRouteFragment.createInstance();
			getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment).commit();
		}*/
		
		
	}
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putBoolean("loading", mLoading);
		
	}
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
		mLoading = savedInstanceState.getBoolean("loading");
	}
	
	
	@Override
	public void onStart()
	{
		super.onStart();
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		mLocationItem = menu.findItem(R.id.menu_my_location);
		if (mLoading)
		{
			mLocationItem.setActionView(R.layout.refresh_menuitem);
		}
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.activity_choose_origin_dest, menu);
		return true;
		//return super.onCreateOptionsMenu(menu);
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
		case R.id.menu_my_location:
			refreshData(item);
			return true;
		
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	/**
	 * if not already loading, refresh the data for the selected content
	 * (mSelectedMenuItem). show loading spinner instead of refresh button.
	 * 
	 * @param item
	 */
	private void refreshData(MenuItem item)
	{
		if (!mLoading && mBoundLocation)
		{
			mServiceLocation.setOnNewLocationReceived(this);
			mServiceLocation.startLocationListening();
			mLocationItem = item;
			mLoading = true;
			showLoadSpinner();

		}
	}
	
	private void showLoadSpinner()
	{
		if (mLocationItem != null)
		{
			if (mLoading)
			{
				mLocationItem.setActionView(R.layout.refresh_menuitem);
			} else
			{
				mLocationItem.setActionView(null);
			}
		}
	}
	
	@Override
	public void onFindTripSuggestion(TripRequest tripRequest)
	{
		if (tripRequest.isValid())
		{
			Intent service = new Intent(this, TripService.class);
			service.putExtra(TripFetcher.TRIP_REQUEST, tripRequest);
			startService(service);
			Intent activity = new Intent(this, TripSuggestionsActivity.class);
			activity.putExtra(TripRequest.tag, tripRequest);
			startActivity(activity);
		} else
		{
			Toast.makeText(this, "Not valid", Toast.LENGTH_SHORT).show();
		}	
	}
	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute)
	{
		Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragmentEnterAddresses);
		if(f!=null)
		{
			((ChooseOriginDestFragment)f).onTimeSet(view, hourOfDay, minute);
		}
		
	}
	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
	{
		Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragmentEnterAddresses);
		if(f!=null)
		{
			((ChooseOriginDestFragment)f).onDateSet(view, year, monthOfYear, dayOfMonth);
		}
		
	}
	@Override
	public void onNewLocationReceived(Location loc)
	{		
		if(mBoundLocation)
		{
			mServiceLocation.stopLocationListening();
			mServiceLocation.geocode(loc);
		}
		else
		{
			mLoading = false;
			showLoadSpinner();			
		}
		
	}
	@Override
	public void onAddressGeocoded(String address)
	{
		mLoading = false;
		showLoadSpinner();
		Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragmentEnterAddresses);
		if(f!=null)
		{					
			((ChooseOriginDestFragment)f).setAddress(address);
		}
		mServiceLocation.setOnNewLocationReceived(null);
	}

}
