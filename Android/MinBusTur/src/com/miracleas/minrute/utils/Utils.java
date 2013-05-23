package com.miracleas.minrute.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;

public class Utils
{
	public static boolean copyDbToSdCard(String dbName)
	{
		File dbFile = new File(Environment.getDataDirectory() + "/data/com.miracleas.minbustur/databases/" + dbName);
		File exportDir = new File(Environment.getExternalStorageDirectory(), "");
		return Utils.moveFile(dbFile, exportDir);
	}
	

	public static boolean moveFile(File source, File destination)
	{
		File dbFile = source;

		File exportDir = destination;
		if (!exportDir.exists())
		{
			exportDir.mkdirs();
		}
		File file = new File(exportDir, dbFile.getName());

		try
		{
			file.createNewFile();
			copyFile(dbFile, file);
			return true;
		}
		catch (Exception e)
		{
			Log.e("DBHelper", e.getMessage(), e);
			return false;
		}
	}

	public static boolean copyFile(InputStream assetfile, File destination)
	{
		boolean result = false;
		// Open your local db as the input stream
		InputStream myInput = null;
		// Open the empty db as the output stream
		OutputStream myOutput = null;
		try
		{
			myInput = assetfile;
			myOutput = new FileOutputStream(destination);

			// transfer bytes from the inputfile to the outputfile
			byte[] buffer = new byte[1024];
			int length;
			while ((length = myInput.read(buffer)) > 0)
			{
				myOutput.write(buffer, 0, length);
			}
			result = true;

		}
		catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			// Close the streams
			try
			{
				myOutput.flush();
				myOutput.close();
				myInput.close();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return result;
	}

	private static void copyFile(File src, File dst) throws IOException
	{
		FileChannel inChannel = new FileInputStream(src).getChannel();
		FileChannel outChannel = new FileOutputStream(dst).getChannel();
		try
		{
			inChannel.transferTo(0, inChannel.size(), outChannel);
		}
		finally
		{
			if (inChannel != null)
				inChannel.close();
			if (outChannel != null)
				outChannel.close();
		}
	}

	public static void copyStream(InputStream is, OutputStream os)
	{
		final int buffer_size = 1024;
		try
		{
			byte[] bytes = new byte[buffer_size];
			for (;;)
			{
				int count = is.read(bytes, 0, buffer_size);
				if (count == -1)
					break;
				os.write(bytes, 0, count);
			}
		}
		catch (Exception ex)
		{
		}
	}

	public static void getDisplayInfo(Activity a, String tag)
	{
		// Determine screen size
		if ((a.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE)
		{
			// Toast.makeText(a, "Large screen",Toast.LENGTH_LONG).show();
			Log.d(tag, "Large screen");

		}
		else if ((a.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_NORMAL)
		{
			// Toast.makeText(a, "Normal sized screen" ,
			// Toast.LENGTH_LONG).show();
			Log.d(tag, "Normal sized screen");
		}
		else if ((a.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_SMALL)
		{
			// Toast.makeText(a, "Small sized screen" ,
			// Toast.LENGTH_LONG).show();
			Log.d(tag, "Small sized screen");
		}
		else if ((a.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE)
		{
			// Toast.makeText(a, "Xlarge sized screen" ,
			// Toast.LENGTH_LONG).show();
			Log.d(tag, "Xlarge sized screen");
		}
		else
		{
			// Toast.makeText(a, "Screen size is neither large, normal or small"
			// , Toast.LENGTH_LONG).show();
			Log.d(tag, "Screen size is neither large, normal or small");
		}

		// Determine density
		DisplayMetrics metrics = new DisplayMetrics();
		a.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int density = metrics.densityDpi;

		if (density == DisplayMetrics.DENSITY_HIGH)
		{
			// Toast.makeText(a, "DENSITY_HIGH... Density is " +
			// String.valueOf(density), Toast.LENGTH_LONG).show();
			Log.d(tag, "DENSITY_HIGH... Density is " + String.valueOf(density));
		}

		else if (density == DisplayMetrics.DENSITY_MEDIUM)
		{
			// Toast.makeText(a, "DENSITY_MEDIUM... Density is " +
			// String.valueOf(density), Toast.LENGTH_LONG).show();
			Log.d(tag, "DENSITY_MEDIUM... Density is " + String.valueOf(density));
		}
		else if (density == DisplayMetrics.DENSITY_LOW)
		{
			// Toast.makeText(a, "DENSITY_LOW... Density is " +
			// String.valueOf(density), Toast.LENGTH_LONG).show();
			Log.d(tag, "DENSITY_LOW... Density is " + String.valueOf(density));
		}
		else if (density == DisplayMetrics.DENSITY_XHIGH)
		{
			// Toast.makeText(a, "DENSITY_XHIGH... Density is " +
			// String.valueOf(density), Toast.LENGTH_LONG).show();
			Log.d(tag, "DENSITY_XHIGH... Density is " + String.valueOf(density));
		}
		else
		{
			// Toast.makeText(a,
			// "Density is neither HIGH, MEDIUM OR LOW.  Density is " +
			// String.valueOf(density), Toast.LENGTH_LONG).show();
			Log.d(tag, "Density is neither HIGH, MEDIUM OR LOW.  Density is " + String.valueOf(density));
		}

	}
}
