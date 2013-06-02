package com.miracleas.minrute.provider;

import java.util.ArrayList;

import android.annotation.SuppressLint;
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
	public static final String DATABASE_NAME = "minrejseplan";
	public static final int DATABASE_VERSION = 8;
	// Logging helper tag. No significance to providers.
	private static final String tag = MinBusTurProvider.class.getName();

	// Provide a mechanism to identify all the incoming uri patterns.
	private static final UriMatcher sUriMatcher;
	private static final int INCOMING_ADDRESS_COLLECTION_URI_INDICATOR = 1;
	private static final int INCOMING_SINGLE_ADDRESS_URI_INDICATOR = 2;
	private static final int INCOMING_TRIP_COLLECTION_URI_INDICATOR = 4;
	private static final int INCOMING_SINGLE_TRIP_URI_INDICATOR = 5;
	private static final int INCOMING_TRIPLEG_COLLECTION_URI_INDICATOR = 6;
	private static final int INCOMING_SINGLE_TRIPLEG_URI_INDICATOR = 7;
	private static final int INCOMING_JOURNEY_DETAIL_COLLECTION_URI_INDICATOR = 10;
	private static final int INCOMING_SINGLE_JOURNEY_DETAIL_URI_INDICATOR = 11;
	private static final int INCOMING_JOURNEY_DETAIL_STOP_COLLECTION_URI_INDICATOR = 12;
	private static final int INCOMING_SINGLE_JOURNEY_DETAIL_STOP_URI_INDICATOR = 13;
	private static final int INCOMING_JOURNEY_DETAIL_NOTE_COLLECTION_URI_INDICATOR = 14;

	private static final int INCOMING_STOP_IMAGE_COLLECTION_URI_INDICATOR = 17;
	private static final int INCOMING_SINGLE_STOP_IMAGE_URI_INDICATOR = 19;
	private static final int INCOMING_STOP_DEPARTURES_COLLECTION_URI_INDICATOR = 18;
	
	private static final int INCOMING_TRIP_VOICE_COLLECTION_URI_INDICATOR = 20;
	private static final int INCOMING_SINGLE_TRIP_VOICE_URI_INDICATOR = 21;
	private static final int INCOMING_ADDRESS_GPS_COLLECTION_URI_INDICATOR = 22;
	
	private static final int INCOMING_TRIP_LEG_ADDRESS_GPS_JOIN_URI_INDICATOR = 23;
	private static final int INCOMING_TRIPLEG_IMAGES_COLLECTION_URI_INDICATOR = 24;
	
	private static final int INCOMING_GEOFENCE_COLLECTION_URI_INDICATOR = 25;
	private static final int INCOMING_SINGLE_GEOFENCE_URI_INDICATOR = 26;
	

	static
	{
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AddressProviderMetaData.AUTHORITY, AddressProviderMetaData.COLLECTION_TYPE, INCOMING_ADDRESS_COLLECTION_URI_INDICATOR);
		sUriMatcher.addURI(AddressProviderMetaData.AUTHORITY, AddressProviderMetaData.COLLECTION_TYPE + "/#", INCOMING_SINGLE_ADDRESS_URI_INDICATOR);
		sUriMatcher.addURI(TripMetaData.AUTHORITY, TripMetaData.COLLECTION_TYPE, INCOMING_TRIP_COLLECTION_URI_INDICATOR);
		sUriMatcher.addURI(TripMetaData.AUTHORITY, TripMetaData.COLLECTION_TYPE + "/#", INCOMING_SINGLE_TRIP_URI_INDICATOR);
		sUriMatcher.addURI(TripLegMetaData.AUTHORITY, TripLegMetaData.COLLECTION_TYPE, INCOMING_TRIPLEG_COLLECTION_URI_INDICATOR);
		sUriMatcher.addURI(TripLegMetaData.AUTHORITY, TripLegMetaData.COLLECTION_TYPE + "/#", INCOMING_SINGLE_TRIPLEG_URI_INDICATOR);
		sUriMatcher.addURI(TripLegDetailMetaData.AUTHORITY, TripLegDetailMetaData.COLLECTION_TYPE, INCOMING_JOURNEY_DETAIL_COLLECTION_URI_INDICATOR);
		sUriMatcher.addURI(TripLegDetailMetaData.AUTHORITY, TripLegDetailMetaData.COLLECTION_TYPE + "/#", INCOMING_SINGLE_JOURNEY_DETAIL_URI_INDICATOR);
		sUriMatcher.addURI(TripLegDetailStopMetaData.AUTHORITY, TripLegDetailStopMetaData.COLLECTION_TYPE, INCOMING_JOURNEY_DETAIL_STOP_COLLECTION_URI_INDICATOR);
		sUriMatcher.addURI(TripLegDetailStopMetaData.AUTHORITY, TripLegDetailStopMetaData.COLLECTION_TYPE + "/#", INCOMING_SINGLE_JOURNEY_DETAIL_STOP_URI_INDICATOR);
		sUriMatcher.addURI(TripLegDetailNoteMetaData.AUTHORITY, TripLegDetailNoteMetaData.COLLECTION_TYPE, INCOMING_JOURNEY_DETAIL_NOTE_COLLECTION_URI_INDICATOR);
		
		sUriMatcher.addURI(StopImagesMetaData.AUTHORITY, StopImagesMetaData.COLLECTION_TYPE, INCOMING_STOP_IMAGE_COLLECTION_URI_INDICATOR);
		sUriMatcher.addURI(StopImagesMetaData.AUTHORITY, StopImagesMetaData.COLLECTION_TYPE+ "/#", INCOMING_SINGLE_STOP_IMAGE_URI_INDICATOR);
		sUriMatcher.addURI(TripLegDetailStopDeparturesMetaData.AUTHORITY, TripLegDetailStopDeparturesMetaData.COLLECTION_TYPE, INCOMING_STOP_DEPARTURES_COLLECTION_URI_INDICATOR);
		sUriMatcher.addURI(GeofenceTransitionMetaData.AUTHORITY, GeofenceTransitionMetaData.COLLECTION_TYPE, INCOMING_TRIP_VOICE_COLLECTION_URI_INDICATOR);
		sUriMatcher.addURI(GeofenceTransitionMetaData.AUTHORITY, GeofenceTransitionMetaData.COLLECTION_TYPE + "/#", INCOMING_SINGLE_TRIP_VOICE_URI_INDICATOR);
		sUriMatcher.addURI(AddressGPSMetaData.AUTHORITY, AddressGPSMetaData.COLLECTION_TYPE, INCOMING_ADDRESS_GPS_COLLECTION_URI_INDICATOR);
		sUriMatcher.addURI(TripLegMetaData.AUTHORITY, TripLegMetaData.COLLECTION_TYPE + "/"+AddressGPSMetaData.TABLE_NAME, INCOMING_TRIP_LEG_ADDRESS_GPS_JOIN_URI_INDICATOR);
		
		sUriMatcher.addURI(TripLegMetaData.AUTHORITY, TripLegMetaData.COLLECTION_TYPE + "/"+StopImagesMetaData.TABLE_NAME, INCOMING_TRIPLEG_IMAGES_COLLECTION_URI_INDICATOR);
		
		sUriMatcher.addURI(GeofenceMetaData.AUTHORITY, GeofenceMetaData.COLLECTION_TYPE, INCOMING_GEOFENCE_COLLECTION_URI_INDICATOR);
		sUriMatcher.addURI(GeofenceMetaData.AUTHORITY, GeofenceMetaData.COLLECTION_TYPE + "/#", INCOMING_SINGLE_GEOFENCE_URI_INDICATOR);
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
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TripLegDetailMetaData.TABLE_NAME + getTableSchemaStart() + TripLegDetailMetaData.getTableSchema() + getTableSchemaEnd());
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TripLegDetailStopMetaData.TABLE_NAME + getTableSchemaStart() + TripLegDetailStopMetaData.getTableSchema() + getTableSchemaEnd());
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TripLegDetailNoteMetaData.TABLE_NAME + getTableSchemaStart() + TripLegDetailNoteMetaData.getTableSchema() + getTableSchemaEnd());
			
			db.execSQL("CREATE TABLE IF NOT EXISTS " + StopImagesMetaData.TABLE_NAME + getTableSchemaStart() + StopImagesMetaData.getTableSchema() + getTableSchemaEnd());
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TripLegDetailStopDeparturesMetaData.TABLE_NAME + getTableSchemaStart() + TripLegDetailStopDeparturesMetaData.getTableSchema() + getTableSchemaEnd());
			db.execSQL("CREATE TABLE IF NOT EXISTS " + GeofenceTransitionMetaData.TABLE_NAME + getTableSchemaStart() + GeofenceTransitionMetaData.getTableSchema() + getTableSchemaEnd());
			db.execSQL("CREATE TABLE IF NOT EXISTS " + AddressGPSMetaData.TABLE_NAME + getTableSchemaStart() + AddressGPSMetaData.getTableSchema() + getTableSchemaEnd());
			
			db.execSQL("CREATE TABLE IF NOT EXISTS " + GeofenceMetaData.TABLE_NAME + getTableSchemaStart() + GeofenceMetaData.getTableSchema() + getTableSchemaEnd());

			
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
			db.execSQL("DROP TABLE IF EXISTS " + TripLegDetailMetaData.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + TripLegDetailStopMetaData.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + TripLegDetailNoteMetaData.TABLE_NAME);
			
			db.execSQL("DROP TABLE IF EXISTS " + StopImagesMetaData.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + TripLegDetailStopDeparturesMetaData.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + GeofenceTransitionMetaData.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + AddressGPSMetaData.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + GeofenceMetaData.TABLE_NAME);
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

	@SuppressLint("NewApi")
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
	{
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		String groupBy = null;
		switch (sUriMatcher.match(uri)) {
		case INCOMING_ADDRESS_COLLECTION_URI_INDICATOR:
			qb.setTables(AddressProviderMetaData.TABLE_NAME);
			//String query = selectionArgs[0];
			//selectionArgs = null;
			/*Log.d(tag, "query: "+query);
			StringBuilder b = new StringBuilder();
			b.append(AddressProviderMetaData.TableMetaData.address).append(" LIKE '").append(query).append("%'");
			qb.setTables(AddressProviderMetaData.TABLE_NAME);
			selection = b.toString();
			groupBy = AddressProviderMetaData.TableMetaData.address;*/
			break;

		case INCOMING_SINGLE_ADDRESS_URI_INDICATOR:
			qb.setTables(AddressProviderMetaData.TABLE_NAME);
			qb.appendWhere(AddressProviderMetaData.TableMetaData._ID + "=" + uri.getPathSegments().get(1));
			break;
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
		case INCOMING_TRIPLEG_IMAGES_COLLECTION_URI_INDICATOR:
			StringBuilder b1 = new StringBuilder();
			b1.append(TripLegMetaData.TABLE_NAME).append(" as t LEFT JOIN ").append(StopImagesMetaData.TABLE_NAME)
			.append(" as i ON (i.").append(StopImagesMetaData.TableMetaData._ID)
			.append("= (SELECT MAX(z.").append(StopImagesMetaData.TableMetaData._ID)
			.append(") FROM ").append(StopImagesMetaData.TABLE_NAME).append(" as z WHERE z.").append(StopImagesMetaData.TableMetaData.STOP_NAME)
			.append("=t.").append(TripLegMetaData.TableMetaData.ORIGIN_NAME).append(" AND ").append(StopImagesMetaData.TableMetaData.URL).append(" NOT NULL")
			.append(" AND (").append(StopImagesMetaData.TableMetaData.TRANSPORT_DIRECTION).append("=").append(TripLegMetaData.TableMetaData.NOTES)
			.append(" OR ").append(TripLegMetaData.TableMetaData.ORIGIN_TYPE).append("='ADR' )")
			.append("))");
			qb.setTables(b1.toString());
			break; 	

		case INCOMING_SINGLE_TRIPLEG_URI_INDICATOR:
			qb.setTables(TripLegMetaData.TABLE_NAME);
			qb.appendWhere(TripLegMetaData.TableMetaData._ID + "=" + uri.getPathSegments().get(1));
			break;
		case INCOMING_JOURNEY_DETAIL_COLLECTION_URI_INDICATOR:
			qb.setTables(TripLegDetailMetaData.TABLE_NAME);
			break;

		case INCOMING_SINGLE_JOURNEY_DETAIL_URI_INDICATOR:
			qb.setTables(TripLegDetailMetaData.TABLE_NAME);
			qb.appendWhere(TripLegDetailMetaData.TableMetaData._ID + "=" + uri.getPathSegments().get(1));
			break;
		case INCOMING_JOURNEY_DETAIL_STOP_COLLECTION_URI_INDICATOR:
			qb.setTables(TripLegDetailStopMetaData.TABLE_NAME);
			break;

		case INCOMING_SINGLE_JOURNEY_DETAIL_STOP_URI_INDICATOR:
			qb.setTables(TripLegDetailStopMetaData.TABLE_NAME);
			qb.appendWhere(TripLegDetailStopMetaData.TableMetaData._ID + "=" + uri.getPathSegments().get(1));
			break;
		case INCOMING_JOURNEY_DETAIL_NOTE_COLLECTION_URI_INDICATOR:
			qb.setTables(TripLegDetailNoteMetaData.TABLE_NAME);
			break;

		case INCOMING_STOP_IMAGE_COLLECTION_URI_INDICATOR:
			qb.setTables(StopImagesMetaData.TABLE_NAME);
			break;
		case INCOMING_SINGLE_STOP_IMAGE_URI_INDICATOR:
			qb.setTables(StopImagesMetaData.TABLE_NAME);
			qb.appendWhere(StopImagesMetaData.TableMetaData._ID + "=" + uri.getPathSegments().get(1));
			break;	
			
		case INCOMING_STOP_DEPARTURES_COLLECTION_URI_INDICATOR:
			qb.setTables(TripLegDetailStopDeparturesMetaData.TABLE_NAME);
			break;
		case INCOMING_TRIP_VOICE_COLLECTION_URI_INDICATOR:
			qb.setTables(GeofenceTransitionMetaData.TABLE_NAME);
			break;
		case INCOMING_SINGLE_TRIP_VOICE_URI_INDICATOR:
			qb.setTables(GeofenceTransitionMetaData.TABLE_NAME);
			qb.appendWhere(GeofenceTransitionMetaData.TableMetaData._ID + "=" + uri.getPathSegments().get(1));
			break;
		case INCOMING_ADDRESS_GPS_COLLECTION_URI_INDICATOR:
			qb.setTables(AddressGPSMetaData.TABLE_NAME);
			break;
		case INCOMING_TRIP_LEG_ADDRESS_GPS_JOIN_URI_INDICATOR:
			StringBuilder b = new StringBuilder();
			b.append(TripLegMetaData.TABLE_NAME).append(" INNER JOIN ").append(AddressGPSMetaData.TABLE_NAME)
			.append(" ON (").append(TripLegMetaData.TableMetaData.ORIGIN_NAME).append("=")
			.append(AddressGPSMetaData.TableMetaData.ADDRESS).append(")");			
			qb.setTables(b.toString());
			break;
		case INCOMING_GEOFENCE_COLLECTION_URI_INDICATOR:
			qb.setTables(GeofenceMetaData.TABLE_NAME);
			break;

			case INCOMING_SINGLE_GEOFENCE_URI_INDICATOR:
			qb.setTables(GeofenceMetaData.TABLE_NAME);
			qb.appendWhere(GeofenceMetaData.TableMetaData._ID + "=" + uri.getPathSegments().get(1));
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// Get the database and run the query
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy, null, sortOrder);

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
		else if (sUriMatcher.match(uri) == INCOMING_JOURNEY_DETAIL_COLLECTION_URI_INDICATOR)
		{
			tbl = TripLegDetailMetaData.TABLE_NAME;
			contentUri = TripLegDetailMetaData.TableMetaData.CONTENT_URI;
		}
		else if (sUriMatcher.match(uri) == INCOMING_JOURNEY_DETAIL_STOP_COLLECTION_URI_INDICATOR)
		{
			tbl = TripLegDetailStopMetaData.TABLE_NAME;
			contentUri = TripLegDetailStopMetaData.TableMetaData.CONTENT_URI;
		}
		else if (sUriMatcher.match(uri) == INCOMING_JOURNEY_DETAIL_NOTE_COLLECTION_URI_INDICATOR)
		{
			tbl = TripLegDetailNoteMetaData.TABLE_NAME;
			contentUri = TripLegDetailNoteMetaData.TableMetaData.CONTENT_URI;
		}
		else if (sUriMatcher.match(uri) == INCOMING_STOP_IMAGE_COLLECTION_URI_INDICATOR)
		{
			tbl = StopImagesMetaData.TABLE_NAME;
			contentUri = StopImagesMetaData.TableMetaData.CONTENT_URI;
		}
		else if (sUriMatcher.match(uri) == INCOMING_STOP_DEPARTURES_COLLECTION_URI_INDICATOR)
		{
			tbl = TripLegDetailStopDeparturesMetaData.TABLE_NAME;
			contentUri = TripLegDetailStopDeparturesMetaData.TableMetaData.CONTENT_URI;
		}
		else if (sUriMatcher.match(uri) == INCOMING_TRIP_VOICE_COLLECTION_URI_INDICATOR)
		{
			tbl = GeofenceTransitionMetaData.TABLE_NAME;
			contentUri = GeofenceTransitionMetaData.TableMetaData.CONTENT_URI;
		}
		else if (sUriMatcher.match(uri) == INCOMING_ADDRESS_GPS_COLLECTION_URI_INDICATOR)
		{
			tbl = AddressGPSMetaData.TABLE_NAME;
			contentUri = AddressGPSMetaData.TableMetaData.CONTENT_URI;
		}
		else if (sUriMatcher.match(uri) == INCOMING_GEOFENCE_COLLECTION_URI_INDICATOR)
		{
			tbl = GeofenceMetaData.TABLE_NAME;
			contentUri = GeofenceMetaData.TableMetaData.CONTENT_URI;
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
		case INCOMING_JOURNEY_DETAIL_COLLECTION_URI_INDICATOR:
			count = db.delete(TripLegDetailMetaData.TABLE_NAME, where, whereArgs);
			break;

		case INCOMING_SINGLE_JOURNEY_DETAIL_URI_INDICATOR:
			String rowId4 = uri.getPathSegments().get(1);
			count = db.delete(TripLegDetailMetaData.TABLE_NAME, TripLegDetailMetaData.TableMetaData._ID + "=" + rowId4 + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;
		case INCOMING_JOURNEY_DETAIL_STOP_COLLECTION_URI_INDICATOR:
			count = db.delete(TripLegDetailStopMetaData.TABLE_NAME, where, whereArgs);
			break;

		case INCOMING_SINGLE_JOURNEY_DETAIL_STOP_URI_INDICATOR:
			String rowId5 = uri.getPathSegments().get(1);
			count = db.delete(TripLegDetailStopMetaData.TABLE_NAME, TripLegDetailStopMetaData.TableMetaData._ID + "=" + rowId5 + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;
		case INCOMING_JOURNEY_DETAIL_NOTE_COLLECTION_URI_INDICATOR:
			count = db.delete(TripLegDetailNoteMetaData.TABLE_NAME, where, whereArgs);
			break;
		case INCOMING_STOP_IMAGE_COLLECTION_URI_INDICATOR:
			count = db.delete(StopImagesMetaData.TABLE_NAME, where, whereArgs);
			break;
		case INCOMING_SINGLE_STOP_IMAGE_URI_INDICATOR:
			String rowId7 = uri.getPathSegments().get(1);
			count = db.delete(StopImagesMetaData.TABLE_NAME, StopImagesMetaData.TableMetaData._ID + "=" + rowId7 + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;
			
		case INCOMING_STOP_DEPARTURES_COLLECTION_URI_INDICATOR:
			count = db.delete(TripLegDetailStopDeparturesMetaData.TABLE_NAME, where, whereArgs);
			break;
		case INCOMING_TRIP_VOICE_COLLECTION_URI_INDICATOR:
			count = db.delete(GeofenceTransitionMetaData.TABLE_NAME, where, whereArgs);
			break;
		case INCOMING_ADDRESS_GPS_COLLECTION_URI_INDICATOR:
			count = db.delete(AddressGPSMetaData.TABLE_NAME, where, whereArgs);
			break;
		case INCOMING_GEOFENCE_COLLECTION_URI_INDICATOR:
			count = db.delete(GeofenceMetaData.TABLE_NAME, where, whereArgs);
			break;

			case INCOMING_SINGLE_GEOFENCE_URI_INDICATOR:
			String rowId6 = uri.getPathSegments().get(1);
			count = db.delete(GeofenceMetaData.TABLE_NAME, GeofenceMetaData.TableMetaData._ID + "=" + rowId6 + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
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
		case INCOMING_JOURNEY_DETAIL_COLLECTION_URI_INDICATOR:
			count = db.update(TripLegDetailMetaData.TABLE_NAME, values, where, whereArgs);
			break;

		case INCOMING_SINGLE_JOURNEY_DETAIL_URI_INDICATOR:
			String rowId4 = uri.getPathSegments().get(1);
			count = db.update(TripLegDetailMetaData.TABLE_NAME, values, TripLegDetailMetaData.TableMetaData._ID + "=" + rowId4 + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;
		case INCOMING_JOURNEY_DETAIL_STOP_COLLECTION_URI_INDICATOR:
			count = db.update(TripLegDetailStopMetaData.TABLE_NAME, values, where, whereArgs);
			break;

		case INCOMING_SINGLE_JOURNEY_DETAIL_STOP_URI_INDICATOR:
			String rowId5 = uri.getPathSegments().get(1);
			count = db.update(TripLegDetailStopMetaData.TABLE_NAME, values, TripLegDetailStopMetaData.TableMetaData._ID + "=" + rowId5 + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;
		case INCOMING_STOP_IMAGE_COLLECTION_URI_INDICATOR:
			count = db.update(StopImagesMetaData.TABLE_NAME, values, where, whereArgs);
			break;
		case INCOMING_SINGLE_STOP_IMAGE_URI_INDICATOR:
			String rowId7 = uri.getPathSegments().get(1);
			count = db.update(StopImagesMetaData.TABLE_NAME, values, StopImagesMetaData.TableMetaData._ID + "=" + rowId7 + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;
		case INCOMING_TRIP_VOICE_COLLECTION_URI_INDICATOR:
			count = db.update(GeofenceTransitionMetaData.TABLE_NAME, values, where, whereArgs);
			break;
		case INCOMING_SINGLE_TRIP_VOICE_URI_INDICATOR:
			String rowId8 = uri.getPathSegments().get(1);
			count = db.update(GeofenceTransitionMetaData.TABLE_NAME, values, GeofenceTransitionMetaData.TableMetaData._ID + "=" + rowId8 + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;
		case INCOMING_ADDRESS_GPS_COLLECTION_URI_INDICATOR:
			count = db.update(AddressGPSMetaData.TABLE_NAME, values, where, whereArgs);
			break;
		case INCOMING_GEOFENCE_COLLECTION_URI_INDICATOR:
			count = db.update(GeofenceMetaData.TABLE_NAME, values, where, whereArgs);
			break;

			case INCOMING_SINGLE_GEOFENCE_URI_INDICATOR:
			String rowId6 = uri.getPathSegments().get(1);
			count = db.update(GeofenceMetaData.TABLE_NAME, values, GeofenceMetaData.TableMetaData._ID + "=" + rowId6 + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
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
