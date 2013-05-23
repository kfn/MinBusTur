package com.miracleas.minbustur;

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.miracleas.minbustur.model.TripRequest;
import com.miracleas.minbustur.provider.JourneyDetailMetaData;
import com.miracleas.minbustur.provider.JourneyDetailStopMetaData;
import com.miracleas.minbustur.provider.TripLegMetaData;

public class TripLegDetailsActivity extends GeofenceActivity implements TripLegDetailsFragment.Callbacks
{
	private long mJourneyId = -1;
	private String mLegId = null;
	private String mTransportType = null;
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_trip_leg_details);

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
			String tripId = intent.getStringExtra(JourneyDetailMetaData.TableMetaData.TRIP_ID);
			String legId = intent.getStringExtra(JourneyDetailMetaData.TableMetaData.LEG_ID);
			String ref = intent.getStringExtra(JourneyDetailMetaData.TableMetaData.REF);
			String transportType = intent.getStringExtra(TripLegMetaData.TableMetaData.TYPE);
			TripLegDetailsFragment fragment = TripLegDetailsFragment.createInstance(tripId, legId, ref, transportType);
			getSupportFragmentManager().beginTransaction().add(R.id.fragmentTripLegDetailsContainer, fragment).commit();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.activity_trip_leg_details, menu);
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
		case R.id.menu_notes:
			if(mJourneyId!=-1)
			{
				TripLegDetailNotesDialog.show(this, mJourneyId);
			}		
			return true;
		case R.id.menu_map:
			if(mJourneyId!=-1)
			{
				Intent activity = new Intent(this, TripLegMapActivity.class);
				activity.putExtra(JourneyDetailMetaData.TableMetaData._ID, mJourneyId);
				activity.putExtra(JourneyDetailMetaData.TableMetaData.LEG_ID, mLegId);
				activity.putExtra(TripLegMetaData.TableMetaData.TYPE, mTransportType);
				startActivity(activity);
			}		
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onStopSelected(String stopId, String lat, String lng, String transportType)
	{
		Intent activity = new Intent(this, TripStopDetailsActivity.class);
		activity.putExtra(JourneyDetailStopMetaData.TableMetaData.LATITUDE, lat);
		activity.putExtra(JourneyDetailStopMetaData.TableMetaData.LONGITUDE, lng);
		activity.putExtra(JourneyDetailStopMetaData.TableMetaData._ID, stopId);
		activity.putExtra(JourneyDetailMetaData.TableMetaData.TYPE, transportType);
		startActivity(activity);
		
	}

	@Override
	public void setJourneyDetailId(long id, String legId, String transportType)
	{
		mJourneyId = id;		
		mLegId = legId;
		mTransportType = transportType;
	}
}
