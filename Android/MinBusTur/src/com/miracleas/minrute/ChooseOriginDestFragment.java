package com.miracleas.minrute;

import java.util.ArrayList;
import java.util.List;

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
		
	protected AddressFetcher mAddressFetcher = null;
	private TripFetcher mTripFetcher = null;
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
			String fromAddress = getAddress(mAutoCompleteTextViewFromAddress, getString(R.string.startaddress));
			String toAddress = getAddress(mAutoCompleteTextViewToAddress, getString(R.string.destination_address));
			if(!TextUtils.isEmpty(fromAddress) && !TextUtils.isEmpty(toAddress))
			{
				onAddressSelected(fromAddress, toAddress);	
			}
		}
	}
	
	private String getAddress(AutoCompleteTextView ac, String hint)
	{
		String address = null;
		String enteredText = ac.getText().toString();
		String hintText = ac.getHint().toString();
		if(!TextUtils.isEmpty(enteredText))
		{
			address = enteredText;
		}
		else if(!hintText.equals(hint))
		{
			address = hintText;
		}
		if(TextUtils.isEmpty(address))
		{
			Toast.makeText(getActivity(), String.format(getString(R.string.trip_request_enter_address), hint), Toast.LENGTH_SHORT).show();
		}
		return address;
	}
	
	private AddressSelected mAddressSelected = null;
	private void onAddressSelected(String originAddress, String destinationAddress)
	{
		if(mAddressSelected==null || mAddressSelected.getStatus()==AsyncTask.Status.FINISHED)
		{
			mTripRequest = new TripRequest();
			mAddressSelected = new AddressSelected(mSelectedAddressFromPosition, mSelectedAddressToPosition);
			mAddressSelected.execute(originAddress,destinationAddress);
		}
		else
		{
			mItemClicked = false;
		}
	}
	
	private class AddressSelected extends AsyncTask<String, Void, Void>
	{
		private List<AddressSearch> mOriginAddresses;
		private List<AddressSearch> mDestAddresses;
		private String mOrginAddress = null;
		private String mDestAddress = null;
		private int mSelectedIndexOriginAddress = -1;
		private int mSelectedDestinationAddress = -1;
		
		
		public AddressSelected(int selectedIndexOriginAddress, int selectedDestinationAddress)
		{
			mSelectedIndexOriginAddress = selectedIndexOriginAddress;
			mSelectedDestinationAddress = selectedDestinationAddress;
		}
		
		@Override
		protected Void doInBackground(String... params)
		{
			mOrginAddress = params[0];
			mDestAddress = params[1];
			List<AddressSearch> originAddresses = findAddrees(mOrginAddress);
			List<AddressSearch> destAddresses = findAddrees(mDestAddress);
			
			if(!originAddresses.isEmpty() && mSelectedIndexOriginAddress!=-1)
			{
				mSelectedIndexOriginAddress = findAddressIndex(mOrginAddress, originAddresses);
				if(mSelectedIndexOriginAddress!=-1)
				{
					AddressSearch orgin = originAddresses.get(mSelectedIndexOriginAddress);
					mTripRequest.setOriginId(orgin.id);
					mTripRequest.setOriginCoordName(orgin.address);
					mTripRequest.setOriginCoordX(orgin.xcoord);
					mTripRequest.setOriginCoordY(orgin.ycoord);
				}
				
			}
			if(!destAddresses.isEmpty() && mSelectedDestinationAddress!=-1)
			{
				mSelectedDestinationAddress = findAddressIndex(mDestAddress, destAddresses);
				if(mSelectedDestinationAddress!=-1)
				{
					AddressSearch dest = destAddresses.get(mSelectedDestinationAddress);
					mTripRequest.setDestId(dest.id);
					mTripRequest.setDestCoordName(dest.address);
					mTripRequest.setDestCoordX(dest.xcoord);
					mTripRequest.setDestCoordY(dest.ycoord);
				}
				
			}
			mOriginAddresses = originAddresses;
			mDestAddresses = destAddresses;
			return null;
		}
		
		private int findAddressIndex(String address, List<AddressSearch> addresses)
		{
			int index = -1;
			if(addresses.size()==1)
			{
				index = 0;
			}
			for(int i = 0; i < addresses.size() && index==-1; i++)
			{
				String current = addresses.get(i).address;
				if(current.equals(address))
				{
					index = i;
				}
			}
			return index;
		}
		
		private List<AddressSearch> findAddrees(String address)
		{
			List<AddressSearch> addresses = new ArrayList<AddressSearch>();
			ContentResolver cr = getActivity().getContentResolver();
			Cursor c = null;
			try
			{
				StringBuilder b = new StringBuilder();
				b.append(AddressProviderMetaData.TableMetaData.address).append(" LIKE '").append(address).append("%') GROUP BY (").append(AddressProviderMetaData.TableMetaData.address);
							
				c = cr.query(AddressProviderMetaData.TableMetaData.CONTENT_URI, PROJECTION, b.toString(), null, AddressProviderMetaData.TableMetaData.address);
				if(c.moveToFirst())
				{
					int iAddress = c.getColumnIndex(AddressProviderMetaData.TableMetaData.address);
					int iY = c.getColumnIndex(AddressProviderMetaData.TableMetaData.Y);
					int iX = c.getColumnIndex(AddressProviderMetaData.TableMetaData.X);
					int iType = c.getColumnIndex(AddressProviderMetaData.TableMetaData.type_int);
					int iId = c.getColumnIndex(AddressProviderMetaData.TableMetaData.id);
					do{
						String addr = c.getString(iAddress);
						String y = c.getString(iY);
						String x = c.getString(iX);
						String type = c.getString(iType);
						String id = c.getString(iId);
						AddressSearch as = new AddressSearch(id, addr, type, x, y);
						addresses.add(as);						
					}while(c.moveToNext());			
				}
			}
			finally
			{
				if(c!=null && !c.isClosed())
				{
					c.close();
				}
			}
			return addresses;
		}
		
		private String getNewSearchAddress(String address)
		{
			if(address.contains(","))
			{
				String[] temp = address.split(",");
				address = temp[0];
			}
			if(address.contains(" "))
			{
				String[] temp = address.split(" ");
				address = temp[0];
			}
			
			
			return address;
		}

		protected void onPostExecute(Void result)
		{
			mItemClicked = false;
			if(TextUtils.isEmpty(mTripRequest.originCoordNameNotEncoded))
            {
                Toast.makeText(getActivity(), String.format(getString(R.string.trip_request_address_not_valid), getString(R.string.startaddress)), Toast.LENGTH_SHORT).show();
                validateAddress(mAutoCompleteTextViewFromAddress, mAutoCompleteAdapterFrom, mOrginAddress, mOriginAddresses, mSelectedIndexOriginAddress);
            }
            else if(TextUtils.isEmpty(mTripRequest.destCoordNameNotEncoded))
            {
                Toast.makeText(getActivity(), String.format(getString(R.string.trip_request_address_not_valid), getString(R.string.destination_address)), Toast.LENGTH_SHORT).show();
                validateAddress(mAutoCompleteTextViewToAddress, mAutoCompleteAdapterTo, mDestAddress, mDestAddresses, mSelectedDestinationAddress);
            }
			else if(mTripRequest.destCoordNameNotEncoded.equals(mTripRequest.originCoordNameNotEncoded))
			{
				Toast.makeText(getActivity(), getString(R.string.start_end_must_not_be_equal), Toast.LENGTH_SHORT).show();
			}
			else if(mTripRequest.isValid())
			{
				mCallbacks.onFindTripSuggestion(mTripRequest);
			}
			else if(!validateAddress(mAutoCompleteTextViewFromAddress, mAutoCompleteAdapterFrom, mOrginAddress, mOriginAddresses, mSelectedIndexOriginAddress))
			{
				Log.d(tag, "originAddress not valid");
				Toast.makeText(getActivity(), String.format(getString(R.string.trip_request_address_not_valid), getString(R.string.startaddress)), Toast.LENGTH_SHORT).show();		
			}
			else if(!validateAddress(mAutoCompleteTextViewToAddress, mAutoCompleteAdapterTo, mDestAddress, mDestAddresses, mSelectedDestinationAddress))
			{
				Log.d(tag, "destAddress not valid");
				Toast.makeText(getActivity(), String.format(getString(R.string.trip_request_address_not_valid), getString(R.string.destination_address)), Toast.LENGTH_SHORT).show();
			}
		}
		
		private boolean validateAddress(AutoCompleteTextView auto, AutoCompleteAddressAdapter adapter, String address, List<AddressSearch> addresses, int selectedIndex)
		{
			boolean valid = true;
			if(selectedIndex==-1)
			{
				auto.requestFocus();
				adapter.resetPreviousConstraint();
				address = getNewSearchAddress(address);	
				auto.setText(address);

				auto.showDropDown();
                valid = false;
			}
			return valid;
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
