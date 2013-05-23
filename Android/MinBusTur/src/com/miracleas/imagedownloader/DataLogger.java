package com.miracleas.imagedownloader;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;

public class DataLogger
{
	public static final int LOAD_IMAGE = 7;

	public static boolean log = true;

	private static SimpleDateFormat formatterForPostDate = new SimpleDateFormat("dd-MM HH:mm:ss", Locale.getDefault());

	public static void Log(Context c, int typeOfCall, long before, long after)
	{
		if (log)
		{
			long duration = after - before;
			ContentValues cv = new ContentValues();
			cv.put(LogProviderMetaData.TableMetaData.DATE, getNowString());
			cv.put(LogProviderMetaData.TableMetaData.DESCRIPTION, getDescription(c, typeOfCall));
			cv.put(LogProviderMetaData.TableMetaData.DURATION, duration);
			ContentResolver cr = c.getContentResolver();
			cr.insert(LogProviderMetaData.TableMetaData.CONTENT_URI, cv);
		}

	}

	public static void Log(Context c, int typeOfCall, long before, long after, String searchQuery)
	{
		if (log)
		{
			long duration = after - before;
			ContentValues cv = new ContentValues();
			cv.put(LogProviderMetaData.TableMetaData.DATE, getNowString());
			cv.put(LogProviderMetaData.TableMetaData.DESCRIPTION, getDescription(c, typeOfCall) + ": " + searchQuery);
			cv.put(LogProviderMetaData.TableMetaData.DURATION, duration);
			ContentResolver cr = c.getContentResolver();
			cr.insert(LogProviderMetaData.TableMetaData.CONTENT_URI, cv);
		}

	}

	private static String getDescription(Context c, int typeOfCall)
	{
		String result = "";
		if (typeOfCall == LOAD_IMAGE)
		{
			result = "billede";
		}
		return result;
	}

	public static String getNowString()
	{
		return formatterForPostDate.format(Calendar.getInstance(Locale.getDefault()).getTime());
	}
}
