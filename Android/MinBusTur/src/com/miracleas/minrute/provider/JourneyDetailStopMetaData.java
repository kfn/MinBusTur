package com.miracleas.minrute.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class JourneyDetailStopMetaData
{
	public static final String PROVIDER_NAME = "MinBusTurProvider";
	public static final String PACKAGE_NAME = "com.miracleas.minrute.provider.";
	public static final String AUTHORITY = PACKAGE_NAME + PROVIDER_NAME;
	public static final String COLLECTION_TYPE = "journey_detail_stops";
	public static final String ITEM_TYPE = "journey_detail_stop";
	public static final String TABLE_NAME = JourneyDetailStopMetaData.COLLECTION_TYPE;

	private JourneyDetailStopMetaData()
	{
	}

	public static String getTableSchema()
	{
		return TableMetaData._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ TableMetaData.JOURNEY_DETAIL_ID + " INTEGER," 
				+ TableMetaData.NAME + " TEXT," 
				+ TableMetaData.LATITUDE + " INTEGER," 
				+ TableMetaData.LONGITUDE + " INTEGER,"
				+ TableMetaData.ROUTE_ID_X + " INTEGER,"
				+ TableMetaData.ARR_TIME + " TEXT,"
				+ TableMetaData.ARR_DATE + " TEXT,"
				+ TableMetaData.DEP_TIME + " TEXT,"
				+ TableMetaData.DEP_DATE + " TEXT,"
				+ TableMetaData.TRIP_ID + " TEXT,"
				+ TableMetaData.LEG_ID + " TEXT,"
				+ TableMetaData.TRACK + " TEXT,"
				+ TableMetaData.RT_DEP_TIME + " TEXT,"
				+ TableMetaData.RT_ARR_TIME + " TEXT,"
				+ TableMetaData.RT_DEP_DATE + " TEXT,"
				+ TableMetaData.RT_ARR_DATE + " TEXT,"
				+ TableMetaData.IS_PART_OF_USER_ROUTE + " INTEGER,"
				+ TableMetaData.SEARCH_ID + " TEXT"
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

		public static final String NAME = "name";
		public static final String LATITUDE = "y";
		public static final String LONGITUDE = "x";
		public static final String ROUTE_ID_X = "routeIdx";
		public static final String ARR_TIME = "arrTime";
		public static final String ARR_DATE = "arrDate";
		public static final String DEP_TIME = "depTime";
		public static final String DEP_DATE = "depDate";
		public static final String JOURNEY_DETAIL_ID = "journey_detail_id";
		public static final String LEG_ID = "leg_id";
		public static final String TRIP_ID = "stop_trip_id";
		public static final String TRACK = "track";
		public static final String RT_DEP_TIME = "rtDepTime";
		public static final String RT_DEP_DATE = "rtDepDate";
		public static final String RT_ARR_TIME = "rtArrTime";
		public static final String RT_ARR_DATE = "rtArrDate";
		public static final String IS_PART_OF_USER_ROUTE = "partOfUserRoute";
		public static final String SEARCH_ID = "searchId";


	}
}
