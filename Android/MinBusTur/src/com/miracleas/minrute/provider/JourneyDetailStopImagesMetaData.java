package com.miracleas.minrute.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class JourneyDetailStopImagesMetaData
{
	public static final String PROVIDER_NAME = "MinBusTurProvider";
	public static final String PACKAGE_NAME = "com.miracleas.minrute.provider.";
	public static final String AUTHORITY = PACKAGE_NAME + PROVIDER_NAME;
	public static final String COLLECTION_TYPE = "journey_detail_stop_images";
	public static final String ITEM_TYPE = "journey_detail_stop_image";
	public static final String TABLE_NAME = JourneyDetailStopImagesMetaData.COLLECTION_TYPE;

	private JourneyDetailStopImagesMetaData()
	{
	}

	public static String getTableSchema()
	{
		return TableMetaData._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ TableMetaData.URL + " TEXT," 
				+ TableMetaData.LAT + " TEXT,"
				+ TableMetaData.LNG + " TEXT,"
				+ TableMetaData.FILE_ID + " TEXT,"
				+ TableMetaData.UPLOADED + " INTEGER INTEGER DEFAULT 0,"
				+ TableMetaData.IS_UPLOADING + " INTEGER DEFAULT 0,"
				+ TableMetaData.FILE_LOCALE_PATH + " TEXT,"
				+ TableMetaData.FILE_TITLE + " TEXT,"
				+ TableMetaData.FILE_MIME_TYPE + " TEXT,"
				+ TableMetaData.STOP_NAME + " TEXT,"
				+ TableMetaData.IS_GOOGLE_STREET_LAT_LNG + " INTEGER DEFAULT 0,"
				+ TableMetaData.IS_GOOGLE_STREET_NAME_SEARCH + " INTEGER DEFAULT 0"
				;
	}

	// inner class describing columns and their types
	public static final class TableMetaData implements BaseColumns
	{		
		
		
		
		private TableMetaData()
		{
		}

		// uri and mime type definitions
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + COLLECTION_TYPE);
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + PACKAGE_NAME + ITEM_TYPE;
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd." + PACKAGE_NAME + ITEM_TYPE;

		
		public static final String URL = "img_url";
		public static final String LAT = "lat";
		public static final String LNG = "lng";
		public static final String FILE_ID = "file_id";
		public static final String UPLOADED = "uploaded";
		public static final String IS_UPLOADING = "is_uploading";
		public static final String FILE_MIME_TYPE = "mime_type";
		public static final String FILE_TITLE = "file_title";
		public static final String FILE_LOCALE_PATH = "file_locale_path";
		public static final String STOP_NAME = "stop_name";
		public static final String IS_GOOGLE_STREET_LAT_LNG = "is_google_street_latlng_search";
		public static final String IS_GOOGLE_STREET_NAME_SEARCH = "is_google_street_name_search";
	}
}
