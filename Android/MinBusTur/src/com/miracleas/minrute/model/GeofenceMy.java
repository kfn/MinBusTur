package com.miracleas.minrute.model;

public class GeofenceMy
{
	public String tripId;
	public int radius;
	public int transitionType;
	public double lat;
	public double lng;
	public String id;
	
	public GeofenceMy(String tripId, String id, int radius, int transitionType, double lat, double lng)
	{
		super();
		this.tripId = tripId;
		this.id = id;
		this.radius = radius;
		this.transitionType = transitionType;
		this.lat = lat;
		this.lng = lng;
	}
	
	
}
