package com.miracleas.minbustur;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.LruCache;
import android.support.v4.widget.CursorAdapter;
import android.text.Editable;
import android.text.TextUtils;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.miracleas.minbustur.net.AddressFetcher;
import com.miracleas.minbustur.provider.AddressProviderMetaData;

public class CreateRouteFragment extends SherlockFragment implements TextWatcher, LoaderCallbacks<Cursor>, OnItemSelectedListener, OnItemClickListener, OnFocusChangeListener
{
	public static final String tag = CreateRouteFragment.class.getName();
	private AutoCompleteTextView mAutoCompleteTextViewFromAddress;
	private AutoCompleteTextView mAutoCompleteTextViewToAddress;
	private AutoCompleteTextView mAutoCompleteTextViewToTitle = null;
	private AutoCompleteTextView mAutoCompleteTextViewFromTitle = null;

	private AutoCompleteAddressAdapter mAutoCompleteAdapterFrom = null;
	private AutoCompleteAddressAdapter mAutoCompleteAdapterTo = null;
	private AutoCompleteContactsAdapter mAutoCompleteContactsAdapterTo = null;
	private AutoCompleteContactsAdapter mAutoCompleteContactsAdapterFrom = null;

	private static final int THRESHOLD = 2;
	private static final int LOADER_ADDRESS_FROM = 1;
	private static final int LOADER_ADDRESS_TO = 2;
	private static final int LOADER_TITLE_TO = 3;
	private static final int LOADER_TITLE_FROM = 4;
	private static final String[] PROJECTION = { AddressProviderMetaData.TableMetaData._ID, AddressProviderMetaData.TableMetaData.address, AddressProviderMetaData.TableMetaData.lat, AddressProviderMetaData.TableMetaData.lng };
	private static final String[] PROJECTION_CONTACTS = new String[] { ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME };

	private AddressFetcher mAddressFetcher = null;
	private String mPreviousEnteredAddressFrom = null;
	private String mPreviousEnteredAddressTo = null;
	private String mPreviousEnteredContactFrom = null;
	private String mPreviousEnteredContactTo = null;
	private Handler mHandler = null;
	private LoadAddressesRun mLoadAddressesRun = null;
	private LoadContactRun mLoadContactRun = null;
	private int mActiveLoader = LOADER_ADDRESS_FROM;
	private Uri mDataUri = null;

	private ProgressBar mProgressBarToAddress = null;
	private ProgressBar mProgressBarFromAddress = null;
	private ProgressBar mProgressBarFromTitle = null;
	private ProgressBar mProgressBarToTitle = null;

	private static LruCache<Long, Drawable> cache;
	private ArrayList<Long> id_list = new ArrayList<Long>();
	private Drawable mBitmapDrawableDummy = null;

	private int mLoadCount = 0;

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
		mAutoCompleteTextViewToTitle = (AutoCompleteTextView) rootView.findViewById(R.id.autoCompleteTextViewToTitle);
		mAutoCompleteTextViewFromTitle = (AutoCompleteTextView) rootView.findViewById(R.id.autoCompleteTextViewFromTitle);
		mAutoCompleteTextViewFromAddress = (AutoCompleteTextView) rootView.findViewById(R.id.autoCompleteTextViewFrom);
		mAutoCompleteTextViewToAddress = (AutoCompleteTextView) rootView.findViewById(R.id.autoCompleteTextViewTo);

		mAutoCompleteAdapterFrom = new AutoCompleteAddressAdapter(getActivity(), null, 0);
		mAutoCompleteTextViewFromAddress.setAdapter(mAutoCompleteAdapterFrom);
		mAutoCompleteAdapterTo = new AutoCompleteAddressAdapter(getActivity(), null, 0);
		mAutoCompleteTextViewToAddress.setAdapter(mAutoCompleteAdapterTo);
		mAutoCompleteContactsAdapterTo = new AutoCompleteContactsAdapter(getActivity(), null, 0);
		mAutoCompleteContactsAdapterFrom = new AutoCompleteContactsAdapter(getActivity(), null, 0);
		mAutoCompleteTextViewToTitle.setAdapter(mAutoCompleteContactsAdapterTo);
		mAutoCompleteTextViewFromTitle.setAdapter(mAutoCompleteContactsAdapterFrom);

		initAutoComplete(mAutoCompleteTextViewToTitle);
		initAutoComplete(mAutoCompleteTextViewFromTitle);
		initAutoComplete(mAutoCompleteTextViewFromAddress);
		initAutoComplete(mAutoCompleteTextViewToAddress);

		mProgressBarToAddress = (ProgressBar) rootView.findViewById(R.id.progressBarToAddress);
		mProgressBarFromAddress = (ProgressBar) rootView.findViewById(R.id.progressBarFromAddress);
		mProgressBarFromTitle = (ProgressBar) rootView.findViewById(R.id.progressBarFromTitle);
		mProgressBarToTitle = (ProgressBar) rootView.findViewById(R.id.progressBarToTitle);
		return rootView;
	}

	private void initAutoComplete(AutoCompleteTextView a)
	{
		a.addTextChangedListener(this);
		a.setOnItemSelectedListener(this);
		a.setThreshold(THRESHOLD);
		a.setOnItemClickListener(this);
		a.setOnFocusChangeListener(this);
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
		cache = new LruCache<Long, Drawable>(20);
		mBitmapDrawableDummy = getResources().getDrawable(R.drawable.ic_action_user);
		mHandler = new Handler();
		mLoadAddressesRun = new LoadAddressesRun(null);
		mLoadContactRun = new LoadContactRun(null);
		mAddressFetcher = new AddressFetcher(getActivity());
		Bundle args = new Bundle();
		args.putString(AddressProviderMetaData.TableMetaData.address, "a");
		args.putString(ContactsContract.Contacts.DISPLAY_NAME, "abc");
		mDataUri = Uri.withAppendedPath(AddressProviderMetaData.TableMetaData.CONTENT_URI, "search");
		getSherlockActivity().getSupportLoaderManager().initLoader(LOADER_ADDRESS_FROM, args, this);
		getSherlockActivity().getSupportLoaderManager().initLoader(LOADER_TITLE_FROM, args, this);
		// getSherlockActivity().getSupportLoaderManager().initLoader(LOADER_ADDRESS_TO,
		// args, this);
	}

	public void onDestroy()
	{
		super.onDestroy();
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
			if (isAddressMode())
			{
				mHandler.removeCallbacks(mLoadAddressesRun);
				mLoadAddressesRun = new LoadAddressesRun(value);
				mHandler.postDelayed(mLoadAddressesRun, 2000);
			} else
			{
				mHandler.removeCallbacks(mLoadContactRun);
				mLoadContactRun = new LoadContactRun(value);
				mHandler.postDelayed(mLoadContactRun, 200);
			}
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args)
	{
		if (id == LOADER_ADDRESS_FROM || id == LOADER_ADDRESS_TO)
		{
			String address = args.getString(AddressProviderMetaData.TableMetaData.address);
			Log.d(tag, "create loader: " + address);
			String[] selectionArgs = { address };
			return new CursorLoader(getActivity(), mDataUri, PROJECTION, null, selectionArgs, null);
		} else if (id == LOADER_TITLE_FROM || id == LOADER_TITLE_TO)
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
		/*
		 * if (mActiveLoader == LOADER_ADDRESS_FROM) { Log.d(tag,
		 * "onLoadFinished: LOADER_ADDRESS_TO"); } else if (mActiveLoader ==
		 * LOADER_ADDRESS_TO) { Log.d(tag, "onLoadFinished: LOADER_ADDRESS_TO");
		 * }
		 */

		getAdapter().swapCursor(newCursor);
		if (newCursor != null && newCursor.getCount() > 0)
		{
			getAutoCompleteTextView().showDropDown();
			Log.d(tag, "showDropDown");
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
		onAddressSelect(position, id);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		onAddressSelect(position, id);
	}

	private void onAddressSelect(int position, long id)
	{
		CursorAdapter adapter = getAdapter();
		if (isAddressMode() && adapter instanceof AutoCompleteAddressAdapter)
		{
			AutoCompleteAddressAdapter a = (AutoCompleteAddressAdapter) adapter;
			String text = a.getAddress(position);
			String lat = a.getLat(position);
			String lng = a.getLng(position);
			setPreviousEnteredText(text);
			setSelectedValue(text);
		}
		else if(adapter instanceof AutoCompleteContactsAdapter)
		{
			AutoCompleteContactsAdapter a = (AutoCompleteContactsAdapter)adapter;
			String name = a.getName(position);
			setPreviousEnteredText(name);
			setSelectedValue(name);
			loadContactAddress(id);
		}

	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0)
	{
		// TODO Auto-generated method stub

	}

	private void loadAddress(String query)
	{
		mLoadCount++;
		setPreviousEnteredText(query);
		Log.d(tag, "lockup for: " + query);
		new LoadAddresses().execute(query);
		Bundle args = new Bundle();
		args.putString(AddressProviderMetaData.TableMetaData.address, query);
		getProgressBar().setVisibility(View.VISIBLE);
		getSherlockActivity().getSupportLoaderManager().restartLoader(mActiveLoader, args, this);
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

		protected void onPostExecute(Void result)
		{
			mLoadCount--;
			if (mLoadCount <= 0)
			{
				getProgressBar().setVisibility(View.GONE);
				mLoadCount = 0;
			}
				
		}
	}

	private class AutoCompleteAddressAdapter extends CursorAdapter
	{
		private int iAddress;
		private int iLat;
		private int iLng;
		private LayoutInflater mInf = null;

		public AutoCompleteAddressAdapter(Context context, Cursor c, int flags)
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

	private void loadContact(String query)
	{
		setPreviousEnteredText(query);
		Log.d(tag, "lockup for: " + query);
		Bundle args = new Bundle();
		args.putString(ContactsContract.Contacts.DISPLAY_NAME, query);
		getSherlockActivity().getSupportLoaderManager().restartLoader(mActiveLoader, args, this);
	}

	private class AutoCompleteContactsAdapter extends CursorAdapter
	{
		private int iName;
		private int iImg;
		private int iId;
		private LayoutInflater mInf = null;

		public AutoCompleteContactsAdapter(Context context, Cursor c, int flags)
		{
			super(context, c, flags);
			mInf = LayoutInflater.from(context);
		}

		@Override
		public void bindView(View v, Context context, Cursor cursor)
		{
			TextView tv = (TextView) v;
			tv.setText(cursor.getString(iName));
			//tv.setCompoundDrawablesWithIntrinsicBounds(mBitmapDrawableDummy, null, null, null);
			
			loadImageOfContact(tv, cursor);
		}

		private void loadImageOfContact(TextView v, Cursor cursor)
		{
			Object tag;
			long con_id = cursor.getLong(iId);
			if (!id_list.contains(Long.valueOf(con_id)))
			{
				id_list.add(con_id);
			}
			Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, con_id);
			tag = con_id;
			v.setTag(tag);
			Drawable d = getBitmapFromCache(con_id);
			if (d != null)
			{
				v.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);
			} else
			{
				ImageLoader loader = new ImageLoader(v, contactUri, con_id);
				loader.execute();
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
				iName = newCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
				iId = newCursor.getColumnIndex(ContactsContract.Contacts._ID);
			}
			return super.swapCursor(newCursor);
		}

		public String getImage(int position)
		{
			String lat = null;
			Cursor c = getCursor();
			if (c.moveToPosition(position))
			{
				lat = c.getString(iImg);
			}
			return lat;
		}

		public String getName(int position)
		{
			String address = null;
			Cursor c = getCursor();
			if (c.moveToPosition(position))
			{
				address = c.getString(iName);
			}
			return address;
		}
	}

	private CursorAdapter getAdapter()
	{
		if (mActiveLoader == LOADER_ADDRESS_FROM)
		{
			return mAutoCompleteAdapterFrom;
		} else if (mActiveLoader == LOADER_ADDRESS_TO)
		{
			return mAutoCompleteAdapterTo;
		} else if (mActiveLoader == LOADER_TITLE_FROM)
		{
			return mAutoCompleteContactsAdapterFrom;
		} else if (mActiveLoader == LOADER_TITLE_TO)
		{
			return mAutoCompleteContactsAdapterTo;
		}
		return null;
	}

	private String getPreviousEnteredText()
	{
		if (mActiveLoader == LOADER_ADDRESS_FROM)
		{
			return mPreviousEnteredAddressFrom;
		} else if (mActiveLoader == LOADER_ADDRESS_TO)
		{
			return mPreviousEnteredAddressTo;
		} else if (mActiveLoader == LOADER_TITLE_FROM)
		{
			return mPreviousEnteredContactFrom;
		} else if (mActiveLoader == LOADER_TITLE_TO)
		{
			return mPreviousEnteredContactTo;
		}
		return "";
	}

	private void setPreviousEnteredText(String text)
	{
		if (mActiveLoader == LOADER_ADDRESS_FROM)
		{
			mPreviousEnteredAddressFrom = text;
		} else if (mActiveLoader == LOADER_ADDRESS_TO)
		{
			mPreviousEnteredAddressTo = text;
		} else if (mActiveLoader == LOADER_TITLE_FROM)
		{
			mPreviousEnteredContactFrom = text;
		} else if (mActiveLoader == LOADER_TITLE_TO)
		{
			mPreviousEnteredContactTo = text;
		}
	}

	private void setSelectedValue(String text)
	{
		if (mActiveLoader == LOADER_ADDRESS_FROM)
		{
			mAutoCompleteTextViewFromAddress.setText(text);
		} else if (mActiveLoader == LOADER_ADDRESS_TO)
		{
			mAutoCompleteTextViewToAddress.setText(text);
		} else if (mActiveLoader == LOADER_TITLE_FROM)
		{
			mAutoCompleteTextViewFromTitle.setText(text);
		} else if (mActiveLoader == LOADER_TITLE_TO)
		{
			mAutoCompleteTextViewToTitle.setText(text);
		}
	}

	private AutoCompleteTextView getAutoCompleteTextView()
	{
		if (mActiveLoader == LOADER_ADDRESS_FROM)
		{
			return mAutoCompleteTextViewFromAddress;
		} else if (mActiveLoader == LOADER_ADDRESS_TO)
		{
			return mAutoCompleteTextViewToAddress;
		} else if (mActiveLoader == LOADER_TITLE_FROM)
		{
			return mAutoCompleteTextViewFromTitle;
		} else if (mActiveLoader == LOADER_TITLE_TO)
		{
			return mAutoCompleteTextViewToTitle;
		}
		return null;
	}

	private ProgressBar getProgressBar()
	{
		if (mActiveLoader == LOADER_ADDRESS_FROM)
		{
			return mProgressBarFromAddress;
		} else if (mActiveLoader == LOADER_ADDRESS_TO)
		{
			return mProgressBarToAddress;
		} else if (mActiveLoader == LOADER_TITLE_FROM)
		{
			return mProgressBarFromTitle;
		} else if (mActiveLoader == LOADER_TITLE_TO)
		{
			return mProgressBarToTitle;
		}
		return null;
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus)
	{
		int id = v.getId();
		switch (id)
		{
		case R.id.autoCompleteTextViewToTitle:
			mActiveLoader = LOADER_TITLE_TO;
			break;
		case R.id.autoCompleteTextViewFromTitle:
			mActiveLoader = LOADER_TITLE_FROM;
			break;
		case R.id.autoCompleteTextViewFrom:
			mActiveLoader = LOADER_ADDRESS_FROM;
			break;
		case R.id.autoCompleteTextViewTo:
			mActiveLoader = LOADER_ADDRESS_TO;
			break;
		default:
			break;
		}
	}

	private boolean isAddressMode()
	{
		return mActiveLoader == LOADER_ADDRESS_FROM || mActiveLoader == LOADER_ADDRESS_TO;
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
			loadAddress(mQuery);
		}
	}

	private class LoadContactRun implements Runnable
	{
		private final String mQuery;

		LoadContactRun(String q)
		{
			mQuery = q;
		}

		@Override
		public void run()
		{
			loadContact(mQuery);
		}
	}

	private class ImageLoader extends AsyncTask<Void, Void, Drawable>
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
	private void addBitmapToCache(Long key, Drawable bitmap)
	{
		if (getBitmapFromCache(key) == null)
		{
			cache.put(key, bitmap);
		}
	}

	/** Retrive image from cache */
	private Drawable getBitmapFromCache(Long key)
	{
		return cache.get(key);
	}
	
	private void loadContactAddress(long id)
	{
		new LoadContactAddress().execute(id);
	}
	
	private static final String[] PROJECTION_CONTACT_ADDRESS = {ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS};
	private class LoadContactAddress extends AsyncTask<Long, Void, String>
	{	
		protected String doInBackground(Long... args)
		{
			String address = "";
			ContentResolver cr = getActivity().getContentResolver();
			Cursor c = null;
			try
			{
				String selection = ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID + "=?";
				String[] selectionArgs = {args[0]+""};
				c = cr.query(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI, PROJECTION_CONTACT_ADDRESS, selection, selectionArgs, null);
				if(c.moveToFirst())
				{
					address = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS));
				}
			}
			finally
			{
				if(c!=null)
					c.close();
			}
			
			return address;
		}

		protected void onPostExecute(String address)
		{
			if(mActiveLoader==LOADER_TITLE_FROM && TextUtils.isEmpty(mAutoCompleteTextViewFromAddress.getText()))
			{
				mAutoCompleteTextViewFromAddress.setText(address);
			}
			else if(mActiveLoader==LOADER_TITLE_TO && TextUtils.isEmpty(mAutoCompleteTextViewToAddress.getText()))
			{
				mAutoCompleteTextViewToAddress.setText(address);
			}
		}
	}
}
