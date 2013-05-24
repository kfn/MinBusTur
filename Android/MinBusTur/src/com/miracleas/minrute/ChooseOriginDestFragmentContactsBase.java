package com.miracleas.minrute;

import java.io.InputStream;
import java.util.ArrayList;



import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.util.LruCache;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;

public abstract class ChooseOriginDestFragmentContactsBase extends ChooseOriginDestFragmentBase implements LoaderCallbacks<Cursor>, OnClickListener, OnFocusChangeListener
{
	protected static LruCache<Long, Drawable> cache;
	protected ArrayList<Long> id_list = new ArrayList<Long>();
	protected Drawable mBitmapDrawableDummy = null;
	
	protected ProgressBar mProgressBarFromTitle = null;
	protected ProgressBar mProgressBarToTitle = null;
	private AutoCompleteContactsAdapter mAutoCompleteContactsAdapterTo = null;
	private AutoCompleteContactsAdapter mAutoCompleteContactsAdapterFrom = null;
	private LoadContactRun mLoadContactRun = null;
	protected AutoCompleteTextView mAutoCompleteTextViewToTitle = null;
	protected AutoCompleteTextView mAutoCompleteTextViewFromTitle = null;
	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = super.onCreateView(inflater, container, savedInstanceState);
		/*mAutoCompleteContactsAdapterTo = new AutoCompleteContactsAdapter(getActivity(), null, 0);
		mAutoCompleteContactsAdapterFrom = new AutoCompleteContactsAdapter(getActivity(), null, 0);
		mAutoCompleteTextViewToTitle = (AutoCompleteTextView) rootView.findViewById(R.id.autoCompleteTextViewToTitle);
		mAutoCompleteTextViewFromTitle = (AutoCompleteTextView) rootView.findViewById(R.id.autoCompleteTextViewFromTitle);
		mAutoCompleteTextViewToTitle.setAdapter(mAutoCompleteContactsAdapterTo);
		mAutoCompleteTextViewFromTitle.setAdapter(mAutoCompleteContactsAdapterFrom);
		mProgressBarFromTitle = (ProgressBar) rootView.findViewById(R.id.progressBarFromTitle);
		mProgressBarToTitle = (ProgressBar) rootView.findViewById(R.id.progressBarToTitle);
		*/
		
		return rootView;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		cache = new LruCache<Long, Drawable>(20);
		mBitmapDrawableDummy = getResources().getDrawable(R.drawable.ic_action_user);
	}
	
	private class LoadContactRun implements Runnable
	{
		private final String mQuery;
		private final int loaderId;

		LoadContactRun(String q, int loaderId)
		{
			mQuery = q;
			this.loaderId = loaderId;
		}

		@Override
		public void run()
		{
			loadContact(mQuery, loaderId);
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
			// tv.setCompoundDrawablesWithIntrinsicBounds(mBitmapDrawableDummy,
			// null, null, null);

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
	
	private void loadContact(String query, int loaderId)
	{
		
		Bundle args = new Bundle();
		args.putString(ContactsContract.Contacts.DISPLAY_NAME, query);
		if (getSherlockActivity().getSupportLoaderManager().getLoader(loaderId) == null)
		{
			getSherlockActivity().getSupportLoaderManager().initLoader(loaderId, args, this);
		} else
		{
			getSherlockActivity().getSupportLoaderManager().restartLoader(loaderId, args, this);
		}

	}
	
	private void loadContactAddress(long id, int loaderId)
	{
		new LoadContactAddress(loaderId).execute(id);
	}

	private static final String[] PROJECTION_CONTACT_ADDRESS = { ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS };

	private class LoadContactAddress extends AsyncTask<Long, Void, String>
	{
		private final int loaderId;

		LoadContactAddress(int loaderId)
		{
			this.loaderId = loaderId;
		}

		protected String doInBackground(Long... args)
		{
			String address = "";
			ContentResolver cr = getActivity().getContentResolver();
			Cursor c = null;
			try
			{
				String selection = ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID + "=?";
				String[] selectionArgs = { args[0] + "" };
				c = cr.query(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI, PROJECTION_CONTACT_ADDRESS, selection, selectionArgs, null);
				if (c.moveToFirst())
				{
					address = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS));
				}
			} finally
			{
				if (c != null)
					c.close();
			}

			return address;
		}

		protected void onPostExecute(String address)
		{
			if (loaderId == LoaderConstants.LOADER_TITLE_FROM && TextUtils.isEmpty(mAutoCompleteTextViewFromAddress.getText()))
			{
				mAutoCompleteTextViewFromAddress.setText(address);
			} else if (loaderId == LoaderConstants.LOADER_TITLE_TO && TextUtils.isEmpty(mAutoCompleteTextViewToAddress.getText()))
			{
				mAutoCompleteTextViewToAddress.setText(address);
			}
		}
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
	

}
