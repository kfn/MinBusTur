package com.miracleas.minbustur.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class TripLegMetaData
{
	public static final String PROVIDER_NAME = "MinBusTurProvider";
	public static final String PACKAGE_NAME = "com.miracleas.minbustur.provider.";
	public static final String AUTHORITY = PACKAGE_NAME + PROVIDER_NAME;
	public static final String COLLECTION_TYPE = "legs";
	public static final String ITEM_TYPE = "leg";
	public static final String TABLE_NAME = TripLegMetaData.COLLECTION_TYPE;

	private TripLegMetaData()
	{
	}

	public static String getTableSchema()
	{
		return TableMetaData._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ TableMetaData.STEP_NUMBER + " INTEGER," 
				+ TableMetaData.TRIP_ID + " LONG," 
				+ TableMetaData.NAME + " TEXT," 
				+ TableMetaData.TYPE + " TEXT," 
				+ TableMetaData.NOTES + " TEXT," 
				+ TableMetaData.REF + " TEXT," 
				+ TableMetaData.ORIGIN_NAME + " TEXT," 
				+ TableMetaData.ORIGIN_DATE + " TEXT," 
				+ TableMetaData.ORIGIN_ROUTE_ID + " TEXT," 
				+ TableMetaData.ORIGIN_TIME + " TEXT," 
				+ TableMetaData.ORIGIN_TYPE + " TEXT," 
				+ TableMetaData.DEST_NAME + " TEXT,"
				+ TableMetaData.DEST_DATE + " TEXT,"
				+ TableMetaData.DEST_ROUTE_ID + " TEXT,"
				+ TableMetaData.DEST_TIME + " TEXT,"
				+ TableMetaData.DEST_TYPE + " TEXT,"
				+ TableMetaData.DURATION + " LONG,"
				+ TableMetaData.DURATION_FORMATTED + " TEXT,"	
				+ TableMetaData.PROGRESS_BAR_PROGRESS + " INTEGER,"	
				+ TableMetaData.PROGRESS_BAR_MAX + " INTEGER,"	
				+ TableMetaData.DEPARTURES_IN_TIME_LABEL + " TEXT,"	
				+ TableMetaData.COMPLETED + " TEXT,"	
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
		
		public static final String STEP_NUMBER = "step_number";
		public static final String TRIP_ID = "trip_id";
		public static final String NAME = "leg_name";
		public static final String TYPE = "leg_type";
		public static final String NOTES = "leg_notes";
		public static final String REF = "leg_journey_detail_ref";
		
		public static final String ORIGIN_NAME = "origin_name";
		public static final String ORIGIN_DATE = "origin_date";
		public static final String ORIGIN_ROUTE_ID = "origin_routeIdx";
		public static final String ORIGIN_TIME = "origin_time";
		public static final String ORIGIN_TYPE = "origin_type";
		public static final String DEST_NAME = "dest_name";
		public static final String DEST_DATE = "dest_date";
		public static final String DEST_ROUTE_ID = "dest_routeIdx";
		public static final String DEST_TIME = "dest_time";
		public static final String DEST_TYPE = "dest_type";
		public static final String DURATION = "duration";
		public static final String updated = "updated";
		public static final String DURATION_FORMATTED = "duaration_formatted";
		
		public static final String PROGRESS_BAR_PROGRESS = "progress_bar_progress";
		public static final String PROGRESS_BAR_MAX = "progress_bar_max";
		public static final String DEPARTURES_IN_TIME_LABEL = "progress_departures_in_label";
		public static final String COMPLETED = "completed";
		/*public static final String ORIGIN_LATITUDE = "origin_latitude";
		public static final String ORIGIN_LONGITUDE = "origin_longitude";
		public static final String DEST_LATITUDE = "dest_latitude";
		public static final String DEST_LONGITUDE = "dest_longitude";*/

	}
}
