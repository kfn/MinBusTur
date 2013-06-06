package com.miracleas.minrute;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.miracleas.camera.PhotoGoogleDriveActivity;
import com.miracleas.imagedownloader.IImageDownloader;
import com.miracleas.imagedownloader.Utils;
import com.miracleas.minrute.model.TripLeg;
import com.miracleas.minrute.model.TripLegStop;
import com.miracleas.minrute.net.BaseFetcher;
import com.miracleas.minrute.provider.StopImagesMetaData;
import com.miracleas.minrute.provider.TripLegDetailStopMetaData;

/**
 * A fragment representing a single Ejendom detail screen. This fragment is
 * either contained in a {@link PrisniveauActivity} in two-pane mode (on
 * tablets) or a {@link ToiletDetailActivity} on handsets.
 */
public class TripStopDetailsImagesFragment extends LocaleImageHandlerFragment implements LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener, OnClickListener
{
	public static final String tag = TripStopDetailsImagesFragment.class.getName();
	private ImageButton btnClose = null;

	private static final int LOAD_TOILET_IMAGES = 2;
	private boolean mIsTablet = false;
	private int mImageThumbSize;
    private int mImageThumbSpacing;
    private GridView mGridView = null;
    private ImageAdapter mAdapter;
    private IImageDownloader mImageDownloaderActivity = null;
    private Button mBtnTakePicture;
    private Button mBtnUploadPictures;
    private int mItemHeight = 0;
    private boolean mHasImagesNotUploaded = false;
    private ProgressBar mProgressBarUpload = null;
    
	/**
	 * The columns needed by the cursor adapter
	 */
	protected static final String[] PROJECTION = new String[] { 
		StopImagesMetaData.TableMetaData._ID,
		StopImagesMetaData.TableMetaData.URL,
		StopImagesMetaData.TableMetaData.FILE_LOCALE_PATH
	};
	
	public static TripStopDetailsImagesFragment createInstance(TripLegStop stop, TripLeg leg)
	{
		Bundle args = new Bundle();
		args.putParcelable(TripLegStop.tag, stop);
		args.putParcelable(TripLeg.tag, leg);
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
		if (!(activity instanceof PhotoGoogleDriveActivity))
		{
			throw new IllegalStateException("Activity must be a PhotoGoogleDriveActivity.");
		}
		mImageDownloaderActivity = (IImageDownloader)activity;
		mImageDownloaderActivity.setLoadingImage(R.drawable.empty_photo);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(false);
		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);
       
        initUploadServiceBroadcastReceiver();
		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		final View v = inflater.inflate(R.layout.fragment_trip_stop_detail_images, container, false);
		mAdapter = new ImageAdapter(getActivity(), null);	
		mProgressBarUpload = (ProgressBar)v.findViewById(R.id.progressBarUpload);
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
					mImageDownloaderActivity.setPauseWork(true);
				} else
				{
					mImageDownloaderActivity.setPauseWork(false);
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
				getSherlockActivity().getSupportLoaderManager().initLoader(LoaderConstants.LOADER_TRIP_STOP_IMAGES, getArguments(), TripStopDetailsImagesFragment.this);
				getSherlockActivity().getSupportLoaderManager().initLoader(LoaderConstants.LOADER_COUNT_OF_NOT_UPLOADED_IMAGES, getArguments(), TripStopDetailsImagesFragment.this);
			}
		});
		mBtnTakePicture = (Button)v.findViewById(R.id.btnAddPicture);
		mBtnUploadPictures = (Button)v.findViewById(R.id.btnUploadPictures);
		mBtnUploadPictures.setOnClickListener(this);
		PhotoGoogleDriveActivity activity = (PhotoGoogleDriveActivity)getActivity();
		Bundle args = getArguments();
		
		TripLegStop stop = args.getParcelable(TripLegStop.tag);
		TripLeg leg = args.getParcelable(TripLeg.tag);
		activity.setBtnListenerOrDisable(mBtnTakePicture, stop, leg);
		return v;
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args)
	{
		TripLegStop stop = args.getParcelable(TripLegStop.tag);
		TripLeg leg = args.getParcelable(TripLeg.tag);
		
		if (id == LoaderConstants.LOADER_TRIP_STOP_IMAGES)
		{
			
			String selection = StopImagesMetaData.TableMetaData.STOP_NAME + "=? AND "+StopImagesMetaData.TableMetaData.TRANSPORT_DIRECTION + "=?";
			String[] selectionArgs = {stop.name, leg.notes};				
			return new CursorLoader(getActivity(), StopImagesMetaData.TableMetaData.CONTENT_URI, PROJECTION, selection, selectionArgs, null);
		}
		else if (id == LoaderConstants.LOADER_COUNT_OF_NOT_UPLOADED_IMAGES)
		{
			
			String selection = StopImagesMetaData.TableMetaData.STOP_NAME + "=? AND "
			   +StopImagesMetaData.TableMetaData.UPLOADED+"=? AND "+StopImagesMetaData.TableMetaData.TRANSPORT_DIRECTION + "=?";;
			String[] selectionArgs = {stop.name, "0", leg.notes};				
			return new CursorLoader(getActivity(), StopImagesMetaData.TableMetaData.CONTENT_URI, PROJECTION, selection, selectionArgs, null);
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
		else if (loader.getId() == LoaderConstants.LOADER_COUNT_OF_NOT_UPLOADED_IMAGES)
		{
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			if(cursor.moveToFirst())
			{	
				params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				mBtnUploadPictures.setVisibility(View.VISIBLE);
				mHasImagesNotUploaded = true;
			}
			else
			{
				params.addRule(RelativeLayout.CENTER_HORIZONTAL);
				mBtnUploadPictures.setVisibility(View.GONE);
				if(mHasImagesNotUploaded)
				{
					Toast.makeText(getActivity(), getString(R.string.uploading_finished), Toast.LENGTH_LONG).show();
					mHasImagesNotUploaded = false;
					mProgressBarUpload.setVisibility(View.GONE);
				}
			}
			mBtnTakePicture.setLayoutParams(params);
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

	private void resetLoader(Bundle args)
	{
		if (args != null)
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
		final Intent i = new Intent(getActivity(), TripStopDetailsImagePagerActivity.class);
		i.putExtra(TripStopDetailsImagePagerActivity.EXTRA_IMAGE_POSITION, position);
		Bundle args = getArguments();
		TripLegStop stop = args.getParcelable(TripLegStop.tag);
		TripLeg leg = args.getParcelable(TripLeg.tag);
		i.putExtra(TripLegStop.tag, stop);
		i.putExtra(TripLeg.tag, leg);
		
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
		
		private int mNumColumns = 0;
		private int mActionBarHeight = 0;
		private GridView.LayoutParams mImageViewLayoutParams;
		private int iUrl;
		private int iId = -1;
		private int iLocalePath = -1;

		public ImageAdapter(Context context, Cursor data)
		{
			super(context, data, 0);
			mContext = context;
			mImageViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);	
		}

		@Override
		public void bindView(View v, Context context, Cursor cursor)
		{
			String url = cursor.getString(iUrl);
			if(TextUtils.isEmpty(url))
			{
				long imgId = cursor.getLong(iId);
				String pathToFile = cursor.getString(iLocalePath);
				loadLocaleImage(imgId, pathToFile, (ImageView) v);
			}
			else
			{
				mImageDownloaderActivity.download(url, ((ImageView)v));	
			}
			
		}

		public Cursor swapCursor(Cursor newCursor)
		{
			if(newCursor!=null)
			{
				iUrl = newCursor.getColumnIndex(StopImagesMetaData.TableMetaData.URL);
				iId = newCursor.getColumnIndex(StopImagesMetaData.TableMetaData._ID);
				iLocalePath = newCursor.getColumnIndex(StopImagesMetaData.TableMetaData.FILE_LOCALE_PATH);
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
			mImageDownloaderActivity.setImageSize(height);
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

	@Override
	protected int getImageHeight()
	{
		return mItemHeight;
	}

	@Override
	protected int getImageWidth()
	{
		return mItemHeight;
	}

	@Override
	public void onClick(View v)
	{
		if(v.getId()==R.id.btnUploadPictures)
		{
			((PhotoGoogleDriveActivity)getActivity()).startUploadService();
		}
		
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		removeBroadcastReceiver();
	}
	
	private void removeBroadcastReceiver()
	{
		if(mServerResponseReceiver!=null)
		{
			LocalBroadcastManager r = LocalBroadcastManager.getInstance(getActivity());
			r.unregisterReceiver(mServerResponseReceiver);
		}
	}
	
	protected void initUploadServiceBroadcastReceiver()
	{
		LocalBroadcastManager r = LocalBroadcastManager.getInstance(getActivity());
		IntentFilter filter = new IntentFilter();
		filter.addAction(BaseFetcher.BROADCAST_SERVER_RESPONSE_ACTION);
		r.registerReceiver(mServerResponseReceiver, filter);
	}
	
	   BroadcastReceiver mServerResponseReceiver = new BroadcastReceiver()
		{

			@Override
			public void onReceive(Context context, Intent intent)
			{
				String action = intent.getAction();
				if (action.equals(BaseFetcher.BROADCAST_SERVER_RESPONSE_ACTION))
				{						
					boolean showProgress = intent.getBooleanExtra(BaseFetcher.BROADCAST_SHOW_PROGRESS, false);
					String msg = intent.getStringExtra(BaseFetcher.BROADCAST_MSG);
					if(!TextUtils.isEmpty(msg))
					{						
						Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
					}	
					if(showProgress)
					{
						mProgressBarUpload.setVisibility(View.VISIBLE);
					}
					else
					{
						mProgressBarUpload.setVisibility(View.GONE);
					}
					
				} 
			}
		};
	


}
