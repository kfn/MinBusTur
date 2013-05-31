package com.miracleas.minrute.model;

import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;

import com.google.android.gms.location.Geofence;
import com.miracleas.minrute.provider.GeofenceMetaData;

public class GeofenceHelper
{
	public static final String LEG_ID_WITH_STOP_ID = "legIdStopId";
	public static final String LEG_ID = "lgId";
	public static final String DELIMITER = "-";

	public static Geofence toGeofence(String id, int transitionType, double lat, double lng, float radius, long expirationDuration)
	{
		// Build a new Geofence object
		return new Geofence.Builder().setRequestId(id).setTransitionTypes(transitionType).setCircularRegion(lat, lng, radius).setExpirationDuration(expirationDuration).build();
	}

	public static void saveGeofence(GeofenceMy geo, ArrayList<ContentProviderOperation> dbOperations, ContentResolver cr)
	{
		ContentValues values = new ContentValues();
		values.put(GeofenceMetaData.TableMetaData.geofence_id, geo.id);

		String where = GeofenceMetaData.TableMetaData.geofence_id + "=?";
		String[] selectionArgs = { geo.id };
		int updated = cr.update(GeofenceMetaData.TableMetaData.CONTENT_URI, values, where, selectionArgs);

		if (updated == 0)
		{
			values.put(GeofenceMetaData.TableMetaData.LAT, geo.lat);
			values.put(GeofenceMetaData.TableMetaData.LNG, geo.lng);
			values.put(GeofenceMetaData.TableMetaData.TRIP_ID, geo.tripId);
			values.put(GeofenceMetaData.TableMetaData.RADIUS, geo.radius);
			values.put(GeofenceMetaData.TableMetaData.TRANSITION_TYPE, geo.transitionType);
			ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(GeofenceMetaData.TableMetaData.CONTENT_URI);
			b.withValues(values);
			dbOperations.add(b.build());
		}
	}

	public static int getRadius(String typeOfTransport)
	{
		int radius = 10;
		if (typeOfTransport.equals(TripLeg.TYPE_WALK))
		{
			radius = 50;
		} else if (typeOfTransport.equals(TripLeg.TYPE_BUS))
		{
			radius = 100;
		} else if (typeOfTransport.equals(TripLeg.TYPE_EXB))
		{
			radius = 100;
		} else if (typeOfTransport.equals(TripLeg.TYPE_IC))
		{
			radius = 100;
		} else if (typeOfTransport.equals(TripLeg.TYPE_LYN))
		{
			radius = 100;
		} else if (typeOfTransport.equals(TripLeg.TYPE_REG))
		{
			radius = 100;
		} else if (typeOfTransport.equals(TripLeg.TYPE_TB))
		{
			radius = 100;
		} else if (typeOfTransport.equals(TripLeg.TYPE_TRAIN))
		{
			radius = 100;
		}
		return radius;
	}
}
