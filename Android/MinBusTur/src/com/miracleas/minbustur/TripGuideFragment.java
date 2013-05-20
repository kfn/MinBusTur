package com.miracleas.minbustur;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient.OnAddGeofencesResultListener;
import com.miracleas.minbustur.TripSuggestionsFragment.Callbacks;
import com.miracleas.minbustur.model.Trip;
import com.miracleas.minbustur.model.TripRequest;
import com.miracleas.minbustur.provider.JourneyDetailMetaData;
import com.miracleas.minbustur.provider.JourneyDetailStopMetaData;
import com.miracleas.minbustur.provider.TripLegMetaData;
import com.miracleas.minbustur.provider.TripMetaData;
import com.miracleas.minbustur.provider.TripMetaData.TableMetaData;
import com.miracleas.minbustur.service.JourneyDetailsService;
import com.miracleas.minbustur.service.ReceiveTransitionsIntentService;

public class TripGuideFragment extends SherlockListFragment implements LoaderCallbacks<Cursor>, OnItemClickListener
{
	private static final String[] PROJECTION = { 
		TripLegMetaData.TableMetaData._ID, 
		TripLegMetaData.TableMetaData.DEST_DATE,
		TripLegMetaData.TableMetaData.DEST_NAME,
		TripLegMetaData.TableMetaData.DEST_ROUTE_ID,
		TripLegMetaData.TableMetaData.DEST_TIME,
		TripLegMetaData.TableMetaData.DEST_TYPE,
		TripLegMetaData.TableMetaData.ORIGIN_DATE,
		TripLegMetaData.TableMetaData.ORIGIN_NAME ,
		TripLegMetaData.TableMetaData.ORIGIN_ROUTE_ID,
		TripLegMetaData.TableMetaData.ORIGIN_TIME,
		TripLegMetaData.TableMetaData.ORIGIN_TYPE,
		TripLegMetaData.TableMetaData.DURATION,
		TripLegMetaData.TableMetaData.DURATION_FORMATTED,
		TripLegMetaData.TableMetaData.NAME,
		TripLegMetaData.TableMetaData.NOTES,
		TripLegMetaData.TableMetaData.REF,
		TripLegMetaData.TableMetaData.TYPE,
		
		TripLegMetaData.TableMetaData.PROGRESS_BAR_PROGRESS,
		TripLegMetaData.TableMetaData.PROGRESS_BAR_MAX,
		TripLegMetaData.TableMetaData.DEPARTURES_IN_TIME_LABEL,
		TripLegMetaData.TableMetaData.COMPLETED};
	private static final String[] PROJECTION_LEGS = { 
		TripLegMetaData.TableMetaData.REF, TripLegMetaData.TableMetaData._ID
	};
	private static final String[] PROJECTION_JORNEY_DETAILS = { 
		JourneyDetailStopMetaData.TableMetaData._ID,
		JourneyDetailStopMetaData.TableMetaData.LATITUDE,
		JourneyDetailStopMetaData.TableMetaData.LONGITUDE,
		JourneyDetailStopMetaData.TableMetaData.LEG_ID,
		JourneyDetailStopMetaData.TableMetaData.DEP_DATE,
		JourneyDetailStopMetaData.TableMetaData.DEP_TIME
	};
	private TripAdapter mTripAdapter = null;
	private long[] mLegIds;

	public static TripGuideFragment createInstance(String tripId, int stepCount, TripRequest tripRequest)
	{
		TripGuideFragment f = new TripGuideFragment();
		Bundle args = new Bundle();
		args.putString(TripLegMetaData.TableMetaData._ID, tripId);
		args.putString(TripLegMetaData.TableMetaData.STEP_NUMBER, stepCount+"");
		args.putParcelable("TripRequest", tripRequest);
		f.setArguments(args);
		return f;
	}
	
	public TripGuideFragment(){}
		
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View listView = super.onCreateView(inflater, container, savedInstanceState);
		View rootView = inflater.inflate(R.layout.fragment_trip_guide, container, false);
		FrameLayout frame = (FrameLayout)rootView.findViewById(R.id.listContainer);
		frame.addView(listView);
		return rootView;
		
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		mTripAdapter = new TripAdapter(getActivity(), null, 0);
		mLegIds = new long[2];
		setListAdapter(mTripAdapter);
		getLoaderManager().initLoader(LoaderConstants.LOAD_TRIP_LEGS, getArguments(), this);
		getLoaderManager().initLoader(LoaderConstants.LOAD_TRIP_LEG_FIRST_LAST, getArguments(), this);
		getLoaderManager().initLoader(LoaderConstants.LOAD_START_END_POSITION, getArguments(), this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args)
	{		
		if(id==LoaderConstants.LOAD_TRIP_LEGS)
		{
			String selection = TripLegMetaData.TableMetaData.TRIP_ID + "=?";
			String[] selectionArgs = {args.getString(TripLegMetaData.TableMetaData._ID)};
			return new CursorLoader(getActivity(), TripLegMetaData.TableMetaData.CONTENT_URI, PROJECTION, selection, selectionArgs, null);
		}
		else if(id==LoaderConstants.LOAD_TRIP_LEG_FIRST_LAST)
		{
			String selection = TripLegMetaData.TableMetaData.TRIP_ID +"=? AND "+TripLegMetaData.TableMetaData.TYPE+" NOT LIKE ?";
			String[] selectionArgs = {args.getString(TripLegMetaData.TableMetaData._ID), "WALK"};
			return new CursorLoader(getActivity(), TripLegMetaData.TableMetaData.CONTENT_URI, PROJECTION_LEGS, selection, selectionArgs, TripLegMetaData.TableMetaData.STEP_NUMBER);
		}
		else if(id==LoaderConstants.LOAD_START_END_POSITION)
		{
			String selection = JourneyDetailStopMetaData.TableMetaData.TRIP_ID + "=?";
			String[] selectionArgs = {args.getString(TripLegMetaData.TableMetaData._ID)};
			return new CursorLoader(getActivity(), JourneyDetailStopMetaData.TableMetaData.CONTENT_URI, PROJECTION_JORNEY_DETAILS, selection, selectionArgs, null);
		}
		return null;
	}


	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor newCursor)
	{
		if(loader.getId()==LoaderConstants.LOAD_TRIP_LEGS)
		{
			mTripAdapter.swapCursor(newCursor);	
		}
		else if(loader.getId()==LoaderConstants.LOAD_TRIP_LEG_FIRST_LAST)
		{
			if(newCursor.moveToFirst() && getArguments()!=null)
			{
				Intent service = new Intent(getActivity(), JourneyDetailsService.class);
				service.putExtra(JourneyDetailsService.TRIP_ID, getArguments().getString(TripLegMetaData.TableMetaData._ID));
				loadTripLegs(newCursor, service, true);
				if(newCursor.moveToLast())
				{
					loadTripLegs(newCursor, service, false);
					getActivity().startService(service);
					getLoaderManager().destroyLoader(LoaderConstants.LOAD_TRIP_LEG_FIRST_LAST);
				}
				
			}
		}
		else if(loader.getId()==LoaderConstants.LOAD_START_END_POSITION)
		{		
			if(newCursor.moveToFirst())
			{	
				
				Geofence g1 = loadGeofenceInfo(newCursor, true);
				if(newCursor.moveToLast())
				{
					Geofence g2 = loadGeofenceInfo(newCursor, false);
					List<Geofence> geofences = new ArrayList<Geofence>();
					loadStartAndEndGeofences(geofences);
					geofences.add(g1);
					geofences.add(g2);
					GeofenceActivity geo = (GeofenceActivity)getActivity();
					geo.addGeofences(geofences);
					getLoaderManager().destroyLoader(LoaderConstants.LOAD_START_END_POSITION);
				}
			}
			
			
		}
	}
	
	private void loadTripLegs(Cursor newCursor, Intent service, boolean first)
	{		
		int iRef = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.REF);
		int iLegId = newCursor.getColumnIndex(TripLegMetaData.TableMetaData._ID);
		long id = newCursor.getLong(iLegId);
		int i = first ? 0 : 1;
		mLegIds[i] = id;
		int count = i + 1;
		service.putExtra(JourneyDetailsService.URL+count, newCursor.getString(iRef));	
		service.putExtra(JourneyDetailsService.LEG+count, id+"");	
	}
	
	private Geofence loadGeofenceInfo(Cursor newCursor, boolean first)
	{
		int iId = newCursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData._ID);
		int iLat = newCursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.LATITUDE);
		int iLng = newCursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.LONGITUDE);
		int iLegId = newCursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.LEG_ID);
			
		long stopId = newCursor.getLong(iId);
		double lat = (double)newCursor.getInt(iLat) / 1000000d;
		double lng = (double)newCursor.getInt(iLng) / 1000000d;
		int transitionId = -1;
		if(first) //skal laves om, saa foerste geofence er den placering hvor man er
		{
			transitionId = Geofence.GEOFENCE_TRANSITION_ENTER;
		}
		else
		{
			transitionId = Geofence.GEOFENCE_TRANSITION_ENTER;
		}
		Geofence g = toGeofence(stopId+"", transitionId, lat, lng, 10, DateUtils.HOUR_IN_MILLIS);
		return g;		
	}
	
	private void loadStartAndEndGeofences(List<Geofence> geofences)
	{
		TripRequest tripRequest = getArguments().getParcelable(TripRequest.tag);
		double lat = (double)(Integer.parseInt(tripRequest.getOriginCoordX()) / 1000000d);
		double lng = (double)(Integer.parseInt(tripRequest.getOriginCoordY()) / 1000000d);
		Geofence origin = toGeofence("origin", Geofence.GEOFENCE_TRANSITION_EXIT, lat, lng, 10, DateUtils.HOUR_IN_MILLIS);
		
		lat = (double)(Integer.parseInt(tripRequest.getDestCoordX()) / 1000000d);
		lng = (double)(Integer.parseInt(tripRequest.getDestCoordY()) / 1000000d);
		Geofence dest = toGeofence("dest", Geofence.GEOFENCE_TRANSITION_ENTER, lat, lng, 10, DateUtils.HOUR_IN_MILLIS);
		
		geofences.add(origin);
		geofences.add(dest);
	}
	
	public Geofence toGeofence(String id, int transitionType, double lat, double lng, float radius, long expirationDuration)
	{
		// Build a new Geofence object
		return new Geofence.Builder().setRequestId(id).setTransitionTypes(transitionType).setCircularRegion(lat, lng, radius).setExpirationDuration(expirationDuration).build();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader)
	{
		if(loader.getId()==LoaderConstants.LOAD_TRIP_LEGS)
		{
			mTripAdapter.swapCursor(null);	
		}			
	}
	
	private class TripAdapter extends CursorAdapter
	{
		private int iDestName;
		private int iOriginName;
		private int iOriginRouteId;
		private int iDestDate;
		private int iDestRouteId;
		private int iDestTime;
		private int iDestType;
		private int iOriginDate;
		
		private int iOriginTime = 0;
		private int iOriginType = 0;
		private int iDuration = 0;
		private int iDurationFormatted = 0;
		private int iName = 0;
		private int iNotes = 0;
		private int iRef = 0;
		private int iType = 0;
		private int iProgressBarProgress = 0;
		private int iProgressBarMax = 0;
		private int iDeparturesInTimeLabel = 0;
		private int iCompleted = 0;
		
		
		
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
			textViewTime.setText(cursor.getString(iOriginTime)+"-"+cursor.getString(iDestTime));
			
			TextView textViewOriginName = (TextView)v.findViewById(R.id.textViewOriginName);
			textViewOriginName.setText(String.format(getString(R.string.from), cursor.getString(iOriginName)));
			
			String originLocationType = cursor.getString(iOriginType);
			if(originLocationType.equals("ADR"))
			{
				textViewOriginName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_home, 0, 0, 0);
			}
			else if(originLocationType.equals("ST"))
			{
				textViewOriginName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_myplaces, 0, 0, 0);
			}
			
			TextView textViewDestName = (TextView)v.findViewById(R.id.textViewDestName);
			textViewDestName.setText(String.format(getString(R.string.to), cursor.getString(iDestName)));
			
			String destLocationType = cursor.getString(iDestType);
			if(destLocationType.equals("ADR"))
			{
				textViewDestName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_home, 0, 0, 0);
			}
			else if(destLocationType.equals("ST"))
			{
				textViewDestName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_myplaces, 0, 0, 0);
			}
			

			
			TextView textViewDuration = (TextView)v.findViewById(R.id.textViewDuration);
			textViewDuration.setText(String.format(getString(R.string.in_duration), cursor.getString(iName), cursor.getString(iDurationFormatted)));
			
			String type = cursor.getString(iType);
			if(type.equals(Trip.TYPE_WALK))
			{
				textViewDuration.setCompoundDrawablesWithIntrinsicBounds(R.drawable.walking, 0, 0, 0);
			}
			else if(type.equals(Trip.TYPE_BUS))
			{
				textViewDuration.setCompoundDrawablesWithIntrinsicBounds(R.drawable.driving, 0, 0, 0);
			}
			else if(type.equals(Trip.TYPE_IC))
			{
				textViewDuration.setCompoundDrawablesWithIntrinsicBounds(R.drawable.driving, 0, 0, 0);
			}
			else if(type.equals(Trip.TYPE_TRAIN))
			{
				textViewDuration.setCompoundDrawablesWithIntrinsicBounds(R.drawable.driving, 0, 0, 0);
			}
			else if(type.equals(Trip.TYPE_LYN))
			{
				textViewDuration.setCompoundDrawablesWithIntrinsicBounds(R.drawable.driving, 0, 0, 0);
			}
			else if(type.equals(Trip.TYPE_REG))
			{
				textViewDuration.setCompoundDrawablesWithIntrinsicBounds(R.drawable.driving, 0, 0, 0);
			}
			String notes = cursor.getString(iNotes);
			TextView textViewNotes = (TextView)v.findViewById(R.id.textViewNotes);
			if(!TextUtils.isEmpty(notes))
			{				
				textViewNotes.setText(notes);	
				textViewNotes.setVisibility(View.VISIBLE);
			}
			else
			{
				textViewNotes.setVisibility(View.GONE);
			}
			
			TextView textViewDeparturesIn = (TextView)v.findViewById(R.id.textViewDeparturesIn);
			textViewDeparturesIn.setText(cursor.getString(iDeparturesInTimeLabel));
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			return mInf.inflate(R.layout.item_trip_guide, null);
		}
		
		public Cursor swapCursor(Cursor newCursor)
		{
			if (newCursor != null)
			{
				iDestDate = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.DEST_DATE);
				iDestName = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.DEST_NAME);
				iDestRouteId = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.DEST_ROUTE_ID);
				iDestTime = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.DEST_TIME);
				iDestType = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.DEST_TYPE);
				iOriginDate = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.ORIGIN_DATE);				
				iOriginName = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.ORIGIN_NAME);
				iOriginRouteId = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.ORIGIN_ROUTE_ID);
				iOriginTime = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.ORIGIN_TIME);
				iOriginType = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.ORIGIN_TYPE);	
				iDuration = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.DURATION);
				iDurationFormatted = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.DURATION_FORMATTED);
				iName = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.NAME);
				iNotes = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.NOTES);			
				iRef = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.REF);
				iType = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.TYPE);
				iProgressBarProgress = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.PROGRESS_BAR_PROGRESS);
				iProgressBarMax = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.PROGRESS_BAR_MAX);
				iDeparturesInTimeLabel = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.DEPARTURES_IN_TIME_LABEL);
				iCompleted = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.COMPLETED);
			}
			return super.swapCursor(newCursor);
		}
	}
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
	{
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		if (!(activity instanceof GeofenceActivity))
		{
			throw new IllegalStateException("Activity must be a GeofenceActivity.");
		}			
	}
}
