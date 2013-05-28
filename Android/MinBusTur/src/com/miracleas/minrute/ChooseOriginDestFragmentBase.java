package com.miracleas.minrute;

import java.util.Calendar;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.FilterQueryProvider;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.TimePicker;

import com.actionbarsherlock.app.SherlockFragment;
import com.miracleas.minrute.model.AddressSearch;
import com.miracleas.minrute.model.TripRequest;
import com.miracleas.minrute.provider.AddressProviderMetaData;
import com.miracleas.minrute.utils.DateHelper;

public abstract class ChooseOriginDestFragmentBase extends SherlockFragment implements LoaderCallbacks<Cursor>, OnClickListener, OnFocusChangeListener
{
	public static final String tag = ChooseOriginDestFragmentBase.class.getName();
	protected AutoCompleteTextView mAutoCompleteTextViewFromAddress;
	protected AutoCompleteTextView mAutoCompleteTextViewToAddress;
	protected int mSelectedAddressFromPosition = -1;
	protected int mSelectedAddressToPosition = -1;
	
	protected AutoCompleteAddressAdapter mAutoCompleteAdapterFrom = null;
	protected AutoCompleteAddressAdapter mAutoCompleteAdapterTo = null;	

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
	
	protected TextView mTextViewDate = null;
	protected TextView mTextViewTime = null;
	
	

	
	ArrayAdapter<String> adapter;
    String dates[] = { ""};
	
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
		View rootView = inflater.inflate(R.layout.fragment_choose_orign_dest, container, false);
		mTextViewDate = (TextView)rootView.findViewById(R.id.btnDate);
		mTextViewTime = (TextView)rootView.findViewById(R.id.btnTime);
		mTextViewDate.setOnClickListener(this);
		mTextViewTime.setOnClickListener(this);
		
		Calendar c = Calendar.getInstance();
		
		mTextViewDate.setText(getDateFormat(c));
		mTextViewTime.setText(getTimeFormat(c));
		mTripRequest.setTime(DateHelper.convertDateToString(c, DateHelper.formatterTime));
		mTripRequest.setDate(DateHelper.convertDateToString(c, DateHelper.formatterDateRejseplanen));
		
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
				mAutoCompleteTextViewToAddress.clearFocus();
				
				//ViewHelper.hideKeyboard(getActivity(), mAutoCompleteTextViewToAddress);
			}			
		});
		initAutoComplete(mAutoCompleteTextViewFromAddress);
		initAutoComplete(mAutoCompleteTextViewToAddress);
		
		mProgressBarToAddress = (ProgressBar) rootView.findViewById(R.id.progressBarToAddress);
		mProgressBarFromAddress = (ProgressBar) rootView.findViewById(R.id.progressBarFromAddress);
		
		mBtnFindRoute = rootView.findViewById(R.id.btnFindRoute);
		mBtnFindRoute.setOnClickListener(this);
		
		RadioButton radioBtnArrival = (RadioButton)rootView.findViewById(R.id.radio_arrival);
		RadioButton radioBtnDepartue = (RadioButton)rootView.findViewById(R.id.radio_departue);
		radioBtnArrival.setOnClickListener(this);
		radioBtnDepartue.setOnClickListener(this);
		return rootView;
	}
	
	public abstract void textChangedHelper(CharSequence s, int loaderId);

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
		else
		{
			if(mAutoCompleteTextViewFromAddress==v)
			{
				String s = mAutoCompleteTextViewFromAddress.getText().toString();
				if(!TextUtils.isEmpty(s))
				{
					mAutoCompleteTextViewFromAddress.setHint(mAutoCompleteTextViewFromAddress.getText());
					mAutoCompleteTextViewFromAddress.setText("");
				}
				
			}
			else if(mAutoCompleteTextViewToAddress==v)
			{
				String s = mAutoCompleteTextViewToAddress.getText().toString();
				if(!TextUtils.isEmpty(s))
				{
					mAutoCompleteTextViewToAddress.setHint(mAutoCompleteTextViewToAddress.getText());
					mAutoCompleteTextViewToAddress.setText("");
				}
			}
		}
	}
	
	public void setAddress(String address)
	{
		
		if(mAutoCompleteTextViewToAddress==mFocusedView)
		{
			mAutoCompleteTextViewToAddress.setHint(address);
			mAutoCompleteAdapterTo.getFilter().filter(address);
		}
		else
		{
			mAutoCompleteTextViewFromAddress.setHint(address);
			mAutoCompleteAdapterFrom.getFilter().filter(address);			
		}
	}
	
	public int getActiveLoader()
	{
		int loaderId = 0;
		int id = mFocusedView.getId();
		switch(id)
		{
		case R.id.autoCompleteTextViewFrom:
			loaderId = LoaderConstants.LOADER_ADDRESS_FROM;
			break;
		case R.id.autoCompleteTextViewTo:
			loaderId = LoaderConstants.LOADER_ADDRESS_TO;
			break;
		}
		return loaderId;
	}
	@Override
	public void onClick(View v)
	{
		if(v.getId()==R.id.btnDate)
		{
			DialogFragment newFragment = new DatePickerFragment();
		    newFragment.show(getActivity().getSupportFragmentManager(), "datePicker");
		}
		else if(v.getId()==R.id.btnTime)
		{
			DialogFragment newFragment = new TimePickerFragment();
		    newFragment.show(getActivity().getSupportFragmentManager(), "timePicker");
		}
		else if(v.getId()==R.id.radio_departue)
		{
			onRadioButtonClicked(v);
		}
		else if(v.getId()==R.id.radio_arrival)
		{
			onRadioButtonClicked(v);
		}
		
	}
	
	
	public void onTimeSet(TimePicker view, int hourOfDay, int minute)
	{
		Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, hourOfDay);
		c.set(Calendar.MINUTE, minute);
		mTextViewTime.setText(getTimeFormat(c));
		mTripRequest.setTime(DateHelper.convertDateToString(c, DateHelper.formatterTime));
	}
	
	public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
	{
		Calendar c = Calendar.getInstance();
		c.set(Calendar.YEAR, year);
		c.set(Calendar.MONTH, monthOfYear);
		c.set(Calendar.DAY_OF_MONTH, dayOfMonth);		
		mTextViewDate.setText(getDateFormat(c));
		mTripRequest.setDate(DateHelper.convertDateToString(c, DateHelper.formatterDateRejseplanen));
	}
	
	private String getDateFormat(Calendar c)
	{
		String month = String.format(Locale.getDefault(),"%tB",c);
		StringBuilder b = new StringBuilder();
		b.append(c.get(Calendar.DAY_OF_MONTH)).append(". ").append(month).append(" ").append(c.get(Calendar.YEAR));
		return b.toString();
	}
	
	private String getTimeFormat(Calendar c)
	{
		return DateHelper.convertDateToString(c, DateHelper.formatterTime);
	}
	
	private void onRadioButtonClicked(View view) {
	    // Is the button now checked?
	    boolean checked = ((RadioButton) view).isChecked();
	    
	    // Check which radio button was clicked
	    switch(view.getId()) {
	        case R.id.radio_arrival:
	            if (checked)
	            	mTripRequest.setSearchForArrival(1);
	            break;
	        case R.id.radio_departue:
	            if (checked)
	            	mTripRequest.setSearchForArrival(0);
	            break;
	    }
	}
	
	protected class AutoCompleteAddressAdapter extends CursorAdapter
	{
		private int iAddress;
		private int iY;
		private int iX;
		private int iType;
		private int iId;
		private LayoutInflater mInf = null;
		private CharSequence mPreviousConstraint = "";
		private int mLoaderId = 0;
		
		public AutoCompleteAddressAdapter(Context context, Cursor c, int flags, final int loaderId)
		{
			super(context, c, FLAG_REGISTER_CONTENT_OBSERVER);
			mInf = LayoutInflater.from(context);
			mLoaderId = loaderId;
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
					//) GROUP BY (").append(AddressProviderMetaData.TableMetaData.address);
					StringBuilder b = new StringBuilder();
					b.append(AddressProviderMetaData.TableMetaData.address).append(" LIKE '").append(constraint).append("%') GROUP BY (").append(AddressProviderMetaData.TableMetaData.address);
					return getActivity().getContentResolver().query(AddressProviderMetaData.TableMetaData.CONTENT_URI, PROJECTION, b.toString(), null, AddressProviderMetaData.TableMetaData.address+" LIMIT 10");
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
				oldCursor.close();
			}
			getActivity().startManagingCursor(newCursor);
			
			if(newCursor.getCount()==1)
			{
				if(mLoaderId==LoaderConstants.LOADER_ADDRESS_FROM)
				{		
					mSelectedAddressFromPosition = 0;
				}
				else if(mLoaderId==LoaderConstants.LOADER_ADDRESS_TO)
				{
					mSelectedAddressToPosition = 0;
				}	
			}
			

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


}
