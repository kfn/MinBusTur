package com.miracleas.minrute.model;


import java.text.ParseException;
import java.util.Calendar;

import com.miracleas.minrute.R;
import com.miracleas.minrute.utils.DateHelper;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.util.TimeUtils;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

public class TripLeg implements Parcelable
{
	public static final String tag = TripLeg.class.getName();
	
	public static final String TYPE_BUS = "BUS";
	public static final String TYPE_WALK = "WALK";
	public static final String TYPE_TRAIN = "TOG";
	public static final String TYPE_IC = "IC";
	public static final String TYPE_LYN = "LYN";
	public static final String TYPE_REG = "REG";
	public static final String TYPE_EXB = "EXB";
	public static final String TYPE_TB = "TB";
    public static final String TYPE_BOAT = "F";
    public static final String TYPE_S_TRAIN = "S";
	
	
	public String tripId;
	public String name;
	public String type;
	public String notes;
	public String ref;
	public TripLocation origin;
	public TripLocation dest;
	public String updated;
	private long duration = 0;
	private StringBuilder b;
	private Calendar start;
	private Calendar end;

	
	//---------------
	public int id;
	public String originName;
	public String destName;
	public long departureTime = 0;
	public String originTime;
	public String destTime;
	public int step = 0;
	public boolean isOrigin = false;
	public boolean isDestiation = false;
	public int transitionType;
	//---------------

	
	
	public TripLeg(Parcel in)
	{
		tripId = in.readString();
		type = in.readString();
		ref = in.readString();
		id = in.readInt();
		originName = in.readString();
		destName = in.readString();
		originTime = in.readString();
		destTime = in.readString();
		notes = in.readString();
		transitionType = in.readInt();
	}
	@Override
	public int describeContents()
	{
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(this.tripId);
		dest.writeString(this.type);
		dest.writeString(this.ref);
		dest.writeInt(this.id);
		dest.writeString(this.originName);	
		dest.writeString(destName);
		dest.writeString(originTime);
		dest.writeString(destTime);
		dest.writeString(notes);
		dest.writeInt(this.transitionType);
	}
	public static final Parcelable.Creator<TripLeg> CREATOR = new Parcelable.Creator<TripLeg>()
	{
		public TripLeg createFromParcel(Parcel in)
		{
			return new TripLeg(in);
		}

		public TripLeg[] newArray(int size)
		{
			return new TripLeg[size];
		}
	};
	
	
	public TripLeg(){}
	
	public long getCalculatedDuration()
	{
		if(duration==0 && origin!=null && dest!=null)
		{
			if(!TextUtils.isEmpty(origin.time) && !TextUtils.isEmpty(dest.time))
			{
				try
				{
					start = DateHelper.parseToCalendar(origin.date+" "+origin.time, DateHelper.formatter);
					end = DateHelper.parseToCalendar(dest.date+" "+dest.time, DateHelper.formatter);
					duration = end.getTimeInMillis() - start.getTimeInMillis();
				} catch (ParseException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return duration;
	}
	
	public long getDuration()
	{
		return duration;
	}
	
	public void setDuration(long duration)
	{
		this.duration = duration;
	}
	
	public String getFormattedDuration()
	{
		b = new StringBuilder();
		TimeUtils.formatDuration(duration, b);
		//Log.d("TripLeg", b.toString());
		return b.toString();
	}
	
	public long getCalculatedDeparturesIn()
	{
		long s = 0l;
		if(start!=null)
		{
			s = start.getTimeInMillis() - System.currentTimeMillis();
		}
		return s;
	}
	
	public long getCalculatedDepartures()
	{
		long s = 0l;
		if(start!=null)
		{
			s = start.getTimeInMillis();
		}
		return s;
	}
	
	public long getArrivesIn()
	{
		long s = 0l;
		if(end!=null)
		{
			s = end.getTimeInMillis() - System.currentTimeMillis();
		}
		return s;
	}
	
	public String getFormattedOriginAddress()
	{
		String[] temp = originName.split(",");
		if(temp.length>1)
		{
			return temp[0]+", "+temp[1];
		}
		else
		{
			return temp[0];
		}
	}
	
	public String getFormattedDestAddress()
	{
		String[] temp = destName.split(",");
		if(temp.length>1)
		{
			return temp[0]+", "+temp[1];
		}
		else
		{
			return temp[0];
		}
	}
	
	private Time getTime(String text)
	{
		int pos = text.charAt(':');
		int hour = Integer.parseInt(text.substring(0, pos));
		int minute = Integer.parseInt(text.substring(pos+1, text.length()-1));
		Time t = new Time();
		t.second = 0;
		t.hour = hour;
		t.minute = minute;
		return t;
	}
	
	public static int getIcon(String type)
	{
		int icon = 0;
		if(type.equals(TripLeg.TYPE_WALK))
		{
			icon = R.drawable.walking;
		}
		else if(type.equals(TripLeg.TYPE_BUS))
		{
			icon = R.drawable.driving;
		}
		else if(type.equals(TripLeg.TYPE_EXB))
		{
			icon = R.drawable.driving;
		}
		else if(type.equals(TripLeg.TYPE_TB))
		{
			icon = R.drawable.driving;
		}
		else if(type.equals(TripLeg.TYPE_IC))
		{
			icon = R.drawable.driving;
		}
		else if(type.equals(TripLeg.TYPE_TRAIN))
		{
			icon = R.drawable.driving;
		}
		else if(type.equals(TripLeg.TYPE_LYN))
		{
			icon = R.drawable.driving;
		}
		else if(type.equals(TripLeg.TYPE_REG))
		{
			icon = R.drawable.driving;
		}
        else if(type.equals(TripLeg.TYPE_BOAT))
        {
            icon = R.drawable.driving;
        }
        else if(type.equals(TripLeg.TYPE_S_TRAIN))
        {
            icon = R.drawable.driving;
        }
		return icon;
	}
	
	public static boolean isTrain(String type)
	{
		return (type.equals(TripLeg.TYPE_REG) || type.equals(TripLeg.TYPE_LYN) || type.equals(TripLeg.TYPE_TRAIN) || type.equals(TripLeg.TYPE_IC) || type.equals(TripLeg.TYPE_S_TRAIN) );
	}
	
	public boolean isWalk()
	{
		return type.equals(TYPE_WALK);
	}
	
	public boolean isTrain()
	{
		return isTrain(type);
	}
	
	public boolean isBus()
	{
		return (type.equals(TripLeg.TYPE_BUS) || type.equals(TripLeg.TYPE_EXB) || type.equals(TripLeg.TYPE_TB));
	}
	
	public static int getRadius(String typeOfTransport)
	{
		int radius = 10;
		if (typeOfTransport.equals(TripLeg.TYPE_WALK))
		{
			radius = 50;
		} else if (typeOfTransport.equals(TripLeg.TYPE_BUS))
		{
			radius = 120;
		} else if (typeOfTransport.equals(TripLeg.TYPE_EXB))
		{
			radius = 120;
		} else if (typeOfTransport.equals(TripLeg.TYPE_IC))
		{
			radius = 160;
		} else if (typeOfTransport.equals(TripLeg.TYPE_LYN))
		{
			radius = 180;
		} else if (typeOfTransport.equals(TripLeg.TYPE_REG))
		{
			radius = 180;
		} else if (typeOfTransport.equals(TripLeg.TYPE_TB))
		{
			radius = 180;
		} else if (typeOfTransport.equals(TripLeg.TYPE_TRAIN))
		{
			radius = 180;
		}
        else if (typeOfTransport.equals(TripLeg.TYPE_S_TRAIN))
        {
            radius = 180;
        }
        else if (typeOfTransport.equals(TripLeg.TYPE_BOAT))
        {
            radius = 280;
        }
		return radius;
	}

}
