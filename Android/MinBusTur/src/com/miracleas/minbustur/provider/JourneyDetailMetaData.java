package com.miracleas.minbustur.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class JourneyDetailMetaData
{
	public static final String PROVIDER_NAME = "MinBusTurProvider";
	public static final String PACKAGE_NAME = "com.miracleas.minbustur.provider.";
	public static final String AUTHORITY = PACKAGE_NAME + PROVIDER_NAME;
	public static final String COLLECTION_TYPE = "journey_details";
	public static final String ITEM_TYPE = "journey_detail";
	public static final String TABLE_NAME = JourneyDetailMetaData.COLLECTION_TYPE;

	private JourneyDetailMetaData()
	{
	}

	public static String getTableSchema()
	{
		return TableMetaData._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
				TableMetaData.NAME + " TEXT," 
				+ TableMetaData.NAME_ROUTE_ID_X_FROM + " INTEGER," 
				+ TableMetaData.NAME_ROUTE_ID_X_TO + " INTEGER,"
				+ TableMetaData.TYPE + " TEXT,"
				+ TableMetaData.TYPE_ROUTE_ID_X_FROM + " TEXT,"
				+ TableMetaData.TYPE_ROUTE_ID_X_TO + " TEXT,"
				+ TableMetaData.REF + " TEXT,"
				+ TableMetaData.TRIP_ID + " LONG,"
				+ TableMetaData.LEG_ID + " LONG,"
				+ TableMetaData.COUNT_OF_STOPS + " INTEGER"
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
		public static final String NAME_ROUTE_ID_X_FROM = "name_routeIdxFrom";
		public static final String NAME_ROUTE_ID_X_TO = "name_routeIdxTo";
		public static final String TYPE = "type";
		public static final String TYPE_ROUTE_ID_X_FROM = "type_routeIdxFrom";
		public static final String TYPE_ROUTE_ID_X_TO = "type_routeIdxTo";	
		public static final String REF = "journey_ref";	
		public static final String TRIP_ID = "trip_id";
		public static final String LEG_ID = "leg_id";
		public static final String COUNT_OF_STOPS = "count_of_stops";	
	}
}
