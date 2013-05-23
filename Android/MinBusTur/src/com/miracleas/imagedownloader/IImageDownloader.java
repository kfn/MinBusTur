package com.miracleas.imagedownloader;

import android.content.Context;
import android.widget.ImageView;

public interface IImageDownloader
{
	public static final String IMAGE_CACHE_DIR = "images";
	// Default memory cache size
	public static final int DEFAULT_MEM_CACHE_SIZE = 1024 * 1024 * 8; // 8MB
	// Default disk cache size
	public static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 15; // 15MB

	boolean download(String url, ImageView imageView);

	void setPauseWork(boolean pause);

	public void setLoadingImage(int resId);

	int getPlaceHolderRessource();

	void setExitTasksEarly(boolean b);

	void flushCache();

	void closeCache();

	void setImageFadeIn(boolean fadeIn);

	void setImageSize(int size);
	
	void cancelMyWork(ImageView imageView);

	/**
	 * set Context for logging. Only for test
	 * 
	 * @param c
	 */
	void setContext(Context c);
	
	void setAuthToken(String authToken);
}
