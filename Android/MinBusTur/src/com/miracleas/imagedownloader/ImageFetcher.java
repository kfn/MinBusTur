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

package com.miracleas.imagedownloader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.miracleas.minrute.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.v4.app.FragmentManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

/**
 * A simple subclass of {@link ImageResizer} that fetches and resizes images
 * fetched from a URL.
 */
public class ImageFetcher extends ImageResizer
{
	private static final String TAG = Utils.tag;
	private static final int HTTP_CACHE_SIZE = 10 * 1024 * 1024; // 10MB
	private static final String HTTP_CACHE_DIR = "http";
	private static final int IO_BUFFER_SIZE = 8 * 1024;

	private DiskLruCache mHttpDiskCache;
	private File mHttpCacheDir;
	private boolean mHttpDiskCacheStarting = true;
	private final Object mHttpDiskCacheLock = new Object();
	private static final int DISK_CACHE_INDEX = 0;

	private static ImageFetcher instance = null;

	public static ImageFetcher getInstance(Activity activity, FragmentManager manager, String imageCacheDir)
	{
		// if(instance==null)
		// {
		// Toast.makeText(activity, "Cache created", Toast.LENGTH_LONG).show();
		final DisplayMetrics displayMetrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

		final int height = activity.getResources().getDimensionPixelSize(R.dimen.image_height);// displayMetrics.heightPixels;
		final int width = activity.getResources().getDimensionPixelSize(R.dimen.image_width);

		// For this sample we'll use half of the longest width to resize our
		// images. As the
		// image scaling ensures the image is larger than this, we should be
		// left with a
		// resolution that is appropriate for both portrait and landscape. For
		// best image quality
		// we shouldn't divide by 2, but this will use more memory and require a
		// larger memory
		// cache.
		final int longest = (height > width ? height : width); // / 2;
		ImageCache.ImageCacheParams cacheParams = null;
		instance = new ImageFetcher(activity, longest);
		try
		{
			cacheParams = new ImageCache.ImageCacheParams(activity, imageCacheDir);
			cacheParams.setMemCacheSizePercent(activity, 0.40f); // Set memorycache to
																	// 40% of
																	// mem class
			instance.addImageCache(manager, cacheParams);
			instance.setImageFadeIn(false);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Toast.makeText(activity, activity.getString(R.string.cache_unavailable), Toast.LENGTH_LONG).show();
		}
		// }

		return instance;
	}

	/**
	 * Initialize providing a target image width and height for the processing
	 * images.
	 * 
	 * @param context
	 * @param imageWidth
	 * @param imageHeight
	 * @throws Exception
	 */
	public ImageFetcher(Context context, int imageWidth, int imageHeight)
	{
		super(context, imageWidth, imageHeight);
		init(context);
	}

	/**
	 * Initialize providing a single target image size (used for both width and
	 * height);
	 * 
	 * @param context
	 * @param imageSize
	 * @throws Exception
	 */
	public ImageFetcher(Context context, int imageSize)
	{
		super(context, imageSize);
		init(context);
	}

	private void init(Context context)
	{
		checkConnection(context);
		try
		{
			mHttpCacheDir = ImageCache.getDiskCacheDir(context, HTTP_CACHE_DIR);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}

	@Override
	protected void initDiskCacheInternal()
	{
		super.initDiskCacheInternal();
		initHttpDiskCache();
	}

	private void initHttpDiskCache()
	{
		if (!mHttpCacheDir.exists())
		{
			mHttpCacheDir.mkdirs();
		}
		synchronized (mHttpDiskCacheLock)
		{
			if (ImageCache.getUsableSpace(mHttpCacheDir) > HTTP_CACHE_SIZE)
			{
				try
				{
					mHttpDiskCache = DiskLruCache.open(mHttpCacheDir, 1, 1, HTTP_CACHE_SIZE);
					if (Utils.DEBUG)
					{
						Log.d(TAG, "HTTP cache initialized");
					}
				}
				catch (IOException e)
				{
					mHttpDiskCache = null;
				}
			}
			mHttpDiskCacheStarting = false;
			mHttpDiskCacheLock.notifyAll();
		}
	}

	@Override
	protected void clearCacheInternal()
	{
		super.clearCacheInternal();
		synchronized (mHttpDiskCacheLock)
		{
			if (mHttpDiskCache != null && !mHttpDiskCache.isClosed())
			{
				try
				{
					mHttpDiskCache.delete();
					if (Utils.DEBUG)
					{
						Log.d(TAG, "HTTP cache cleared");
					}
				}
				catch (IOException e)
				{
					Log.e(TAG, "clearCacheInternal - " + e);
				}
				mHttpDiskCache = null;
				mHttpDiskCacheStarting = true;
				initHttpDiskCache();
			}
		}
	}

	@Override
	protected void flushCacheInternal()
	{
		super.flushCacheInternal();
		synchronized (mHttpDiskCacheLock)
		{
			if (mHttpDiskCache != null)
			{
				try
				{
					mHttpDiskCache.flush();
					if (Utils.DEBUG)
					{
						Log.d(TAG, "HTTP cache flushed");
					}
				}
				catch (IOException e)
				{
					Log.e(TAG, "flush - " + e);
				}
			}
		}
	}

	@Override
	protected void closeCacheInternal()
	{
		super.closeCacheInternal();
		synchronized (mHttpDiskCacheLock)
		{
			if (mHttpDiskCache != null)
			{
				try
				{
					if (!mHttpDiskCache.isClosed())
					{
						mHttpDiskCache.close();
						mHttpDiskCache = null;
						if (Utils.DEBUG)
						{
							Log.d(TAG, "HTTP cache closed");
							// Log.d(TAG, "Imagefetcher nulled");
						}
						// instance = null;
					}
				}
				catch (IOException e)
				{
					Log.e(TAG, "closeCacheInternal - " + e);
				}
			}
		}
	}

	/**
	 * Simple network connection check.
	 * 
	 * @param context
	 */
	private void checkConnection(Context context)
	{
		final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		if (networkInfo == null || !networkInfo.isConnectedOrConnecting())
		{
			Toast.makeText(context, R.string.no_network_connection_toast, Toast.LENGTH_LONG).show();
			Log.e(TAG, "checkConnection - no connection found");
		}
	}

	/**
	 * The main process method, which will be called by the ImageWorker in the
	 * AsyncTask background thread.
	 * 
	 * @param data
	 *            The data to load the bitmap, in this case, a regular http URL
	 * @return The downloaded and resized bitmap
	 */
	private Bitmap processBitmap(String data)
	{
		if (Utils.DEBUG)
		{
			Log.d(TAG, "processBitmap - " + data);
		}

		final String key = ImageCache.hashKeyForDisk(data);
		FileDescriptor fileDescriptor = null;
		FileInputStream fileInputStream = null;
		DiskLruCache.Snapshot snapshot;
		synchronized (mHttpDiskCacheLock)
		{
			// Wait for disk cache to initialize
			while (mHttpDiskCacheStarting)
			{
				try
				{
					mHttpDiskCacheLock.wait();
				}
				catch (InterruptedException e)
				{
				}
			}

			if (mHttpDiskCache != null)
			{
				try
				{
					snapshot = mHttpDiskCache.get(key);
					if (snapshot == null)
					{
						if (Utils.DEBUG)
						{
							Log.d(TAG, "processBitmap, not found in http cache, downloading...");
						}
						DiskLruCache.Editor editor = mHttpDiskCache.edit(key);
						if (editor != null)
						{
							if (downloadUrlToStream(data, editor.newOutputStream(DISK_CACHE_INDEX)))
							{
								editor.commit();
							}
							else
							{
								editor.abort();
							}
						}
						snapshot = mHttpDiskCache.get(key);
					}
					if (snapshot != null)
					{
						fileInputStream = (FileInputStream) snapshot.getInputStream(DISK_CACHE_INDEX);
						fileDescriptor = fileInputStream.getFD();
					}
				}
				catch (IOException e)
				{
					Log.e(TAG, "processBitmap - " + e);
				}
				catch (IllegalStateException e)
				{
					Log.e(TAG, "processBitmap - " + e);
				}
				finally
				{
					if (fileDescriptor == null && fileInputStream != null)
					{
						try
						{
							fileInputStream.close();
						}
						catch (IOException e)
						{
						}
					}
				}
			}
		}

		Bitmap bitmap = null;
		if (fileDescriptor != null)
		{
			bitmap = decodeSampledBitmapFromDescriptor(fileDescriptor, mImageWidth, mImageHeight);
		}
		if (fileInputStream != null)
		{
			try
			{
				fileInputStream.close();
			}
			catch (IOException e)
			{
			}
		}
		return bitmap;
	}

	@Override
	protected Bitmap processBitmap(Object data)
	{
		return processBitmap(String.valueOf(data));
	}

	/**
	 * Download a bitmap from a URL and write the content to an output stream.
	 * 
	 * @param urlString
	 *            : The URL to fetch
	 * @return true if successful, false otherwise
	 */
	public boolean downloadUrlToStream(String urlString, OutputStream outputStream)
	{
		long before = 0;
		if (Utils.DEBUG)
		{
			Log.d(Utils.tag, "Start download");
			before = System.currentTimeMillis();
		}
		disableConnectionReuseIfNecessary();
		HttpURLConnection urlConnection = null;
		BufferedOutputStream out = null;
		BufferedInputStream in = null;

		try
		{
			final URL url = new URL(urlString);
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setConnectTimeout(5000);
			if(mAuthToken!=null)
			{
				urlConnection.setRequestProperty("Authorization", "OAuth " + mAuthToken);
			}
			
			in = new BufferedInputStream(urlConnection.getInputStream(), IO_BUFFER_SIZE);
			out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);

			int b;
			while ((b = in.read()) != -1)
			{
				out.write(b);
			}
			if (Utils.DEBUG && getContext() != null)
			{
				long after = System.currentTimeMillis();
				String[] temp = urlString.split("/");
				DataLogger.Log(getContext(), DataLogger.LOAD_IMAGE, before, after, temp[temp.length - 1]);
				Log.d(Utils.tag, "download time: " + (after - before) + " ms");
			}
			return true;
		}
		catch (final IOException e)
		{
			Log.e(TAG, "Error in downloadBitmap - " + e);
			e.printStackTrace();
		}
		finally
		{
			if (urlConnection != null)
			{
				urlConnection.disconnect();
			}
			try
			{
				if (out != null)
				{
					out.close();
				}
				if (in != null)
				{
					in.close();
				}
			}
			catch (final IOException e)
			{
			}
		}
		return false;
	}

	/**
	 * Workaround for bug pre-Froyo, see here for more info:
	 * http://android-developers.blogspot.com/2011/09/androids-http-clients.html
	 */
	public static void disableConnectionReuseIfNecessary()
	{
		// HTTP connection reuse which was buggy pre-froyo
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO)
		{
			System.setProperty("http.keepAlive", "false");
		}
	}
}
