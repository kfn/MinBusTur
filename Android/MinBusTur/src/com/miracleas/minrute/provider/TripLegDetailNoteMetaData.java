package com.miracleas.minrute.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class TripLegDetailNoteMetaData
{
	public static final String PROVIDER_NAME = "MinBusTurProvider";
	public static final String PACKAGE_NAME = "com.miracleas.minrute.provider.";
	public static final String AUTHORITY = PACKAGE_NAME + PROVIDER_NAME;
	public static final String COLLECTION_TYPE = "journey_detail_notes";
	public static final String ITEM_TYPE = "journey_detail_note";
	public static final String TABLE_NAME = TripLegDetailNoteMetaData.COLLECTION_TYPE;

	private TripLegDetailNoteMetaData()
	{
	}

	public static String getTableSchema()
	{
		return TableMetaData._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
				TableMetaData.NOTE + " TEXT," 
				+ TableMetaData.JOURNEY_DETAIL_ID + " INTEGER," 
				+ TableMetaData.ROUTE_ID_X_FROM + " INTEGER,"
				+ TableMetaData.ROUTE_ID_X_TO + " INTEGER"
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

		public static final String NOTE = "text";
		public static final String JOURNEY_DETAIL_ID = "journey_detail_id";
		public static final String ROUTE_ID_X_FROM = "routeIdxFrom";
		public static final String ROUTE_ID_X_TO = "routeIdxTo";
	}
}
