package com.miracleas.minrute;

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

import com.miracleas.minrute.model.TripLeg;
import com.miracleas.minrute.model.TripLegStop;
import com.miracleas.minrute.model.TripRequest;
import com.miracleas.minrute.provider.TripLegDetailMetaData;
import com.miracleas.minrute.provider.TripLegDetailNoteMetaData;
import com.miracleas.minrute.provider.TripLegDetailStopMetaData;
import com.miracleas.minrute.provider.TripLegMetaData;
import com.miracleas.minrute.provider.TripMetaData;
import com.miracleas.minrute.provider.TripMetaData.TableMetaData;

public class TripLegDetailsFragment extends SherlockListFragment implements LoaderCallbacks<Cursor>, OnItemClickListener
{
	private static final String[] PROJECTION = { 
		TripLegDetailMetaData.TableMetaData._ID, 
		TripLegDetailMetaData.TableMetaData.NAME,
		TripLegDetailMetaData.TableMetaData.TYPE
	};
	private static final String[] PROJECTION_STOP = { 
		TripLegDetailStopMetaData.TableMetaData._ID, 
		TripLegDetailStopMetaData.TableMetaData.ARR_DATE,
		TripLegDetailStopMetaData.TableMetaData.ARR_TIME,
		TripLegDetailStopMetaData.TableMetaData.DEP_DATE,
		TripLegDetailStopMetaData.TableMetaData.DEP_TIME,
		TripLegDetailStopMetaData.TableMetaData.LATITUDE,
		TripLegDetailStopMetaData.TableMetaData.LONGITUDE,
		TripLegDetailStopMetaData.TableMetaData.NAME,
		TripLegDetailStopMetaData.TableMetaData.TRACK,
		TripLegDetailStopMetaData.TableMetaData.IS_PART_OF_USER_ROUTE,
	};

	private TripAdapter mTripAdapter = null;
	private TextView mTextViewJourneyName = null;
	private long journeyDetailId;
	private TripLeg mTripLeg = null;
	private TextView mTextViewJourneyTimes = null;
	
	public static TripLegDetailsFragment createInstance(TripLeg leg)
	{
		TripLegDetailsFragment f = new TripLegDetailsFragment();
		Bundle args = new Bundle();
		args.putParcelable(TripLeg.tag, leg);
		/*args.putString(JourneyDetailMetaData.TableMetaData.TRIP_ID, leg.tripId);
		args.putString(JourneyDetailMetaData.TableMetaData.LEG_ID, leg.id+"");
		args.putString(JourneyDetailMetaData.TableMetaData.REF, leg.ref);
		args.putString(TripLegMetaData.TableMetaData.TYPE, leg.type);
		args.putString(TripLegMetaData.TableMetaData.ORIGIN_NAME, leg.origin.name);*/
		
		f.setArguments(args);
		return f;
	}
	
	public TripLegDetailsFragment(){}
		
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mTripLeg = getArguments().getParcelable(TripLeg.tag);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View listView = super.onCreateView(inflater, container, savedInstanceState);
		View rootView = inflater.inflate(R.layout.fragment_trip_leg_details, container, false);
		FrameLayout frame = (FrameLayout)rootView.findViewById(R.id.listContainer);
		frame.addView(listView);
		mTextViewJourneyName = (TextView)rootView.findViewById(R.id.textViewJourneyName);
		mTextViewJourneyTimes = (TextView)rootView.findViewById(R.id.textViewJourneyTimes);
		//String locationName = getArguments().getString(TripLegMetaData.TableMetaData.ORIGIN_NAME);
		mTextViewJourneyTimes.setText(mTripLeg.originTime + "-"+mTripLeg.destTime);
		return rootView;
		
	}
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);	
		
		mTripAdapter = new TripAdapter(getActivity(), null, 0, mTripLeg.type);
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
			String selection = TripLegDetailMetaData.TableMetaData.LEG_ID + "=?";
			String[] selectionArgs = {mTripLeg.id+""};
			return new CursorLoader(getActivity(), TripLegDetailMetaData.TableMetaData.CONTENT_URI, PROJECTION, selection, selectionArgs, null);
		}
		else if(id==LoaderConstants.LOADER_TRIP_LEG_STOP_DETAILS)
		{
			String selection = TripLegDetailStopMetaData.TableMetaData.JOURNEY_DETAIL_ID + "=?";
			String[] selectionArgs = {args.getLong(TripLegDetailMetaData.TableMetaData._ID)+""};
			return new CursorLoader(getActivity(), TripLegDetailStopMetaData.TableMetaData.CONTENT_URI, PROJECTION_STOP, selection, selectionArgs, null);
		}

		return null;
	}


	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor newCursor)
	{
		int id = loader.getId();
		if(id==LoaderConstants.LOADER_TRIP_LEG_DETAILS && newCursor.moveToFirst())
		{
			journeyDetailId = newCursor.getLong(newCursor.getColumnIndex(TripLegDetailMetaData.TableMetaData._ID));
			String transportType = mTripLeg.type;
			String legId = mTripLeg.id+"";
			Bundle args = new Bundle();
			args.putLong(TripLegDetailMetaData.TableMetaData._ID, journeyDetailId);
			getLoaderManager().initLoader(LoaderConstants.LOADER_TRIP_LEG_STOP_DETAILS, args, this);

			mCallbacks.setJourneyDetailId(journeyDetailId, legId, transportType);
			String name = newCursor.getString(newCursor.getColumnIndex(TripLegDetailMetaData.TableMetaData.NAME));
			mTextViewJourneyName.setText(name+"\n"+mTripLeg.originName+"-"+mTripLeg.destName);
			String type = newCursor.getString(newCursor.getColumnIndex(TripLegDetailMetaData.TableMetaData.TYPE));
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
		private int iId;
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
				iArrTime = newCursor.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.ARR_TIME);
				iDepTime = newCursor.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.DEP_TIME);
				iLat = newCursor.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.LATITUDE);
				iLng = newCursor.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.LONGITUDE);
				iName = newCursor.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.NAME);		
				iTrack = newCursor.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.TRACK);	
				iPartOfUserRoute = newCursor.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.IS_PART_OF_USER_ROUTE);	
				iId = newCursor.getColumnIndex(TripLegDetailStopMetaData.TableMetaData._ID);	
			}
			return super.swapCursor(newCursor);
		}
		
		public TripLegStop getTripLegStop(int position)
		{
			TripLegStop stop = null;;
			Cursor c = getCursor();
			if(c.moveToPosition(position))
			{
				int id = (int)c.getLong(iId);
				String name = c.getString(iName);
				String lng = c.getString(iLng);
				String lat = c.getString(iLat);
				stop = new TripLegStop(lat, lng, name, id);
			}
			return stop;
		}
	}
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		TripLegStop stop = mTripAdapter.getTripLegStop(position);		
		mCallbacks.onStopSelected(stop, mTripLeg);		
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
		public void onStopSelected(TripLegStop stop, TripLeg leg);
		public void setJourneyDetailId(long id, String LegId, String transportType);
	}

	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sDummyCallbacks = new Callbacks()
	{

		@Override
		public void onStopSelected(TripLegStop stop, TripLeg leg)
		{
			
		}

		@Override
		public void setJourneyDetailId(long id, String LegId, String transportType)
		{
			
		}
		
	};

}
