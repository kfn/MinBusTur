package com.miracleas.minrute.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class TripVoiceMetaData
{
	public static final String PROVIDER_NAME = "MinBusTurProvider";
	public static final String PACKAGE_NAME = "com.miracleas.minrute.provider.";
	public static final String AUTHORITY = PACKAGE_NAME + PROVIDER_NAME;
	public static final String COLLECTION_TYPE = "trip_voices";
	public static final String ITEM_TYPE = "trip_voice";
	public static final String TABLE_NAME = TripVoiceMetaData.COLLECTION_TYPE;

	private TripVoiceMetaData()
	{
	}

	public static String getTableSchema()
	{
		return TableMetaData._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
				TableMetaData.DEPARTURES_IN + " TEXT," 
				+ TableMetaData.TRIP_ID + " INTEGER," 
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

		public static final String DEPARTURES_IN = "voice_departures_in";
		public static final String TRIP_ID = "foreign_trip_id";
		public static final String updated = "updated";

	}
}
