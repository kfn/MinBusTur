package com.miracleas.minrute.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class TripMetaData
{
	public static final String PROVIDER_NAME = "MinBusTurProvider";
	public static final String PACKAGE_NAME = "com.miracleas.minrute.provider.";
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
				+ TableMetaData.DURATION_LABEL + " TEXT," 
				+ TableMetaData.LEG_COUNT + " INTEGER,"
				+ TableMetaData.LEG_NAMES + " TEXT,"
				+ TableMetaData.LEG_TYPES + " TEXT,"
				+ TableMetaData.TRANSPORT_CHANGES + " INTEGER,"
				+ TableMetaData.DEPATURE_TIME + " TEXT,"
				+ TableMetaData.DEPATURES_IN_TIME_LABEL + " TEXT,"
				+ TableMetaData.DEPATURES_IN_TIME + " LONG,"
				+ TableMetaData.DURATION_WALK + " TEXT,"
				+ TableMetaData.DURATION_BUS + " TEXT,"
				+ TableMetaData.DURATION_TRAIN + " TEXT,"
				+ TableMetaData.ARRIVAL_TIME + " TEXT,"
				+ TableMetaData.ARRIVES_IN_TIME_LABEL + " TEXT,"
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
		public static final String DURATION_LABEL = "duration_label";
		public static final String LEG_COUNT = "leg_count";
		public static final String LEG_NAMES = "leg_names";
		public static final String LEG_TYPES = "leg_types";
		public static final String DEPATURE_TIME = "depature_time";
		public static final String ARRIVAL_TIME = "arrival_time";
		public static final String TRANSPORT_CHANGES = "transport_changes";
		public static final String DURATION_TRAIN = "depature_train";
		public static final String DURATION_BUS = "depature_bus";
		public static final String DURATION_WALK = "depature_walk";
		public static final String updated = "updated";
		public static final String ARRIVES_IN_TIME_LABEL = "arrives_in";
		public static final String DEPATURES_IN_TIME_LABEL = "departures_in_time_label";
		public static final String DEPATURES_IN_TIME = "departures_in_time";

	}
}
