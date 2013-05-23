package com.miracleas.minbustur;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.miracleas.minbustur.provider.JourneyDetailMetaData;
import com.miracleas.minbustur.provider.JourneyDetailStopMetaData;
import com.miracleas.minbustur.provider.JourneyDetailStopMetaData.TableMetaData;
import com.miracleas.minbustur.widget.TableRowKeyValue;

/**
 * A fragment representing a single Ejendom detail screen. This fragment is
 * either contained in a {@link PrisniveauActivity} in two-pane mode (on
 * tablets) or a {@link ToiletDetailActivity} on handsets.
 */
public class TripStopDetailsFragment extends SherlockFragment implements LoaderCallbacks<Cursor>
{
	public static final String tag = TripStopDetailsFragment.class.getName();
	
	private static final int LOAD_TOILET = 1;
	
	private TableLayout mTblLayout = null;
	private TextView mTextViewTitle = null;
	
	public static TripStopDetailsFragment createInstance(String stopId, String lat, String lng, String transportType)
	{
		TripStopDetailsFragment f = new TripStopDetailsFragment();
		Bundle args = new Bundle();
		args.putString(JourneyDetailStopMetaData.TableMetaData._ID, stopId);
		args.putString(JourneyDetailStopMetaData.TableMetaData.LATITUDE, lat);
		args.putString(JourneyDetailStopMetaData.TableMetaData.LONGITUDE, lng);
		args.putString(JourneyDetailMetaData.TableMetaData.TYPE, transportType);
		f.setArguments(args);
		return f;
	}
	

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public TripStopDetailsFragment()
	{
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getLoaderManager().initLoader(LoaderConstants.LOADER_TRIP_STOP_DETAILS, getArguments(), this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.fragment_trip_stop_details, container, false);
		mTblLayout = (TableLayout)rootView.findViewById(R.id.tblTripStopDetails);
		mTextViewTitle = (TextView)rootView.findViewById(R.id.textViewTitle);
		return rootView;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args)
	{
		if (id == LoaderConstants.LOADER_TRIP_STOP_DETAILS)
		{
			String selection = null;
			String[] selectionArgs = null;				
			Uri uri =  Uri.withAppendedPath(JourneyDetailStopMetaData.TableMetaData.CONTENT_URI, args.getString(JourneyDetailStopMetaData.TableMetaData._ID));
			return new CursorLoader(getActivity(), uri, null, selection, selectionArgs, null);
		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
	{
		if(loader.getId()==LoaderConstants.LOADER_TRIP_STOP_DETAILS)
		{
			if(cursor.moveToFirst())
			{					
				Context c = getActivity();
				int iName = cursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.NAME);
				int iArrTime = cursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.ARR_TIME);
				int iArrDate = cursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.ARR_DATE);
				int iDepTime = cursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.DEP_TIME);
				int iDepDate = cursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.DEP_DATE);
				int iTrack = cursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.TRACK);
				int iRtDepTime = cursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.RT_DEP_TIME);
				int iRtDepDate = cursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.RT_DEP_DATE);
				int iRtArrTime = cursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.RT_ARR_TIME);
				int iRtArrDate = cursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.RT_ARR_DATE);
				do
				{
					String transportType = getArguments().getString(JourneyDetailMetaData.TableMetaData.TYPE);
					String name = cursor.getString(iName);
					mTextViewTitle.setText(name);
					addKeyValue(getString(R.string.transport_type), transportType, c);
					addKeyValue(getString(R.string.arrDateTime), cursor.getString(iArrTime), c);
					addKeyValue(getString(R.string.depDateTime), cursor.getString(iDepTime), c);
					addKeyValue(getString(R.string.track), cursor.getString(iTrack), c);
					addKeyValue(getString(R.string.rtArrTime), cursor.getString(iRtArrTime), c);
					addKeyValue(getString(R.string.rtDepTime), cursor.getString(iRtDepTime), c);
				}
				while(cursor.moveToNext());
				getLoaderManager().destroyLoader(LoaderConstants.LOADER_TRIP_STOP_DETAILS);
			}
		}					
	}
	
	private void addKeyValue(String key, String value, Context c)
	{
		if(!TextUtils.isEmpty(value))
		{
			TableRowKeyValue row = new TableRowKeyValue(c);
			row.setKey(key);
			row.setValue(value);
			mTblLayout.addView(row);
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
