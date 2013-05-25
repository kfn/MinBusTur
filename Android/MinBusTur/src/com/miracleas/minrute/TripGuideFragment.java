package com.miracleas.minrute;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
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

import com.miracleas.minrute.TripSuggestionsFragment.Callbacks;
import com.miracleas.minrute.model.Trip;
import com.miracleas.minrute.model.TripLeg;
import com.miracleas.minrute.model.TripRequest;
import com.miracleas.minrute.provider.JourneyDetailMetaData;
import com.miracleas.minrute.provider.JourneyDetailStopMetaData;
import com.miracleas.minrute.provider.TripLegMetaData;
import com.miracleas.minrute.provider.TripMetaData;
import com.miracleas.minrute.provider.TripMetaData.TableMetaData;
import com.miracleas.minrute.provider.TripVoiceMetaData;
import com.miracleas.minrute.service.JourneyDetailsGeofenceService;
import com.miracleas.minrute.service.JourneyDetailsService;
import com.miracleas.minrute.service.ReceiveTransitionsIntentService;
import com.miracleas.minrute.utils.App;

public class TripGuideFragment extends SherlockListFragment implements LoaderCallbacks<Cursor>, OnItemClickListener, com.miracleas.minrute.service.UpdateVoiceTripService.OnVoiceServiceReadyListener
{
	private static final String[] PROJECTION = { TripLegMetaData.TableMetaData._ID, TripLegMetaData.TableMetaData.DEST_DATE, TripLegMetaData.TableMetaData.DEST_NAME, TripLegMetaData.TableMetaData.DEST_ROUTE_ID, TripLegMetaData.TableMetaData.DEST_TIME,
			TripLegMetaData.TableMetaData.DEST_TYPE, TripLegMetaData.TableMetaData.ORIGIN_DATE, TripLegMetaData.TableMetaData.ORIGIN_NAME, TripLegMetaData.TableMetaData.ORIGIN_ROUTE_ID, TripLegMetaData.TableMetaData.ORIGIN_TIME, TripLegMetaData.TableMetaData.ORIGIN_TYPE,
			TripLegMetaData.TableMetaData.DURATION, TripLegMetaData.TableMetaData.DURATION_FORMATTED, TripLegMetaData.TableMetaData.NAME, TripLegMetaData.TableMetaData.NOTES, TripLegMetaData.TableMetaData.REF, TripLegMetaData.TableMetaData.TYPE,
			TripLegMetaData.TableMetaData.ORIGIN_RT_TRACK, TripLegMetaData.TableMetaData.DEST_TRACK,

			TripLegMetaData.TableMetaData.PROGRESS_BAR_PROGRESS, TripLegMetaData.TableMetaData.PROGRESS_BAR_MAX, TripLegMetaData.TableMetaData.DEPARTURES_IN_TIME_LABEL, TripLegMetaData.TableMetaData.COMPLETED };
	private static final String[] PROJECTION_LEGS = { TripLegMetaData.TableMetaData.REF, TripLegMetaData.TableMetaData._ID };
	
	private static final String[] PROJECTION_JORNEY_DETAILS = { JourneyDetailStopMetaData.TableMetaData._ID, JourneyDetailStopMetaData.TableMetaData.LATITUDE, JourneyDetailStopMetaData.TableMetaData.LONGITUDE, JourneyDetailStopMetaData.TableMetaData.LEG_ID,
			JourneyDetailStopMetaData.TableMetaData.DEP_DATE, JourneyDetailStopMetaData.TableMetaData.DEP_TIME };
	
	private static final String[] PROJECTION_TRIP = { 
		TripVoiceMetaData.TableMetaData.DEPARTURES_IN
	};
	private TripAdapter mTripAdapter = null;
	private long[] mLegIds;
	private String mTextToSpeak;

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
		outState.putString("mTextToSpeak", mTextToSpeak);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View listView = super.onCreateView(inflater, container, savedInstanceState);
		View rootView = inflater.inflate(R.layout.fragment_trip_guide, container, false);
		FrameLayout frame = (FrameLayout) rootView.findViewById(R.id.listContainer);
		frame.addView(listView);
		if(savedInstanceState!=null)
		{
			mTextToSpeak = savedInstanceState.getString("mTextToSpeak");
		}
		
		return rootView;

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		mTripAdapter = new TripAdapter(getActivity(), null, 0);
		mLegIds = new long[2];
		getListView().setOnItemClickListener(this);
		setListAdapter(mTripAdapter);
		getLoaderManager().initLoader(LoaderConstants.LOAD_TRIP_LEGS, getArguments(), this);
		getLoaderManager().initLoader(LoaderConstants.LOAD_TRIP_LEG_FIRST_LAST, getArguments(), this);
		getLoaderManager().initLoader(LoaderConstants.LOAD_START_END_POSITION, getArguments(), this);
		
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args)
	{
		if (id == LoaderConstants.LOAD_TRIP_LEGS)
		{
			String selection = TripLegMetaData.TableMetaData.TRIP_ID + "=?";
			String[] selectionArgs = { args.getString(TripMetaData.TableMetaData._ID) };
			return new CursorLoader(getActivity(), TripLegMetaData.TableMetaData.CONTENT_URI, PROJECTION, selection, selectionArgs, null);
		} else if (id == LoaderConstants.LOAD_TRIP_LEG_FIRST_LAST)
		{
			String selection = TripLegMetaData.TableMetaData.TRIP_ID + "=? AND " + TripLegMetaData.TableMetaData.TYPE + " NOT LIKE ?";
			String[] selectionArgs = { args.getString(TripMetaData.TableMetaData._ID), "WALK" };
			return new CursorLoader(getActivity(), TripLegMetaData.TableMetaData.CONTENT_URI, PROJECTION_LEGS, selection, selectionArgs, TripLegMetaData.TableMetaData.STEP_NUMBER);
		} else if (id == LoaderConstants.LOAD_START_END_POSITION)
		{
			String selection = JourneyDetailStopMetaData.TableMetaData.TRIP_ID + "=?";
			String[] selectionArgs = { args.getString(TripMetaData.TableMetaData._ID) };
			return new CursorLoader(getActivity(), JourneyDetailStopMetaData.TableMetaData.CONTENT_URI, PROJECTION_JORNEY_DETAILS, selection, selectionArgs, null);
		}
		/*else if (id == LoaderConstants.LOAD_GUIDE_VOICE_TRIP)
		{
			String selection =  TripVoiceMetaData.TableMetaData.TRIP_ID + "=?";
			String[] selectionArgs = {args.getString(TripMetaData.TableMetaData._ID)};
			return new CursorLoader(getActivity(), TripVoiceMetaData.TableMetaData.CONTENT_URI, PROJECTION_TRIP, selection, selectionArgs, TripVoiceMetaData.TableMetaData._ID+" LIMIT 1");
		}*/
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor newCursor)
	{
		if (loader.getId() == LoaderConstants.LOAD_TRIP_LEGS)
		{
			mTripAdapter.swapCursor(newCursor);
		} 
		else if (loader.getId() == LoaderConstants.LOAD_TRIP_LEG_FIRST_LAST)
		{
			if (newCursor.moveToFirst() && getArguments() != null)
			{
				Intent service = new Intent(getActivity(), JourneyDetailsGeofenceService.class);
				service.putExtra(JourneyDetailsGeofenceService.TRIP_ID, getArguments().getString(TripLegMetaData.TableMetaData._ID));
				loadTripLegs(newCursor, service, true);
				if (newCursor.moveToLast())
				{
					loadTripLegs(newCursor, service, false);
					getActivity().startService(service);
					getLoaderManager().destroyLoader(LoaderConstants.LOAD_TRIP_LEG_FIRST_LAST);
				}

			}
		}
		else if (loader.getId() == LoaderConstants.LOAD_START_END_POSITION)
		{
			if (newCursor.moveToFirst())
			{

				Geofence g1 = loadGeofenceInfo(newCursor, true);
				if (newCursor.moveToLast())
				{
					Geofence g2 = loadGeofenceInfo(newCursor, false);
					List<Geofence> geofences = new ArrayList<Geofence>();
					if (loadStartAndEndGeofences(geofences))
					{
						geofences.add(g1);
						geofences.add(g2);
						GeofenceActivity geo = (GeofenceActivity) getActivity();
						geo.addGeofences(geofences);
						getLoaderManager().destroyLoader(LoaderConstants.LOAD_START_END_POSITION);
					}
				}
			}

		}
		/*else if(loader.getId() == LoaderConstants.LOAD_GUIDE_VOICE_TRIP)
		{
			if(newCursor.moveToFirst())
			{
				String s = newCursor.getString(newCursor.getColumnIndex(TripVoiceMetaData.TableMetaData.DEPARTURES_IN));
				mTextToSpeak = String.format(getString(R.string.voice_departure), s);
				MinRuteBaseActivity base = (MinRuteBaseActivity)getActivity();
				if(base.mService.isVoiceInitialized())
				{
					base.mService.playVoice(mTextToSpeak);
				}
				else
				{
					base.mService.setTextToSpeak(mTextToSpeak);
					startTextSpeach();
				}				
			}
		}*/
	}

	private void loadTripLegs(Cursor newCursor, Intent service, boolean first)
	{
		int iRef = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.REF);
		int iLegId = newCursor.getColumnIndex(TripLegMetaData.TableMetaData._ID);
		long id = newCursor.getLong(iLegId);
		int i = first ? 0 : 1;
		mLegIds[i] = id;
		int count = i + 1;
		service.putExtra(JourneyDetailsGeofenceService.URL + count, newCursor.getString(iRef));
		service.putExtra(JourneyDetailsGeofenceService.LEG + count, id + "");
	}

	private Geofence loadGeofenceInfo(Cursor newCursor, boolean first)
	{
		int iId = newCursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData._ID);
		int iLat = newCursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.LATITUDE);
		int iLng = newCursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.LONGITUDE);
		int iLegId = newCursor.getColumnIndex(JourneyDetailStopMetaData.TableMetaData.LEG_ID);

		long stopId = newCursor.getLong(iId);
		double lat = (double) newCursor.getInt(iLat) / 1000000d;
		double lng = (double) newCursor.getInt(iLng) / 1000000d;
		int transitionId = -1;
		if (first) // skal laves om, saa foerste geofence er den placering hvor
					// man er
		{
			transitionId = Geofence.GEOFENCE_TRANSITION_ENTER;
		} else
		{
			transitionId = Geofence.GEOFENCE_TRANSITION_ENTER;
		}
		Geofence g = toGeofence(stopId + "", transitionId, lat, lng, 10, DateUtils.HOUR_IN_MILLIS);
		return g;
	}

	private boolean loadStartAndEndGeofences(List<Geofence> geofences)
	{
		boolean savedGeofences = false;
		TripRequest tripRequest = getArguments().getParcelable(TripRequest.tag);
		if (tripRequest != null)
		{
			double lat = (double) (Integer.parseInt(tripRequest.getOriginCoordY()) / 1000000d);
			double lng = (double) (Integer.parseInt(tripRequest.getOriginCoordX()) / 1000000d);
			Geofence origin1 = toGeofence(mLegIds[0] + "enter", Geofence.GEOFENCE_TRANSITION_ENTER, lat, lng, 10, DateUtils.HOUR_IN_MILLIS);
			Geofence origin2 = toGeofence(mLegIds[0] + "exit", Geofence.GEOFENCE_TRANSITION_EXIT, lat, lng, 10, DateUtils.HOUR_IN_MILLIS);

			lat = (double) (Integer.parseInt(tripRequest.getDestCoordY()) / 1000000d);
			lng = (double) (Integer.parseInt(tripRequest.getDestCoordX()) / 1000000d);
			Geofence dest = toGeofence(mLegIds[1] + "", Geofence.GEOFENCE_TRANSITION_ENTER, lat, lng, 10, DateUtils.HOUR_IN_MILLIS);

			geofences.add(origin1);
			geofences.add(origin2);
			geofences.add(dest);
			savedGeofences = true;
		}
		return savedGeofences;
	}

	public Geofence toGeofence(String id, int transitionType, double lat, double lng, float radius, long expirationDuration)
	{
		// Build a new Geofence object
		return new Geofence.Builder().setRequestId(id).setTransitionTypes(transitionType).setCircularRegion(lat, lng, radius).setExpirationDuration(expirationDuration).build();
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
		private int iGeofenceTransition = 0;
		private int iRtTrack = 0;
		private int iDestTrack = 0;

		private LayoutInflater mInf = null;
		

		public TripAdapter(Context context, Cursor c, int flags)
		{
			super(context, c, flags);
			mInf = LayoutInflater.from(context);
		}

		@Override
		public void bindView(View v, Context context, Cursor cursor)
		{

			
			String originTime = cursor.getString(iOriginTime);
			TextView textViewTime = (TextView) v.findViewById(R.id.textViewTime);
			textViewTime.setText(originTime);

			TextView textViewOriginName = (TextView) v.findViewById(R.id.textViewOriginName);
			textViewOriginName.setText(String.format(getString(R.string.from), cursor.getString(iOriginName)));

			String originLocationType = cursor.getString(iOriginType);
			if (originLocationType.equals("ADR"))
			{
				textViewOriginName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_home, 0, 0, 0);
			} else if (originLocationType.equals("ST"))
			{
				textViewOriginName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_myplaces, 0, 0, 0);
			}
			TextView textViewDestName = (TextView) v.findViewById(R.id.textViewDestName);
			if (cursor.isLast())
			{
				textViewTime.setText(originTime + " - " + cursor.getString(iDestTime));
				textViewDestName.setText(String.format(getString(R.string.to), cursor.getString(iDestName)));

				String destLocationType = cursor.getString(iDestType);
				if (destLocationType.equals("ADR"))
				{
					textViewDestName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_home, 0, 0, 0);
				} else if (destLocationType.equals("ST"))
				{
					textViewDestName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_myplaces, 0, 0, 0);
				}
				textViewDestName.setVisibility(View.VISIBLE);
			} else
			{
				textViewDestName.setVisibility(View.GONE);
			}

			String type = cursor.getString(iType);
			String track = cursor.getString(iRtTrack);
			TextView textViewTransportType = (TextView) v.findViewById(R.id.textViewTransportType);
			if (!TextUtils.isEmpty(track))
			{
				textViewTransportType.setText(String.format(getString(R.string.transport_type_and_track_number), cursor.getString(iName), cursor.getString(iRtTrack)));
			} else
			{
				textViewTransportType.setText(cursor.getString(iName));
			}

			int iconRes = TripLeg.getIcon(type);
			textViewTransportType.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);

			String notes = cursor.getString(iNotes);
			TextView textViewNotes = (TextView) v.findViewById(R.id.textViewNotes);
			if (!TextUtils.isEmpty(notes))
			{
				textViewNotes.setText(notes);
				textViewNotes.setVisibility(View.VISIBLE);
			} else
			{
				textViewNotes.setVisibility(View.GONE);
			}

			TextView textViewDeparturesIn = (TextView) v.findViewById(R.id.textViewDeparturesIn);
			textViewDeparturesIn.setText(cursor.getString(iDeparturesInTimeLabel));

			int geofenceTransitionType = cursor.getInt(iGeofenceTransition);
			if (geofenceTransitionType == Geofence.GEOFENCE_TRANSITION_ENTER)
			{
				v.setBackgroundResource(R.color.green_transparent);
			} else if (geofenceTransitionType == Geofence.GEOFENCE_TRANSITION_EXIT)
			{
				v.setBackgroundResource(R.color.black_transparent);
			} else
			{
				v.setBackgroundResource(R.drawable.selectable_background_minrutevejledning);
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
				iDeparturesInTimeLabel = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.DEPARTURES_IN_TIME_LABEL);
				iCompleted = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.COMPLETED);
				iRtTrack = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.ORIGIN_RT_TRACK);
				iDestTrack = newCursor.getColumnIndex(TripLegMetaData.TableMetaData.DEST_TRACK);
			}
			return super.swapCursor(newCursor);
		}

		String getRef(int position)
		{
			String ref = "";
			Cursor c = getCursor();
			if (c.moveToPosition(position))
			{
				ref = c.getString(iRef);
			}
			return ref;
		}

		String getType(int position)
		{
			String type = "";
			Cursor c = getCursor();
			if (c.moveToPosition(position))
			{
				type = c.getString(iType);
			}
			return type;
		}

		String getOriginName(int position)
		{
			String name = "";
			Cursor c = getCursor();
			if (c.moveToPosition(position))
			{
				name = c.getString(iOriginName);
			}
			return name;
		}

		String getDestName(int position)
		{
			String name = "";
			Cursor c = getCursor();
			if (c.moveToPosition(position))
			{
				name = c.getString(iDestName);
			}
			return name;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		String ref = mTripAdapter.getRef(position);
		String type = mTripAdapter.getType(position);
		String tripId = getArguments().getString(TripMetaData.TableMetaData._ID);
		if (!TextUtils.isEmpty(ref))
		{
			String origin = mTripAdapter.getOriginName(position);
			String dest = mTripAdapter.getDestName(position);
			Intent service = new Intent(getActivity(), JourneyDetailsService.class);
			service.putExtra(JourneyDetailsService.LEG, id + "");
			service.putExtra(JourneyDetailsService.TRIP_ID, tripId);
			service.putExtra(JourneyDetailsService.URL, ref);
			service.putExtra(JourneyDetailsService.ADDRESS_DEST, dest);
			service.putExtra(JourneyDetailsService.ADDRESS_ORIGIN, origin);
			getActivity().startService(service);
		}

		mCallbacks.onTripLegSelected(tripId, id + "", ref, type);
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

		mCallbacks = (Callbacks) activity;
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
		public void onTripLegSelected(String tripId, String legId, String ref, String transportType);
	}

	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sDummyCallbacks = new Callbacks()
	{
		@Override
		public void onTripLegSelected(String tripId, String legId, String ref, String transportType)
		{
		}

	};
	private int MY_DATA_CHECK_CODE = 554;

	private void startTextSpeach()
	{
		Intent checkIntent = new Intent();
		checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		getActivity().startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{	
		if (App.SUPPORTS_JELLY_BEAN || requestCode == MY_DATA_CHECK_CODE)
		{
			if (App.SUPPORTS_JELLY_BEAN || resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS)
			{
				// success, create the TTS instance
				MinRuteBaseActivity base = (MinRuteBaseActivity)getActivity();
				base.mService.startTextToSpeech();
			} else
			{
				// missing data, install it
				Intent installIntent = new Intent();
				installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(installIntent);
			}
		}
		
	}
	@Override
	public void onDestroy()
	{
		super.onDestroy();	
	}

	public void onConnectedService()
	{
		String id = getArguments().getString(TripMetaData.TableMetaData._ID);
		((MinRuteBaseActivity)getActivity()).mService.initVoiceService(id, this);
		
	}

	@Override
	public void onVoiceServiceReady()
	{
		//getLoaderManager().initLoader(LoaderConstants.LOAD_GUIDE_VOICE_TRIP, getArguments(), this);	
		startTextSpeach();
	}
}
