package com.miracleas.minbustur;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.miracleas.minbustur.provider.TripMetaData;
import com.miracleas.minbustur.provider.TripMetaData.TableMetaData;

public class TripSuggestionsFragment extends SherlockListFragment implements LoaderCallbacks<Cursor>, OnItemClickListener
{
	private static final String[] PROJECTION = { TripMetaData.TableMetaData._ID, 
				TableMetaData.DURATION_LABEL,
				TableMetaData.LEG_COUNT,
				TableMetaData.LEG_NAMES,
				TableMetaData.LEG_TYPES,
				TableMetaData.TRANSPORT_CHANGES,
				TableMetaData.DEPATURE_TIME,
				TableMetaData.DEPATURES_IN_TIME_LABEL ,
				TableMetaData.ARRIVAL_TIME,
				TableMetaData.DURATION_WALK,
				TableMetaData.DURATION_BUS,
				TableMetaData.DURATION_TRAIN,
				TableMetaData.ARRIVAL_TIME,
				TableMetaData.ARRIVES_IN_TIME_LABEL};
	private TripAdapter mTripAdapter = null;
	
	
	
	public static TripSuggestionsFragment createInstance()
	{
		TripSuggestionsFragment f = new TripSuggestionsFragment();
		Bundle args = new Bundle();
		f.setArguments(args);
		return f;
	}
	
	public TripSuggestionsFragment(){}
		
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View listView = super.onCreateView(inflater, container, savedInstanceState);
		View rootView = inflater.inflate(R.layout.fragment_trip_suggestions, container, false);
		FrameLayout frame = (FrameLayout)rootView.findViewById(R.id.listContainer);
		frame.addView(listView);
		return rootView;
		
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		mTripAdapter = new TripAdapter(getActivity(), null, 0);
		setListAdapter(mTripAdapter);
		getLoaderManager().initLoader(LoaderConstants.LOAD_TRIP_SUGGESTIONS, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1)
	{		
		return new CursorLoader(getActivity(), TripMetaData.TableMetaData.CONTENT_URI, PROJECTION, null, null, TripMetaData.TableMetaData.DEPATURES_IN_TIME+","+TripMetaData.TableMetaData.DURATION);
	}


	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor newCursor)
	{
		mTripAdapter.swapCursor(newCursor);		
	}


	@Override
	public void onLoaderReset(Loader<Cursor> loader)
	{
		mTripAdapter.swapCursor(null);			
	}
	
	private class TripAdapter extends CursorAdapter
	{
		private int iArrivalTime;
		private int iArrivalInTime;
		private int iDepatureTime;
		private int iDepatureInTime;
		private int iDuration;
		private int iLegCount;
		private int iLegNames;
		private int iLegTypes;
		private int iTransportChanges;
		private int iDurationBus;
		private int iDurationWalk;
		private int iDurationIC;
		private int iDurationTrain;
		
		private LayoutInflater mInf = null;

		public TripAdapter(Context context, Cursor c, int flags)
		{
			super(context, c, flags);
			mInf = LayoutInflater.from(context);
		}

		@Override
		public void bindView(View v, Context context, Cursor cursor)
		{
			TextView textViewDepatureIn = (TextView)v.findViewById(R.id.textViewDepatureIn);
			TextView textViewDepatureTime = (TextView)v.findViewById(R.id.textViewDepatureTime);
			TextView textViewDuration = (TextView)v.findViewById(R.id.textViewDuration);
			TextView textViewArrivalAt = (TextView)v.findViewById(R.id.textViewArrivalAt);
			TextView textViewArrivalTime = (TextView)v.findViewById(R.id.textViewArrivalTime);
			TextView textViewTransport = (TextView)v.findViewById(R.id.textViewTransport);			
			textViewDepatureTime.setText(String.format(getString(R.string.parenthese_value), cursor.getString(iDepatureTime)));
			textViewDepatureIn.setText(String.format(getString(R.string.departure), cursor.getString(iDepatureInTime)));
			textViewDuration.setText(String.format(getString(R.string.duration), cursor.getString(iDuration)));
			textViewArrivalTime.setText(String.format(getString(R.string.parenthese_value), cursor.getString(iArrivalTime)));
			textViewArrivalAt.setText(String.format(getString(R.string.arrival_within), cursor.getString(iArrivalInTime)));
			textViewTransport.setText(cursor.getString(iLegNames));	
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			return mInf.inflate(R.layout.item_trip_suggestion, null);
		}

		public Cursor swapCursor(Cursor newCursor)
		{
			if (newCursor != null)
			{
				iDuration = newCursor.getColumnIndex(TripMetaData.TableMetaData.DURATION_LABEL);
				iArrivalTime = newCursor.getColumnIndex(TripMetaData.TableMetaData.ARRIVAL_TIME);
				iLegCount = newCursor.getColumnIndex(TripMetaData.TableMetaData.LEG_COUNT);
				iLegNames = newCursor.getColumnIndex(TripMetaData.TableMetaData.LEG_NAMES);
				iLegTypes = newCursor.getColumnIndex(TripMetaData.TableMetaData.LEG_TYPES);
				iTransportChanges = newCursor.getColumnIndex(TripMetaData.TableMetaData.TRANSPORT_CHANGES);				
				iDepatureTime = newCursor.getColumnIndex(TripMetaData.TableMetaData.DEPATURE_TIME);
				iDepatureInTime = newCursor.getColumnIndex(TripMetaData.TableMetaData.DEPATURES_IN_TIME_LABEL);
				iArrivalTime = newCursor.getColumnIndex(TripMetaData.TableMetaData.ARRIVAL_TIME);
				iArrivalInTime = newCursor.getColumnIndex(TripMetaData.TableMetaData.ARRIVES_IN_TIME_LABEL);	
				iDurationBus = newCursor.getColumnIndex(TripMetaData.TableMetaData.DURATION_BUS);
				iDurationWalk = newCursor.getColumnIndex(TripMetaData.TableMetaData.DURATION_WALK);
				iDurationTrain = newCursor.getColumnIndex(TripMetaData.TableMetaData.DURATION_TRAIN);
				iDurationBus = newCursor.getColumnIndex(TripMetaData.TableMetaData.DURATION_BUS);
	
			}
			return super.swapCursor(newCursor);
		}
	}
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
	{
		// TODO Auto-generated method stub
		
	}

}