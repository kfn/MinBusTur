package com.miracleas.minrute.provider;

import android.net.Uri;
import android.provider.BaseColumns;
/**
 * Table with the results from StopsNearby service.
 * When we have selected a trip, we want the GPS choords on all dest locations.
 * We only have the names, so the service is necessary. This table makes
 * sure that we only use the webservice once for each stop. Next time we want the
 * GPS choords, we can use this table instead. This table should be synced to all 
 * users through google drive.
 * @author kfn
 *
 */
public class AddressGPSMetaData
{
	public static final String PROVIDER_NAME = "MinBusTurProvider";
	public static final String PACKAGE_NAME = "com.miracleas.minrute.provider.";
	public static final String AUTHORITY = PACKAGE_NAME + PROVIDER_NAME;
	public static final String COLLECTION_TYPE = "leg_gps_choords";
	public static final String ITEM_TYPE = "leg_gps_choord";
	public static final String TABLE_NAME = AddressGPSMetaData.COLLECTION_TYPE;

	private AddressGPSMetaData()
	{
	}

	public static String getTableSchema()
	{
		return 
				TableMetaData.ADDRESS + " TEXT PRIMARY KEY," 
				+ TableMetaData.LATITUDE_Y + " INTEGER," 
				+ TableMetaData.LONGITUDE_X + " INTEGER," 
				+ TableMetaData.type_int + " INTEGER," 
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
		
		public static final String ADDRESS = "stop_address";
		public static final String LATITUDE_Y = "stop_latitude_y";
		public static final String LONGITUDE_X = "stop_longitude_x";
		public static final String updated = "stop_updated";
		public static String type_int = "stop_type_int";

	}
}
