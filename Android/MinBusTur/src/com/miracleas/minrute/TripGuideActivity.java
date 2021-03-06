package com.miracleas.minrute;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import com.miracleas.imagedownloader.IImageDownloader;
import com.miracleas.imagedownloader.ImageFetcher;
import com.miracleas.minrute.model.TripLeg;
import com.miracleas.minrute.model.TripRequest;
import com.miracleas.minrute.provider.SavedTripMetaData;
import com.miracleas.minrute.provider.TripLegMetaData;
import com.miracleas.minrute.provider.TripMetaData;
import com.miracleas.minrute.utils.MyPrefs;

public class TripGuideActivity extends GeofenceActivity implements TripGuideFragment.Callbacks, IImageDownloader, OnSharedPreferenceChangeListener, SaveTripDialogFragment.Callbacks, ChooseLegItemActionDialog.LegItemActionDialogListener
{
	public static final String tag = TripGuideActivity.class.getName();
	private IImageDownloader mImageLoader = null;
    private SaveTripTask mSaveTripTask = null;


    private String mAuth = null;
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
			setAuthTokenHeader(getAuthToken());
			//removeAllGeofences();
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
		getSupportMenuInflater().inflate(R.menu.activity_tripguide, menu);
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
         case R.id.menu_save_trip:
             showConfirmDialog();
             return true;
         case R.id.menu_phone:
             Intent intent = new Intent(Intent.ACTION_DIAL);
             startActivity(intent);
             return true;
		
		default:
			return super.onOptionsItemSelected(item);
		}
	}

    private void showConfirmDialog()
    {
        SherlockDialogFragment newFragment = SaveTripDialogFragment.newInstance();
        newFragment.show(getSupportFragmentManager(), "dialog");
    }

    private void saveTrip(String title)
    {
        if(mSaveTripTask==null || mSaveTripTask.getStatus() == AsyncTask.Status.FINISHED)
        {
            TripRequest tripRequest = getIntent().getParcelableExtra(TripRequest.tag);
            mSaveTripTask = new SaveTripTask(title);
            mSaveTripTask.execute(tripRequest);
        }
    }

    @Override
    public void onOk(String title)
    {
        saveTrip(title);
    }

    @Override
    public void onCancel()
    {

    }

    private class SaveTripTask extends AsyncTask<TripRequest, Void, Uri>
    {
        private String mTitle = null;
        private SaveTripTask(String title)
        {
            mTitle = title;
        }

        @Override
        protected Uri doInBackground(TripRequest... tripRequests)
        {
            TripRequest request = tripRequests[0];
            ContentValues values = new ContentValues();
            values.put(SavedTripMetaData.TableMetaData.DESTINATION_ADDRESS, request.destCoordNameNotEncoded);
            values.put(SavedTripMetaData.TableMetaData.ORIGIN_ADDRESS, request.originCoordNameNotEncoded);
            values.put(SavedTripMetaData.TableMetaData.WAY_POINT_ADDRESS, request.waypointNameNotEncoded);
            values.put(SavedTripMetaData.TableMetaData.DESTINATION_ID, request.getDestId());
            values.put(SavedTripMetaData.TableMetaData.ORIGIN_ID, request.getOriginId());
            values.put(SavedTripMetaData.TableMetaData.WAY_POINT_ID, request.getWayPointId());
            values.put(SavedTripMetaData.TableMetaData.TITLE, mTitle);
            values.put(SavedTripMetaData.TableMetaData.DEST_LNG_X, request.getDestCoordX());
            values.put(SavedTripMetaData.TableMetaData.DESTINATION_LAT_Y, request.getDestCoordY());
            values.put(SavedTripMetaData.TableMetaData.ORIGIN_LAT_Y, request.getOriginCoordY());
            values.put(SavedTripMetaData.TableMetaData.ORIGIN_LNG_X, request.getOriginCoordX());
            values.put(SavedTripMetaData.TableMetaData.SEARCH_FOR_ARRIVAL, request.getSearchForArrival());
            return getContentResolver().insert(SavedTripMetaData.TableMetaData.CONTENT_URI, values);

        }
        @Override
        public void onPostExecute(Uri result)
        {
            if(result!=null)
            {
                Toast.makeText(TripGuideActivity.this, getString(R.string.trip_saved), Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(TripGuideActivity.this, getString(R.string.trip_not_saved), Toast.LENGTH_SHORT).show();
            }
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
	public void onTripLegSelected(TripLeg leg)
	{
		if(!TextUtils.isEmpty(leg.ref))
		{
			Intent activity = new Intent(this, TripLegDetailsActivity.class);
			activity.putExtra(TripLeg.tag, leg);		
			startActivity(activity);
		}		
		else
		{
			
		}
	}
	
	
	@Override
	public void onConnectedServiceVoice()
	{		
		super.onConnectedServiceVoice();
		TripGuideFragment f = (TripGuideFragment)getSupportFragmentManager().findFragmentById(R.id.fragmentTripGuideContainer);
		if(f!=null)
		{
			f.onConnectedService();
		}
	}
	
	@Override
	public void onConnectedServiceLocation()
	{
		mServiceLocation.startLocationListening();
	}
	
	public IImageDownloader getIImageDownloader()
	{
		if (mImageLoader == null)
		{
			final DisplayMetrics displayMetrics = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
			final int height = displayMetrics.heightPixels;
			final int width = displayMetrics.widthPixels;
			mImageLoader = ImageFetcher.getInstance(this, getSupportFragmentManager(), getImgCacheDir(), height, width);
		}
		return mImageLoader;
	}

	public String getImgCacheDir()
	{
		return IImageDownloader.IMAGE_CACHE_DIR;
	}

	@Override
	public void onResume()
	{
		super.onResume();
		setExitTasksEarly(false);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		setExitTasksEarly(true);
		flushCache();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		closeCache();
	}

	@Override
	public void closeCache()
	{

		getIImageDownloader().closeCache();
	}

	@Override
	public void flushCache()
	{

		getIImageDownloader().flushCache();

	}

	@Override
	public void setExitTasksEarly(boolean exitEarly)
	{
		getIImageDownloader().setExitTasksEarly(exitEarly);
	}

	@Override
	public boolean download(String url, ImageView imageView)
	{
		// TODO Auto-generated method stub
		return getIImageDownloader().download(url, imageView);
	}

	@Override
	public void setPauseWork(boolean pause)
	{
		getIImageDownloader().setPauseWork(pause);

	}

	@Override
	public void setLoadingImage(int resId)
	{
		getIImageDownloader().setLoadingImage(resId);

	}

	@Override
	public int getPlaceHolderRessource()
	{
		return getIImageDownloader().getPlaceHolderRessource();
	}

	@Override
	public void setImageFadeIn(boolean fadeIn)
	{
		getIImageDownloader().setImageFadeIn(fadeIn);

	}

	@Override
	public void setImageSize(int size)
	{
		getIImageDownloader().setImageSize(size);

	}

	@Override
	public void cancelMyWork(ImageView imageView)
	{
		getIImageDownloader().cancelMyWork(imageView);

	}

	@Override
	public void setContext(Context c)
	{
		getIImageDownloader().setContext(c);

	}

	@Override
	public void setAuthTokenHeader(String authToken)
	{
		if (authToken != null)
		{
			getIImageDownloader().setAuthTokenHeader(authToken);
		}

	}


	public String getAuthToken()
	{
		if (mAuth == null)
		{
			mAuth = MyPrefs.getString(this, MyPrefs.GOOGLE_DRIVE_AUTH, null);
		}
		return mAuth;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		Log.d(tag, "onSharedPreferenceChanged:"+key);
		if(key.equals(MyPrefs.GOOGLE_DRIVE_AUTH))
		{
			setAuthTokenHeader(key);
		}		
	}

	@Override
	public void onLegItemActionClick(DialogInterface dialog, int which, long itemId, int listPosition)
	{
		ChooseLegItemActionDialog.LegItemActionDialogListener f = (ChooseLegItemActionDialog.LegItemActionDialogListener)getSupportFragmentManager().findFragmentById(R.id.fragmentTripGuideContainer);
		if(f!=null)
		{
			f.onLegItemActionClick(dialog, which, itemId, listPosition);
		}
		
	}

	@Override
	protected void onServerResponse(boolean success)
	{
		// TODO Auto-generated method stub
		
	}


}
