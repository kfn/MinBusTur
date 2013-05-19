package com.miracleas.minbustur.model;

public class Trip
{
	public String id = "";
	public long duration;
	public int legCount = 0;
	private StringBuilder names = new StringBuilder();
	private StringBuilder types = new StringBuilder();
	
	public void addName(String name)
	{
		names.append(name).append(",");
	}
	
	public void addType(String type)
	{
		types.append(type).append(",");
	}
	
	public String getNames()
	{
		if(names.length()>1)
		{
			String s = names.toString();
			int i = s.lastIndexOf(",");
			return s.substring(0, i);
		}
		else
		{
			return "";
		}
	}
	
	public String getTypes()
	{
		if(types.length()>1)
		{
			String s = types.toString();
			int i = s.lastIndexOf(",");
			return s.substring(0, i);
		}
		else
		{
			return "";
		}
	}
	
	public void incrementLegCount()
	{
		legCount++;
	}
}
