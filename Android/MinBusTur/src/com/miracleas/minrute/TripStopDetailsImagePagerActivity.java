/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.miracleas.minrute;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.miracleas.camera.PhotoGoogleDriveActivity;
import com.miracleas.imagedownloader.IImageDownloader;
import com.miracleas.imagedownloader.ImageDownloaderActivity;
import com.miracleas.imagedownloader.ImageFetcher;
import com.miracleas.imagedownloader.Utils;

import com.miracleas.minrute.model.MyLittleImage;
import com.miracleas.minrute.provider.JourneyDetailStopImagesMetaData;
import com.miracleas.minrute.provider.JourneyDetailStopMetaData;

public class TripStopDetailsImagePagerActivity extends PhotoGoogleDriveActivity implements OnClickListener, LoaderCallbacks<Cursor>
{
	private static final String CACHE_DIR = "images";
	public static final String EXTRA_IMAGE_POSITION = "extra_image";
	public static final String EXTRA_IMAGES = "extra_images";

	private static final int LOAD_TOILET_IMAGES = 5;

	/**
	 * The columns needed by the cursor adapter
	 */
	protected static final String[] PROJECTION = new String[] { 
		JourneyDetailStopImagesMetaData.TableMetaData._ID, 
		JourneyDetailStopImagesMetaData.TableMetaData.URL,
		JourneyDetailStopImagesMetaData.TableMetaData.FILE_LOCALE_PATH};

	private IImageDownloader mImageLoader = null;
	private ImagePagerAdapter mAdapter;
	private ViewPager mPager;

	@TargetApi(11)
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		if (BuildConfig.DEBUG)
		{
			Utils.enableStrictMode();
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_trip_stop_details_image);
		mImageLoader = getIImageDownloader();

		// Fetch screen height and width, to use as our max size when loading
		// images as this
		// activity runs full screen
		final DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		final int height = displayMetrics.heightPixels;
		final int width = displayMetrics.widthPixels;

		// Set up ViewPager and backing adapter
		mPager = (ViewPager) findViewById(R.id.pager);
		mPager.setPageMargin((int) getResources().getDimension(R.dimen.image_detail_pager_margin));
		mPager.setOffscreenPageLimit(2);

		// Set up activity to go full screen
		getWindow().addFlags(LayoutParams.FLAG_FULLSCREEN);

		// Enable some additional newer visibility and ActionBar features to
		// create a more
		// immersive photo viewing experience
		if (Utils.hasHoneycomb())
		{
			final ActionBar actionBar = getActionBar();

			// Hide title text and set home as up
			actionBar.setDisplayShowTitleEnabled(false);
			actionBar.setDisplayHomeAsUpEnabled(true);

			// Hide and show the ActionBar as the visibility changes
			mPager.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener()
			{
				@Override
				public void onSystemUiVisibilityChange(int vis)
				{
					if ((vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0)
					{
						actionBar.hide();
					} else
					{
						actionBar.show();
					}
				}
			});

			// Start low profile mode and hide ActionBar
			mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
			actionBar.hide();
		}

		getSupportLoaderManager().restartLoader(LOAD_TOILET_IMAGES, getIntent().getExtras(), this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case android.R.id.home:
			// NavUtils.navigateUpFromSameTask(this);
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * The main adapter that backs the ViewPager. A subclass of
	 * FragmentStatePagerAdapter as there could be a large number of items in
	 * the ViewPager and we don't want to retain them all in memory at once but
	 * create/destroy them on the fly.
	 */
	private class ImagePagerAdapter extends FragmentStatePagerAdapter
	{
		private MyLittleImage[] mImages;

		public ImagePagerAdapter(FragmentManager fm, MyLittleImage[] images)
		{
			super(fm);
			mImages = images;
		}

		@Override
		public int getCount()
		{
			return mImages.length;
		}

		@Override
		public Fragment getItem(int position)
		{
			return TripStopDetailsImagePagerFragmentItem.newInstance(mImages[position]);
		}
	}

	/**
	 * Set on the ImageView in the ViewPager children fragments, to
	 * enable/disable low profile mode when the ImageView is touched.
	 */
	@TargetApi(11)
	@Override
	public void onClick(View v)
	{
		final int vis = mPager.getSystemUiVisibility();
		if ((vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0)
		{
			mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
		} else
		{
			mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
		}
	}

	@Override
	public IImageDownloader getIImageDownloader()
	{
		if (mImageLoader == null)
		{
			mImageLoader = ImageFetcher.getInstance(this, getSupportFragmentManager(), CACHE_DIR);
			mImageLoader.setLoadingImage(R.drawable.empty_photo);
		}
		return mImageLoader;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args)
	{
		if (id == LOAD_TOILET_IMAGES && args.containsKey(JourneyDetailStopMetaData.TableMetaData._ID))
		{
			//i.putExtra(JourneyDetailStopMetaData.TableMetaData.LATITUDE, args.getString(JourneyDetailStopMetaData.TableMetaData.LATITUDE));
			//i.putExtra(JourneyDetailStopMetaData.TableMetaData.LONGITUDE, args.getString(JourneyDetailStopMetaData.TableMetaData.LONGITUDE));
			String selection = JourneyDetailStopImagesMetaData.TableMetaData.LAT + "=? AND "+JourneyDetailStopImagesMetaData.TableMetaData.LNG + "=?";
			String[] selectionArgs = { args.getString(JourneyDetailStopMetaData.TableMetaData.LATITUDE), args.getString(JourneyDetailStopMetaData.TableMetaData.LONGITUDE) };
			return new CursorLoader(this, JourneyDetailStopImagesMetaData.TableMetaData.CONTENT_URI, PROJECTION, selection, selectionArgs, null);
		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
	{
		if (loader.getId() == LOAD_TOILET_IMAGES && cursor.moveToFirst())
		{
			MyLittleImage[] images = new MyLittleImage[cursor.getCount()];

			int iUrl = cursor.getColumnIndex(JourneyDetailStopImagesMetaData.TableMetaData.URL);
			int iId = cursor.getColumnIndex(JourneyDetailStopImagesMetaData.TableMetaData._ID);
			int iPathToImg = cursor.getColumnIndex(JourneyDetailStopImagesMetaData.TableMetaData.FILE_LOCALE_PATH);
			int i = 0;
			do
			{
				MyLittleImage img = new MyLittleImage(cursor.getString(iUrl), cursor.getString(iPathToImg), cursor.getLong(iId));
				images[i] = img;
				i++;
			} while (cursor.moveToNext());
			// Set up ViewPager and backing adapter
			mAdapter = new ImagePagerAdapter(getSupportFragmentManager(), images);
			mPager.setAdapter(mAdapter);

			// Set the current item based on the extra passed in to this
			// activity
			final int extraCurrentItem = getIntent().getIntExtra(EXTRA_IMAGE_POSITION, -1);
			if (extraCurrentItem != -1)
			{
				mPager.setCurrentItem(extraCurrentItem);
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader)
	{
		if (loader.getId() == LOAD_TOILET_IMAGES)
		{

		}

	}

}
