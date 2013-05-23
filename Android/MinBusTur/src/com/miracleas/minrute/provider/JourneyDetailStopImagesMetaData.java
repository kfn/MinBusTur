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
				+ TableMetaData.JOURNEY_DETAIL_STOP_ID + " INTEGER," 
				+ TableMetaData.URL + " TEXT," 
				+ TableMetaData.LAT + " TEXT,"
				+ TableMetaData.LNG + " TEXT,"
				+ TableMetaData.FILE_ID + " TEXT"
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

		public static final String JOURNEY_DETAIL_STOP_ID = "stop_id";
		public static final String URL = "img_url";
		public static final String LAT = "lat";
		public static final String LNG = "lng";
		public static final String FILE_ID = "file_id";
	
	}
}
