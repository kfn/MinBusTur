package com.miracleas.minrute.model;

import android.os.Parcel;
import android.os.Parcelable;

public class TripLegStop implements Parcelable
{
	public static final String tag = TripLegStop.class.getName();
	
	public String lat;
	public String lng;
	public String name;
	public int id;
	public int legId;
	
	public boolean isFirst = false;
	public boolean isLast = false;
	public boolean isBeforLast = false;

	public String transportType;
	
	public TripLegStop(String lat, String lng, String name, int id)
	{
		super();
		this.lat = lat;
		this.lng = lng;
		this.name = name;
		this.id = id;
	}

	public TripLegStop(Parcel in)
	{
		lat = in.readString();
		lng = in.readString();
		name = in.readString();
		id = in.readInt();
	}
	
	@Override
	public int describeContents()
	{
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(lat);
		dest.writeString(lng);
		dest.writeString(name);
		dest.writeInt(id);
	}
	
	public static final Parcelable.Creator<TripLegStop> CREATOR = new Parcelable.Creator<TripLegStop>()
	{
		public TripLegStop createFromParcel(Parcel in)
		{
			return new TripLegStop(in);
		}

		public TripLegStop[] newArray(int size)
		{
			return new TripLegStop[size];
		}
	};

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TripLegStop other = (TripLegStop) obj;
		if (name == null)
		{
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	
	
}
