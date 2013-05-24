package com.miracleas.minrute.model;

import android.os.Parcel;
import android.os.Parcelable;

public class MyLittleImage implements Parcelable
{
	public String url;
	public String path;
	public long id;
	
	public MyLittleImage(){}
	
	public MyLittleImage(String url, String path, long id)
	{
		super();
		this.url = url;
		this.path = path;
		this.id = id;
	}
	
	public MyLittleImage(Parcel in)
	{
		super();
		this.url = in.readString();
		this.path = in.readString();
		this.id = in.readLong();
	}

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(url);
		dest.writeString(path);
		dest.writeLong(id);
	}		
	
	public static final Parcelable.Creator<MyLittleImage> CREATOR = new Parcelable.Creator<MyLittleImage>()
	{
		public MyLittleImage createFromParcel(Parcel in)
		{
			return new MyLittleImage(in);
		}

		public MyLittleImage[] newArray(int size)
		{
			return new MyLittleImage[size];
		}
	};
}
