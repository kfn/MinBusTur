package com.miracleas.imagedownloader;

public interface ImageDownloaderActivity
{
	IImageDownloader getIImageDownloader();

	void closeCache();

	void flushCache();

	void setExitTasksEarly(boolean exitEarly);
}
