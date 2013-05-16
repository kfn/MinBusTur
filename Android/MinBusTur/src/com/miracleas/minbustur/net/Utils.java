package com.miracleas.minbustur.net;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

public class Utils
{

	public static String convertStreamToString(HttpResponse response, String iso) throws IOException
	{
		HttpEntity entity = response.getEntity();
		InputStream instream = entity.getContent();
		return convertStreamToString(instream, iso);
	}

	public static String convertStreamToString(InputStream is, String iso) throws IOException
	{
		if (is != null)
		{
			StringBuilder sb = new StringBuilder();
			String line = null;

			try
			{
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, iso));
				while ((line = reader.readLine()) != null)
				{
					sb.append(line);
				}
			}
			finally
			{
				is.close();
			}
			return sb.toString();
		}
		else
		{
			return "";
		}
	}

	public static String convertStreamToString2(InputStream is)
	{
		ByteArrayOutputStream oas = new ByteArrayOutputStream();
		Utils.copyStream(is, oas);
		String t = oas.toString();
		try
		{
			oas.close();
			oas = null;
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return t;
	}

	private static void copyStream(InputStream is, OutputStream os)
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

	public static boolean isConnected()
	{
		ConnectivityManager mConnectivity = null;
		TelephonyManager mTelephony = null;
		// Skip if no connection, or background data disabled
		NetworkInfo info = mConnectivity.getActiveNetworkInfo();
		if (info == null || !mConnectivity.getBackgroundDataSetting())
		{
			return false;
		}

		// Only update if WiFi or 3G is connected and not roaming
		int netType = info.getType();
		int netSubtype = info.getSubtype();
		if (netType == ConnectivityManager.TYPE_WIFI)
		{
			return info.isConnected();
		}
		else if (netType == ConnectivityManager.TYPE_MOBILE && netSubtype == TelephonyManager.NETWORK_TYPE_UMTS && !mTelephony.isNetworkRoaming())
		{
			return info.isConnected();
		}
		else
		{
			return false;
		}
	}

	public static boolean isWifiConnected()
	{
		ConnectivityManager mConnectivity = null;
		TelephonyManager mTelephony = null;
		// Skip if no connection, or background data disabled
		NetworkInfo info = mConnectivity.getActiveNetworkInfo();
		if (info == null || !mConnectivity.getBackgroundDataSetting())
		{
			return false;
		}

		// Only update if WiFi or 3G is connected and not roaming
		int netType = info.getType();
		if (netType == ConnectivityManager.TYPE_WIFI)
		{
			return info.isConnected();
		}
		else
		{
			return false;
		}
	}
}
