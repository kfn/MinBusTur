package com.miracleas.minbustur;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.miracleas.camera.PhotoIntentActivity;
import com.miracleas.imagedownloader.ImageDownloaderActivity;
import com.miracleas.imagedownloader.Utils;
import com.miracleas.minbustur.provider.JourneyDetailStopImagesMetaData;
import com.miracleas.minbustur.provider.JourneyDetailStopMetaData;

/**
 * A fragment representing a single Ejendom detail screen. This fragment is
 * either contained in a {@link PrisniveauActivity} in two-pane mode (on
 * tablets) or a {@link ToiletDetailActivity} on handsets.
 */
public class TripStopDetailsImagesFragment extends SherlockFragment implements LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener
{
	public static final String tag = TripStopDetailsImagesFragment.class.getName();
	private ImageButton btnClose = null;

	private static final int LOAD_TOILET_IMAGES = 2;
	private boolean mIsTablet = false;
	private int mImageThumbSize;
    private int mImageThumbSpacing;
    private GridView mGridView = null;
    private ImageAdapter mAdapter;
    private ImageDownloaderActivity mImageDownloaderActivity = null;
    private Button mBtnTakePicture;
    
	/**
	 * The columns needed by the cursor adapter
	 */
	protected static final String[] PROJECTION = new String[] { 
		JourneyDetailStopImagesMetaData.TableMetaData._ID,
		JourneyDetailStopImagesMetaData.TableMetaData.URL
	};
	
	public static TripStopDetailsImagesFragment createInstance(String stopId)
	{
		Bundle args = new Bundle();
		args.putString(JourneyDetailStopMetaData.TableMetaData._ID, stopId);
		TripStopDetailsImagesFragment fragment = new TripStopDetailsImagesFragment();
		fragment.setArguments(args);
		return fragment;
	}
	
	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public TripStopDetailsImagesFragment()
	{
	}
	
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		if (!(activity instanceof PhotoIntentActivity))
		{
			throw new IllegalStateException("Activity must be a PhotoIntentActivity.");
		}
		mImageDownloaderActivity = (ImageDownloaderActivity)activity;
		mImageDownloaderActivity.getIImageDownloader().setLoadingImage(R.drawable.empty_photo);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(false);
		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);
	
		getSherlockActivity().getSupportLoaderManager().initLoader(LoaderConstants.LOADER_TRIP_STOP_IMAGES, getArguments(), this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		final View v = inflater.inflate(R.layout.fragment_trip_stop_detail_images, container, false);
		mAdapter = new ImageAdapter(getActivity(), null);		
		mGridView = (GridView) v.findViewById(R.id.gridView);		
		mGridView.setAdapter(mAdapter);
		mGridView.setOnItemClickListener(this);
		mGridView.setOnScrollListener(new AbsListView.OnScrollListener()
		{
			@Override
			public void onScrollStateChanged(AbsListView absListView, int scrollState)
			{
				// Pause fetcher to ensure smoother scrolling when flinging
				if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING)
				{
					mImageDownloaderActivity.getIImageDownloader().setPauseWork(true);
				} else
				{
					mImageDownloaderActivity.getIImageDownloader().setPauseWork(false);
				}
			}

			@Override
			public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount)
			{
			}
		});

		// This listener is used to get the final width of the GridView and then
		// calculate the
		// number of columns and the width of each column. The width of each
		// column is variable
		// as the GridView has stretchMode=columnWidth. The column width is used
		// to set the height
		// of each view so we get nice square thumbnails.
		mGridView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
		{
			@Override
			public void onGlobalLayout()
			{
				if (mAdapter.getNumColumns() == 0)
				{
					final int numColumns = (int) Math.floor(mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
					if (numColumns > 0)
					{
						final int columnWidth = (mGridView.getWidth() / numColumns) - mImageThumbSpacing;
						mAdapter.setNumColumns(numColumns);
						mAdapter.setItemHeight(columnWidth);
						if (BuildConfig.DEBUG)
						{
							Log.d(tag, "onCreateView - numColumns set to " + numColumns);
						}
					}
				}
			}
		});
		mBtnTakePicture = (Button)v.findViewById(R.id.btnAddPicture);
		PhotoIntentActivity activity = (PhotoIntentActivity)getActivity();
		activity.setBtnListenerOrDisable(mBtnTakePicture);
		return v;
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args)
	{
		if (id == LoaderConstants.LOADER_TRIP_STOP_IMAGES && args.containsKey(JourneyDetailStopMetaData.TableMetaData._ID))
		{
			String selection = JourneyDetailStopImagesMetaData.TableMetaData.JOURNEY_DETAIL_STOP_ID + "=?";
			String[] selectionArgs = {args.getString(JourneyDetailStopMetaData.TableMetaData._ID)};				
			return new CursorLoader(getActivity(), JourneyDetailStopImagesMetaData.TableMetaData.CONTENT_URI, PROJECTION, selection, selectionArgs, null);
		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
	{
		if(loader.getId()==LoaderConstants.LOADER_TRIP_STOP_IMAGES)
		{
			mAdapter.swapCursor(cursor);
		}					
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader)
	{
		if(loader.getId()==LoaderConstants.LOADER_TRIP_STOP_IMAGES)
		{
			mAdapter.swapCursor(null);
		}
				
	}

	public void resetLoader(Bundle args)
	{
		if (args != null && args.containsKey(JourneyDetailStopMetaData.TableMetaData._ID))
		{
			if(getLoaderManager().getLoader(LoaderConstants.LOADER_TRIP_STOP_IMAGES)==null)
			{
				getSherlockActivity().getSupportLoaderManager().initLoader(LoaderConstants.LOADER_TRIP_STOP_IMAGES, args, this);
			}
			else
			{
				getSherlockActivity().getSupportLoaderManager().restartLoader(LoaderConstants.LOADER_TRIP_STOP_IMAGES, args, this);
			}
			
		}
		else
		{
			Log.e(tag, "missing parameters for fragment");
		}
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id)
	{
		final Intent i = new Intent(getActivity(), TripStopDetailsImageActivity.class);
		i.putExtra(TripStopDetailsImageActivity.EXTRA_IMAGE, (int) id);
		Bundle args = getArguments();
		i.putExtra(JourneyDetailStopMetaData.TableMetaData._ID, args.getString(JourneyDetailStopMetaData.TableMetaData._ID));
		
		if (Utils.hasJellyBean())
		{
			// makeThumbnailScaleUpAnimation() looks kind of ugly here as the
			// loading spinner may
			// show plus the thumbnail image in GridView is cropped. so using
			// makeScaleUpAnimation() instead.
			android.app.ActivityOptions options = android.app.ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight());
			getActivity().startActivity(i, options.toBundle());
		} else
		{
			startActivity(i);
		}
	}
	
	/**
	 * The main adapter that backs the GridView. This is fairly standard except
	 * the number of columns in the GridView is used to create a fake top row of
	 * empty views as we use a transparent ActionBar and don't want the real top
	 * row of images to start off covered by it.
	 */
	private class ImageAdapter extends CursorAdapter
	{

		private final Context mContext;
		private int mItemHeight = 0;
		private int mNumColumns = 0;
		private int mActionBarHeight = 0;
		private GridView.LayoutParams mImageViewLayoutParams;
		private LayoutInflater mInf = null;
		private int iUrl;
		

		public ImageAdapter(Context context, Cursor data)
		{
			super(context, data, 0);
			mContext = context;
			mImageViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			mInf = LayoutInflater.from(context);
		}

		@Override
		public void bindView(View v, Context context, Cursor cursor)
		{
			mImageDownloaderActivity.getIImageDownloader().download(cursor.getString(iUrl), ((ImageView)v));	
		}
		
		public Cursor swapCursor(Cursor newCursor)
		{
			if(newCursor!=null)
			{
				iUrl = newCursor.getColumnIndex(JourneyDetailStopImagesMetaData.TableMetaData.URL);
			}
			return super.swapCursor(newCursor);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			ImageView imageView = new ImageView(context);
			imageView.setLayoutParams(mImageViewLayoutParams);
			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
			return imageView;
		}

		/**
		 * Sets the item height. Useful for when we know the column width so the
		 * height can be set to match.
		 * 
		 * @param height
		 */
		public void setItemHeight(int height)
		{
			if (height == mItemHeight)
			{
				return;
			}
			mItemHeight = height;
			mImageViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT, mItemHeight);
			mImageDownloaderActivity.getIImageDownloader().setImageSize(height);
			notifyDataSetChanged();
		}

		public void setNumColumns(int numColumns)
		{
			mNumColumns = numColumns;
		}

		public int getNumColumns()
		{
			return mNumColumns;
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inf)
	{	
		inf.inflate(R.menu.activity_details, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		boolean handled = false;

		switch (item.getItemId()) {
		
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	


}
