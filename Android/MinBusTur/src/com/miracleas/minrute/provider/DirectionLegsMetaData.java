package com.miracleas.minrute.provider;

import android.net.Uri;
import android.provider.BaseColumns;


public class DirectionLegsMetaData
{
	public static final String PROVIDER_NAME = "MinBusTurProvider";
	public static final String PACKAGE_NAME = "com.miracleas.minrute.provider.";
	public static final String AUTHORITY = PACKAGE_NAME + PROVIDER_NAME;
	public static final String COLLECTION_TYPE = "direction_legs";
	public static final String ITEM_TYPE = "direction_leg";
	public static final String TABLE_NAME = DirectionLegsMetaData.COLLECTION_TYPE;

	private DirectionLegsMetaData()
	{
	}

	public static String getTableSchema()
	{
		return TableMetaData._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
				TableMetaData.DISTANCE + " TEXT,"
				+ TableMetaData.DURATION + " TEXT,"
				+ TableMetaData.END_ADDRESS + " TEXT,"
				+ TableMetaData.START_ADDRESS + " TEXT,"
				+ TableMetaData.updated + " LONG,"
                + TableMetaData.TRIP_LEG_ID + " INTEGER,"
                + TableMetaData.OVERVIEW_POLYLINE + " TEXT"
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
		
		public static final String DISTANCE = "distance";
		public static final String DURATION = "duration";
		public static final String END_ADDRESS = "end_address";
		public static final String START_ADDRESS = "start_address";
        public static final String updated = "updated";
        public static final String TRIP_LEG_ID = "trip_leg_id";
        public static final String OVERVIEW_POLYLINE = "overview_polyline";


	}
}
