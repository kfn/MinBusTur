package com.miracleas.minbustur.provider;

import java.util.ArrayList;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class MinBusTurProvider extends ContentProvider
{
	public static final String DATABASE_NAME = AddressProviderMetaData.COLLECTION_TYPE;
	public static final int DATABASE_VERSION = 5;
	// Logging helper tag. No significance to providers.
	private static final String tag = MinBusTurProvider.class.getName();

	// Provide a mechanism to identify all the incoming uri patterns.
	private static final UriMatcher sUriMatcher;
	private static final int INCOMING_ADDRESS_COLLECTION_URI_INDICATOR = 1;
	private static final int INCOMING_SINGLE_ADDRESS_URI_INDICATOR = 2;
	private static final int INCOMING_ADDRESS_SEARCH_URI_INDICATOR = 3;
	private static final int INCOMING_TRIP_COLLECTION_URI_INDICATOR = 4;
	private static final int INCOMING_SINGLE_TRIP_URI_INDICATOR = 5;
	private static final int INCOMING_TRIPLEG_COLLECTION_URI_INDICATOR = 6;
	private static final int INCOMING_SINGLE_TRIPLEG_URI_INDICATOR = 7;
	private static final int INCOMING_WAYPOINT_IMG_COLLECTION_URI_INDICATOR = 8;
	private static final int INCOMING_SINGLE_WAYPOINT_IMG_URI_INDICATOR = 9;
	

	static
	{
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AddressProviderMetaData.AUTHORITY, AddressProviderMetaData.COLLECTION_TYPE, INCOMING_ADDRESS_COLLECTION_URI_INDICATOR);
		sUriMatcher.addURI(AddressProviderMetaData.AUTHORITY, AddressProviderMetaData.COLLECTION_TYPE + "/#", INCOMING_SINGLE_ADDRESS_URI_INDICATOR);
		sUriMatcher.addURI(AddressProviderMetaData.AUTHORITY, AddressProviderMetaData.COLLECTION_TYPE+ "/search", INCOMING_ADDRESS_SEARCH_URI_INDICATOR);
		sUriMatcher.addURI(TripMetaData.AUTHORITY, TripMetaData.COLLECTION_TYPE, INCOMING_TRIP_COLLECTION_URI_INDICATOR);
		sUriMatcher.addURI(TripMetaData.AUTHORITY, TripMetaData.COLLECTION_TYPE + "/#", INCOMING_SINGLE_TRIP_URI_INDICATOR);
		sUriMatcher.addURI(TripLegMetaData.AUTHORITY, TripLegMetaData.COLLECTION_TYPE, INCOMING_TRIPLEG_COLLECTION_URI_INDICATOR);
		sUriMatcher.addURI(TripLegMetaData.AUTHORITY, TripLegMetaData.COLLECTION_TYPE + "/#", INCOMING_SINGLE_TRIPLEG_URI_INDICATOR);
		sUriMatcher.addURI(TripLegImageMetaData.AUTHORITY, TripLegImageMetaData.COLLECTION_TYPE, INCOMING_WAYPOINT_IMG_COLLECTION_URI_INDICATOR);
		sUriMatcher.addURI(TripLegImageMetaData.AUTHORITY, TripLegImageMetaData.COLLECTION_TYPE + "/#", INCOMING_SINGLE_WAYPOINT_IMG_URI_INDICATOR);
		
	}

	/**
	 * This class helps open, create, and upgrade the database file.
	 */
	protected static class DatabaseHelper extends SQLiteOpenHelper
	{

		DatabaseHelper(Context context)
		{
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			Log.d(tag, "inner oncreate called");
			db.execSQL("CREATE TABLE IF NOT EXISTS " + AddressProviderMetaData.TABLE_NAME + getTableSchemaStart() + AddressProviderMetaData.getTableSchema() + getTableSchemaEnd());
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TripMetaData.TABLE_NAME + getTableSchemaStart() + TripMetaData.getTableSchema() + getTableSchemaEnd());
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TripLegMetaData.TABLE_NAME + getTableSchemaStart() + TripLegMetaData.getTableSchema() + getTableSchemaEnd());
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TripLegImageMetaData.TABLE_NAME + getTableSchemaStart() + TripLegImageMetaData.getTableSchema() + getTableSchemaEnd());
		}

		protected static String getTableSchemaStart()
		{
			return " (";
		}

		protected static String getTableSchemaEnd()
		{
			return ");";
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			Log.d(tag, "inner onupgrade called");
			Log.w(tag, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + AddressProviderMetaData.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + TripMetaData.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + TripLegMetaData.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + TripLegImageMetaData.TABLE_NAME);
			onCreate(db);
		}
	}

	protected DatabaseHelper mOpenHelper;

	@Override
	public boolean onCreate()
	{
		Log.d(tag, "main onCreate called");
		mOpenHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
	{
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		switch (sUriMatcher.match(uri)) {
		case INCOMING_ADDRESS_COLLECTION_URI_INDICATOR:
			qb.setTables(AddressProviderMetaData.TABLE_NAME);
			break;

		case INCOMING_SINGLE_ADDRESS_URI_INDICATOR:
			qb.setTables(AddressProviderMetaData.TABLE_NAME);
			qb.appendWhere(AddressProviderMetaData.TableMetaData._ID + "=" + uri.getPathSegments().get(1));
			break;
			
		case INCOMING_ADDRESS_SEARCH_URI_INDICATOR:
			String query = selectionArgs[0];
			StringBuilder b = new StringBuilder();
			b.append("SELECT * FROM ").append(AddressProviderMetaData.TABLE_NAME).append(" WHERE ")
			.append(AddressProviderMetaData.TableMetaData.address).append(" LIKE '").append(query)
			.append("%'").append(" GROUP BY ").append(AddressProviderMetaData.TableMetaData.address)
			.append(" ORDER BY ").append(AddressProviderMetaData.TableMetaData.address);
			
			SQLiteDatabase db = mOpenHelper.getReadableDatabase();
			Cursor c = db.rawQuery(b.toString(), null);
			c.setNotificationUri(getContext().getContentResolver(), uri);
			return c;
		case INCOMING_TRIP_COLLECTION_URI_INDICATOR:
			qb.setTables(TripMetaData.TABLE_NAME);
			break;

		case INCOMING_SINGLE_TRIP_URI_INDICATOR:
			qb.setTables(TripMetaData.TABLE_NAME);
			qb.appendWhere(TripMetaData.TableMetaData._ID + "=" + uri.getPathSegments().get(1));
			break;
		case INCOMING_TRIPLEG_COLLECTION_URI_INDICATOR:
			qb.setTables(TripLegMetaData.TABLE_NAME);
			break;

		case INCOMING_SINGLE_TRIPLEG_URI_INDICATOR:
			qb.setTables(TripLegMetaData.TABLE_NAME);
			qb.appendWhere(TripLegMetaData.TableMetaData._ID + "=" + uri.getPathSegments().get(1));
			break;
			
		case INCOMING_WAYPOINT_IMG_COLLECTION_URI_INDICATOR:
			qb.setTables(TripLegImageMetaData.TABLE_NAME);
			break;

		case INCOMING_SINGLE_WAYPOINT_IMG_URI_INDICATOR:
			qb.setTables(TripLegImageMetaData.TABLE_NAME);
			qb.appendWhere(TripLegImageMetaData.TableMetaData._ID + "=" + uri.getPathSegments().get(1));
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// Get the database and run the query
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

		// example of getting a count
		// int i = c.getCount();

		// Tell the cursor what uri to watch,
		// so it knows when its source data changes
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public String getType(Uri uri)
	{
		switch (sUriMatcher.match(uri)) {
		case INCOMING_ADDRESS_COLLECTION_URI_INDICATOR:
			return AddressProviderMetaData.TableMetaData.CONTENT_TYPE;

		case INCOMING_SINGLE_ADDRESS_URI_INDICATOR:
			return AddressProviderMetaData.TableMetaData.CONTENT_ITEM_TYPE;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues)
	{
		// Validate the requested uri
		String tbl = null;
		Uri contentUri = null;

		if (sUriMatcher.match(uri) == INCOMING_ADDRESS_COLLECTION_URI_INDICATOR)
		{
			tbl = AddressProviderMetaData.TABLE_NAME;
			contentUri = AddressProviderMetaData.TableMetaData.CONTENT_URI;
		}
		else if (sUriMatcher.match(uri) == INCOMING_TRIP_COLLECTION_URI_INDICATOR)
		{
			tbl = TripMetaData.TABLE_NAME;
			contentUri = TripMetaData.TableMetaData.CONTENT_URI;
		}
		else if (sUriMatcher.match(uri) == INCOMING_TRIPLEG_COLLECTION_URI_INDICATOR)
		{
			tbl = TripLegMetaData.TABLE_NAME;
			contentUri = TripLegMetaData.TableMetaData.CONTENT_URI;
		}
		else if (sUriMatcher.match(uri) == INCOMING_WAYPOINT_IMG_COLLECTION_URI_INDICATOR)
		{
			tbl = TripLegImageMetaData.TABLE_NAME;
			contentUri = TripLegImageMetaData.TableMetaData.CONTENT_URI;
		}
		else
		{
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		ContentValues values;
		if (initialValues != null)
		{
			values = new ContentValues(initialValues);
		}
		else
		{
			values = new ContentValues();
		}

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		long rowId = db.insert(tbl, null, values);
		if (rowId > 0)
		{
			Uri insertedItem = ContentUris.withAppendedId(contentUri, rowId);
			getContext().getContentResolver().notifyChange(insertedItem, null);

			return insertedItem;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs)
	{
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case INCOMING_ADDRESS_COLLECTION_URI_INDICATOR:
			count = db.delete(AddressProviderMetaData.TABLE_NAME, where, whereArgs);
			break;

		case INCOMING_SINGLE_ADDRESS_URI_INDICATOR:
			String rowId = uri.getPathSegments().get(1);
			count = db.delete(AddressProviderMetaData.TABLE_NAME, AddressProviderMetaData.TableMetaData._ID + "=" + rowId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;
		case INCOMING_TRIP_COLLECTION_URI_INDICATOR:
			count = db.delete(TripMetaData.TABLE_NAME, where, whereArgs);
			break;

		case INCOMING_SINGLE_TRIP_URI_INDICATOR:
			String rowId1 = uri.getPathSegments().get(1);
			count = db.delete(TripMetaData.TABLE_NAME, TripMetaData.TableMetaData._ID + "=" + rowId1 + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;
		case INCOMING_TRIPLEG_COLLECTION_URI_INDICATOR:
			count = db.delete(TripLegMetaData.TABLE_NAME, where, whereArgs);
			break;

		case INCOMING_SINGLE_TRIPLEG_URI_INDICATOR:
			String rowId2 = uri.getPathSegments().get(1);
			count = db.delete(TripLegMetaData.TABLE_NAME, TripLegMetaData.TableMetaData._ID + "=" + rowId2 + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;
		case INCOMING_WAYPOINT_IMG_COLLECTION_URI_INDICATOR:
			count = db.delete(TripLegImageMetaData.TABLE_NAME, where, whereArgs);
			break;

		case INCOMING_SINGLE_WAYPOINT_IMG_URI_INDICATOR:
			String rowId3 = uri.getPathSegments().get(1);
			count = db.delete(TripLegImageMetaData.TABLE_NAME, TripLegImageMetaData.TableMetaData._ID + "=" + rowId3 + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs)
	{
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case INCOMING_ADDRESS_COLLECTION_URI_INDICATOR:
			count = db.update(AddressProviderMetaData.TABLE_NAME, values, where, whereArgs);
			break;

		case INCOMING_SINGLE_ADDRESS_URI_INDICATOR:
			String rowId = uri.getPathSegments().get(1);
			count = db.update(AddressProviderMetaData.TABLE_NAME, values, AddressProviderMetaData.TableMetaData._ID + "=" + rowId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;
		case INCOMING_TRIP_COLLECTION_URI_INDICATOR:
			count = db.update(TripMetaData.TABLE_NAME, values, where, whereArgs);
			break;

		case INCOMING_SINGLE_TRIP_URI_INDICATOR:
			String rowId1 = uri.getPathSegments().get(1);
			count = db.update(TripMetaData.TABLE_NAME, values, TripMetaData.TableMetaData._ID + "=" + rowId1 + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;
		case INCOMING_TRIPLEG_COLLECTION_URI_INDICATOR:
			count = db.update(TripLegMetaData.TABLE_NAME, values, where, whereArgs);
			break;

		case INCOMING_SINGLE_TRIPLEG_URI_INDICATOR:
			String rowId2 = uri.getPathSegments().get(1);
			count = db.update(TripLegMetaData.TABLE_NAME, values, TripLegMetaData.TableMetaData._ID + "=" + rowId2 + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;

		case INCOMING_WAYPOINT_IMG_COLLECTION_URI_INDICATOR:
			count = db.update(TripLegImageMetaData.TABLE_NAME, values, where, whereArgs);
			break;

		case INCOMING_SINGLE_WAYPOINT_IMG_URI_INDICATOR:
			String rowId3 = uri.getPathSegments().get(1);
			count = db.update(TripLegImageMetaData.TABLE_NAME, values, TripLegImageMetaData.TableMetaData._ID + "=" + rowId3 + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}
	
	@Override
	public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException
	{

		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		db.beginTransaction();
		try
		{
			final int numOperations = operations.size();
			final ContentProviderResult[] results = new ContentProviderResult[numOperations];
			for (int i = 0; i < numOperations; i++)
			{
				results[i] = operations.get(i).apply(this, results, i);
			}
			db.setTransactionSuccessful();

			return results;
		} finally
		{
			db.endTransaction();
		}
	}
}
