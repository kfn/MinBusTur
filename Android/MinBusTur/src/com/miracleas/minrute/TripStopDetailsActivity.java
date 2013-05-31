package com.miracleas.minrute;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.miracleas.camera.PhotoGoogleDriveActivity;
import com.miracleas.camera.PhotoIntentActivity;
import com.miracleas.imagedownloader.IImageDownloader;
import com.miracleas.imagedownloader.ImageDownloaderActivity;
import com.miracleas.imagedownloader.ImageFetcher;

import com.miracleas.minrute.model.NearbyLocationRequest;
import com.miracleas.minrute.model.TripLeg;
import com.miracleas.minrute.model.TripLegStop;
import com.miracleas.minrute.provider.TripLegDetailMetaData;
import com.miracleas.minrute.provider.StopImagesMetaData;
import com.miracleas.minrute.provider.TripLegDetailStopMetaData;

public class TripStopDetailsActivity extends PhotoGoogleDriveActivity implements com.actionbarsherlock.app.ActionBar.TabListener
{

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	private ViewPager mViewPager;
	private TripLeg mTripLeg = null;
	private TripLegStop mTripLegStop = null;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_toilet_details);

		// Set up the action bar.
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		// Show the Up button in the action bar.
		actionBar.setDisplayHomeAsUpEnabled(true);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.setOffscreenPageLimit(3);

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
		Intent intent = getIntent();
		mTripLegStop = intent.getParcelableExtra(TripLegStop.tag);
		mTripLeg = intent.getParcelableExtra(TripLeg.tag);		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inf = getSupportMenuInflater();
		inf.inflate(R.menu.activity_details, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		boolean handled = false;

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
			// NavUtils.navigateUpFromSameTask(this);
			//finish();
			//NavUtils.navigateUpFromSameTask(this);
			NavUtils.navigateUpTo(this, getIntent());
			return true;
		case R.id.menu_direction:
			handled = true;
			navigateToDirectionMap();
			break;
		case R.id.menu_streetview:
			handled = true;
			showStreetView(mTripLegStop.lat, mTripLegStop.lng);
			break;

		default:
			return super.onOptionsItemSelected(item);
		}

		return handled;

	}

	private void showStreetView(String latitude, String longitude)
	{
		double lat = (double)(Integer.parseInt(latitude)) / 1000000d;
		double lng = (double)(Integer.parseInt(longitude)) / 1000000d;
		String uri = "google.streetview:cbll=" + lat + "," + lng + "&cbp=1,99.56,,1,-5.27&mz=21";
		Intent streetView = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri));
		startActivity(streetView);
	}

	@Override
	public void onTabSelected(Tab tab, android.support.v4.app.FragmentTransaction ft)
	{
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());

	}

	@Override
	public void onTabUnselected(Tab tab, android.support.v4.app.FragmentTransaction ft)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onTabReselected(Tab tab, android.support.v4.app.FragmentTransaction ft)
	{
		// TODO Auto-generated method stub

	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter
	{

		public SectionsPagerAdapter(FragmentManager fm)
		{
			super(fm);
		}

		@Override
		public Fragment getItem(int position)
		{

			// getItem is called to instantiate the fragment for the given page.
			// Return a DummySectionFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			Fragment fragment = null;
			if (position == 0)
			{
				
				String id = getIntent().getStringExtra(TripLegDetailStopMetaData.TableMetaData._ID);
				fragment = TripStopDetailsFragment.createInstance(mTripLegStop, mTripLeg);
			}
			else if (position == 1)
			{
				fragment = TripStopDetailsImagesFragment.createInstance(mTripLegStop, mTripLeg);
			}
			else if (position == 2)
			{
				fragment = TripStopDetailsDepartureBoardFragment.createInstance(mTripLegStop, mTripLeg);
			} 
			return fragment;
		}

		@Override
		public int getCount()
		{
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position)
		{
			switch (position)
			{
			case 0:
				return getString(R.string.title_section_tripstop_details).toUpperCase();
			case 1:
				return getString(R.string.title_section_tripstop_images).toUpperCase();
			case 2:
				return getString(R.string.title_section_tripstop_departures).toUpperCase();
			}
		
		
			return null;
		}
	}



	public void navigateToDirectionMap()
	{
		if (mTripLegStop!=null)
		{
			double lat = (double)(Integer.parseInt(mTripLegStop.lat)) / 1000000d;
			double lng = (double)(Integer.parseInt(mTripLegStop.lng)) / 1000000d;
			String uriString = "http://maps.google.com/maps?daddr=" +lat + "," + lng + "&view=map";
			Uri uri = Uri.parse(uriString);
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(intent);
		}
	}
	


}
