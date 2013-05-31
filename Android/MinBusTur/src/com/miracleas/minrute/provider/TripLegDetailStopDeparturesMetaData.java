package com.miracleas.minrute.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class TripLegDetailStopDeparturesMetaData
{
	public static final String PROVIDER_NAME = "MinBusTurProvider";
	public static final String PACKAGE_NAME = "com.miracleas.minrute.provider.";
	public static final String AUTHORITY = PACKAGE_NAME + PROVIDER_NAME;
	public static final String COLLECTION_TYPE = "trip_stop_departures";
	public static final String ITEM_TYPE = "trip_stop_departure";
	public static final String TABLE_NAME = TripLegDetailStopDeparturesMetaData.COLLECTION_TYPE;

	private TripLegDetailStopDeparturesMetaData()
	{
	}

	public static String getTableSchema()
	{
		return TableMetaData._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ TableMetaData.STOP_ID + " INTEGER," 
				+ TableMetaData.STOP_SEARCH_ID + " TEXT," 
				+ TableMetaData.NAME + " TEXT," 
				+ TableMetaData.TYPE + " TEXT," 
				+ TableMetaData.STOP + " TEXT,"
				+ TableMetaData.TIME + " TEXT,"
				+ TableMetaData.DATE + " TEXT,"
				+ TableMetaData.DIRECTION + " TEXT,"
				+ TableMetaData.REF + " TEXT,"
				+ TableMetaData.UPDATED + " LONG"
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

		public static final String STOP_ID = "trip_stop_id";	
		public static final String NAME = "name";
		public static final String TYPE = "type";
		public static final String STOP = "stop";
		public static final String TIME = "stop_time";
		public static final String DATE = "stop_date";
		public static final String DIRECTION = "direction";
		public static final String REF = "ref";
		public static final String UPDATED = "updated";
		public static final String STOP_SEARCH_ID = "stop_search_id";
	}
}
