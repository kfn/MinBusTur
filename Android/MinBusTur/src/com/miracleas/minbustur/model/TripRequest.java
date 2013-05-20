package com.miracleas.minbustur.model;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.protocol.HTTP;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class TripRequest implements Parcelable
{
	public static final String tag = TripRequest.class.getName();
	private String originId;
	private String originCoordX;
	private String originCoordY;
	private String originCoordName = "";
	public String originCoordNameNotEncoded = "";
	private String destId;
	private String destCoordX;
	private String destCoordY;
	private String destCoordName = "";
	public String destCoordNameNotEncoded = "";
	private String viaId;
	private String date; //If the date is not set the current date will be used (server time)
	private String time; //If the time is not set the current time will be used (server time)
	private int searchForArrival;  // 0 or 1
	private int useTog = 1; //default 1
	private int useBus = 1; //default 1
	private int useMetro = 1; //default 1
	
	public TripRequest(){
		
	}
	
	public boolean isValid()
	{
		return (!TextUtils.isEmpty(originId) && !TextUtils.isEmpty(destId)) || (!TextUtils.isEmpty(originCoordX) && !TextUtils.isEmpty(originCoordY)
				&& !TextUtils.isEmpty(originCoordName) && !TextUtils.isEmpty(destCoordX) && !TextUtils.isEmpty(destCoordY) && !TextUtils.isEmpty(destCoordName));
	}
	
	
	public String getOriginId()
	{
		return originId;
	}
	public void setOriginId(String originId) 
	{
		if(TextUtils.isEmpty(originId))
		{
			this.originId = originId;
		}
		else
		{
			try
			{
				this.originId = URLEncoder.encode(originId, HTTP.ISO_8859_1);
			} catch (UnsupportedEncodingException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
	}
	public String getOriginCoordX()
	{
		return originCoordX;
	}
	public void setOriginCoordX(String originCoordX)
	{
		this.originCoordX = originCoordX;
	}
	public String getOriginCoordY()
	{
		return originCoordY;
	}
	public void setOriginCoordY(String originCoordY)
	{
		this.originCoordY = originCoordY;
	}
	public String getOriginCoordName()
	{
		return originCoordName;
	}
	public void setOriginCoordName(String originCoordName)
	{
		originCoordNameNotEncoded = originCoordName;
		if(TextUtils.isEmpty(originCoordName))
		{
			this.originCoordName = originCoordName;
		}
		else
		{
			try
			{
				this.originCoordName = URLEncoder.encode(originCoordName, HTTP.ISO_8859_1);
			} catch (UnsupportedEncodingException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
	}
	public String getDestId()
	{
		return destId;
	}
	public void setDestId(String destId)
	{
		if(TextUtils.isEmpty(destId))
		{
			this.destId = destId;
		}
		else
		{
			try
			{
				this.destId = URLEncoder.encode(destId, HTTP.ISO_8859_1);
			} catch (UnsupportedEncodingException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
	}
	public String getDestCoordX()
	{
		return destCoordX;
	}
	public void setDestCoordX(String destCoordX)
	{
		this.destCoordX = destCoordX;
	}
	public String getDestCoordY()
	{
		return destCoordY;
	}
	public void setDestCoordY(String destCoordY)
	{
		this.destCoordY = destCoordY;
	}
	public String getDestCoordName()
	{
		return destCoordName;
	}
	public void setDestCoordName(String destCoordName) 	{
		destCoordNameNotEncoded = destCoordName;
		if(TextUtils.isEmpty(destCoordName))
		{
			this.destCoordName = destCoordName;
		}
		else
		{
			try
			{
				this.destCoordName = URLEncoder.encode(destCoordName, HTTP.ISO_8859_1);
			} catch (UnsupportedEncodingException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
	}
	public String getViaId()
	{
		return viaId;
	}

	public void setViaId(String viaId)
	{
		if(TextUtils.isEmpty(viaId))
		{
			this.viaId = viaId;
		}
		else
		{
			try
			{
				this.viaId = URLEncoder.encode(viaId, HTTP.ISO_8859_1);
			} catch (UnsupportedEncodingException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}			
	}
	public String getDate()
	{
		return date;
	}
	public void setDate(String date)
	{
		this.date = date;
	}
	public String getTime()
	{
		return time;
	}
	public void setTime(String time)
	{
		this.time = time;
	}
	public int getSearchForArrival()
	{
		return searchForArrival;
	}
	public void setSearchForArrival(int searchForArrival)
	{
		this.searchForArrival = searchForArrival;
	}
	public int getUseTog()
	{
		return useTog;
	}
	public void setUseTog(int useTog)
	{
		this.useTog = useTog;
	}
	public int getUseBus()
	{
		return useBus;
	}
	public void setUseBus(int useBus)
	{
		this.useBus = useBus;
	}
	public int getUseMetro()
	{
		return useMetro;
	}
	public void setUseMetro(int useMetro)
	{
		this.useMetro = useMetro;
	}


	public int describeContents()
	{
		return 0;
	}
	public TripRequest(Parcel in)
	{
		searchForArrival = in.readInt();
		useBus = in.readInt();
		useMetro = in.readInt();
		useTog = in.readInt();
		date = in.readString();
		destCoordName = in.readString();
		destCoordX = in.readString();
		destCoordY = in.readString();
		destId = in.readString();
		originCoordName = in.readString();
		originCoordX = in.readString();
		originCoordY = in.readString();
		originId = in.readString();
		time = in.readString();
		viaId = in.readString();
	}

	public void writeToParcel(Parcel out, int flags)
	{
		out.writeInt(this.searchForArrival);
		out.writeInt(this.useBus);
		out.writeInt(this.useMetro);
		out.writeInt(this.useTog);
		out.writeString(this.date);
		out.writeString(this.destCoordName);
		out.writeString(this.destCoordX);	
		out.writeString(this.destCoordY);
		out.writeString(this.destId);
		out.writeString(this.originCoordName);
		out.writeString(this.originCoordX);
		out.writeString(this.originCoordY);
		out.writeString(this.originId);
		out.writeString(this.time);
		out.writeString(this.viaId);
		
	}

	public static final Parcelable.Creator<TripRequest> CREATOR = new Parcelable.Creator<TripRequest>()
	{
		public TripRequest createFromParcel(Parcel in)
		{
			return new TripRequest(in);
		}

		public TripRequest[] newArray(int size)
		{
			return new TripRequest[size];
		}
	};
}
