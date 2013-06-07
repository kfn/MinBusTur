package com.miracleas.minrute;

import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.miracleas.minrute.model.TripRequest;
import com.miracleas.minrute.net.TripFetcher;
import com.miracleas.minrute.service.TripService;

public class ChooseOriginDestActivity extends GeofenceActivity implements ChooseOriginDestFragment.Callbacks, android.app.DatePickerDialog.OnDateSetListener, android.app.TimePickerDialog.OnTimeSetListener,
		com.miracleas.minrute.service.LocationService.OnNewLocationReceivedListener, OnNavigationListener, ChooseDestinationDialog.NoticeDialogListener, SavedTripsFragment.Callbacks
{
	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * current dropdown position.
	 */
	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
	private MenuItem mLocationItem;
	private boolean mIsLoadingMyLocation = false;

	private ChooseOriginDestFragment mChooseOriginDestFragment = null;
	private SavedTripsFragment mSavedTripsFragment = null;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Set up the action bar.
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		createDropDown(actionBar);

	}

	private void createDropDown(ActionBar actionBar)
	{
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		// Set up the dropdown list navigation in the action bar.
		actionBar.setListNavigationCallbacks(
		// Specify a SpinnerAdapter to populate the dropdown list.
				new ArrayAdapter<String>(getSupportActionBar().getThemedContext(), android.R.layout.simple_list_item_1, android.R.id.text1, new String[] { getString(R.string.title_section1), getString(R.string.title_section2) }), this);
	}

	

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState)
	{
		// Restore the previously serialized current dropdown position.
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM))
		{
			getSupportActionBar().setSelectedNavigationItem(savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
			mIsLoadingMyLocation = savedInstanceState.getBoolean("loading");
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		// Serialize the current dropdown position.
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getSupportActionBar().getSelectedNavigationIndex());
		outState.putBoolean("loading", mIsLoadingMyLocation);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.main, menu);
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
			//NavUtils.navigateUpFromSameTask(this);

			return true;
         case R.id.menu_settings:
             Intent intent = new Intent(this, SettingsActivity.class);
             startActivity(intent);
             return true;
		
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onStart()
	{
		super.onStart();

        if(mBoundLocation && !mIsLoadingMyLocation)
        {
            mServiceLocation.stopLocationListening();
        }
		if (isGoogleServiceConnected())
		{
			removeSavedGeofences();
		}

	}


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        getChooseOriginDestFragment().onActivityResult(requestCode, resultCode, data);
    }

    public void findMyLocation()
    {
    	findMyLocation(mLocationItem);

    }

	/**
	 * if not already loading, refresh the data for the selected content
	 * (mSelectedMenuItem). show loading spinner instead of refresh button.
	 * 
	 * @param item
	 */
	private void findMyLocation(MenuItem item)
	{
		if (!mIsLoadingMyLocation && mBoundLocation)
		{
			mServiceLocation.setOnNewLocationReceived(this);
			mServiceLocation.startLocationListening();
			//mLocationItem = item;
			mIsLoadingMyLocation = true;
			showLoadSpinner();

		}
	}

	private void showLoadSpinner()
	{
		if (mLocationItem != null)
		{
			if (mIsLoadingMyLocation)
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
	public void onNewLocationReceived(Location loc)
	{
		if (mBoundLocation)
		{
			mServiceLocation.stopLocationListening();
			mServiceLocation.geocode(loc);
		} else
		{
			mIsLoadingMyLocation = false;
			showLoadSpinner();
		}

	}

	@Override
	public void onAddressGeocoded(String address)
	{
		mIsLoadingMyLocation = false;
		showLoadSpinner();
		getChooseOriginDestFragment().setAddress(address);
		mServiceLocation.setOnNewLocationReceived(null);
	}

	@Override
	public void onConnectedServiceVoice()
	{
        Log.d(tag, "onConnectedServiceVoice");
		if (mBoundVoice)
		{
            Log.d(tag, "stop voice");
            mServiceVoice.stopVoices();
		}
		super.onConnectedServiceVoice();
	}
	
	private ChooseOriginDestFragment getChooseOriginDestFragment()
	{
		if (mChooseOriginDestFragment == null)
		{
            mChooseOriginDestFragment = (ChooseOriginDestFragment)getSupportFragmentManager().findFragmentByTag(ChooseOriginDestFragment.tag);
            if(mChooseOriginDestFragment==null)
            {
                mChooseOriginDestFragment = ChooseOriginDestFragment.createInstance();
            }
		}
		return mChooseOriginDestFragment;
	}
	
	private SavedTripsFragment getSavedTripsFragment()
	{
		if (mSavedTripsFragment == null)
		{
            mSavedTripsFragment = (SavedTripsFragment)getSupportFragmentManager().findFragmentByTag(SavedTripsFragment.tag);
            if(mSavedTripsFragment==null)
            {
                mSavedTripsFragment = SavedTripsFragment.createInstance();
            }
		}
		return mSavedTripsFragment;
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId)
	{
		// When the given dropdown item is selected, show its contents in the
		// container view.
		Fragment fragment = null;
        String tag = null;
		if(itemPosition==0)
		{
			fragment = getChooseOriginDestFragment();
            tag = ChooseOriginDestFragment.tag;
		}
		else
		{
			fragment = getSavedTripsFragment();
            tag = SavedTripsFragment.tag;
		}
		
		getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment, tag).commit();
		return true;
	}

	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute)
	{
		getChooseOriginDestFragment().onTimeSet(view, hourOfDay, minute);
		
	}

	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
	{
		getChooseOriginDestFragment().onDateSet(view, year, monthOfYear, dayOfMonth);		
	}

    @Override
    public void onDialogLocationTypeClick(DialogInterface dialog, int which, String tag)
    {
        getChooseOriginDestFragment().onDialogLocationTypeClick(dialog, which, tag);
    }

	@Override
	public void onSavedTripSelected(TripRequest tripRequest)
	{
		onFindTripSuggestion(tripRequest);		
	}

	@Override
	protected void onServerResponse(boolean success)
	{
		// TODO Auto-generated method stub
		
	}
	@Override
	protected void showDialogInstallVoiceSupport()
	{
		SettingsActivity.setIsVoiceOn(this, false);
		ConfirmDialogFragment dialog = ConfirmDialogFragment.newInstance(R.string.confirm_install_voice);
		dialog.show(getSupportFragmentManager(), "ConfirmVoiceInstall");
	}
	
	@Override
    public void doPositiveClick()
    {
		SettingsActivity.setLanguage(this, getString(R.string.pref_voice_language_english_key));
		showDialogInstallVoiceSupportHelper();
    }
}
