package com.miracleas.minbustur;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.miracleas.minbustur.model.NearbyLocationRequest;
import com.miracleas.minbustur.provider.JourneyDetailStopDeparturesMetaData;
import com.miracleas.minbustur.provider.JourneyDetailStopMetaData;
import com.miracleas.minbustur.service.DepartureBoardsService;

public class TripStopDetailsDepartureBoardFragment extends SherlockFragment implements LoaderCallbacks<Cursor>, OnItemClickListener, OnClickListener
{
	private static final String[] PROJECTION_DEPARTURE = { 
		JourneyDetailStopDeparturesMetaData.TableMetaData._ID, 
		JourneyDetailStopDeparturesMetaData.TableMetaData.DATE,
		JourneyDetailStopDeparturesMetaData.TableMetaData.DIRECTION,
		JourneyDetailStopDeparturesMetaData.TableMetaData.NAME,
		JourneyDetailStopDeparturesMetaData.TableMetaData.REF,
		JourneyDetailStopDeparturesMetaData.TableMetaData.STOP,
		JourneyDetailStopDeparturesMetaData.TableMetaData.STOP_ID,
		JourneyDetailStopDeparturesMetaData.TableMetaData.STOP_SEARCH_ID,
		JourneyDetailStopDeparturesMetaData.TableMetaData.TIME,
		JourneyDetailStopDeparturesMetaData.TableMetaData.TYPE,
	};


	private TripAdapter mTripAdapter = null;
	private ListView mListView = null;
	private NearbyLocationRequest mNearbyLocationRequest = null;
	private View mLoadingView = null;
	private Button mBtnFetchDepartures = null;
	
	public static TripStopDetailsDepartureBoardFragment createInstance(String stopId, String lat, String lng)
	{
		TripStopDetailsDepartureBoardFragment f = new TripStopDetailsDepartureBoardFragment();
		Bundle args = new Bundle();
		args.putString(JourneyDetailStopMetaData.TableMetaData._ID, stopId);
		args.putString(JourneyDetailStopMetaData.TableMetaData.LATITUDE, lat);
		args.putString(JourneyDetailStopMetaData.TableMetaData.LONGITUDE, lng);
		f.setArguments(args);
		return f;
	}
	
	public TripStopDetailsDepartureBoardFragment(){}
		
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.fragment_trip_stop_details_departures, container, false);
		mBtnFetchDepartures = (Button)v.findViewById(R.id.btnFetchDepartures);
		mBtnFetchDepartures.setOnClickListener(this);
		mListView = (ListView)v.findViewById(android.R.id.list);
		
		return v;
		
	}
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);	
		mTripAdapter = new TripAdapter(getActivity(), null, 0);
		mListView.setOnItemClickListener(this);
		mListView.setAdapter(mTripAdapter);		
		
		Bundle args = getArguments();
		mNearbyLocationRequest = new NearbyLocationRequest();
		mNearbyLocationRequest.stopId = args.getString(JourneyDetailStopMetaData.TableMetaData._ID);
		mNearbyLocationRequest.coordX = args.getString(JourneyDetailStopMetaData.TableMetaData.LONGITUDE);
		mNearbyLocationRequest.coordY = args.getString(JourneyDetailStopMetaData.TableMetaData.LATITUDE);
		
		getLoaderManager().initLoader(LoaderConstants.LOADER_STOP_DEPARTURES, getArguments(), this);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);		
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args)
	{		
		if(id==LoaderConstants.LOADER_STOP_DEPARTURES)
		{
			String selection = JourneyDetailStopDeparturesMetaData.TableMetaData.STOP_ID + "=?";
			String[] selectionArgs = {args.getString(JourneyDetailStopMetaData.TableMetaData._ID)};
			return new CursorLoader(getActivity(), JourneyDetailStopDeparturesMetaData.TableMetaData.CONTENT_URI, PROJECTION_DEPARTURE, selection, selectionArgs, null);
		}

		return null;
	}


	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor newCursor)
	{
		int id = loader.getId();
		if(id==LoaderConstants.LOADER_STOP_DEPARTURES)
		{
			mTripAdapter.swapCursor(newCursor);	
		}
	}


	@Override
	public void onLoaderReset(Loader<Cursor> loader)
	{
		mTripAdapter.swapCursor(null);			
	}
	
	private class TripAdapter extends CursorAdapter
	{
		private int iDate;
		private int iDirection;
		private int iRef;
		private int iStop;
		private int iName;
		private int iTime;
		private int iType;
		
		private LayoutInflater mInf = null;

		public TripAdapter(Context context, Cursor c, int flags)
		{
			super(context, c, flags);
			mInf = LayoutInflater.from(context);
		}

		@Override
		public void bindView(View v, Context context, Cursor cursor)
		{
			TextView textViewTime = (TextView)v.findViewById(R.id.textViewTime);
			TextView textViewDate = (TextView)v.findViewById(R.id.textViewDate);
			TextView textViewName = (TextView)v.findViewById(R.id.textViewName);
			
			TextView textViewDirection = (TextView)v.findViewById(R.id.textViewDirection);
			
			String time = cursor.getString(iTime);
			String date = cursor.getString(iDate);
			textViewTime.setText(time);
			textViewDate.setText(date);
			textViewName.setText(cursor.getString(iStop));
			textViewDirection.setText(String.format(getString(R.string.transport_direction), cursor.getString(iDirection)));
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			return mInf.inflate(R.layout.item_stop_departure, null);
		}

		public Cursor swapCursor(Cursor newCursor)
		{
			if (newCursor != null)
			{
				iDate = newCursor.getColumnIndex(JourneyDetailStopDeparturesMetaData.TableMetaData.DATE);
				iDirection = newCursor.getColumnIndex(JourneyDetailStopDeparturesMetaData.TableMetaData.DIRECTION);
				iName = newCursor.getColumnIndex(JourneyDetailStopDeparturesMetaData.TableMetaData.NAME);
				iRef = newCursor.getColumnIndex(JourneyDetailStopDeparturesMetaData.TableMetaData.REF);
				iStop = newCursor.getColumnIndex(JourneyDetailStopDeparturesMetaData.TableMetaData.STOP);		
				iTime = newCursor.getColumnIndex(JourneyDetailStopDeparturesMetaData.TableMetaData.TIME);	
				iType = newCursor.getColumnIndex(JourneyDetailStopDeparturesMetaData.TableMetaData.TYPE);	
			}
			return super.swapCursor(newCursor);
		}
		
		public String getRef(int position)
		{
			String lat = "";
			Cursor c = getCursor();
			if(c.moveToPosition(position))
			{
				lat = c.getString(iRef);
			}
			return lat;
		}
	}
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
			
	}
	
	/**
	 * sets the ListView empty view.
	 * 
	 * @param emptyView
	 */
	private void setEmptyView(View emptyView)
	{
		if (mLoadingView != null)
		{
			((ViewGroup) mListView.getParent()).removeView(mLoadingView);
		}
		mLoadingView = emptyView;
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
		mLoadingView.setLayoutParams(params);
		((ViewGroup) mListView.getParent()).addView(mLoadingView);
		mListView.setEmptyView(mLoadingView);
	}

	@Override
	public void onClick(View v)
	{
		if(v.getId()==R.id.btnFetchDepartures)
		{
			Intent service = new Intent(getActivity(), DepartureBoardsService.class);
			service.putExtra(DepartureBoardsService.REQUEST, mNearbyLocationRequest);
			getActivity().startService(service);
			setEmptyView(LayoutInflater.from(getActivity()).inflate(R.layout.empty_list_view_waiting, null));
		}
		
	}
}
