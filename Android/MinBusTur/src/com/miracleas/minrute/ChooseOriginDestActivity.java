package com.miracleas.minrute;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.miracleas.minrute.model.TripRequest;
import com.miracleas.minrute.net.TripFetcher;
import com.miracleas.minrute.service.TripService;

public class ChooseOriginDestActivity extends GeofenceActivity implements ActionBar.TabListener,ChooseOriginDestFragment.Callbacks, android.app.DatePickerDialog.OnDateSetListener, 
							android.app.TimePickerDialog.OnTimeSetListener, com.miracleas.minrute.service.LocationService.OnNewLocationReceivedListener
{
	
	private MenuItem mLocationItem;
	private boolean mLoading = false;
	
	private SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_choose_origin_dest);

		// Set up the action bar.
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setOffscreenPageLimit(2);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener()
		{
			@Override
			public void onPageSelected(int position)
			{
				actionBar.setSelectedNavigationItem(position);	
			}
		});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++)
		{
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
			actionBar.addTab(actionBar.newTab().setText(mSectionsPagerAdapter.getPageTitle(i)).setTabListener(this));
		}
		
		
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
		if (servicesConnected())
		{
			removeSavedGeofences();
		}
		
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
		mSectionsPagerAdapter.getChooseOriginDestFragment().onTimeSet(view, hourOfDay, minute);	
	}
	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
	{
		mSectionsPagerAdapter.getChooseOriginDestFragment().onDateSet(view, year, monthOfYear, dayOfMonth);		
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
		mSectionsPagerAdapter.getChooseOriginDestFragment().setAddress(address);
		mServiceLocation.setOnNewLocationReceived(null);
	}
	@Override
	public void onConnectedServiceVoice()
	{
		if(mBoundVoice)
		{
			mServiceVoice.stopVoices();
		}
		super.onConnectedServiceVoice();		
	}
	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft)
	{
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		int position = tab.getPosition();
		mViewPager.setCurrentItem(position);
		
	}
	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft)
	{
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft)
	{
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentStatePagerAdapter
	{
		private ChooseOriginDestFragment mChooseOriginDestFragment = null;
		private SavedTripsFragment mSavedTrips = null;
		

		public SectionsPagerAdapter(FragmentManager fm)
		{
			super(fm);
		}

		@Override
		public Fragment getItem(int position)
		{
			Fragment f = null;
			if (position == 0)
			{
				f = getChooseOriginDestFragment();
			} 
			else if (position == 1)
			{
				if (mSavedTrips == null)
				{
					mSavedTrips = SavedTripsFragment.createInstance();
				}
				f = mSavedTrips;
			} 

			return f;
		}

		@Override
		public int getCount()
		{
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position)
		{
			switch (position)
			{
			case 0:
				return getString(R.string.title_section1).toUpperCase();
			case 1:
				return getString(R.string.title_section2).toUpperCase();
			}
			return null;
		}
		
		public ChooseOriginDestFragment getChooseOriginDestFragment()
		{
			if (mChooseOriginDestFragment == null)
			{
				mChooseOriginDestFragment = ChooseOriginDestFragment.createInstance();
			}
			return mChooseOriginDestFragment;
		}
	}

}
