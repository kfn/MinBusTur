package com.miracleas.minrute.model;

public class AddressSearch
{
	public static final int TYPE_STATION_STOP = 1;
	public static final int TYPE_ADRESSE = 2;
	
	public String id;
	public String value;
	public String address;
	public String latitude;
	public String longitude;
	public String u;
	public String l;
	public String b;
	public String p;
	public String extId;
	public String type;
	public String typeStr;
	public String xcoord;
	public String ycoord;
	public String state;
	public String prodClass;
	public String weight;
	
	public AddressSearch(){}
	public AddressSearch(String id, String address, String type, String xcoord, String ycoord)
	{
		super();
		this.id = id;
		this.address = address;
		this.type = type;
		this.xcoord = xcoord;
		this.ycoord = ycoord;
	}
	
	
}
