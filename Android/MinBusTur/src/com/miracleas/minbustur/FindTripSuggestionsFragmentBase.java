package com.miracleas.minbustur;

import java.io.InputStream;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.util.LruCache;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FilterQueryProvider;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.actionbarsherlock.app.SherlockFragment;
import com.miracleas.minbustur.model.TripRequest;
import com.miracleas.minbustur.net.AddressFetcher;
import com.miracleas.minbustur.provider.AddressProviderMetaData;

public abstract class FindTripSuggestionsFragmentBase extends SherlockFragment implements LoaderCallbacks<Cursor>, OnClickListener, OnFocusChangeListener
{
	protected AutoCompleteTextView mAutoCompleteTextViewFromAddress;
	protected AutoCompleteTextView mAutoCompleteTextViewToAddress;
	protected AutoCompleteTextView mAutoCompleteTextViewToTitle = null;
	protected AutoCompleteTextView mAutoCompleteTextViewFromTitle = null;

	protected static final int THRESHOLD = 2;
	
	protected static final String[] PROJECTION = { AddressProviderMetaData.TableMetaData._ID, AddressProviderMetaData.TableMetaData.id, AddressProviderMetaData.TableMetaData.address, AddressProviderMetaData.TableMetaData.Y, AddressProviderMetaData.TableMetaData.X, AddressProviderMetaData.TableMetaData.type_int };
	protected static final String[] PROJECTION_CONTACTS = new String[] { ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME };

	
	protected String mPreviousEnteredAddressFrom = null;
	protected String mPreviousEnteredAddressTo = null;
	protected String mPreviousEnteredContactFrom = null;
	protected String mPreviousEnteredContactTo = null;
	protected Handler mHandler = null;
	protected Uri mDataUri = null;

	protected ProgressBar mProgressBarToAddress = null;
	protected ProgressBar mProgressBarFromAddress = null;
	protected ProgressBar mProgressBarFromTitle = null;
	protected ProgressBar mProgressBarToTitle = null;
	protected View mBtnFindRoute = null;

	protected static LruCache<Long, Drawable> cache;
	protected ArrayList<Long> id_list = new ArrayList<Long>();
	protected Drawable mBitmapDrawableDummy = null;

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
		mAutoCompleteTextViewToTitle = (AutoCompleteTextView) rootView.findViewById(R.id.autoCompleteTextViewToTitle);
		mAutoCompleteTextViewFromTitle = (AutoCompleteTextView) rootView.findViewById(R.id.autoCompleteTextViewFromTitle);
		mAutoCompleteTextViewFromAddress = (AutoCompleteTextView) rootView.findViewById(R.id.autoCompleteTextViewFrom);
		mAutoCompleteTextViewToAddress = (AutoCompleteTextView) rootView.findViewById(R.id.autoCompleteTextViewTo);

		initAutoComplete(mAutoCompleteTextViewToTitle);
		initAutoComplete(mAutoCompleteTextViewFromTitle);
		initAutoComplete(mAutoCompleteTextViewFromAddress);
		mAutoCompleteTextViewFromAddress.setOnItemClickListener(new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				onAddressFromSelected(position);				
			}
		});
		initAutoComplete(mAutoCompleteTextViewToAddress);
		mAutoCompleteTextViewToAddress.setOnItemClickListener(new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				onAddressToSelected(position);			
			}
		});
		mProgressBarToAddress = (ProgressBar) rootView.findViewById(R.id.progressBarToAddress);
		mProgressBarFromAddress = (ProgressBar) rootView.findViewById(R.id.progressBarFromAddress);
		mProgressBarFromTitle = (ProgressBar) rootView.findViewById(R.id.progressBarFromTitle);
		mProgressBarToTitle = (ProgressBar) rootView.findViewById(R.id.progressBarToTitle);
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
	
	protected class ImageLoader extends AsyncTask<Void, Void, Drawable>
	{
		private TextView mView;
		private Uri mUri;
		private Object tag;
		private long position;

		public ImageLoader(TextView view, Uri uri, long position)
		{

			if (view == null)
			{
				throw new IllegalArgumentException("View Cannot be null");
			}
			if (uri == null)
			{
				throw new IllegalArgumentException("Uri cant be null");
			}
			mView = view;
			tag = mView.getTag();
			this.position = position;
			mUri = uri;
		}

		protected Drawable doInBackground(Void... args)
		{
			Bitmap bitmap;
			// Load image from the Content Provider
			InputStream in = ContactsContract.Contacts.openContactPhotoInputStream(getActivity().getContentResolver(), mUri);
			bitmap = BitmapFactory.decodeStream(in);
			return new BitmapDrawable(getResources(), bitmap);
		}

		protected void onPostExecute(Drawable bitmap)
		{
			if (bitmap == null || bitmap.getIntrinsicHeight()==0)
			{
				bitmap = mBitmapDrawableDummy;
			}
			// If is in somewhere else, do not temper
			Long viewTag = (Long) mView.getTag();
			if (!viewTag.equals(tag))
				return;
			// If no image was there and do not put it to cache
			if (bitmap != null)
			{
				mView.setCompoundDrawablesWithIntrinsicBounds(bitmap, null, null, null);
				addBitmapToCache(position, bitmap);
				return;
			} else
			{

			}
			// Otherwise, welcome to cache
			return;
		}
	}

	/** Add image to cache */
	protected void addBitmapToCache(Long key, Drawable bitmap)
	{
		if (getBitmapFromCache(key) == null)
		{
			cache.put(key, bitmap);
		}
	}

	/** Retrive image from cache */
	protected Drawable getBitmapFromCache(Long key)
	{
		return cache.get(key);
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
