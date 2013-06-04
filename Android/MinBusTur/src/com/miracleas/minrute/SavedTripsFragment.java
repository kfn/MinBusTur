package com.miracleas.minrute;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockListFragment;
import com.miracleas.minrute.TripSuggestionsFragment.Callbacks;
import com.miracleas.minrute.model.TripLegStop;
import com.miracleas.minrute.model.TripRequest;
import com.miracleas.minrute.provider.SavedTripMetaData;
import com.miracleas.minrute.provider.TripLegDetailStopMetaData;
import com.miracleas.minrute.provider.TripMetaData;

public class SavedTripsFragment extends SherlockFragment implements LoaderCallbacks<Cursor>, OnItemClickListener
{
	public static final String tag = SavedTripsFragment.class.getName();
    private TripAdapter mTripAdapter = null;
    private View mLoadingView = null;
    private ListView mListView;
    
	public static SavedTripsFragment createInstance()
	{
		SavedTripsFragment f = new SavedTripsFragment();
		Bundle args = new Bundle();
		f.setArguments(args);
		return f;
	}

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public SavedTripsFragment()
	{
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_saved_trips, null);
        mListView = (ListView)view.findViewById(android.R.id.list);
        mListView.setOnItemClickListener(this);
        View empty = getLayoutInflater(savedInstanceState).inflate(R.layout.empty_list_view, null);
        TextView textViewEmpty = (TextView)empty.findViewById(R.id.textViewEmptyText);
        textViewEmpty.setText(R.string.no_saved_trips);
        setEmptyView(empty);
               
        return view;
    }


    @Override
    public void onViewCreated(View v, Bundle savedInstanceState)
    {
        super.onViewCreated(v, savedInstanceState);
        mTripAdapter = new TripAdapter(getActivity(), null, 0);
        mListView.setAdapter(mTripAdapter);      
        getLoaderManager().initLoader(LoaderConstants.LOAD_SAVED_TRIPS, getArguments(), this);
        
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
	public Loader<Cursor> onCreateLoader(int id, Bundle args)
	{
		if (id == LoaderConstants.LOAD_SAVED_TRIPS)
		{
			String selection = null;
			String[] selectionArgs = null;
			return new CursorLoader(getActivity(), SavedTripMetaData.TableMetaData.CONTENT_URI, null, selection, selectionArgs, SavedTripMetaData.TableMetaData.TITLE);
		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
	{
		if(loader.getId()==LoaderConstants.LOAD_SAVED_TRIPS)
		{
			mTripAdapter.swapCursor(cursor);
		}					
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		mCallbacks.onSavedTripSelected(mTripAdapter.getTripRequest(position));		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader)
	{
		if(loader.getId()==LoaderConstants.LOAD_SAVED_TRIPS)
		{
            mTripAdapter.swapCursor(null);
		}	
	}

    private class TripAdapter extends CursorAdapter
    {
        private int iTitle;
        private int iDestAddress;
        private int iDestId;
        private int iOriginAddress;
        private int iOriginId;
        private int iWayPointAddress;
        private int iWayPointId;
        private int iOriginX;
        private int iOriginY;
        private int iDestX;
        private int iDestY;
        private int iSearchForArrival;

        private LayoutInflater mInf = null;

        public TripAdapter(Context context, Cursor c, int flags)
        {
            super(context, c, flags);
            mInf = LayoutInflater.from(context);
        }

        @Override
        public void bindView(View v, Context context, Cursor cursor)
        {
            TextView textViewTitle = (TextView)v.findViewById(R.id.textViewTitle);
            TextView textViewOrigin = (TextView)v.findViewById(R.id.textViewOrigin);
            TextView textViewWaypoint = (TextView)v.findViewById(R.id.textViewWaypoint);
            TextView textViewDestination = (TextView)v.findViewById(R.id.textViewDestination);

            String title = cursor.getString(iTitle);
            String origin = cursor.getString(iOriginAddress);
            String waypoint = cursor.getString(iWayPointAddress);
            String destination = cursor.getString(iDestAddress);

            textViewTitle.setText(title);
            textViewOrigin.setText(origin);
            if(TextUtils.isEmpty(waypoint))
            {
                textViewWaypoint.setText("");
                textViewWaypoint.setVisibility(View.GONE);
            }
            else
            {
                textViewWaypoint.setText(waypoint);
                textViewWaypoint.setVisibility(View.VISIBLE);
            }
            textViewDestination.setText(destination);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent)
        {
            return mInf.inflate(R.layout.item_saved_trips, null);
        }

        public Cursor swapCursor(Cursor newCursor)
        {
            if (newCursor != null)
            {
                iTitle = newCursor.getColumnIndex(SavedTripMetaData.TableMetaData.TITLE);
                iDestAddress = newCursor.getColumnIndex(SavedTripMetaData.TableMetaData.DESTINATION_ADDRESS);
                iDestId = newCursor.getColumnIndex(SavedTripMetaData.TableMetaData.DESTINATION_ID);
                iOriginAddress = newCursor.getColumnIndex(SavedTripMetaData.TableMetaData.ORIGIN_ADDRESS);
                iOriginId = newCursor.getColumnIndex(SavedTripMetaData.TableMetaData.ORIGIN_ID);
                iWayPointAddress = newCursor.getColumnIndex(SavedTripMetaData.TableMetaData.WAY_POINT_ADDRESS);
                iWayPointId = newCursor.getColumnIndex(SavedTripMetaData.TableMetaData.WAY_POINT_ID);
                iOriginX = newCursor.getColumnIndex(SavedTripMetaData.TableMetaData.ORIGIN_LNG_X);
                iOriginY = newCursor.getColumnIndex(SavedTripMetaData.TableMetaData.ORIGIN_LAT_Y);
                iDestY = newCursor.getColumnIndex(SavedTripMetaData.TableMetaData.DESTINATION_LAT_Y);
                iDestX = newCursor.getColumnIndex(SavedTripMetaData.TableMetaData.DEST_LNG_X);
                iSearchForArrival = newCursor.getColumnIndex(SavedTripMetaData.TableMetaData.SEARCH_FOR_ARRIVAL);
            }
            return super.swapCursor(newCursor);
        }
        
        public TripRequest getTripRequest(int position)
        {
        	TripRequest r = new TripRequest();
        	Cursor cursor = getCursor();
        	if(cursor.moveToPosition(position))
        	{       		
                String origin = cursor.getString(iOriginAddress);
                String waypoint = cursor.getString(iWayPointAddress);
                String destination = cursor.getString(iDestAddress);
                String originId = cursor.getString(iOriginId);
                String waypointId = cursor.getString(iWayPointId);
                String destinationId = cursor.getString(iDestId);
                int destLat = cursor.getInt(iDestY);
                int destLng = cursor.getInt(iDestX);
                int originLat = cursor.getInt(iOriginY);
                int originLng = cursor.getInt(iOriginX);
                int searchForArrival = cursor.getInt(iSearchForArrival);
                r = new TripRequest(origin, waypoint, destination, originId, waypointId, destinationId, destLat, destLng, originLat, originLng, searchForArrival);
        	}
        	return r;
        }
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
		public void onSavedTripSelected(TripRequest tripRequest);
	}

	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sDummyCallbacks = new Callbacks()
	{

		@Override
		public void onSavedTripSelected(TripRequest tripRequest)
		{
			// TODO Auto-generated method stub
			
		}
		
	};


}