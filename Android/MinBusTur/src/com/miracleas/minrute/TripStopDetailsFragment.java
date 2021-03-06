package com.miracleas.minrute;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.miracleas.minrute.model.TripLeg;
import com.miracleas.minrute.model.TripLegStop;
import com.miracleas.minrute.provider.TripLegDetailMetaData;
import com.miracleas.minrute.provider.TripLegDetailStopMetaData;
import com.miracleas.minrute.widget.TableRowKeyValue;

/**
 * A fragment representing a single Ejendom detail screen. This fragment is
 * either contained in a {@link PrisniveauActivity} in two-pane mode (on
 * tablets) or a {@link ToiletDetailActivity} on handsets.
 */
public class TripStopDetailsFragment extends SherlockFragment implements LoaderCallbacks<Cursor>
{
	public static final String tag = TripStopDetailsFragment.class.getName();
	
	private TableLayout mTblLayout = null;
	private TextView mTextViewTitle = null;
	
	
	public static TripStopDetailsFragment createInstance(TripLegStop stop, TripLeg leg)
	{
		TripStopDetailsFragment f = new TripStopDetailsFragment();
		Bundle args = new Bundle();
		args.putParcelable(TripLegStop.tag, stop);
		args.putParcelable(TripLeg.tag, leg);
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
		TripLegStop stop = args.getParcelable(TripLegStop.tag);
		
		if (stop.id!=-1 && id == LoaderConstants.LOADER_TRIP_STOP_DETAILS)
		{
			String selection = null;
			String[] selectionArgs = null;		
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
			if(cursor.moveToFirst())
			{					
				Context c = getActivity();
				int iName = cursor.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.NAME);
				int iArrTime = cursor.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.ARR_TIME);
				int iArrDate = cursor.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.ARR_DATE);
				int iDepTime = cursor.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.DEP_TIME);
				int iDepDate = cursor.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.DEP_DATE);
				int iTrack = cursor.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.TRACK);
				int iRtDepTime = cursor.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.RT_DEP_TIME);
				int iRtDepDate = cursor.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.RT_DEP_DATE);
				int iRtArrTime = cursor.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.RT_ARR_TIME);
				int iRtArrDate = cursor.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.RT_ARR_DATE);
				do
				{
					String transportType = getArguments().getString(TripLegDetailMetaData.TableMetaData.TYPE);
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
