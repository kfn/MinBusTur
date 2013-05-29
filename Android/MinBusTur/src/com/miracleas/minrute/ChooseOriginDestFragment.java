package com.miracleas.minrute;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.LruCache;
import android.support.v4.widget.CursorAdapter;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.FilterQueryProvider;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.miracleas.minrute.TripSuggestionsFragment.Callbacks;
import com.miracleas.minrute.model.AddressSearch;
import com.miracleas.minrute.model.TripRequest;
import com.miracleas.minrute.net.AddressFetcher;
import com.miracleas.minrute.net.TripFetcher;
import com.miracleas.minrute.provider.AddressProviderMetaData;
import com.miracleas.minrute.service.TripService;
import com.miracleas.minrute.utils.ViewHelper;

public class ChooseOriginDestFragment extends ChooseOriginDestFragmentBase
{
	public static final String tag = ChooseOriginDestFragment.class.getName();
	
	private LoadAddressesFromRun mLoadAddressesFromRun = null;
	private LoadAddressesToRun mLoadAddressesToRun = null;
	
	private LoadTrips mLoadTrips = null;
	protected AddressFetcher mAddressFetcher = null;
	private TripFetcher mTripFetcher = null;
	private static boolean mIsLoadingAddresses = false;
	private boolean mUpdateCursor = true;
	private boolean mItemClicked = false;
	

	public static ChooseOriginDestFragment createInstance()
	{
		ChooseOriginDestFragment fragment = new ChooseOriginDestFragment();
		Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}

	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = super.onCreateView(inflater, container, savedInstanceState);
		mAutoCompleteAdapterFrom = new AutoCompleteAddressAdapter(getActivity(), null, 0, LoaderConstants.LOADER_ADDRESS_FROM, "FromAdapter");
		mAutoCompleteTextViewFromAddress.setAdapter(mAutoCompleteAdapterFrom);
		mAutoCompleteAdapterTo = new AutoCompleteAddressAdapter(getActivity(), null, 0, LoaderConstants.LOADER_ADDRESS_TO, "ToAdapter");
		mAutoCompleteTextViewToAddress.setAdapter(mAutoCompleteAdapterTo);		
		return rootView;
	}

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public ChooseOriginDestFragment()
	{
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mHandler = new Handler();
		mLoadAddressesFromRun = new LoadAddressesFromRun(null, 0);		
		mLoadAddressesToRun = new LoadAddressesToRun(null, 0);		
		mDataUri = AddressProviderMetaData.TableMetaData.CONTENT_URI;
		mAddressFetcher = new AddressFetcher(getActivity(), mDataUri);
	}

	/*@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args)
	{
		if (id == LoaderConstants.LOADER_ADDRESS_FROM || id == LoaderConstants.LOADER_ADDRESS_TO)
		{
			String address = args.getString(AddressProviderMetaData.TableMetaData.address);
			Log.d(tag, "create loader id " + id + ": " + address);
			String[] selectionArgs = { address };
			return new CursorLoader(getActivity(), AddressProviderMetaData.TableMetaData.CONTENT_URI, PROJECTION, null, selectionArgs, AddressProviderMetaData.TableMetaData.address + " LIMIT 20");
		} else if (id == LoaderConstants.LOADER_TITLE_FROM || id == LoaderConstants.LOADER_TITLE_TO)
		{
			String name = args.getString(ContactsContract.Contacts.DISPLAY_NAME);
			Log.d(tag, "create loader: " + name);
			String selection = ContactsContract.Contacts.DISPLAY_NAME + " LIKE '%" + name + "%'";
			String[] selectionArgs = null;// { name };
			return new CursorLoader(getActivity(), ContactsContract.Contacts.CONTENT_URI, PROJECTION_CONTACTS, selection, null, null);
		}
		return null;
	}*/

	/*@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor newCursor)
	{
		if (newCursor.isClosed())
			return;
		Log.d(tag, "onLoadFinished id Swap cursor");
		int currentLoaderId = loader.getId();
		int focusedLoader = getActiveLoader();
		if (currentLoaderId != focusedLoader)
		{
			Log.d(tag, "ignored loader changed");
		} else if (loader.getId() == LoaderConstants.LOADER_ADDRESS_FROM)
		{
			mAutoCompleteAdapterFrom.swapCursor(newCursor);
			updateCursor(newCursor, mAutoCompleteTextViewFromAddress);
		} else if (loader.getId() == LoaderConstants.LOADER_ADDRESS_TO)
		{
			mAutoCompleteAdapterTo.swapCursor(newCursor);
			updateCursor(newCursor, mAutoCompleteTextViewToAddress);
		} 
	}*/

	/*private void updateCursor(Cursor newCursor, AutoCompleteTextView a)
	{

		if (newCursor != null && newCursor.getCount() > 0)
		{
			if (!a.isPopupShowing())
			{
				a.showDropDown();
				Log.d(tag, "showDropDown");
			}
		}

	}*/

	/*@Override
	public void onLoaderReset(Loader<Cursor> loader)
	{
		if (loader.getId() == LoaderConstants.LOADER_ADDRESS_FROM)
		{
			mAutoCompleteAdapterFrom.swapCursor(null);
		} else if (loader.getId() == LoaderConstants.LOADER_ADDRESS_TO)
		{
			mAutoCompleteAdapterTo.swapCursor(null);
		} 
	}*/
	

	@Override
	public void textChangedHelper(CharSequence s, int loaderId)
	{
		Log.d(tag, "textChangedHelper");
		final String value = s.toString();
		if (!mItemClicked && value.length() > THRESHOLD)
		{
			if (loaderId == LoaderConstants.LOADER_ADDRESS_TO || loaderId == LoaderConstants.LOADER_ADDRESS_FROM)
			{
				boolean doAddressSearch = true;
				if (loaderId == LoaderConstants.LOADER_ADDRESS_TO)
				{
					doAddressSearch = !mTripRequest.destCoordNameNotEncoded.equals(value);
					if(doAddressSearch)
					{
						mHandler.removeCallbacks(mLoadAddressesToRun);
						mLoadAddressesToRun = new LoadAddressesToRun(value, loaderId);
						mHandler.postDelayed(mLoadAddressesToRun, 1000);
					}
					
				} else
				{
					doAddressSearch = !mTripRequest.originCoordNameNotEncoded.equals(value);
					if(doAddressSearch)
					{
						mHandler.removeCallbacks(mLoadAddressesFromRun);
						mLoadAddressesFromRun = new LoadAddressesFromRun(value, loaderId);
						mHandler.postDelayed(mLoadAddressesFromRun, 1000);
					}
				}


			} 
			/*else if (loaderId == LoaderConstants.LOADER_TITLE_FROM || loaderId == LoaderConstants.LOADER_TITLE_TO)
			{
				mHandler.removeCallbacks(mLoadContactRun);
				mLoadContactRun = new LoadContactRun(value, loaderId);
				mHandler.postDelayed(mLoadContactRun, 200);
			}*/
		}

	}

	private LoadAddresses mLoadAddresses = null;

	private void loadAddress(String query, int loaderId)
	{
		if (mLoadAddresses == null || mLoadAddresses.getStatus() == AsyncTask.Status.FINISHED)
		{
			mLoadCount++;
			query = query.trim();
			Log.d(tag, "lookup for: " + query);
			mLoadAddresses = new LoadAddresses(loaderId);
			mLoadAddresses.execute(query);
			
			getProgressBar(loaderId).setVisibility(View.VISIBLE);
			if (loaderId == LoaderConstants.LOADER_ADDRESS_FROM)
			{
				mAutoCompleteAdapterFrom.runQueryOnBackgroundThread(query);
			} 
			else if (loaderId == LoaderConstants.LOADER_ADDRESS_TO)
			{
				mAutoCompleteAdapterTo.runQueryOnBackgroundThread(query);
			}
		} 
		else
		{
			if(loaderId==LoaderConstants.LOADER_ADDRESS_TO)
			{
				Log.d(tag, "delay lookup for: " + query);
				mHandler.removeCallbacks(mLoadAddressesToRun);
				mLoadAddressesToRun = new LoadAddressesToRun(query, loaderId);
				mHandler.postDelayed(mLoadAddressesToRun, 500);
			}
			else if(loaderId==LoaderConstants.LOADER_ADDRESS_TO)
			{
				Log.d(tag, "delay lookup for: " + query);
				mHandler.removeCallbacks(mLoadAddressesFromRun);
				mLoadAddressesFromRun = new LoadAddressesFromRun(query, loaderId);
				mHandler.postDelayed(mLoadAddressesFromRun, 500);
			}
			
		}
	}

	private ProgressBar getProgressBar(int loaderId)
	{
		if (loaderId == LoaderConstants.LOADER_ADDRESS_FROM)
		{
			return mProgressBarFromAddress;
		} else 
		{
			return mProgressBarToAddress;
		}
	}

	private class LoadAddresses extends AsyncTask<String, Void, Void>
	{
		private int mLoaderId = 0;
		private String mAddress = null;
		public void onPreExecute()
		{
			mIsLoadingAddresses = true;
		}

		LoadAddresses(int loaderId)
		{
			mLoaderId = loaderId;
		}

		@Override
		protected Void doInBackground(String... params)
		{
			mAddress = params[0];
			try
			{

				mAddressFetcher.performGeocode(params[0]); // mDataUri
			} catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return null;
		}

		protected void onPostExecute(Void result)
		{
			mIsLoadingAddresses = false;
			mLoadCount--;
			getProgressBar(mLoaderId).setVisibility(View.GONE);
			if(mLoaderId==LoaderConstants.LOADER_ADDRESS_FROM)
			{		
				mAutoCompleteAdapterFrom.getFilter().filter(mAddress);
			}
			else if(mLoaderId==LoaderConstants.LOADER_ADDRESS_TO)
			{
				mAutoCompleteAdapterTo.getFilter().filter(mAddress);
			}	
		}
	}

	private class LoadAddressesToRun implements Runnable
	{
		private final String mQuery;
		private final int loaderId;

		LoadAddressesToRun(String q, int loaderId)
		{
			mQuery = q;
			this.loaderId = loaderId;
		}

		@Override
		public void run()
		{
			loadAddress(mQuery, loaderId);
		}
	}


	private class LoadAddressesFromRun implements Runnable
	{
		private final String mQuery;
		private final int loaderId;

		LoadAddressesFromRun(String q, int loaderId)
		{
			mQuery = q;
			this.loaderId = loaderId;
		}

		@Override
		public void run()
		{
			loadAddress(mQuery, loaderId);
		}
	}


	private class LoadTrips extends AsyncTask<String, Void, Void>
	{
		@Override
		protected Void doInBackground(String... params)
		{

			try
			{
				mTripFetcher.tripSearch(mTripRequest);
			} catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return null;
		}

		protected void onPostExecute(Void result)
		{
			
		}
	}

	@Override
	public void onClick(View v)
	{
		super.onClick(v);
		if (v.getId() == R.id.btnFindRoute)
		{		
			onAddressSelected(mSelectedAddressFromPosition, mSelectedAddressToPosition);	
			/*int posFrom = mSelectedAddressFromPosition;
			int posTo = mSelectedAddressToPosition;
			if(posFrom!=-1 && posTo!=-1)
			{
				onAddressSelected(posFrom, posTo);			
			}
			else if(mTripRequest.isValid())
			{
				mCallbacks.onFindTripSuggestion(mTripRequest);
			}
			else
			{
				String from = mAutoCompleteTextViewFromAddress.getHint().toString();				
				String to = mAutoCompleteTextViewToAddress.getHint().toString();
				Toast.makeText(getActivity(), getString(R.string.trip_request_not_valid), Toast.LENGTH_SHORT).show();
			}*/
			
			
		}
	}
	
	private void onAddressSelected2(boolean origin)
	{
		mItemClicked = true;
		if (origin)
		{
			mAutoCompleteTextViewToAddress.requestFocus();
		} else
		{
			ViewHelper.hideKeyboard(getActivity(), mAutoCompleteTextViewToAddress);
		}
		

	}
	private AddressSelected mAddressSelected = null;
	private void onAddressSelected(int positionOrigin, int positionDest)
	{
		if(mAddressSelected==null || mAddressSelected.getStatus()==AsyncTask.Status.FINISHED)
		{
			mAddressSelected = new AddressSelected();
			mAddressSelected.execute(positionOrigin,positionDest);
		}
		else
		{
			mItemClicked = false;
		}
	}
	
	private class AddressSelected extends AsyncTask<Integer, Void, Void>
	{
		
		@Override
		protected Void doInBackground(Integer... params)
		{
			if(params[0]==-1)
			{
				int originPos = mAutoCompleteAdapterFrom.getPosition(mAutoCompleteTextViewFromAddress.getHint().toString());
				params[0] = originPos;
			}
			if(params[1]==-1)
			{
				int destPos = mAutoCompleteAdapterTo.getPosition(mAutoCompleteTextViewToAddress.getHint().toString());
				params[1] = destPos;
			}
			
			String text = mAutoCompleteAdapterFrom.getAddress(params[0]);
			String y = mAutoCompleteAdapterFrom.getY(params[0]);
			String x = mAutoCompleteAdapterFrom.getX(params[0]);
			String positionId = mAutoCompleteAdapterFrom.getId(params[0]);
			mTripRequest.setOriginId(positionId);
			mTripRequest.setOriginCoordName(text);
			mTripRequest.setOriginCoordX(x);
			mTripRequest.setOriginCoordY(y);
			
			text = mAutoCompleteAdapterTo.getAddress(params[1]);
			y = mAutoCompleteAdapterTo.getY(params[1]);
			x = mAutoCompleteAdapterTo.getX(params[1]);
			positionId = mAutoCompleteAdapterTo.getId(params[1]);
			mTripRequest.setDestId(positionId);
			mTripRequest.setDestCoordName(text);
			mTripRequest.setDestCoordX(x);
			mTripRequest.setDestCoordY(y);		
			return null;
		}
		
		

		protected void onPostExecute(Void result)
		{
			mItemClicked = false;
			
			if(mTripRequest.destCoordNameNotEncoded.equals(mTripRequest.originCoordNameNotEncoded))
			{
				Toast.makeText(getActivity(), getString(R.string.start_end_must_not_be_equal), Toast.LENGTH_SHORT).show();
			}
			else if(mTripRequest.isValid())
			{
				mCallbacks.onFindTripSuggestion(mTripRequest);
			}
			else
			{
				Toast.makeText(getActivity(), getString(R.string.trip_request_not_valid), Toast.LENGTH_SHORT).show();
			}
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
		public void onFindTripSuggestion(TripRequest tripRequest);
	}

	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sDummyCallbacks = new Callbacks()
	{

		@Override
		public void onFindTripSuggestion(TripRequest tripRequest)
		{
			// TODO Auto-generated method stub

		}

	};
}
