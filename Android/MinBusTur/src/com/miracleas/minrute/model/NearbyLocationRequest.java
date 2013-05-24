package com.miracleas.minrute.model;

import android.os.Parcel;
import android.os.Parcelable;

public class NearbyLocationRequest implements Parcelable
{
	public String stopId;
	public String coordX;
	public String coordY;
	public int maxRadius;
	public int maxNumber;
	private double lat;
	private double lng;
	public String stopName;
	
	public NearbyLocationRequest(){}
	
	public NearbyLocationRequest(String stopId, String coordX,  String coordY, String stopName)
	{
		this(stopId, coordX, coordY, 10, 1, stopName);
	}
	
	public NearbyLocationRequest(String stopId, String coordX, String coordY, int maxRadius, int maxNumber, String stopName)
	{
		super();
		this.stopId = stopId;
		this.coordX = coordX;
		this.coordY = coordY;
		this.maxRadius = maxRadius;
		this.maxNumber = maxNumber;
		this.stopName = stopName;
		
		lat = (double)(Integer.parseInt(coordY) / 1000000d);
		lng = (double)(Integer.parseInt(coordX) / 1000000d);
	}
	
	

	
	public double getLat()
	{
		return lat;
	}


	public double getLng()
	{
		return lng;
	}

	public NearbyLocationRequest(Parcel in)
	{
		stopId = in.readString();
		coordX = in.readString();
		coordY = in.readString();
		maxRadius = in.readInt();
		maxNumber = in.readInt();
		stopName = in.readString();
	}
	
	@Override
	public int describeContents()
	{
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(this.stopId);
		dest.writeString(this.coordX);
		dest.writeString(this.coordY);
		dest.writeInt(this.maxRadius);
		dest.writeInt(this.maxNumber);	
		dest.writeString(stopName);
	}
	
	public static final Parcelable.Creator<NearbyLocationRequest> CREATOR = new Parcelable.Creator<NearbyLocationRequest>()
	{
		public NearbyLocationRequest createFromParcel(Parcel in)
		{
			return new NearbyLocationRequest(in);
		}

		public NearbyLocationRequest[] newArray(int size)
		{
			return new NearbyLocationRequest[size];
		}
	};
}
