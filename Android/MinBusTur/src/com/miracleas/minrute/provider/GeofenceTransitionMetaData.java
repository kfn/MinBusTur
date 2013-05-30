package com.miracleas.minrute.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class GeofenceTransitionMetaData
{
	public static final String PROVIDER_NAME = "MinBusTurProvider";
	public static final String PACKAGE_NAME = "com.miracleas.minrute.provider.";
	public static final String AUTHORITY = PACKAGE_NAME + PROVIDER_NAME;
	public static final String COLLECTION_TYPE = "geofence_transitions";
	public static final String ITEM_TYPE = "geofence_transition";
	public static final String TABLE_NAME = GeofenceTransitionMetaData.COLLECTION_TYPE;

	private GeofenceTransitionMetaData()
	{
	}

	public static String getTableSchema()
	{
		return TableMetaData._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
				TableMetaData.GEOFENCE_TRANSITION_TYPE + " INTEGER," 
				+ TableMetaData.GEOFENCE_ID + " TEXT," 
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

		public static final String GEOFENCE_TRANSITION_TYPE = "geofence_transition_type";
		public static final String GEOFENCE_ID = "trip_leg_id";
		public static final String updated = "updated";

	}
}
