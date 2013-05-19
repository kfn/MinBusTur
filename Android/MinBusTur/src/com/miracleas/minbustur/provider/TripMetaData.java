package com.miracleas.minbustur.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class TripMetaData
{
	public static final String PROVIDER_NAME = "MinBusTurProvider";
	public static final String PACKAGE_NAME = "com.miracleas.minbustur.provider.";
	public static final String AUTHORITY = PACKAGE_NAME + PROVIDER_NAME;
	public static final String COLLECTION_TYPE = "trips";
	public static final String ITEM_TYPE = "trip";
	public static final String TABLE_NAME = TripMetaData.COLLECTION_TYPE;

	private TripMetaData()
	{
	}

	public static String getTableSchema()
	{
		return TableMetaData._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
				TableMetaData.DURATION + " LONG," 
				+ TableMetaData.LEG_COUNT + " INTEGER,"
				+ TableMetaData.LEG_NAMES + " TEXT,"
				+ TableMetaData.LEG_TYPES + " TEXT,"
				+ TableMetaData.updated + " LONG"
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

		public static final String DURATION = "duration";
		public static final String LEG_COUNT = "leg_count";
		public static final String LEG_NAMES = "leg_names";
		public static final String LEG_TYPES = "leg_types";
		public static final String updated = "updated";

	}
}
