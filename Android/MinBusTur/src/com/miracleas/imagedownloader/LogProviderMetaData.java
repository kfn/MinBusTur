package com.miracleas.imagedownloader;

import android.net.Uri;
import android.provider.BaseColumns;

public class LogProviderMetaData
{
	public static final String PROVIDER_NAME = "LogProvider";
	public static final String PACKAGE_NAME = "com.miracleas.imagedownloader.";
	public static final String AUTHORITY = PACKAGE_NAME + PROVIDER_NAME;
	public static final String COLLECTION_TYPE = "log_datas";
	public static final String ITEM_TYPE = "log_data";

	private LogProviderMetaData()
	{
	}

	public static String getTableSchema()
	{
		return TableMetaData._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + TableMetaData.DATE + " TEXT," + TableMetaData.DURATION + " LONG," + TableMetaData.DESCRIPTION + " TEXT";
	}

	// inner class describing columns and their types
	public static final class TableMetaData implements BaseColumns
	{

		// uri and mime type definitions
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + COLLECTION_TYPE);
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + PACKAGE_NAME + ITEM_TYPE;
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd." + PACKAGE_NAME + ITEM_TYPE;

		private TableMetaData()
		{
		}

		public static final String DATE = "_date";
		public static final String DURATION = "_duration";
		public static final String DESCRIPTION = "_description";
		public static final String DEFAULT_SORT_ORDER = TableMetaData.DATE + " ASC";

	}
}
