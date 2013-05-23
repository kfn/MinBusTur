package com.miracleas.imagedownloader;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.miracleas.imagedownloader.LogProviderMetaData.TableMetaData;

public class LogProvider extends ContentProvider
{
	public static final String DATABASE_NAME = LogProviderMetaData.COLLECTION_TYPE + ".db";
	public static final int DATABASE_VERSION = 1;
	// Logging helper tag. No significance to providers.
	private static final String tag = LogProvider.class.getName();
	private static final String TABLE_NAME = LogProviderMetaData.ITEM_TYPE;

	// Provide a mechanism to identify all the incoming uri patterns.
	protected static UriMatcher sUriMatcher;
	protected static final int INCOMING_PROPERTY_COLLECTION_URI_INDICATOR = 1;
	protected static final int INCOMING_SINGLE_PROPERTY_URI_INDICATOR = 2;

	static
	{
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(LogProviderMetaData.AUTHORITY, LogProviderMetaData.COLLECTION_TYPE, INCOMING_PROPERTY_COLLECTION_URI_INDICATOR);
		sUriMatcher.addURI(LogProviderMetaData.AUTHORITY, LogProviderMetaData.COLLECTION_TYPE + "/#", INCOMING_SINGLE_PROPERTY_URI_INDICATOR);

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
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + getTableSchemaStart() + LogProviderMetaData.getTableSchema() + getTableSchemaEnd());
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
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
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
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		Cursor c = null;
		switch (sUriMatcher.match(uri)) {
		case INCOMING_PROPERTY_COLLECTION_URI_INDICATOR:
			qb.setTables(TABLE_NAME);
			// qb.setProjectionMap(mProjectionMap);
			break;
		case INCOMING_SINGLE_PROPERTY_URI_INDICATOR:
			qb.setTables(TABLE_NAME);
			// qb.setProjectionMap(mProjectionMap);
			qb.appendWhere(TableMetaData._ID + "=" + uri.getPathSegments().get(1));
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// Get the database and run the query

		c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

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
		case INCOMING_PROPERTY_COLLECTION_URI_INDICATOR:
			return TableMetaData.CONTENT_TYPE;

		case INCOMING_SINGLE_PROPERTY_URI_INDICATOR:
			return TableMetaData.CONTENT_ITEM_TYPE;

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

		if (sUriMatcher.match(uri) == INCOMING_PROPERTY_COLLECTION_URI_INDICATOR)
		{
			tbl = TABLE_NAME;
			contentUri = LogProviderMetaData.TableMetaData.CONTENT_URI;
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
		case INCOMING_PROPERTY_COLLECTION_URI_INDICATOR:
			count = db.delete(TABLE_NAME, where, whereArgs);
			break;

		case INCOMING_SINGLE_PROPERTY_URI_INDICATOR:
			String rowId = uri.getPathSegments().get(1);
			count = db.delete(TABLE_NAME, TableMetaData._ID + "=" + rowId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
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
		case INCOMING_PROPERTY_COLLECTION_URI_INDICATOR:
			count = db.update(TABLE_NAME, values, where, whereArgs);
			break;

		case INCOMING_SINGLE_PROPERTY_URI_INDICATOR:
			String rowId = uri.getPathSegments().get(1);
			count = db.update(TABLE_NAME, values, TableMetaData._ID + "=" + rowId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}
}
