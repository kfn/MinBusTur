package com.miracleas.minrute.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class MyPrefs
{
	public static final String PREFS_NAME = "minrute_prefs";
	public static final String GOOGLE_DRIVE_AUTH = "GOOGLE_DRIVE_AUTH";
	
	public static void setIntValue(Context context, String key, int value)
	{
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(key, value);
		editor.commit();
	}

	public static int getIntValue(Context context, String key, int defaultValue)
	{
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		return settings.getInt(key, defaultValue);
	}

	public static void setLongValue(Context context, String key, long value)
	{
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putLong(key, value);
		editor.commit();
	}

	public static long getLongValue(Context context, String key, long defaultValue)
	{
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		return settings.getLong(key, defaultValue);
	}

	public static void setFloatValue(Context context, String key, float value)
	{
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putFloat(key, value);
		editor.commit();
	}

	public static float getFloatValue(Context context, String key, float defaultValue)
	{
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		return settings.getFloat(key, defaultValue);
	}

	public static void setBoolean(Context context, String key, boolean send)
	{
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(key, send);
		editor.commit();
	}

	public static boolean getBoolean(Context context, String key, boolean defaultValue)
	{
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		return settings.getBoolean(key, defaultValue);
	}

	public static void setString(Context context, String key, String value)
	{
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(key, value);
		editor.commit();
	}

	public static String getString(Context context, String key, String defaultValue)
	{
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		return settings.getString(key, defaultValue);
	}

}
