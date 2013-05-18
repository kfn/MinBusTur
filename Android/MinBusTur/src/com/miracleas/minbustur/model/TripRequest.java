package com.miracleas.minbustur.model;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.protocol.HTTP;

public class TripRequest
{
	private String originId;
	private String originCoordX;
	private String originCoordY;
	private String originCoordName;
	private String destId;
	private String destCoordX;
	private String destCoordY;
	private String destCoordName;
	private String viaId;
	private String date; //If the date is not set the current date will be used (server time)
	private String time; //If the time is not set the current time will be used (server time)
	private int searchForArrival;  // 0 or 1
	private int useTog = 1; //default 1
	private int useBus = 1; //default 1
	private int useMetro = 1; //default 1
	
	
	public String getOriginId()
	{
		return originId;
	}
	public void setOriginId(String originId) throws UnsupportedEncodingException
	{
		this.originId = URLEncoder.encode(originId, HTTP.UTF_8);
	}
	public String getOriginCoordX()
	{
		return originCoordX;
	}
	public void setOriginCoordX(String originCoordX) throws UnsupportedEncodingException
	{
		this.originCoordX = URLEncoder.encode(originCoordX, HTTP.UTF_8);
	}
	public String getOriginCoordY()
	{
		return originCoordY;
	}
	public void setOriginCoordY(String originCoordY) throws UnsupportedEncodingException
	{
		this.originCoordY = URLEncoder.encode(originCoordY, HTTP.UTF_8);
	}
	public String getOriginCoordName()
	{
		return originCoordName;
	}
	public void setOriginCoordName(String originCoordName) throws UnsupportedEncodingException
	{
		this.originCoordName = URLEncoder.encode(originCoordName, HTTP.UTF_8);
	}
	public String getDestId()
	{
		return destId;
	}
	public void setDestId(String destId) throws UnsupportedEncodingException
	{
		this.destId = URLEncoder.encode(destId, HTTP.UTF_8);
	}
	public String getDestCoordX()
	{
		return destCoordX;
	}
	public void setDestCoordX(String destCoordX) throws UnsupportedEncodingException
	{
		this.destCoordX = URLEncoder.encode(destCoordX, HTTP.UTF_8);
	}
	public String getDestCoordY()
	{
		return destCoordY;
	}
	public void setDestCoordY(String destCoordY) throws UnsupportedEncodingException
	{
		this.destCoordY = URLEncoder.encode(destCoordY, HTTP.UTF_8);
	}
	public String getDestCoordName()
	{
		return destCoordName;
	}
	public void setDestCoordName(String destCoordName) throws UnsupportedEncodingException
	{
		this.destCoordName = URLEncoder.encode(destCoordName, HTTP.UTF_8);
	}
	public String getViaId()
	{
		return viaId;
	}
	public void setViaId(String viaId) throws UnsupportedEncodingException
	{
		this.viaId = URLEncoder.encode(viaId, HTTP.UTF_8);
	}
	public String getDate()
	{
		return date;
	}
	public void setDate(String date) throws UnsupportedEncodingException
	{
		this.date = URLEncoder.encode(date, HTTP.UTF_8);
	}
	public String getTime()
	{
		return time;
	}
	public void setTime(String time) throws UnsupportedEncodingException
	{
		this.time = URLEncoder.encode(time, HTTP.UTF_8);
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
}
