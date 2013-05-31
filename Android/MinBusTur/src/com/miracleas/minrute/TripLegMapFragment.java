package com.miracleas.minrute;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.database.Cursor;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.miracleas.minrute.model.TripLeg;
import com.miracleas.minrute.model.TripLegStop;
import com.miracleas.minrute.provider.TripLegDetailMetaData;
import com.miracleas.minrute.provider.TripLegDetailStopMetaData;

public class TripLegMapFragment extends SupportMapFragment implements LoaderCallbacks<Cursor>, OnCameraChangeListener, OnMarkerClickListener, OnInfoWindowClickListener
{
	private TripLeg mLeg = null;
	private boolean mStartUp = true;
	private ProgressBar mProgressBar = null;
	private boolean mIsTwoPane = false;
	private Location mLocation = null;

	private Map<LatLng, LegStop> mMarkers = new HashMap<LatLng, LegStop>();
	/**
	 * The columns needed by the cursor adapter
	 */
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
	
	public static TripLegMapFragment createInstance(String journeyId, TripLeg leg)
	{
		TripLegMapFragment f = new TripLegMapFragment();
		Bundle args = new Bundle();
		args.putString(TripLegDetailMetaData.TableMetaData._ID, journeyId);
		args.putParcelable(TripLeg.tag, leg);
		f.setArguments(args);
		return f;
	}
	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public TripLegMapFragment()
	{
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

		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onDetach()
	{
		super.onDetach();

		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sDummyCallbacks;
	}

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		final View mapView = super.onCreateView(inflater, container, savedInstanceState);
		View v = inflater.inflate(R.layout.map, null);
		FrameLayout frame = (FrameLayout)v.findViewById(R.id.map);
		frame.addView(mapView);
		
		mLeg = getArguments().getParcelable(TripLeg.tag);
		
		if (mapView.getViewTreeObserver().isAlive())
		{
			mapView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener()
			{
				@SuppressLint("NewApi")
				// We check which build version we are using.
				@Override
				public void onGlobalLayout()
				{
					if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
					{
						mapView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
						if (mLocation != null)
						{
							showPosition(mLocation.getLatitude(), mLocation.getLongitude());
						}
					} else
					{
						mapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
						if (mLocation != null)
						{
							showPosition(mLocation.getLatitude(), mLocation.getLongitude());
						}
					}
					
					setUpMap();
					getActivity().getSupportLoaderManager().initLoader(LoaderConstants.LOADER_TRIP_LEG_STOPS_MAP, getArguments(), TripLegMapFragment.this);
				}
			});
		}
		mProgressBar = (ProgressBar) v.findViewById(R.id.progressBar1);
		if (savedInstanceState != null)
		{
			mLocation = (Location) savedInstanceState.getParcelable("location");
		}
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		if (getMap() != null)
		{
			Location loc = getCenterLocation();
			if(loc!=null)
			{
				outState.putParcelable("location", loc);
			}
			
		}

	}

	/**
	 * This is where we can add markers or lines, add listeners or move the
	 * camera.
	 * <p>
	 * This should only be called once and when we are sure that {@link #getMap()}
	 * is not null.
	 */
	protected void setUpMap()
	{
		if (getMap() != null)
		{

			// Setting an info window adapter allows us to change the both the
			// contents and look of the
			// info window.
			//getMap().setInfoWindowAdapter(new CustomInfoWindowAdapter());
			getMap().setOnInfoWindowClickListener(this);
			
			getMap().setMyLocationEnabled(true);
			// Setting an info window adapter allows us to change the both the
			// contents and look of the
			// info window.
			getMap().setOnMarkerClickListener(this);

			getMap().setOnCameraChangeListener(this);
			Location loc = getMap().getMyLocation();
			if(loc!=null)
			{
				showPosition(loc.getLatitude(), loc.getLongitude());
			}
			
		}
	}

	@Override
	public boolean onMarkerClick(final Marker marker)
	{
		LatLng pos = marker.getPosition();

		LegStop wrap = mMarkers.get(pos);
		if (wrap != null)
		{

			if (mIsTwoPane)
			{

			} else
			{
				/*LayoutInflater inf = LayoutInflater.from(getActivity());
				View popupView = inf.inflate(R.layout.toilet_dialog_info, null);  
	             final PopupWindow popupWindow = new PopupWindow(
	               popupView, 
	               RelativeLayout.LayoutParams.WRAP_CONTENT,  
	               RelativeLayout.LayoutParams.WRAP_CONTENT); 
	           
	             
	             popupWindow.showAtLocation(getView(), Gravity.LEFT , 40, 50);*/
	             
	             
				//ToiletDialog.showDialog(getSherlockActivity(), wrap.iconRes, marker.getTitle(), wrap.getDistance(), marker.getSnippet(), wrap.latLng.latitude, wrap.latLng.longitude, wrap.dbId);
			}

		}
		return false;
	}
	
	@Override
	public void onInfoWindowClick(Marker marker)
	{
		LegStop wrap = mMarkers.get(marker.getPosition());
		TripLegStop stop = new TripLegStop(wrap.latitude+"", wrap.longitude+"", marker.getSnippet(), wrap.dbId);
		mCallbacks.onStopSelected(stop, mLeg);
	}

	@Override
	public void onCameraChange(CameraPosition pos)
	{

	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args)
	{
		if (id == LoaderConstants.LOADER_TRIP_LEG_STOPS_MAP)
		{			
			String selection = TripLegDetailStopMetaData.TableMetaData.JOURNEY_DETAIL_ID + "=?";
			String[] selectionArgs = { args.getString(TripLegDetailMetaData.TableMetaData._ID) };			
			return new CursorLoader(getActivity(), TripLegDetailStopMetaData.TableMetaData.CONTENT_URI, PROJECTION_STOP, selection, selectionArgs, TripLegDetailStopMetaData.TableMetaData.ROUTE_ID_X);
		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
	{
		if (loader.getId() == LoaderConstants.LOADER_TRIP_LEG_STOPS_MAP)
		{
			showToiletsOnMapHelper(cursor);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> cursor)
	{
		clearMap();
		mProgressBar.setVisibility(View.VISIBLE);
	}

	@Override
	public void onResume()
	{
		super.onResume();
	}



	private void showToiletsOnMapHelper(final Cursor data)
	{
		clearMap();
		if (getMap() != null && data.moveToFirst())
		{			
			Location myLocation = getMap().getMyLocation();
			int iId = data.getColumnIndex(TripLegDetailStopMetaData.TableMetaData._ID);
			int iArrTime = data.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.ARR_TIME);
			int iDepTime = data.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.DEP_TIME);
			int iLat = data.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.LATITUDE);
			int iLng = data.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.LONGITUDE);
			int iName = data.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.NAME);		
			int iTrack = data.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.TRACK);	
			int iPartOfUserRoute = data.getColumnIndex(TripLegDetailStopMetaData.TableMetaData.IS_PART_OF_USER_ROUTE);	
			
			Builder bounds = LatLngBounds.builder();

			LegStop closestToilet = null;
			do
			{
				String strLat = data.getString(iLat);
				String strLng = data.getString(iLng);
				if(!TextUtils.isEmpty(strLat) && !TextUtils.isEmpty(strLng))
				{
					String arrTime = data.getString(iArrTime);
					String track = data.getString(iTrack);;
					String depTime = data.getString(iDepTime);
					
					double lat = (double)(Integer.parseInt(strLat) / 1000000d);
					double lng = (double)(Integer.parseInt(strLng) / 1000000d);
					Location toiletLocation = new Location("");
					toiletLocation.setLatitude(lat);
					toiletLocation.setLongitude(lng);
					float toiletDistance = -1;
					if (myLocation != null && mStartUp)
					{
						toiletDistance = myLocation.distanceTo(toiletLocation);
					}

					boolean hasTrack = !TextUtils.isEmpty(data.getString(iTrack));
					boolean isPartOfUsersRoute = data.getInt(iPartOfUserRoute)==1;
					int iconRes = getIconRes(hasTrack, isPartOfUsersRoute);
					int id = data.getInt(iId);
					LegStop current = new LegStop(new LatLng(lat, lng), toiletDistance, id, iconRes, strLat, strLng);

					if (closestToilet == null || toiletDistance < closestToilet.distance)
					{
						closestToilet = current;
					}

					String address = data.getString(iName);
					String time = "";
					if(!TextUtils.isEmpty(arrTime) && !TextUtils.isEmpty(depTime) && !depTime.equals(arrTime))
					{
						time = "ank. "+arrTime+"/afg. "+depTime;
					}
					else if(!TextUtils.isEmpty(arrTime))
					{
						time = arrTime;
					}
					else if(!TextUtils.isEmpty(arrTime))
					{
						time = depTime;
					}		

					MarkerOptions marker = new MarkerOptions().position(current.latLng).title(time).snippet(address).icon(BitmapDescriptorFactory.fromResource(iconRes));
					Marker addedMarker = getMap().addMarker(marker);
					mMarkers.put(addedMarker.getPosition(), current);
					if(isPartOfUsersRoute)
					{
						bounds.include(addedMarker.getPosition());
					}
					
				}
				
				
				

			} while (data.moveToNext());

			mStartUp = false;

			LatLngBounds visibleBounds = this.getMap().getProjection().getVisibleRegion().latLngBounds;
			boolean zoom = getMap().getCameraPosition().zoom < 14 || !visibleBounds.contains(closestToilet.latLng);
			if (zoom)
			{
				if (myLocation != null)
				{
					bounds = LatLngBounds.builder();
					bounds.include(closestToilet.latLng).include(new LatLng(myLocation.getLatitude(), myLocation.getLongitude()));
				}

				LatLngBounds latLngBounds = bounds.build();
				getMap().animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100));
			}
			
		}
		mProgressBar.setVisibility(View.GONE);
	}

	private int getIconRes(boolean hasTrack, boolean isStopInUsersRoute)
	{
		
		int resId = 0;
		
		if(isStopInUsersRoute)
		{
			resId = R.drawable.ic_menu_myplaces; //ic_menu_myplaces_black
		}
		else
		{
			resId = android.R.drawable.ic_menu_myplaces;
		}
		return resId;
	}

	public void showPosition(double lat, double lng)
	{
		if (getMap() != null && lat != -1 && lng != -1)
		{
			final LatLng position = new LatLng(lat, lng);
			LatLngBounds bounds = new LatLngBounds.Builder().include(position).build();
			getMap().moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
			getMap().moveCamera(CameraUpdateFactory.zoomTo(16));
		}
	}

	private boolean checkReady()
	{
		if (getMap() == null)
		{
			return false;
		}
		return true;
	}

	private void clearMap()
	{
		if (!checkReady())
		{
			return;
		}
		getMap().clear();
		mMarkers.clear();
		mProgressBar.setVisibility(View.VISIBLE);
	}

	public void onDestroy()
	{
		super.onDestroy();
	}

	public Location getCenterLocation()
	{
		
		LatLng latLng = getMap().getCameraPosition().target;
		Location loc = new Location("");
		loc.setLatitude(latLng.latitude);
		loc.setLongitude(latLng.longitude);
		return loc;
	}

	private class LegStop
	{
		LatLng latLng = null;
		int dbId = -1;
		float distance = 0;
		int iconRes = -1;
		String latitude;
		String longitude;

		public LegStop(LatLng latLng, float distance, int dbId, int iconRes, String lat, String lng)
		{
			super();
			this.latLng = latLng;
			this.dbId = dbId;
			this.distance = distance;
			this.iconRes = iconRes;
			this.latitude = lat;
			this.longitude = lng;
		}

		public String getDistance()
		{
			String result = "";
			if (distance > 1000)
			{
				result = getFormatedValue((distance / 1000), 2) + " km";
			} else if (distance != -1)
			{
				result = getFormatedValue(distance, 2) + " m";
			}
			return result;
		}
	}

	/**
	 * The fragment's current callback object, which is notified of list item
	 * clicks.
	 */
	private Callbacks mCallbacks = sDummyCallbacks;

	/**
	 * A callback interface that all activities containing this fragment must
	 * implement. This mechanism allows activities to be notified of item
	 * selections.
	 */
	public interface Callbacks
	{
		/**
		 * Callback for when an item has been selected.
		 */
		public void onStopSelected(TripLegStop stop, TripLeg leg);

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
	};
	static NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("da", "dk"));

	public static String getFormatedValue(double paramNumber, int noOfDigit)
	{
		double tempSubtractNum = paramNumber % (10 * noOfDigit);
		double tempResultNum = (paramNumber - tempSubtractNum);
		if (tempSubtractNum >= (5 * noOfDigit))
		{
			tempResultNum = tempResultNum + (10 * noOfDigit);
		}
		return currencyFormat.format(tempResultNum);
	}

	public void setLocation(Location location)
	{
		this.mLocation = location;
	}
		
}
