package com.miracleas.minbustur.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class RouteMetaData
{
	public static final String PROVIDER_NAME = "MinBusTurProvider";
	public static final String PACKAGE_NAME = "com.miracleas.minbustur.provider.";
	public static final String AUTHORITY = PACKAGE_NAME + PROVIDER_NAME;
	public static final String COLLECTION_TYPE = "routes";
	public static final String ITEM_TYPE = "route";
	public static final String TABLE_NAME = RouteMetaData.COLLECTION_TYPE;

	private RouteMetaData()
	{
	}

	public static String getTableSchema()
	{
		return TableMetaData._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ TableMetaData.route_id + " TEXT," 
				+ TableMetaData.title + " TEXT," 
				+ TableMetaData.start_address + " TEXT," 
				+ TableMetaData.start_lat + " TEXT,"
				+ TableMetaData.start_lng + " TEXT,"
				+ TableMetaData.start_type + " TEXT,"
				+ TableMetaData.end_address + " TEXT," 
				+ TableMetaData.end_lat + " TEXT,"
				+ TableMetaData.end_lng + " TEXT,"
				+ TableMetaData.end_type + " TEXT,"
				+ TableMetaData.description + " TEXT,"
				+ TableMetaData.duration + " LONG,"
				+ TableMetaData.transport_title + " TEXT,"
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

		
		public static final String route_id = "route_foreign_id";
		public static final String title = "title";
		public static final String start_address = "start_address";		
		public static final String start_lat = "start_lat";
		public static final String start_lng = "start_lng";
		public static final String start_type = "start_type";
		public static final String end_address = "end_address";
		public static final String end_lat = "end_lat";
		public static final String end_lng = "end_lng";
		public static final String end_type = "end_type";
		public static final String duration = "duration";
		public static final String description = "description";
		public static final String transport_title = "transport_title";	
		public static final String updated = "updated";

	}
}
