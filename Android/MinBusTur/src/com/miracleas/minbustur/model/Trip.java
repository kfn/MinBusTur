package com.miracleas.minbustur.model;

import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;


public class Trip
{
	public static final String TYPE_BUS = "BUS";
	public static final String TYPE_WALK = "WALK";
	public static final String TYPE_TRAIN = "TOG";
	public static final String TYPE_IC = "IC";
	public static final String TYPE_LYN = "LYN";
	
	
	public String id = "";
	private long durationWalk = 0;
	private long durationBus = 0;
	private long durationTrain = 0;
	private StringBuilder names = new StringBuilder();
	private StringBuilder types = new StringBuilder();
	private int transportChanges;
	private List<TripLeg> legs = new ArrayList<TripLeg>();
	
	public void addLeg(TripLeg leg)
	{
		legs.add(leg);
		incrementCount(leg);
		addName(leg.name, leg.type);
		addType(leg.type);
	}
	
	private void addName(String name, String type)
	{
		if(!type.equals(TYPE_WALK))
		{
			names.append(name).append(",");
		}
		
	}
	
	private void addType(String type)
	{
		types.append(type).append(",");
	}
	
	public long getDeparturesInTime()
	{
		long t = 0l;
		if(legs.size()>0)
		{
			t = legs.get(0).getDeparturesIn();
		}
		return t;
	}
	
	public long getArrivesInTime()
	{
		long t = 0l;
		if(legs.size()>0)
		{
			t = legs.get(legs.size()-1).getArrivesIn();
		}
		return t;
	}
	
	private void incrementCount(TripLeg leg)
	{
		if(TextUtils.isEmpty(leg.type))
		{
			
		}
		else if(leg.type.equals(TYPE_WALK))
		{
			durationWalk = durationWalk + leg.getDuration();
		}
		else if(leg.type.equals(TYPE_BUS))
		{
			transportChanges++;
			durationBus = durationBus + leg.getDuration();
		}
		else if(leg.type.equals(TYPE_IC))
		{
			transportChanges++;
			durationTrain = durationTrain + leg.getDuration();
		}
		else if(leg.type.equals(TYPE_TRAIN))
		{
			transportChanges++;
			durationTrain = durationTrain + leg.getDuration();
		}
		else if(leg.type.equals(TYPE_LYN))
		{
			transportChanges++;
			durationTrain = durationTrain + leg.getDuration();
		}
	}
	
	public long getTotalDuration()
	{
		return durationWalk + durationBus  + durationTrain;
	}
	
	public long getDurationWalk()
	{
		return durationWalk;
	}

	public long getDurationBus()
	{
		return durationBus;
	}

	public long getDurationTrain()
	{
		return durationTrain;
	}

	public int getTransportChanges()
	{
		return transportChanges - 1;
	}
	
	public int getLegCount()
	{
		return legs.size();
	}
	
	public String getDepatureTime()
	{
		String s = "";
		if(!legs.isEmpty())
		{
			s = legs.get(0).origin.time;
		}
		return s;
	}
	
	public String getArrivalTime()
	{
		String s = "";
		if(!legs.isEmpty())
		{
			s = legs.get(legs.size()-1).dest.time;
		}
		return s;
	}
	
	public String getDepaturesAt()
	{
		String s = "";
		if(!legs.isEmpty())
		{
			s = legs.get(0).origin.time;
		}
		return s;
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
}
