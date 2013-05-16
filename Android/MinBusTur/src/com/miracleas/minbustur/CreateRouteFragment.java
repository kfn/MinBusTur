package com.miracleas.minbustur;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.miracleas.minbustur.net.AddressFetcher;
import com.miracleas.minbustur.provider.AddressProviderMetaData;

public class CreateRouteFragment extends SherlockFragment implements TextWatcher, LoaderCallbacks<Cursor>, OnItemSelectedListener, OnItemClickListener
{
	public static final String tag = CreateRouteFragment.class.getName();
	private AutoCompleteTextView mAutoCompleteTextViewFrom;
	private AutoCompleteAdapter mAutoCompleteAdapterFrom = null;
	private AutoCompleteTextView mAutoCompleteTextViewTo;
	private AutoCompleteAdapter mAutoCompleteAdapterTo = null;
	private static final int THRESHOLD = 2;
	private static final int LOADER_ADDRESS_FROM = 1;
	private static final int LOADER_ADDRESS_TO = 2;
	private static final String[] PROJECTION = { AddressProviderMetaData.TableMetaData._ID, AddressProviderMetaData.TableMetaData.address, AddressProviderMetaData.TableMetaData.lat, AddressProviderMetaData.TableMetaData.lng };
	private AddressFetcher mAddressFetcher = null;
	private String mPreviousEnteredAddressFrom = null;
	private String mPreviousEnteredAddressTo = null;
	private Handler mHandler = null;
	private LoadAddressesRun mLoadAddressesRun = null;
	private int mActiveLoader = LOADER_ADDRESS_FROM;
	private Uri mDataUri = null;

	public static CreateRouteFragment createInstance()
	{
		CreateRouteFragment fragment = new CreateRouteFragment();
		Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}

	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.fragment_create_route, container, false);
		mAutoCompleteTextViewFrom = (AutoCompleteTextView) rootView.findViewById(R.id.autoCompleteTextViewFrom);
		initAutoComplete(mAutoCompleteTextViewFrom);
		mAutoCompleteAdapterFrom = new AutoCompleteAdapter(getActivity(), null, 0);
		mAutoCompleteTextViewFrom.setAdapter(mAutoCompleteAdapterFrom);
		mAutoCompleteTextViewFrom.setOnFocusChangeListener(new OnFocusChangeListener()
		{

			@Override
			public void onFocusChange(View v, boolean hasFocus)
			{

				mActiveLoader = LOADER_ADDRESS_FROM;

			}
		});

		mAutoCompleteTextViewTo = (AutoCompleteTextView) rootView.findViewById(R.id.autoCompleteTextViewTo);
		initAutoComplete(mAutoCompleteTextViewTo);
		mAutoCompleteAdapterTo = new AutoCompleteAdapter(getActivity(), null, 0);
		mAutoCompleteTextViewTo.setAdapter(mAutoCompleteAdapterTo);
		mAutoCompleteTextViewTo.setOnFocusChangeListener(new OnFocusChangeListener()
		{

			@Override
			public void onFocusChange(View v, boolean hasFocus)
			{

				mActiveLoader = LOADER_ADDRESS_TO;

			}
		});
		return rootView;
	}

	private void initAutoComplete(AutoCompleteTextView a)
	{
		a.addTextChangedListener(this);
		a.setOnItemSelectedListener(this);
		a.setThreshold(THRESHOLD);
		a.setOnItemClickListener(this);
	}

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public CreateRouteFragment()
	{
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mHandler = new Handler();
		mLoadAddressesRun = new LoadAddressesRun(null);
		mAddressFetcher = new AddressFetcher(getActivity());
		Bundle args = new Bundle();
		args.putString(AddressProviderMetaData.TableMetaData.address, "a");
		mDataUri = Uri.withAppendedPath(AddressProviderMetaData.TableMetaData.CONTENT_URI, "search");
		getSherlockActivity().getSupportLoaderManager().initLoader(LOADER_ADDRESS_FROM, args, this);
		// getSherlockActivity().getSupportLoaderManager().initLoader(LOADER_ADDRESS_TO,
		// args, this);
	}

	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
	}

	@Override
	public void afterTextChanged(Editable s)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
		final String value = s.toString();
		if (value.length() > THRESHOLD && !value.equals(getPreviousEnteredText()))
		{
			mHandler.removeCallbacks(mLoadAddressesRun);
			mLoadAddressesRun = new LoadAddressesRun(value);
			mHandler.postDelayed(mLoadAddressesRun, 1000);
		}
	}

	private class LoadAddressesRun implements Runnable
	{
		private final String mQuery;

		LoadAddressesRun(String q)
		{
			mQuery = q;
		}

		@Override
		public void run()
		{
			startLoadAddress(mQuery);
		}
	}

	private class AutoCompleteAdapter extends CursorAdapter
	{
		private int iAddress;
		private int iLat;
		private int iLng;
		private LayoutInflater mInf = null;

		public AutoCompleteAdapter(Context context, Cursor c, int flags)
		{
			super(context, c, flags);
			mInf = LayoutInflater.from(context);
		}

		@Override
		public void bindView(View v, Context context, Cursor cursor)
		{
			TextView tv = (TextView) v;
			tv.setText(cursor.getString(iAddress));
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
				iLat = newCursor.getColumnIndex(AddressProviderMetaData.TableMetaData.lat);
				iLng = newCursor.getColumnIndex(AddressProviderMetaData.TableMetaData.lng);
			}
			return super.swapCursor(newCursor);
		}

		public String getLat(int position)
		{
			String lat = null;
			Cursor c = getCursor();
			if (c.moveToPosition(position))
			{
				lat = c.getString(iLat);
			}
			return lat;
		}

		public String getLng(int position)
		{
			String lng = null;
			Cursor c = getCursor();
			if (c.moveToPosition(position))
			{
				lng = c.getString(iLng);
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
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args)
	{
		String address = args.getString(AddressProviderMetaData.TableMetaData.address);
		Log.d(tag, "create loader: " + address);
		String[] selectionArgs = { address };
		return new CursorLoader(getActivity(), mDataUri, PROJECTION, null, selectionArgs, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor newCursor)
	{
		if (mActiveLoader == LOADER_ADDRESS_FROM)
		{
			Log.d(tag, "onLoadFinished: LOADER_ADDRESS_FROM");
			mAutoCompleteAdapterFrom.swapCursor(newCursor);
			if (newCursor != null && newCursor.getCount() > 0)
			{
				mAutoCompleteTextViewFrom.showDropDown();
			}
		} else if (mActiveLoader == LOADER_ADDRESS_TO)
		{
			Log.d(tag, "onLoadFinished: LOADER_ADDRESS_TO");
			mAutoCompleteAdapterTo.swapCursor(newCursor);
			if (newCursor != null && newCursor.getCount() > 0)
			{
				mAutoCompleteTextViewTo.showDropDown();
			}

		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader)
	{
		if (mActiveLoader == LOADER_ADDRESS_TO)
		{
			Log.d(tag, "onLoaderReset: LOADER_ADDRESS_TO");
			mAutoCompleteAdapterFrom.swapCursor(null);
		} else if (mActiveLoader == LOADER_ADDRESS_FROM)
		{
			Log.d(tag, "onLoaderReset: LOADER_ADDRESS_FROM");
			mAutoCompleteAdapterTo.swapCursor(null);
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View view, int position, long id)
	{
		onAddressSelect(position);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		onAddressSelect(position);
	}

	private void onAddressSelect(int position)
	{
		AutoCompleteAdapter adapter = getAdapter();
		String text = adapter.getAddress(position);
		String lat = adapter.getLat(position);
		String lng = adapter.getLng(position);
		setPreviousEnteredText(text);
		setAddress(text);
	}

	private AutoCompleteAdapter getAdapter()
	{
		if (mActiveLoader == LOADER_ADDRESS_FROM)
		{
			return mAutoCompleteAdapterFrom;
		} else
		{
			return mAutoCompleteAdapterTo;
		}
	}
	private String getPreviousEnteredText()
	{
		if (mActiveLoader == LOADER_ADDRESS_FROM)
		{
			return mPreviousEnteredAddressFrom;
		} else
		{
			return mPreviousEnteredAddressTo;
		}
	}
	private void setPreviousEnteredText(String address)
	{
		if (mActiveLoader == LOADER_ADDRESS_FROM)
		{
			mPreviousEnteredAddressFrom = address;
		} else
		{
			mPreviousEnteredAddressTo = address;
		}
	}
	private void setAddress(String address)
	{
		if (mActiveLoader == LOADER_ADDRESS_FROM)
		{
			mAutoCompleteTextViewFrom.setText(address);
		} else
		{
			mAutoCompleteTextViewTo.setText(address);
		}
	}
	
	

	@Override
	public void onNothingSelected(AdapterView<?> arg0)
	{
		// TODO Auto-generated method stub

	}

	private void startLoadAddress(String query)
	{
		setPreviousEnteredText(query);
		Log.d(tag, "lockup for: " + query);
		new LoadAddresses().execute(query);
		Bundle args = new Bundle();
		args.putString(AddressProviderMetaData.TableMetaData.address, query);
		getSherlockActivity().getSupportLoaderManager().restartLoader(LOADER_ADDRESS_FROM, args, this);

	}

	private class LoadAddresses extends AsyncTask<String, Void, Void>
	{

		@Override
		protected Void doInBackground(String... params)
		{
			try
			{
				mAddressFetcher.performGeocode(params[0], mDataUri);
			} catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
	}

}
