package com.miracleas.minrute;

import java.util.ArrayList;

import com.actionbarsherlock.app.SherlockFragment;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.util.LruCache;
import android.support.v4.widget.CursorAdapter;
import android.widget.ImageView;

public abstract class LocaleImageHandlerFragment extends SherlockFragment
{
	protected static LruCache<Long, Drawable> mCache;
	protected ArrayList<Long> mIdList;
	protected Drawable mBitmapDrawableDummy = null;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mIdList = new ArrayList<Long>();
		mCache = new LruCache<Long, Drawable>(10);
		mBitmapDrawableDummy = getResources().getDrawable(R.drawable.empty_photo);
	}

	protected abstract int getImageHeight();
	protected abstract int getImageWidth();

	protected void loadLocaleImage(long id, String pathToImg, ImageView v)
	{
		Object tag;
		long imgId = id;
		if (!mIdList.contains(Long.valueOf(imgId)))
		{
			mIdList.add(imgId);
		}
		String pathToFile = pathToImg;
		Uri contactUri = Uri.parse(pathToFile);
		tag = imgId;
		v.setTag(tag);
		Drawable d = mCache.get(imgId);
		if (d != null)
		{
			v.setImageDrawable(d);
		} else
		{
			ImageLoader loader = new ImageLoader(v, contactUri, imgId);
			loader.execute();
		}
	}

	protected class ImageLoader extends AsyncTask<Void, Void, Drawable>
	{
		private ImageView mView;
		private Uri mUri;
		private Object tag;
		private long position;

		public ImageLoader(ImageView view, Uri uri, long position)
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
			Bitmap bitmap = scaleImage(mUri.getPath());
			return new BitmapDrawable(getResources(), bitmap);
		}

		public Bitmap scaleImage(String pathToBitmap)
		{

			/*
			 * There isn't enough memory to open up more than a couple camera
			 * photos
			 */
			/* So pre-scale the target bitmap into which the file is decoded */

			/* Get the size of the ImageView */
			int targetW = getImageWidth();
			int targetH = getImageHeight();
			if (targetW == 0)
			{
				targetW = 80;
				targetH = 80;
			}

			/* Get the size of the image */
			BitmapFactory.Options bmOptions = new BitmapFactory.Options();
			bmOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(pathToBitmap, bmOptions);
			int photoW = bmOptions.outWidth;
			int photoH = bmOptions.outHeight;

			/* Figure out which way needs to be reduced less */
			int scaleFactor = 1;
			if ((targetW > 0) || (targetH > 0))
			{
				scaleFactor = Math.min(photoW / targetW, photoH / targetH);
			}

			/* Set bitmap options to scale the image decode target */
			bmOptions.inJustDecodeBounds = false;
			bmOptions.inSampleSize = scaleFactor;
			bmOptions.inPurgeable = true;

			/* Decode the JPEG file into a Bitmap */
			return BitmapFactory.decodeFile(pathToBitmap, bmOptions);

		}

		protected void onPostExecute(Drawable bitmap)
		{
			if (bitmap == null || bitmap.getIntrinsicHeight() == 0)
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
				mView.setImageDrawable(bitmap);
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
		if (mCache.get(key) == null)
		{
			mCache.put(key, bitmap);
		}
	}

	protected Drawable getCachedDrawable(long key)
	{
		return mCache.get(key);
	}
}
