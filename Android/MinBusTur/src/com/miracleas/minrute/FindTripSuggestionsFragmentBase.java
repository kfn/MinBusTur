package com.miracleas.minrute;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;

import com.actionbarsherlock.app.SherlockFragment;

import com.miracleas.minrute.model.TripRequest;
import com.miracleas.minrute.provider.AddressProviderMetaData;
import com.miracleas.minrute.utils.ViewHelper;

public abstract class FindTripSuggestionsFragmentBase extends SherlockFragment implements LoaderCallbacks<Cursor>, OnClickListener, OnFocusChangeListener
{
	protected AutoCompleteTextView mAutoCompleteTextViewFromAddress;
	protected AutoCompleteTextView mAutoCompleteTextViewToAddress;
	protected int mSelectedAddressFromPosition = -1;
	protected int mSelectedAddressToPosition = -1;

	protected static final int THRESHOLD = 2;
	
	protected static final String[] PROJECTION = { AddressProviderMetaData.TableMetaData._ID, AddressProviderMetaData.TableMetaData.id, AddressProviderMetaData.TableMetaData.address, AddressProviderMetaData.TableMetaData.Y, AddressProviderMetaData.TableMetaData.X, AddressProviderMetaData.TableMetaData.type_int };
	protected static final String[] PROJECTION_CONTACTS = new String[] { ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME };

	protected Handler mHandler = null;
	protected Uri mDataUri = null;

	protected ProgressBar mProgressBarToAddress = null;
	protected ProgressBar mProgressBarFromAddress = null;
	
	protected View mBtnFindRoute = null;
	protected int mLoadCount = 0;
	protected TripRequest mTripRequest = null;
	protected View mFocusedView = null;
	
	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		if(savedInstanceState!=null)
		{
			mTripRequest = savedInstanceState.getParcelable("tripRequest");
		}
		else
		{
			mTripRequest = new TripRequest();
		}
		View rootView = inflater.inflate(R.layout.fragment_find_trip_suggestions, container, false);
		
		mAutoCompleteTextViewFromAddress = (AutoCompleteTextView) rootView.findViewById(R.id.autoCompleteTextViewFrom);
		mAutoCompleteTextViewToAddress = (AutoCompleteTextView) rootView.findViewById(R.id.autoCompleteTextViewTo);
		
		mAutoCompleteTextViewFromAddress.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id)
			{
				mSelectedAddressFromPosition = position;
				mAutoCompleteTextViewToAddress.requestFocus();
			}			
		});
		mAutoCompleteTextViewToAddress.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position, long id)
			{
				mSelectedAddressToPosition = position;
				ViewHelper.hideKeyboard(getActivity(), mAutoCompleteTextViewToAddress);
			}			
		});
		initAutoComplete(mAutoCompleteTextViewFromAddress);
		
		mProgressBarToAddress = (ProgressBar) rootView.findViewById(R.id.progressBarToAddress);
		mProgressBarFromAddress = (ProgressBar) rootView.findViewById(R.id.progressBarFromAddress);
		
		mBtnFindRoute = rootView.findViewById(R.id.btnFindRoute);
		mBtnFindRoute.setOnClickListener(this);
		return rootView;
	}
	
	public abstract void textChangedHelper(CharSequence s, int loaderId);
	protected abstract void onAddressFromSelected(int position);
	protected abstract void onAddressToSelected(int position);

	private void initAutoComplete(AutoCompleteTextView a)
	{
		a.setThreshold(THRESHOLD);
		a.setOnFocusChangeListener(this);
	}
	

	
	public void onDestroy()
	{
		super.onDestroy();
	}

	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putParcelable("tripRequest", mTripRequest);
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus)
	{
		if(hasFocus)
		{
			mFocusedView = v;
		}		
	}
	
	public int getActiveLoader()
	{
		int loaderId = 0;
		int id = mFocusedView.getId();
		switch(id)
		{
		case R.id.autoCompleteTextViewToTitle:
			loaderId = LoaderConstants.LOADER_TITLE_TO;
			break;
		case R.id.autoCompleteTextViewFromTitle:
			loaderId = LoaderConstants.LOADER_TITLE_FROM;
			break;
		case R.id.autoCompleteTextViewFrom:
			loaderId = LoaderConstants.LOADER_ADDRESS_FROM;
			break;
		case R.id.autoCompleteTextViewTo:
			loaderId = LoaderConstants.LOADER_ADDRESS_TO;
			break;
		}
		return loaderId;
	}

}
