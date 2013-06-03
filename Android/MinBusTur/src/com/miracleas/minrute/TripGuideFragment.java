package com.miracleas.minrute;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.google.android.gms.location.Geofence;
import com.miracleas.imagedownloader.IImageDownloader;
import com.miracleas.minrute.model.GeofenceHelper;
import com.miracleas.minrute.model.TripLeg;
import com.miracleas.minrute.model.TripRequest;
import com.miracleas.minrute.provider.AddressGPSMetaData;
import com.miracleas.minrute.provider.GeofenceMetaData;
import com.miracleas.minrute.provider.StopImagesMetaData;
import com.miracleas.minrute.provider.TripLegMetaData;
import com.miracleas.minrute.provider.TripMetaData;
import com.miracleas.minrute.service.FetchGeofencesForStopBeforeService;
import com.miracleas.minrute.service.JourneyDetailsService;

public class TripGuideFragment extends SherlockListFragment implements LoaderCallbacks<Cursor>, OnItemClickListener, com.miracleas.minrute.service.UpdateVoiceTripService.OnVoiceServiceReadyListener
{
	//"t."+TripLegMetaData.TableMetaData._ID
	private static final String[] PROJECTION = { "t."+TripLegMetaData.TableMetaData._ID, TripLegMetaData.TableMetaData.DEST_DATE, TripLegMetaData.TableMetaData.DEST_NAME, TripLegMetaData.TableMetaData.DEST_ROUTE_ID, TripLegMetaData.TableMetaData.DEST_TIME,
			TripLegMetaData.TableMetaData.DEST_TYPE, TripLegMetaData.TableMetaData.ORIGIN_DATE, TripLegMetaData.TableMetaData.ORIGIN_NAME, TripLegMetaData.TableMetaData.ORIGIN_ROUTE_ID, TripLegMetaData.TableMetaData.ORIGIN_TIME, TripLegMetaData.TableMetaData.ORIGIN_TYPE,
			TripLegMetaData.TableMetaData.DURATION, TripLegMetaData.TableMetaData.DURATION_FORMATTED, TripLegMetaData.TableMetaData.NAME, TripLegMetaData.TableMetaData.NOTES, TripLegMetaData.TableMetaData.REF, TripLegMetaData.TableMetaData.TYPE,
			TripLegMetaData.TableMetaData.ORIGIN_RT_TRACK, TripLegMetaData.TableMetaData.DEST_TRACK,
			TripLegMetaData.TableMetaData.GEOFENCE_EVENT_ID,StopImagesMetaData.TableMetaData.URL,
			TripLegMetaData.TableMetaData.PROGRESS_BAR_PROGRESS, TripLegMetaData.TableMetaData.PROGRESS_BAR_MAX, TripLegMetaData.TableMetaData.COMPLETED };

	private static final String[] PROJECTION_TRIP_GPS_READY = { TripMetaData.TableMetaData._ID, TripMetaData.TableMetaData.HAS_ALL_ADDRESS_GPSES };

	private IImageDownloader mIImageDownloader = null;
	private TripAdapter mTripAdapter = null;
	
	
	public static final String tag = TripGuideFragment.class.getName();

	public static TripGuideFragment createInstance(String tripId, int stepCount, TripRequest tripRequest)
	{
		TripGuideFragment f = new TripGuideFragment();
		Bundle args = new Bundle();
		args.putString(TripMetaData.TableMetaData._ID, tripId);
		args.putString(TripLegMetaData.TableMetaData.STEP_NUMBER, stepCount + "");
		args.putParcelable(TripRequest.tag, tripRequest);
		f.setArguments(args);
		return f;
	}

	public TripGuideFragment()
	{
	}

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

	}

	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View listView = super.onCreateView(inflater, container, savedInstanceState);
		View rootView = inflater.inflate(R.layout.fragment_trip_guide, container, false);
		FrameLayout frame = (FrameLayout) rootView.findViewById(R.id.listContainer);
		frame.addView(listView);

		return rootView;

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		mTripAdapter = new TripAdapter(getActivity(), null, 0);
		getListView().setOnItemClickListener(this);
		setListAdapter(mTripAdapter);
		getLoaderManager().initLoader(LoaderConstants.LOAD_TRIP_LEGS, getArguments(), this);
		getLoaderManager().initLoader(LoaderConstants.LOAD_HAS_ALL_ADDRESS_GPSES, getArguments(), this);
		// getArguments(), this);
		if (savedInstanceState == null)
		{
			Intent service = new Intent(getActivity(), FetchGeofencesForStopBeforeService.class);
			service.putExtra(FetchGeofencesForStopBeforeService.TRIP_ID, getArguments().getString(TripMetaData.TableMetaData._ID));
			service.putExtra(TripRequest.tag, getArguments().getParcelable(TripRequest.tag));
			getActivity().startService(service);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args)
	{
		if (id == LoaderConstants.LOAD_TRIP_LEGS)
		{
			String selection = TripLegMetaData.TableMetaData.TRIP_ID + "=?";
			String[] selectionArgs = { args.getString(TripMetaData.TableMetaData._ID) };
			Uri uri = Uri.withAppendedPath(TripLegMetaData.TableMetaData.CONTENT_URI, StopImagesMetaData.TABLE_NAME);
			return new CursorLoader(getActivity(), uri, PROJECTION, selection, selectionArgs, null);
		}

		else if (id == LoaderConstants.LOAD_HAS_ALL_ADDRESS_GPSES)
		{
			Uri uri = Uri.withAppendedPath(TripMetaData.TableMetaData.CONTENT_URI, args.getString(TripMetaData.TableMetaData._ID));
			return new CursorLoader(getActivity(), uri, PROJECTION_TRIP_GPS_READY, null, null, null);
		}
		/*
		 * else if (id == LoaderConstants.LOAD_GUIDE_VOICE_TRIP) { String
		 * selection = TripVoiceMetaData.TableMetaData.TRIP_ID + "=?"; String[]
		 * selectionArgs = {args.getString(TripMetaData.TableMetaData._ID)};
		 * return new CursorLoader(getActivity(),
		 * TripVoiceMetaData.TableMetaData.CONTENT_URI, PROJECTION_TRIP,
		 * selection, selectionArgs,
		 * TripVoiceMetaData.TableMetaData._ID+" LIMIT 1"); }
		 */
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor newCursor)
	{
		if (loader.getId() == LoaderConstants.LOAD_TRIP_LEGS)
		{
			Log.d(tag, "onLoadFinished LOAD_TRIP_LEGS");
			mTripAdapter.swapCursor(newCursor);
		} 
		else if (loader.getId() == LoaderConstants.LOAD_HAS_ALL_ADDRESS_GPSES && newCursor.moveToFirst())
		{
			int i = newCursor.getColumnIndex(TripMetaData.TableMetaData.HAS_ALL_ADDRESS_GPSES);
			boolean isGpsDownloaded = newCursor.getInt(i) == 1;
			if (isGpsDownloaded)
			{		
				getLoaderManager().destroyLoader(LoaderConstants.LOAD_HAS_ALL_ADDRESS_GPSES);
				String tripId = getArguments().getString(TripMetaData.TableMetaData._ID);
				insertGeofencesForThisTrip(tripId);
			}
		}
	}

	private LoadGeofencesTask mLoadGeofencesTask = null;

	private void insertGeofencesForThisTrip(String tripId)
	{
		if (mLoadGeofencesTask == null && !TextUtils.isEmpty(tripId))
		{
			mLoadGeofencesTask = new LoadGeofencesTask();
			mLoadGeofencesTask.execute(tripId);
		}
	}

	private class LoadGeofencesTask extends AsyncTask<String, Void, List<Geofence>>
	{
		private final String[] projection = {GeofenceMetaData.TableMetaData.geofence_id,
				GeofenceMetaData.TableMetaData.LAT, GeofenceMetaData.TableMetaData.LNG, 
				GeofenceMetaData.TableMetaData.TRANSITION_TYPE, GeofenceMetaData.TableMetaData.RADIUS };

		@Override
		protected List<Geofence> doInBackground(String... params)
		{
			String tripId = params[0];
			List<Geofence> list = getGeofencesFromGeofenceDb(tripId);											
			return list;
		}
		
		private List<Geofence> getGeofencesFromGeofenceDb(String tripId)
		{
			List<Geofence> geofences = new ArrayList<Geofence>();
			ContentResolver cr = getActivity().getContentResolver();
			Cursor c = null;
			try
			{
				String selection = GeofenceMetaData.TableMetaData.TRIP_ID + "=?";
				String[] selectionArgs = {tripId};
				c = cr.query(GeofenceMetaData.TableMetaData.CONTENT_URI, projection, selection, selectionArgs, null);
				if(c.moveToFirst())
				{
					int iId = c.getColumnIndex(GeofenceMetaData.TableMetaData.geofence_id);
					int iLat = c.getColumnIndex(GeofenceMetaData.TableMetaData.LAT);
					int iLng = c.getColumnIndex(GeofenceMetaData.TableMetaData.LNG);
					int iRadius = c.getColumnIndex(GeofenceMetaData.TableMetaData.RADIUS);
					int iTransition = c.getColumnIndex(GeofenceMetaData.TableMetaData.TRANSITION_TYPE);
					
					do
					{
						String strLat = c.getString(iLat);
						String strLng = c.getString(iLng);
						if(!TextUtils.isEmpty(strLat) && !TextUtils.isEmpty(strLng))
						{
							double latd = Double.parseDouble(strLat);
							double lngd = Double.parseDouble(strLng);					
							
							if(latd!=0d && lngd!=0d)
							{						
								int radius = c.getInt(iRadius);
								String geofenceId = c.getString(iId);
								int transition = c.getInt(iTransition);
								geofences.add(GeofenceHelper.toGeofence(geofenceId, transition, latd, lngd, radius, DateUtils.DAY_IN_MILLIS));
								Log.d(tag, "added id: "+geofenceId);
							}
						}

					} while (c.moveToNext());
				}
			}
			finally
			{
				if(c!=null && !c.isClosed())
				{
					c.close();
				}
			}
			return geofences;
		}
		
		public void onPostExecute(List<Geofence> geofences)
		{
			GeofenceActivity geo = (GeofenceActivity) getActivity();
			geo.addGeofences(geofences);
			getLoaderManager().restartLoader(LoaderConstants.LOAD_TRIP_LEGS, getArguments(), TripGuideFragment.this);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader)
	{
		if (loader.getId() == LoaderConstants.LOAD_TRIP_LEGS)
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

		private int iOriginTime =  -1;
		private int iOriginType =  -1;
		private int iDuration =  -1;
		private int iDurationFormatted =  -1;
		private int iName =  -1;
		private int iNotes =  -1;
		private int iRef =  -1;
		private int iType =  -1;
		private int iProgressBarProgress =  -1;
		private int iProgressBarMax = -1;
		private int iDeparturesInTimeLabel =  -1;
		private int iCompleted =  -1;
		private int iGeofenceTransition =  -1;
		private int iRtTrack =  -1;
		private int iDestTrack =  -1;
		private int iUrl =  -1;

		private LayoutInflater mInf = null;

		public TripAdapter(Context context, Cursor c, int flags)
		{
			super(context, c, flags);
			mInf = LayoutInflater.from(context);
		}

		@Override
		public void bindView(View v, Context context, Cursor cursor)
		{
			TextView textViewOriginName = (TextView) v.findViewById(R.id.textViewOriginName);
			TextView textViewDestName = (TextView) v.findViewById(R.id.textViewDestName);
			TextView textViewTime = (TextView) v.findViewById(R.id.textViewTime);
			TextView textViewTransportType = (TextView) v.findViewById(R.id.textViewTransportType);
			TextView textViewNotes = (TextView) v.findViewById(R.id.textViewNotes);
			//TextView textViewDeparturesIn = (TextView) v.findViewById(R.id.textViewDeparturesIn);
			ImageView imageViewThumb = (ImageView)v.findViewById(R.id.imageViewThumb);
            ImageView imageViewChecked = (ImageView)v.findViewById(R.id.imageViewChecked);
			
			
			String originTime = cursor.getString(iOriginTime);
			String originLocationType = cursor.getString(iOriginType);
			String destLocationType = cursor.getString(iDestType);
			String type = cursor.getString(iType);
			String track = cursor.getString(iRtTrack);
			String notes = cursor.getString(iNotes);
			String url = cursor.getString(iUrl);
			String originName =  cursor.getString(iOriginName);
			String destName =  cursor.getString(iDestName);
			
			if(TextUtils.isEmpty(url))
			{
				imageViewThumb.setVisibility(View.GONE);
				imageViewThumb.setImageResource(R.drawable.empty_photo);
			}
			else
			{
				imageViewThumb.setVisibility(View.VISIBLE);
				mIImageDownloader.download(url, imageViewThumb);
			}
			
			
			textViewOriginName.setText(originName); //.setText(String.format(getString(R.string.from), originName));
			textViewDestName.setText(destName); //.setText(String.format(getString(R.string.to), destName));
			textViewTime.setText(originTime);
			
			if (originLocationType.equals("ADR"))
			{
				textViewOriginName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_home, 0, 0, 0);
			} else if (originLocationType.equals("ST"))
			{
				textViewOriginName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_myplaces, 0, 0, 0);
			}
			if (destLocationType.equals("ADR"))
			{
				textViewDestName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_home, 0, 0, 0);
			} else if (destLocationType.equals("ST"))
			{
				textViewDestName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_myplaces, 0, 0, 0);
			}
			
			if (!TextUtils.isEmpty(track))
			{
				textViewTransportType.setText(String.format(getString(R.string.transport_type_and_track_number), cursor.getString(iName), cursor.getString(iRtTrack)));
			} else
			{
				textViewTransportType.setText(cursor.getString(iName));
			}

			int iconRes = TripLeg.getIcon(type);
			textViewTransportType.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
			if (!TextUtils.isEmpty(notes))
			{
				textViewNotes.setText(notes);
				textViewNotes.setVisibility(View.VISIBLE);
			} else
			{
				textViewNotes.setVisibility(View.GONE);
			}

			
			//textViewDeparturesIn.setText(cursor.getString(iDeparturesInTimeLabel));

			int geofenceTransitionType = cursor.getInt(iGeofenceTransition);
			if (geofenceTransitionType == Geofence.GEOFENCE_TRANSITION_ENTER)
			{
                imageViewChecked.setSelected(true);
			} else if (geofenceTransitionType == Geofence.GEOFENCE_TRANSITION_EXIT)
			{
                imageViewChecked.setSelected(true);
			} else
			{
                imageViewChecked.setSelected(false);
			}
			
			
			if (cursor.isLast())
			{
				textViewNotes.setVisibility(View.GONE);
				textViewTransportType.setVisibility(View.GONE);
				textViewDestName.setVisibility(View.GONE);
			} 
			else
			{
				textViewNotes.setVisibility(View.VISIBLE);
				textViewTransportType.setVisibility(View.VISIBLE);
				textViewDestName.setVisibility(View.VISIBLE);
			}		
			
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
				
				iCompleted = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.COMPLETED);
				iRtTrack = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.ORIGIN_RT_TRACK);
				iDestTrack = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.DEST_TRACK);
				iGeofenceTransition = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.GEOFENCE_EVENT_ID);
				iUrl =  newCursor.getColumnIndex(StopImagesMetaData.TableMetaData.URL);
				
			}
			return super.swapCursor(newCursor);
		}

		
		TripLeg getTripLeg(int position)
		{
			TripLeg leg = new TripLeg();
			Cursor c = getCursor();
			if (c.moveToPosition(position))
			{
				leg.originTime = c.getString(iOriginTime);
				leg.destTime = c.getString(iDestTime);
				leg.destName = c.getString(iDestName);
				leg.originName = c.getString(iOriginName);
				leg.type = c.getString(iType);
				leg.ref = c.getString(iRef);
				leg.notes = c.getString(iNotes);				
			}
			return leg;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		TripLeg leg = mTripAdapter.getTripLeg(position);
		leg.id = (int)id;
		leg.tripId = getArguments().getString(TripMetaData.TableMetaData._ID);
		if (!TextUtils.isEmpty(leg.ref))
		{
			Intent service = new Intent(getActivity(), JourneyDetailsService.class);
			service.putExtra(TripLeg.tag, leg);
			getActivity().startService(service);
			
			mCallbacks.onTripLegSelected(leg);
		}	
	}

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		if (!(activity instanceof GeofenceActivity))
		{
			throw new IllegalStateException("Activity must be a GeofenceActivity.");
		}
		if (!(activity instanceof MinRuteBaseActivity))
		{
			throw new IllegalStateException("Activity must be a MinRuteBaseActivity.");
		}
		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof Callbacks))
		{
			throw new IllegalStateException("Activity must implement fragment's callbacks.");
		}
		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof IImageDownloader))
		{
			throw new IllegalStateException("Activity must implement IImageDownloader.");
		}
		mIImageDownloader = (IImageDownloader)activity;

		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onDetach()
	{
		super.onDetach();

		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sDummyCallbacks;
		mIImageDownloader = null;
	}

	private Callbacks mCallbacks = sDummyCallbacks;

	/**
	 * A callback interface that all activities containing this fragment must
	 * implement.
	 */
	public interface Callbacks
	{
		public void onTripLegSelected(TripLeg leg);
	}

	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sDummyCallbacks = new Callbacks()
	{
		@Override
		public void onTripLegSelected(TripLeg leg)
		{
		}

	};


	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}

	public void onConnectedService()
	{
		String id = getArguments().getString(TripMetaData.TableMetaData._ID);
		((MinRuteBaseActivity) getActivity()).mServiceVoice.LoadTripIdForVoiceService(id, this);

	}

	@Override
	public void onVoiceServiceReady()
	{
		//((MinRuteBaseActivity) getActivity()).mServiceVoice.startDepartureTimer();
	}
}
