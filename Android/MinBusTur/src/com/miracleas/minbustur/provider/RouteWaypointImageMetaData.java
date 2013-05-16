package com.miracleas.minbustur.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class RouteWaypointImageMetaData
{
	public static final String PROVIDER_NAME = "MinBusTurProvider";
	public static final String PACKAGE_NAME = "com.miracleas.minbustur.provider.";
	public static final String AUTHORITY = PACKAGE_NAME + PROVIDER_NAME;
	public static final String COLLECTION_TYPE = "waypoint_images";
	public static final String ITEM_TYPE = "waypoint_image";
	public static final String TABLE_NAME = RouteWaypointImageMetaData.COLLECTION_TYPE;

	private RouteWaypointImageMetaData()
	{
	}

	public static String getTableSchema()
	{
		return TableMetaData._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ TableMetaData.waypoint_url + " TEXT," 
				+ TableMetaData.waypoint_id + " TEXT," 
				+ TableMetaData.address + " TEXT," 
				+ TableMetaData.lat + " TEXT,"
				+ TableMetaData.lng + " TEXT,"
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

		public static final String waypoint_url = "waypoint_img_url";
		public static final String waypoint_id = "waypoint_foreign_img_id";
		public static final String address = "waypoint_img_address";		
		public static final String lat = "waypoint_img_lat";
		public static final String lng = "waypoint_img_lng";		
		public static final String updated = "waypoint_img_updated";

	}
}
