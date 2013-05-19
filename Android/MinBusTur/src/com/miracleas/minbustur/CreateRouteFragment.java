package com.miracleas.minbustur;

import java.io.UnsupportedEncodingException;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
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
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.miracleas.minbustur.model.AddressSearch;
import com.miracleas.minbustur.model.TripRequest;
import com.miracleas.minbustur.net.AddressFetcher;
import com.miracleas.minbustur.net.TripFetcher;
import com.miracleas.minbustur.provider.AddressProviderMetaData;

public class CreateRouteFragment extends CreateRouteFragmentBase 
{
	public static final String tag = CreateRouteFragment.class.getName();
	private AutoCompleteAddressAdapter mAutoCompleteAdapterFrom = null;
	private AutoCompleteAddressAdapter mAutoCompleteAdapterTo = null;
	private AutoCompleteContactsAdapter mAutoCompleteContactsAdapterTo = null;
	private AutoCompleteContactsAdapter mAutoCompleteContactsAdapterFrom = null;
	private LoadAddressesRun mLoadAddressesRun = null;
	private LoadContactRun mLoadContactRun = null;
	private LoadTrips mLoadTrips = null;
	private TripFetcher mTripFetcher = null;
	

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
		View rootView = super.onCreateView(inflater, container, savedInstanceState);
		mAutoCompleteAdapterFrom = new AutoCompleteAddressAdapter(getActivity(), null, 0);
		mAutoCompleteTextViewFromAddress.setAdapter(mAutoCompleteAdapterFrom);
		mAutoCompleteAdapterTo = new AutoCompleteAddressAdapter(getActivity(), null, 0);
		mAutoCompleteTextViewToAddress.setAdapter(mAutoCompleteAdapterTo);
		mAutoCompleteContactsAdapterTo = new AutoCompleteContactsAdapter(getActivity(), null, 0);
		mAutoCompleteContactsAdapterFrom = new AutoCompleteContactsAdapter(getActivity(), null, 0);
		mAutoCompleteTextViewToTitle.setAdapter(mAutoCompleteContactsAdapterTo);
		mAutoCompleteTextViewFromTitle.setAdapter(mAutoCompleteContactsAdapterFrom);
		return rootView;
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
		mDataUri = Uri.withAppendedPath(AddressProviderMetaData.TableMetaData.CONTENT_URI, "search");
		mAddressFetcher = new AddressFetcher(getActivity(), mDataUri);
		Bundle args = new Bundle();
		args.putString(AddressProviderMetaData.TableMetaData.address, "a");
		args.putString(ContactsContract.Contacts.DISPLAY_NAME, "abc");
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
				mHandler.postDelayed(mLoadAddressesRun, 500);
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
			String positionId = a.getId(position);
			if(mActiveLoader == LOADER_ADDRESS_FROM)
			{
				mTripRequest.setOriginId(positionId);
				mTripRequest.setOriginCoordName(text);	
				mTripRequest.setOriginCoordX(lat);
				mTripRequest.setOriginCoordY(lng);
				
			}
			else if(mActiveLoader == LOADER_ADDRESS_TO)
			{
				mTripRequest.setDestId(positionId);
				mTripRequest.setDestCoordName(text);
				mTripRequest.setDestCoordX(lat);
				mTripRequest.setDestCoordY(lng);
			}
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


	private void loadAddress(String query)
	{
		mLoadCount++;
		query = query.trim();
		setPreviousEnteredText(query);
		Log.d(tag, "lookup for: " + query);
		new LoadAddresses().execute(query);
		Bundle args = new Bundle();
		args.putString(AddressProviderMetaData.TableMetaData.address, query);
		getProgressBar().setVisibility(View.VISIBLE);
		if(getSherlockActivity().getSupportLoaderManager().getLoader(mActiveLoader)==null)
		{
			getSherlockActivity().getSupportLoaderManager().initLoader(mActiveLoader, args, this);
		}
		else
		{
			getSherlockActivity().getSupportLoaderManager().restartLoader(mActiveLoader, args, this);
		}
		
	}

	private class LoadAddresses extends AsyncTask<String, Void, Void>
	{

		@Override
		protected Void doInBackground(String... params)
		{
			try
			{
				mAddressFetcher.performGeocode(params[0]); //mDataUri
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
		private int iType;
		private int iId;
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
			int icon = getIcon(cursor.getInt(iType));
			if(icon!=-1)
			{
				tv.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
			}
			
		}
		
		private int getIcon(int type)
		{
			switch(type)
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
				iLat = newCursor.getColumnIndex(AddressProviderMetaData.TableMetaData.lat);
				iLng = newCursor.getColumnIndex(AddressProviderMetaData.TableMetaData.lng);
				iType = newCursor.getColumnIndex(AddressProviderMetaData.TableMetaData.type_int);
				iId = newCursor.getColumnIndex(AddressProviderMetaData.TableMetaData.id);
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
	}

	private void loadContact(String query)
	{
		setPreviousEnteredText(query);
		Log.d(tag, "lockup for: " + query);
		Bundle args = new Bundle();
		args.putString(ContactsContract.Contacts.DISPLAY_NAME, query);
		if(getSherlockActivity().getSupportLoaderManager().getLoader(mActiveLoader)==null)
		{
			getSherlockActivity().getSupportLoaderManager().initLoader(mActiveLoader, args, this);
		}
		else
		{
			getSherlockActivity().getSupportLoaderManager().restartLoader(mActiveLoader, args, this);
		}
		
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
	private void loadTrips()
	{
		if(mTripRequest.isValid())
		{
			if(mLoadTrips==null || mLoadTrips.getStatus()==AsyncTask.Status.FINISHED)
			{
				if(mTripFetcher==null)
				{
					mTripFetcher = new TripFetcher(getActivity(), null, null);
				}
				mLoadTrips = new LoadTrips();
				mLoadTrips.execute(null,null,null);
			}
		}
		else
		{
			Toast.makeText(getActivity(), "Not valid", Toast.LENGTH_SHORT).show();
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
		if(v.getId()==R.id.btnFindRoute)
		{
			loadTrips();
		}
		
	}
}
