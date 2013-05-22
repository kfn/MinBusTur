package com.miracleas.minbustur;

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

import com.miracleas.minbustur.TripSuggestionsFragment.Callbacks;
import com.miracleas.minbustur.model.AddressSearch;
import com.miracleas.minbustur.model.TripRequest;
import com.miracleas.minbustur.net.AddressFetcher;
import com.miracleas.minbustur.net.TripFetcher;
import com.miracleas.minbustur.provider.AddressProviderMetaData;
import com.miracleas.minbustur.service.TripService;
import com.miracleas.minbustur.utils.ViewHelper;

public class FindTripSuggestionsFragment extends FindTripSuggestionsFragmentBase
{
	public static final String tag = FindTripSuggestionsFragment.class.getName();
	private AutoCompleteAddressAdapter mAutoCompleteAdapterFrom = null;
	private AutoCompleteAddressAdapter mAutoCompleteAdapterTo = null;	
	private LoadAddressesRun mLoadAddressesRun = null;
	
	private LoadTrips mLoadTrips = null;
	protected AddressFetcher mAddressFetcher = null;
	private TripFetcher mTripFetcher = null;
	private static boolean mIsLoadingAddresses = false;
	private boolean mUpdateCursor = true;
	private boolean mItemClicked = false;
	

	public static FindTripSuggestionsFragment createInstance()
	{
		FindTripSuggestionsFragment fragment = new FindTripSuggestionsFragment();
		Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}

	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = super.onCreateView(inflater, container, savedInstanceState);
		mAutoCompleteAdapterFrom = new AutoCompleteAddressAdapter(getActivity(), null, 0, LoaderConstants.LOADER_ADDRESS_FROM);
		mAutoCompleteTextViewFromAddress.setAdapter(mAutoCompleteAdapterFrom);
		mAutoCompleteAdapterTo = new AutoCompleteAddressAdapter(getActivity(), null, 0, LoaderConstants.LOADER_ADDRESS_TO);
		mAutoCompleteTextViewToAddress.setAdapter(mAutoCompleteAdapterTo);		
		return rootView;
	}

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public FindTripSuggestionsFragment()
	{
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mHandler = new Handler();
		mLoadAddressesRun = new LoadAddressesRun(null, 0);
		

		mDataUri = null;// Uri.withAppendedPath(AddressProviderMetaData.TableMetaData.CONTENT_URI,
						// "search");
		mAddressFetcher = new AddressFetcher(getActivity(), mDataUri);
		Bundle args = new Bundle();
		args.putString(AddressProviderMetaData.TableMetaData.address, "a");
		args.putString(ContactsContract.Contacts.DISPLAY_NAME, "abc");
	}

	@Override
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
	}

	@Override
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
		} /*else if (loader.getId() == LoaderConstants.LOADER_TITLE_FROM)
		{
			mAutoCompleteContactsAdapterFrom.swapCursor(newCursor);
			updateCursor(newCursor, mAutoCompleteTextViewFromTitle);
		} else if (loader.getId() == LoaderConstants.LOADER_TITLE_TO)
		{
			mAutoCompleteContactsAdapterTo.swapCursor(newCursor);
			updateCursor(newCursor, mAutoCompleteTextViewToTitle);
		}*/
	}

	private void updateCursor(Cursor newCursor, AutoCompleteTextView a)
	{

		if (newCursor != null && newCursor.getCount() > 0)
		{
			if (!a.isPopupShowing())
			{
				a.showDropDown();
				Log.d(tag, "showDropDown");
			}
		}

	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader)
	{
		if (loader.getId() == LoaderConstants.LOADER_ADDRESS_FROM)
		{
			mAutoCompleteAdapterFrom.swapCursor(null);
		} else if (loader.getId() == LoaderConstants.LOADER_ADDRESS_TO)
		{
			mAutoCompleteAdapterTo.swapCursor(null);
		} /*else if (loader.getId() == LoaderConstants.LOADER_TITLE_FROM)
		{
			mAutoCompleteContactsAdapterFrom.swapCursor(null);
		} else if (loader.getId() == LoaderConstants.LOADER_TITLE_TO)
		{
			mAutoCompleteContactsAdapterTo.swapCursor(null);
		}*/
	}

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
				} else
				{
					doAddressSearch = !mTripRequest.originCoordNameNotEncoded.equals(value);
				}
				if (doAddressSearch)
				{
					mHandler.removeCallbacks(mLoadAddressesRun);
					mLoadAddressesRun = new LoadAddressesRun(value, loaderId);
					mHandler.postDelayed(mLoadAddressesRun, 1000);
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

	@Override
	protected void onAddressFromSelected(int position)
	{
		//onAddressSelected(mAutoCompleteAdapterFrom, true, position);
	}
	@Override
	protected void onAddressToSelected(int position)
	{
		//onAddressSelected(mAutoCompleteAdapterTo, false, position);
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
			Bundle args = new Bundle();
			args.putString(AddressProviderMetaData.TableMetaData.address, query);
			getProgressBar(loaderId).setVisibility(View.VISIBLE);
			if (loaderId == LoaderConstants.LOADER_ADDRESS_FROM)
			{
				mAutoCompleteAdapterFrom.runQueryOnBackgroundThread(query);
			} else if (loaderId == LoaderConstants.LOADER_ADDRESS_TO)
			{
				mAutoCompleteAdapterTo.runQueryOnBackgroundThread(query);
			}
		} else
		{
			Log.d(tag, "delay lookup for: " + query);
			mHandler.removeCallbacks(mLoadAddressesRun);
			mLoadAddressesRun = new LoadAddressesRun(query, loaderId);
			mHandler.postDelayed(mLoadAddressesRun, 500);
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
			/*int activeLoader = getActiveLoader();
			if(activeLoader==LoaderConstants.LOADER_ADDRESS_FROM)
			{
				if(!mAutoCompleteTextViewFromAddress.isPopupShowing())
				{
					mAutoCompleteTextViewFromAddress.showDropDown();
				}
			}
			else
			{
				if(!mAutoCompleteTextViewToAddress.isPopupShowing())
				{
					mAutoCompleteTextViewToAddress.showDropDown();
				}
			}*/
		}
	}

	private class AutoCompleteAddressAdapter extends CursorAdapter
	{
		private int iAddress;
		private int iY;
		private int iX;
		private int iType;
		private int iId;
		private LayoutInflater mInf = null;
		private CharSequence mPreviousConstraint = "";
		
		public AutoCompleteAddressAdapter(Context context, Cursor c, int flags, final int loaderId)
		{
			super(context, c, flags);
			mInf = LayoutInflater.from(context);

			setFilterQueryProvider(new FilterQueryProvider()
			{
				@Override
				public Cursor runQuery(CharSequence constraint)
				{
					// Stop the FQP from looking for nothing
					if (constraint == null)
						return null;
					constraint = constraint.toString().trim();
					if(!mPreviousConstraint.equals(constraint))
					{
						textChangedHelper(constraint, loaderId);
						mPreviousConstraint = constraint;
					}

					Log.d(tag, "autocomplete: "+constraint);
					StringBuilder b = new StringBuilder();
					b.append(AddressProviderMetaData.TableMetaData.address).append(" LIKE '").append(constraint).append("%') GROUP BY (").append(AddressProviderMetaData.TableMetaData.address);
					return getActivity().getContentResolver().query(AddressProviderMetaData.TableMetaData.CONTENT_URI, PROJECTION, b.toString(), null, AddressProviderMetaData.TableMetaData.address+" LIMIT 20");
				}
			});
			
		}
		
		//you need to override this to return the string value when
	    //selecting an item from the autocomplete suggestions
	    //just do cursor.getstring(whatevercolumn);
	    @Override
	    public CharSequence convertToString(Cursor cursor) {
	    	return cursor.getString(iAddress);
	    }

		@Override
		public void changeCursor(Cursor newCursor)
		{
			Cursor oldCursor = getCursor();
			super.changeCursor(newCursor);
			if (oldCursor != null && oldCursor != newCursor)
			{
				// adapter has already dealt with closing the cursor
				getActivity().stopManagingCursor(oldCursor);
			}
			getActivity().startManagingCursor(newCursor);
		}

		@Override
		public void bindView(View v, Context context, Cursor cursor)
		{
			TextView tv = (TextView) v;
			tv.setText(cursor.getString(iAddress));
			int icon = getIcon(cursor.getInt(iType));
			if (icon != -1)
			{
				tv.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
			}

		}

		private int getIcon(int type)
		{
			switch (type)
			{
			case AddressSearch.TYPE_ADRESSE:
				return R.drawable.ic_menu_home;
			case AddressSearch.TYPE_STATION_STOP:
				return R.drawable.ic_menu_myplaces;
			default:
				return -1;

			}
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			return mInf.inflate(R.layout.item_address, null);
		}

		public Cursor swapCursor(Cursor newCursor)
		{
			if (newCursor != null)
			{
				iAddress = newCursor.getColumnIndex(AddressProviderMetaData.TableMetaData.address);
				iY = newCursor.getColumnIndex(AddressProviderMetaData.TableMetaData.Y);
				iX = newCursor.getColumnIndex(AddressProviderMetaData.TableMetaData.X);
				iType = newCursor.getColumnIndex(AddressProviderMetaData.TableMetaData.type_int);
				iId = newCursor.getColumnIndex(AddressProviderMetaData.TableMetaData.id);
			}
			return super.swapCursor(newCursor);
		}

		public String getY(int position)
		{
			String lat = null;
			Cursor c = getCursor();
			if (c.moveToPosition(position))
			{
				lat = c.getString(iY);
			}
			return lat;
		}

		public String getX(int position)
		{
			String lng = null;
			Cursor c = getCursor();
			if (c.moveToPosition(position))
			{
				lng = c.getString(iX);
			}
			return lng;
		}

		public String getAddress(int position)
		{
			String address = null;
			Cursor c = getCursor();
			if (c.moveToPosition(position))
			{
				address = c.getString(iAddress);
			}
			return address;
		}

		public String getId(int position)
		{
			String address = null;
			Cursor c = getCursor();
			if (c.moveToPosition(position))
			{
				address = c.getString(iId);
			}
			return address;
		}
		
		public int getPosition(String address)
		{
			int pos = -1;
			Cursor c = getCursor();
			boolean found = false;
			if(c.moveToFirst())
			{
				do{
					String current = c.getString(iAddress);
					if(current.equals(address))
					{
						pos = c.getPosition();
						found = true;
					}
				}
				while(c.moveToNext() && !found);
			}
			return pos;
		}
	}





	private class LoadAddressesRun implements Runnable
	{
		private final String mQuery;
		private final int loaderId;

		LoadAddressesRun(String q, int loaderId)
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
		if (v.getId() == R.id.btnFindRoute)
		{		
			int posFrom = mSelectedAddressFromPosition;
			int posTo = mSelectedAddressToPosition;
			if(posFrom!=-1 && posTo!=-1)
			{
				onAddressSelected(posFrom, posTo);			
			}
			else if(mTripRequest.isValid())
			{
				mCallbacks.onFindTripSuggestion(mTripRequest);
			}
			
			
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
			mCallbacks.onFindTripSuggestion(mTripRequest);
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
