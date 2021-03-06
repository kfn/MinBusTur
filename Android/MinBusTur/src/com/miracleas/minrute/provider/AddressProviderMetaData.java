package com.miracleas.minrute.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class AddressProviderMetaData
{
	public static final String PROVIDER_NAME = "MinBusTurProvider";
	public static final String PACKAGE_NAME = "com.miracleas.minrute.provider.";
	public static final String AUTHORITY = PACKAGE_NAME + PROVIDER_NAME;
	public static final String COLLECTION_TYPE = "addresses";
	public static final String ITEM_TYPE = "address";
	public static final String TABLE_NAME = AddressProviderMetaData.COLLECTION_TYPE;

	private AddressProviderMetaData()
	{
	}

	public static String getTableSchema()
	{
		return TableMetaData._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
				TableMetaData.type + " TEXT," 
				+ TableMetaData.type_int + " INTEGER," 
				+ TableMetaData.address + " TEXT," 
				+ TableMetaData.id + " TEXT," 
				+ TableMetaData.Y + " TEXT," 
				+ TableMetaData.X + " TEXT,"
				+ TableMetaData.searchTerm + " TEXT,"
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

		public static final String type = "type";
		public static final String type_int = "type_int";
		public static final String address = "name";
		public static final String Y = "y";
		public static final String X = "x";
		public static final String searchTerm = "searchTerm";
		public static final String id = "id";
		public static final String updated = "updated";

	}
}
