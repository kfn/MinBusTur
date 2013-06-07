package com.miracleas.minrute;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
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

import com.google.android.gms.location.Geofence;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;
import com.miracleas.minrute.model.GeofenceMy;
import com.miracleas.minrute.model.TripLeg;
import com.miracleas.minrute.model.TripLegStop;
import com.miracleas.minrute.net.DirectionsFetcher;
import com.miracleas.minrute.provider.AddressGPSMetaData;
import com.miracleas.minrute.provider.DirectionMetaData;
import com.miracleas.minrute.provider.GeofenceMetaData;
import com.miracleas.minrute.provider.TripLegDetailMetaData;
import com.miracleas.minrute.provider.TripLegDetailStopMetaData;
import com.miracleas.minrute.service.DirectionsService;
import com.miracleas.minrute.utils.App;

public class TripLegMapFragment extends SupportMapFragment implements LoaderCallbacks<Cursor>, OnCameraChangeListener, OnMarkerClickListener, OnInfoWindowClickListener, SensorEventListener
{
    private static final double TARGET_OFFSET_METERS = 2d;
    private TripLeg mLeg = null;
	private boolean mStartUp = true;
	private ProgressBar mProgressBar = null;
	private boolean mIsTwoPane = false;
	private Location mLocation = null;
    private SensorManager mSensorManager;
    private float[] mRotationMatrix = new float[16];
    private float[] mValues = new float[3];

    private Map<GeofenceMy, Circle> mCiclesMap = new HashMap<GeofenceMy, Circle>();
    private Map<Marker, LegStop> mMarkers = new HashMap<Marker, LegStop>();
    private LoadGpsOnAddressesTask mLoadGpsOnAddressesTask = null;

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

    private static final String[] PROJECTION_DIRECTION = {
            DirectionMetaData.TableMetaData.OVERVIEW_POLYLINE
    };
    private float mAzimuth;
    private float mTilt;
    
    private Object mValueAnimator = null;

    public static TripLegMapFragment createInstance(String journeyId, TripLeg leg)
	{
		TripLegMapFragment f = new TripLegMapFragment();
		Bundle args = new Bundle();
        if(!journeyId.equals("-1"))
        {
            args.putString(TripLegDetailMetaData.TableMetaData._ID, journeyId);
        }
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
        if(savedInstanceState==null)
        {
            Intent service = new Intent(getActivity(), DirectionsService.class);
            TripLeg leg = getArguments().getParcelable(TripLeg.tag);
            service.putExtra(TripLeg.tag, leg);
            getActivity().startService(service);
            android.animation.ValueAnimator f;
       
        }

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
					
					setUpMap(getMap());
					
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


    public void onStart()
    {
        super.onStart();
        registerSensor(getMap());
        startCicleAnimation();
    }

    public void onStop()
    {
        super.onStop();
        if(mSensorManager!=null)
        {
            mSensorManager.unregisterListener(this);
            mSensorManager = null;
        }
        stopCicleAnimation();

    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private void registerSensor(GoogleMap map)
    {

        if(mLeg!=null && mLeg.transitionType==Geofence.GEOFENCE_TRANSITION_ENTER && map!=null && App.SUPPORTS_GINGERBREAD && mSensorManager==null)
        {
            mSensorManager = (SensorManager)getActivity().getSystemService(Activity.SENSOR_SERVICE);
            Sensor vectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            mSensorManager.registerListener(this, vectorSensor, 1600);
            map.getUiSettings().setZoomControlsEnabled(false);
            map.getUiSettings().setZoomGesturesEnabled(false);

        }
    }
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void initAnimator()
    {
    	if(App.SUPPORTS_HONEYCOMB_MR1)
    	{
    		mValueAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f);
    		((android.animation.ValueAnimator)mValueAnimator).setDuration(1400);
    		((android.animation.ValueAnimator)mValueAnimator).setRepeatCount(android.animation.ValueAnimator.INFINITE);
    	}	 
    }
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void startCicleAnimation()
    {
        if(App.SUPPORTS_GINGERBREAD && mValueAnimator!=null)
        {
        	((android.animation.ValueAnimator)mValueAnimator).start();
        }
    }
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void stopCicleAnimation()
    {
        if(App.SUPPORTS_GINGERBREAD && mValueAnimator!=null)
        {
        	((android.animation.ValueAnimator)mValueAnimator).cancel();
        }
    }
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	private void showCircleAnimation()
    {    	
    	if(App.SUPPORTS_HONEYCOMB_MR1)
    	{
    		android.animation.ValueAnimator ani = (android.animation.ValueAnimator)mValueAnimator;   		
        	ani.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener()
        	{		
    			@Override
        		public void onAnimationUpdate(android.animation.ValueAnimator valueAnimator)
        		{
        			float animatedFraction = valueAnimator.getAnimatedFraction();
        			for(GeofenceMy g : mCiclesMap.keySet())
        			{
        				Circle c = mCiclesMap.get(g);
        				c.setRadius(animatedFraction * g.radius);
            			c.setStrokeWidth(1 + animatedFraction * 7);
        			}	
        		}
        	});
        	startCicleAnimation();
    	}
    	
    }

	/**
	 * This is where we can add markers or lines, add listeners or move the
	 * camera.
	 * <p>
	 * This should only be called once and when we are sure that {@link #getMap()}
	 * is not null.
	 */
	protected void setUpMap(GoogleMap map)
	{
		if (map != null)
		{           
            initAnimator();
            // Setting an info window adapter allows us to change the both the
			// contents and look of the
			// info window.
			//getMap().setInfoWindowAdapter(new CustomInfoWindowAdapter());
			map.setOnInfoWindowClickListener(this);
			map.setMyLocationEnabled(true);
			// Setting an info window adapter allows us to change the both the
			// contents and look of the
			// info window.
			map.setOnMarkerClickListener(this);
			map.getUiSettings().setCompassEnabled(true);
			map.setOnCameraChangeListener(this);
			Location loc = map.getMyLocation();
			if(loc!=null)
			{
				showPosition(loc.getLatitude(), loc.getLongitude());
			}
			showOriginDest();
			registerSensor(map);
			
			getActivity().getSupportLoaderManager().initLoader(LoaderConstants.LOADER_TRIP_LEG_STOPS_MAP, getArguments(), TripLegMapFragment.this);
            getActivity().getSupportLoaderManager().initLoader(LoaderConstants.LOADER_TRIP_LEG_DIRECTIONS, getArguments(), TripLegMapFragment.this);
            getActivity().getSupportLoaderManager().initLoader(LoaderConstants.LOAD_GEOFENCES_ON_MAP, getArguments(), TripLegMapFragment.this);
		}
	}


    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        if(getMap()==null && !getMap().isMyLocationEnabled())return;
        Location loc = getMap().getMyLocation();

        if(loc!=null)
        {
            LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());

            SensorManager.getRotationMatrixFromVector(mRotationMatrix, sensorEvent.values);
            SensorManager.getOrientation(mRotationMatrix, mValues);
            mAzimuth = (float)Math.toDegrees(mValues[0]); //direction north
            mTilt = (float)Math.toDegrees(mValues[1]); //device tilt

            float clamped = clamp(0f,mTilt,67.5f);

            CameraPosition cameraPosition = CameraPosition.builder().tilt(clamped).
                    bearing(mAzimuth).
                    zoom(19 + 4 * (mTilt / 90)).
                    target(SphericalUtil.computeOffset(latLng, TARGET_OFFSET_METERS, mAzimuth)).
                    build();

            getMap().moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }

    }

    private float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i)
    {

    }


	@Override
	public boolean onMarkerClick(final Marker marker)
	{


		LegStop wrap = mMarkers.get(marker);
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
		LegStop wrap = mMarkers.get(marker);
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
		if (id == LoaderConstants.LOADER_TRIP_LEG_STOPS_MAP && args.containsKey(TripLegDetailMetaData.TableMetaData._ID))
		{
			String selection = TripLegDetailStopMetaData.TableMetaData.JOURNEY_DETAIL_ID + "=?";
			String[] selectionArgs = { args.getString(TripLegDetailMetaData.TableMetaData._ID) };
			return new CursorLoader(getActivity(), TripLegDetailStopMetaData.TableMetaData.CONTENT_URI, PROJECTION_STOP, selection, selectionArgs, TripLegDetailStopMetaData.TableMetaData.ROUTE_ID_X);
		}
        else if (id == LoaderConstants.LOADER_TRIP_LEG_DIRECTIONS && args.containsKey(TripLeg.tag))
        {
            TripLeg leg = args.getParcelable(TripLeg.tag);
            String selection = DirectionMetaData.TableMetaData.TRIP_LEG_ID + "=? AND "+DirectionMetaData.TableMetaData.DIRECTION_MODE + "=?";
            String[] selectionArgs = { leg.id + "" , DirectionsFetcher.getMode(leg)};
            return new CursorLoader(getActivity(), DirectionMetaData.TableMetaData.CONTENT_URI, PROJECTION_DIRECTION, selection, selectionArgs, null);
        }
        else if (id == LoaderConstants.LOAD_GEOFENCES_ON_MAP && args.containsKey(TripLeg.tag))
        {
            TripLeg leg = args.getParcelable(TripLeg.tag);
            String selection = GeofenceMetaData.TableMetaData.TRIP_ID + "=?";			      
            String[] selectionArgs = { leg.tripId + "" };
            return new CursorLoader(getActivity(), GeofenceMetaData.TableMetaData.CONTENT_URI, null, selection, selectionArgs, null);
        }
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
	{
		if (loader.getId() == LoaderConstants.LOADER_TRIP_LEG_STOPS_MAP)
		{
			showStopsOnMap(cursor);
		}
        else if (loader.getId() == LoaderConstants.LOADER_TRIP_LEG_DIRECTIONS && cursor.moveToFirst())
        {
            int iPolyLines = cursor.getColumnIndex(DirectionMetaData.TableMetaData.OVERVIEW_POLYLINE);
            String poly = cursor.getString(iPolyLines);
            final List<LatLng> decode = PolyUtil.decode(poly);
            PolylineOptions rute = new PolylineOptions();
            Builder bounds = LatLngBounds.builder();
            for (LatLng aDecode : decode)
            {
            	bounds.include(aDecode);
                rute.add(aDecode);
            }
            //puntos is an array where the array returned by the decodePoly method are stored
            rute.color(Color.RED).width(7);
            getMap().addPolyline(rute);
            
            if(!decode.isEmpty() && mLeg!=null && mLeg.transitionType!=Geofence.GEOFENCE_TRANSITION_ENTER)
            {
            	if(decode.isEmpty())
            	{
            		showOriginDest();
            	}
            	else
            	{
            		LatLngBounds latLngBounds = bounds.build();            	
    				getMap().moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100));
            	}
            	
            }

        }
        else if (loader.getId() == LoaderConstants.LOAD_GEOFENCES_ON_MAP)
        {
        	displayGeofences(cursor);
        }
	}
	
	private void displayGeofences(Cursor c)
	{
		if(c.moveToFirst())
		{
			int color = Color.rgb(223, 212, 251);
			GoogleMap map = getMap();
			TripLeg leg = getArguments().getParcelable(TripLeg.tag);
			int iId = c.getColumnIndex(GeofenceMetaData.TableMetaData.geofence_id);
			int iLat = c.getColumnIndex(GeofenceMetaData.TableMetaData.LAT);
			int iLng = c.getColumnIndex(GeofenceMetaData.TableMetaData.LNG);
			int iRadius = c.getColumnIndex(GeofenceMetaData.TableMetaData.RADIUS);
			int iTransType = c.getColumnIndex(GeofenceMetaData.TableMetaData.TRANSITION_TYPE);
			int iCurrentTransType = c.getColumnIndex(GeofenceMetaData.TableMetaData.CURRENT_TRANSITION_STATE);
			do{
				GeofenceMy g = new GeofenceMy(leg.tripId, c.getString(iId), c.getInt(iRadius), c.getInt(iTransType), c.getDouble(iLat),
						c.getDouble(iLng));
				g.currentTransType = c.getInt(iCurrentTransType);
				
				CircleOptions options = new CircleOptions()
			     .center(new LatLng(g.lat, g.lng))
			     .radius(g.radius)			     
			     .strokeColor(color)
			     .strokeWidth(3f)
			     ;
			
				Circle circle = map.addCircle(options);				
				mCiclesMap.put(g, circle);
			}while(c.moveToNext());
			//showCircleAnimation();
		}
	}
 


	@Override
	public void onLoaderReset(Loader<Cursor> cursor)
	{
        if(cursor.getId() == LoaderConstants.LOADER_TRIP_LEG_STOPS_MAP)
        {
            clearMap();
            mProgressBar.setVisibility(View.VISIBLE);
        }
        else if(cursor.getId() == LoaderConstants.LOADER_TRIP_LEG_DIRECTIONS)
        {
        	
        }
        else if (cursor.getId() == LoaderConstants.LOAD_GEOFENCES_ON_MAP)
        {
        	removeGeofenceCircles();
        }

	}

	@Override
	public void onResume()
	{
		super.onResume();
	}



	private void showStopsOnMap(final Cursor data)
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
					mMarkers.put(addedMarker, current);
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
				getMap().moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100));
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

        for(Marker m : mMarkers.keySet())
        {
            m.remove();
        }
        mMarkers.clear();
		mProgressBar.setVisibility(View.VISIBLE);
	}
	
	private void removeGeofenceCircles()
	{
		for(GeofenceMy g : mCiclesMap.keySet())
		{
			Circle c = mCiclesMap.get(g);
			c.remove();
		}
		mCiclesMap.clear();
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
	
	private void showOriginDest()
	{
		if(mLoadGpsOnAddressesTask==null || mLoadGpsOnAddressesTask.getStatus()==AsyncTask.Status.FINISHED)
		{
			mLoadGpsOnAddressesTask = new LoadGpsOnAddressesTask();
			mLoadGpsOnAddressesTask.execute(mLeg);
		}
	}
	
	private class LoadGpsOnAddressesTask extends AsyncTask<TripLeg, Void, List<LatLng>>	
	{

		@Override
		protected List<LatLng> doInBackground(TripLeg... params)
		{
			TripLeg leg = params[0];
			List<LatLng> list = new ArrayList<LatLng>(2);
			list.add(getLatLng(leg.originName));
			list.add(getLatLng(leg.destName));
			return list;
		}
		
		private LatLng getLatLng(String address)
		{
			LatLng latlng = null;
			String[] projection = {AddressGPSMetaData.TableMetaData.LATITUDE_Y, AddressGPSMetaData.TableMetaData.LONGITUDE_X};
			Cursor c = null;
			try
			{
				String selection = AddressGPSMetaData.TableMetaData.ADDRESS + "=?";
				String[] selectionArgs = {address};
				c = getActivity().getContentResolver().query(AddressGPSMetaData.TableMetaData.CONTENT_URI, projection, selection, selectionArgs, AddressGPSMetaData.TableMetaData.ADDRESS+ " LIMIT 1");
				if(c.moveToFirst())
				{
					double lat = (double)(c.getInt(c.getColumnIndex(AddressGPSMetaData.TableMetaData.LATITUDE_Y)) / 1000000d);
					double lng = (double)(c.getInt(c.getColumnIndex(AddressGPSMetaData.TableMetaData.LONGITUDE_X)) / 1000000d);
					latlng = new LatLng(lat, lng);
				}
			}
			finally
			{
				c.close();
			}
			
			return latlng;
		}
		
		public void onPostExecute(List<LatLng> result)
		{
			Builder bounds = LatLngBounds.builder();
			for(LatLng latlng : result)
			{
				if(latlng!=null)
				{
					bounds.include(latlng);
				}
			}
			if(!result.isEmpty())
			{				
				getMap().moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100));				
			}			
		}
		
	}
}
