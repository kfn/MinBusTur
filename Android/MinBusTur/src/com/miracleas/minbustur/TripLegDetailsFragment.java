package com.miracleas.minbustur;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.miracleas.minbustur.model.TripLeg;
import com.miracleas.minbustur.model.TripRequest;
import com.miracleas.minbustur.provider.JourneyDetailMetaData;
import com.miracleas.minbustur.provider.JourneyDetailNoteMetaData;
import com.miracleas.minbustur.provider.JourneyDetailStopMetaData;
import com.miracleas.minbustur.provider.TripLegMetaData;
import com.miracleas.minbustur.provider.TripMetaData;
import com.miracleas.minbustur.provider.TripMetaData.TableMetaData;

public class TripLegDetailsFragment extends SherlockListFragment implements LoaderCallbacks<Cursor>, OnItemClickListener
{
	private static final String[] PROJECTION = { 
		JourneyDetailMetaData.TableMetaData._ID, 
		JourneyDetailMetaData.TableMetaData.NAME,
		JourneyDetailMetaData.TableMetaData.TYPE
	};
	private static final String[] PROJECTION_STOP = { 
		JourneyDetailStopMetaData.TableMetaData._ID, 
		JourneyDetailStopMetaData.TableMetaData.ARR_DATE,
		JourneyDetailStopMetaData.TableMetaData.ARR_TIME,
		JourneyDetailStopMetaData.TableMetaData.DEP_DATE,
		JourneyDetailStopMetaData.TableMetaData.DEP_TIME,
		JourneyDetailStopMetaData.TableMetaData.LATITUDE,
		JourneyDetailStopMetaData.TableMetaData.LONGITUDE,
		JourneyDetailStopMetaData.TableMetaData.NAME,
		JourneyDetailStopMetaData.TableMetaData.TRACK,
		JourneyDetailStopMetaData.TableMetaData.IS_PART_OF_USER_ROUTE,
	};

	private TripAdapter mTripAdapter = null;
	private TextView mTextViewJourneyName = null;
	private long journeyDetailId;
	
	
	public static TripLegDetailsFragment createInstance(String tripId, String legId, String ref, String transportType)
	{
		TripLegDetailsFragment f = new TripLegDetailsFragment();
		Bundle args = new Bundle();
		args.putString(JourneyDetailMetaData.TableMetaData.TRIP_ID, tripId);
		args.putString(JourneyDetailMetaData.TableMetaData.LEG_ID, legId);
		args.putString(JourneyDetailMetaData.TableMetaData.REF, ref);
		args.putString(TripLegMetaData.TableMetaData.TYPE, transportType);
		f.setArguments(args);
		return f;
	}
	
	public TripLegDetailsFragment(){}
		
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View listView = super.onCreateView(inflater, container, savedInstanceState);
		View rootView = inflater.inflate(R.layout.fragment_trip_leg_details, container, false);
		FrameLayout frame = (FrameLayout)rootView.findViewById(R.id.listContainer);
		frame.addView(listView);
		mTextViewJourneyName = (TextView)rootView.findViewById(R.id.textViewJourneyName);
		
		return rootView;
		
	}
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);	
		String transportType = getArguments().getString(TripLegMetaData.TableMetaData.TYPE);
		mTripAdapter = new TripAdapter(getActivity(), null, 0, transportType);
		getListView().setOnItemClickListener(this);
		setListAdapter(mTripAdapter);
		setListShown(false);
		getListView().setOnItemClickListener(this);	
		getLoaderManager().initLoader(LoaderConstants.LOADER_TRIP_LEG_DETAILS, getArguments(), this);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);		
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args)
	{		
		if(id==LoaderConstants.LOADER_TRIP_LEG_DETAILS)
		{
			String selection = JourneyDetailMetaData.TableMetaData.LEG_ID + "=?";
			String[] selectionArgs = {args.getString(JourneyDetailMetaData.TableMetaData.LEG_ID)};
			return new CursorLoader(getActivity(), JourneyDetailMetaData.TableMetaData.CONTENT_URI, PROJECTION, selection, selectionArgs, null);
		}
		else if(id==LoaderConstants.LOADER_TRIP_LEG_STOP_DETAILS)
		{
			String selection = JourneyDetailStopMetaData.TableMetaData.JOURNEY_DETAIL_ID + "=?";
			String[] selectionArgs = {args.getLong(JourneyDetailMetaData.TableMetaData._ID)+""};
			return new CursorLoader(getActivity(), JourneyDetailStopMetaData.TableMetaData.CONTENT_URI, PROJECTION_STOP, selection, selectionArgs, null);
		}

		return null;
	}


	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor newCursor)
	{
		int id = loader.getId();
		if(id==LoaderConstants.LOADER_TRIP_LEG_DETAILS && newCursor.moveToFirst())
		{
			journeyDetailId = newCursor.getLong(newCursor.getColumnIndex(JourneyDetailMetaData.TableMetaData._ID));
			String transportType = getArguments().getString(TripLegMetaData.TableMetaData.TYPE);
			String legId = getArguments().getString(JourneyDetailMetaData.TableMetaData.LEG_ID);
			Bundle args = new Bundle();
			args.putLong(JourneyDetailMetaData.TableMetaData._ID, journeyDetailId);
			getLoaderManager().initLoader(LoaderConstants.LOADER_TRIP_LEG_STOP_DETAILS, args, this);

			mCallbacks.setJourneyDetailId(journeyDetailId, legId, transportType);
			String name = newCursor.getString(newCursor.getColumnIndex(JourneyDetailMetaData.TableMetaData.NAME));
			mTextViewJourneyName.setText(name);
			String type = newCursor.getString(newCursor.getColumnIndex(JourneyDetailMetaData.TableMetaData.TYPE));
			int iconRes = TripLeg.getIcon(type);
			mTextViewJourneyName.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
		}
		else if(id==LoaderConstants.LOADER_TRIP_LEG_STOP_DETAILS)
		{
			setListShown(true);
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
		private int iArrTime;
		private int iDepTime;
		private int iLat;
		private int iLng;
		private int iName;
		private int iTrack;
		private String mTransportType;
		private boolean mIsTrain = false;
		private int iPartOfUserRoute;
		
		private LayoutInflater mInf = null;

		public TripAdapter(Context context, Cursor c, int flags, String transportType)
		{
			super(context, c, flags);
			mInf = LayoutInflater.from(context);
			mTransportType = transportType;
			mIsTrain = TripLeg.isTrain(transportType);
		}

		@Override
		public void bindView(View v, Context context, Cursor cursor)
		{
			TextView textViewDepatureTime = (TextView)v.findViewById(R.id.textViewDepatureTime);
			TextView textViewStopName = (TextView)v.findViewById(R.id.textViewStopName);					
			textViewDepatureTime.setText(cursor.getString(iDepTime));
			String name = cursor.getString(iName);
			String arrTime = cursor.getString(iArrTime);
			String track = cursor.getString(iTrack);;
			String depTime = cursor.getString(iDepTime);
			boolean isPartOfUsersRoute = cursor.getInt(iPartOfUserRoute) == 1;
			
			if(!TextUtils.isEmpty(arrTime) && !TextUtils.isEmpty(depTime) && !depTime.equals(arrTime))
			{
				textViewDepatureTime.setText(arrTime+"\n"+depTime);
			}
			else if(!TextUtils.isEmpty(arrTime))
			{
				textViewDepatureTime.setText(arrTime);
			}
			else if(!TextUtils.isEmpty(arrTime))
			{
				textViewDepatureTime.setText(depTime);
			}		
			
			if(TextUtils.isEmpty(track))
			{
				textViewStopName.setText(name);
			}
			else
			{
				textViewStopName.setText(String.format(getString(R.string.transport_type_and_track_number), name, track));
			}
			
			if(isPartOfUsersRoute)
			{
				textViewStopName.setTextColor(Color.BLACK);
				textViewDepatureTime.setTextColor(Color.BLACK);
				//v.setBackgroundResource(R.drawable.selectable_background_minrutevejledning);
			}
			else
			{
				textViewStopName.setTextColor(Color.GRAY);
				textViewDepatureTime.setTextColor(Color.GRAY);
				//v.setBackgroundResource(R.drawable.selectable_background_minrutevejledning_dark);
			}
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			return mInf.inflate(R.layout.item_trip_leg_detail, null);
		}

		public Cursor swapCursor(Cursor newCursor)
		{
			if (newCursor != null)
			{
				iArrTime = newCursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.ARR_TIME);
				iDepTime = newCursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.DEP_TIME);
				iLat = newCursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.LATITUDE);
				iLng = newCursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.LONGITUDE);
				iName = newCursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.NAME);		
				iTrack = newCursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.TRACK);	
				iPartOfUserRoute = newCursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.IS_PART_OF_USER_ROUTE);	
			}
			return super.swapCursor(newCursor);
		}
	}
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		String legId = getArguments().getString(TripLegMetaData.TableMetaData._ID);
		String tripId = getArguments().getString(TripLegMetaData.TableMetaData.TRIP_ID);
		mCallbacks.onTripLegStopSelected(id+"", tripId, legId);		
	}
	
	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof Callbacks))
		{
			throw new IllegalStateException("Activity must implement fragment's callbacks.");
		}			
		mCallbacks = (Callbacks)activity;
	}
	@Override
	public void onDetach()
	{
		super.onDetach();

		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sDummyCallbacks;
	}
	private Callbacks mCallbacks = sDummyCallbacks;
	
	/**
	 * A callback interface that all activities containing this fragment must
	 * implement.
	 */
	public interface Callbacks
	{
		public void onTripLegStopSelected(String stopId, String tripId, String legId);
		public void setJourneyDetailId(long id, String LegId, String transportType);
	}

	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sDummyCallbacks = new Callbacks()
	{

		@Override
		public void onTripLegStopSelected(String stopId, String tripId, String legId)
		{
			
		}

		@Override
		public void setJourneyDetailId(long id, String LegId, String transportType)
		{
			
		}
		
	};

}
