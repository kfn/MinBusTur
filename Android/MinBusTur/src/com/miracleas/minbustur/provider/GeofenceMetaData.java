package com.miracleas.minbustur.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class GeofenceMetaData
{
	public static final String PROVIDER_NAME = "MinBusTurProvider";
	public static final String PACKAGE_NAME = "com.miracleas.minbustur.provider.";
	public static final String AUTHORITY = PACKAGE_NAME + PROVIDER_NAME;
	public static final String COLLECTION_TYPE = "geofences";
	public static final String ITEM_TYPE = "geofence";
	public static final String TABLE_NAME = GeofenceMetaData.COLLECTION_TYPE;

	private GeofenceMetaData()
	{
	}

	public static String getTableSchema()
	{
		return TableMetaData._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
				TableMetaData.geofence_id + " LONG," 
				+ TableMetaData.geofence_lat + " TEXT," 
				+ TableMetaData.geofence_lng + " TEXT," 
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

		public static final String geofence_id = "geofence_id";
		public static final String geofence_lat = "geofence_lat";
		public static final String geofence_lng = "geofence_lng";
		public static final String updated = "updated";

	}
}
