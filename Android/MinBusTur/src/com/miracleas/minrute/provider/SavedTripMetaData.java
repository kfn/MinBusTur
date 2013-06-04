package com.miracleas.minrute.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class SavedTripMetaData
{
	public static final String PROVIDER_NAME = "MinBusTurProvider";
	public static final String PACKAGE_NAME = "com.miracleas.minrute.provider.";
	public static final String AUTHORITY = PACKAGE_NAME + PROVIDER_NAME;
	public static final String COLLECTION_TYPE = "saved_trips";
	public static final String ITEM_TYPE = "saved_trip";
	public static final String TABLE_NAME = SavedTripMetaData.COLLECTION_TYPE;

	private SavedTripMetaData()
	{
	}

	public static String getTableSchema()
	{
		return TableMetaData._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
				TableMetaData.ORIGIN_ADDRESS + " TEXT,"
				+ TableMetaData.DESTINATION_ADDRESS + " TEXT,"
				+ TableMetaData.WAY_POINT_ADDRESS + " TEXT,"
				+ TableMetaData.ORIGIN_ID + " TEXT,"
				+ TableMetaData.DESTINATION_ID + " TEXT,"
				+ TableMetaData.WAY_POINT_ID + " TEXT,"
				+ TableMetaData.ORIGIN_LAT_Y + " INTEGER,"
				+ TableMetaData.DESTINATION_LAT_Y + " INTEGER,"
				+ TableMetaData.ORIGIN_LNG_X + " INTEGER,"
				+ TableMetaData.DEST_LNG_X + " INTEGER,"
                + TableMetaData.TITLE + " TEXT,"
				+ TableMetaData.updated + " LONG,"
				+ TableMetaData.SEARCH_FOR_ARRIVAL + " INTEGER"
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

		public static final String ORIGIN_ADDRESS = "originAddress";
		public static final String DESTINATION_ADDRESS = "destAddress";
		public static final String WAY_POINT_ADDRESS = "way_point_address";
		public static final String ORIGIN_ID = "origin_id";
		public static final String DESTINATION_ID = "destination_id";
		public static final String WAY_POINT_ID = "way_point_id";
        public static final String updated = "updated";
        public static final String TITLE = "title";
        public static final String DEST_LNG_X = "destx";
		public static final String ORIGIN_LNG_X = "originx";
		public static final String DESTINATION_LAT_Y = "desty";
		public static final String ORIGIN_LAT_Y = "originy";
		public static final String SEARCH_FOR_ARRIVAL = "search_for_arrival";
		
	}
}
