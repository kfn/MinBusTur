package com.miracleas.minrute;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import com.miracleas.minrute.model.TripLeg;
import com.miracleas.minrute.model.TripLegStop;
import com.miracleas.minrute.model.TripRequest;
import com.miracleas.minrute.provider.TripLegDetailMetaData;
import com.miracleas.minrute.provider.StopImagesMetaData;
import com.miracleas.minrute.provider.TripLegDetailStopMetaData;
import com.miracleas.minrute.provider.TripLegMetaData;

public class TripLegDetailsActivity extends GeofenceActivity implements TripLegDetailsFragment.Callbacks, TripLegMapFragment.Callbacks
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
			showList();
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
			//finish();
			//NavUtils.navigateUpFromSameTask(this);
			NavUtils.navigateUpTo(this, getIntent());
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
				/*Intent activity = new Intent(this, TripLegMapActivity.class);
				activity.putExtra(JourneyDetailMetaData.TableMetaData._ID, mJourneyId);
				activity.putExtra(JourneyDetailMetaData.TableMetaData.LEG_ID, mLegId);
				activity.putExtra(TripLegMetaData.TableMetaData.TYPE, mTransportType);
				startActivity(activity);*/
				Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragmentTripLegDetailsContainer);
				if(f instanceof TripLegDetailsFragment)
				{
					showMap();
				}
				else if(f instanceof TripLegMapFragment)
				{
					showList();
				}								
			}		
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void showList()
	{
		Intent intent = getIntent();
		/*String tripId = intent.getStringExtra(JourneyDetailMetaData.TableMetaData.TRIP_ID);
		String legId = intent.getStringExtra(JourneyDetailMetaData.TableMetaData.LEG_ID);
		String ref = intent.getStringExtra(JourneyDetailMetaData.TableMetaData.REF);
		String transportType = intent.getStringExtra(TripLegMetaData.TableMetaData.TYPE);
		String nameOfLocation = intent.getStringExtra(TripLegMetaData.TableMetaData.ORIGIN_NAME);*/
		TripLeg leg = intent.getParcelableExtra(TripLeg.tag);
		TripLegDetailsFragment fragment = TripLegDetailsFragment.createInstance(leg);
		getSupportFragmentManager().beginTransaction().replace(R.id.fragmentTripLegDetailsContainer, fragment).commit();
	}
	
	private void showMap()
	{
		Intent intent = getIntent();
		/*String legId = intent.getStringExtra(JourneyDetailMetaData.TableMetaData.LEG_ID);
		String transportType = intent.getStringExtra(TripLegMetaData.TableMetaData.TYPE);*/
		
		TripLeg leg = intent.getParcelableExtra(TripLeg.tag);
		TripLegMapFragment fragment = TripLegMapFragment.createInstance(mJourneyId+"", leg);
		getSupportFragmentManager().beginTransaction().replace(R.id.fragmentTripLegDetailsContainer, fragment).commit();
	}

	@Override
	public void onStopSelected(TripLegStop stop, TripLeg leg)
	{
		Intent activity = new Intent(this, TripStopDetailsActivity.class);
		activity.putExtra(TripLegStop.tag, stop);
		activity.putExtra(TripLeg.tag, leg);
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
