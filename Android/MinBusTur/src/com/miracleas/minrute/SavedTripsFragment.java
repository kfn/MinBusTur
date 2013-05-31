package com.miracleas.minrute;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.miracleas.minrute.model.TripLegStop;
import com.miracleas.minrute.provider.TripLegDetailStopMetaData;

/**
 * A fragment representing a single Ejendom detail screen. This fragment is
 * either contained in a {@link PrisniveauActivity} in two-pane mode (on
 * tablets) or a {@link ToiletDetailActivity} on handsets.
 */
public class SavedTripsFragment extends SherlockFragment implements LoaderCallbacks<Cursor>
{
	public static final String tag = SavedTripsFragment.class.getName();
	
	
	
	
	public static SavedTripsFragment createInstance()
	{
		SavedTripsFragment f = new SavedTripsFragment();
		Bundle args = new Bundle();
		f.setArguments(args);
		return f;
	}
	

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public SavedTripsFragment()
	{
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		//getLoaderManager().initLoader(LoaderConstants.LOADER_TRIP_STOP_DETAILS, getArguments(), this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.fragment_trip_stop_details, container, false);
		
		return rootView;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args)
	{
		if (id == LoaderConstants.LOADER_TRIP_STOP_DETAILS)
		{
			String selection = null;
			String[] selectionArgs = null;		
			TripLegStop stop = args.getParcelable(TripLegStop.tag);
			Uri uri =  Uri.withAppendedPath(TripLegDetailStopMetaData.TableMetaData.CONTENT_URI, stop.id + "");
			return new CursorLoader(getActivity(), uri, null, selection, selectionArgs, null);
		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
	{
		if(loader.getId()==LoaderConstants.LOADER_TRIP_STOP_DETAILS)
		{
			
		}					
	}
	

	


	@Override
	public void onLoaderReset(Loader<Cursor> loader)
	{
		if(loader.getId()==LoaderConstants.LOADER_TRIP_STOP_DETAILS)
		{
			
		}	
	}



}
