package com.miracleas.minrute.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;

import org.apache.http.protocol.HTTP;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.miracleas.minrute.R;
import com.miracleas.minrute.provider.AddressProviderMetaData;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;
import android.util.Log;

public abstract class BaseFetcher
{
	public static final String tag = BaseFetcher.class.getName();
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String CONTENT_TYPE_JSON = "application/json";
	public static final String CONTENT_TYPE_XML = "application/xml";
	public static final String MIME_FORM_ENCODED = "application/x-www-form-urlencoded";

	public static final String BROADCAST_SERVER_RESPONSE_ACTION = "BROADCAST_MSG_FROM_HTTP_REQUEST_ACTION";
	public static final String BROADCAST_MSG = "BROADCAST_MSG";
	public static final String BROADCAST_MSG_SUCCESS_BOOLEAN = "BROADCAST_MSG_SUCCESS_BOOLEAN";
	public static final String BROADCAST_UNAUTHORIZED_BOOLEAN = "BROADCAST_UNAUTHORIZED_BOOLEAN";
	public static final String LOADING_DATA = "LOADING_DATA";
	public static final String ARGS_FORCE_UPDATE = "ARGS_FORCE_UPDATE";
	
	public static final String BASE_URL = "http://xmlopen.rejseplanen.dk/bin/rest.exe/";

	protected Context mContext = null;
	protected ContentResolver mContentResolver = null;
	protected boolean mSuccess = false;
	protected boolean mForceUpdate = false;
	protected boolean log = true;
	protected long mMaxCacheTime = 0;
	protected static final int TIME_OUT = 20000;
	protected static long MAX_DETAILS_UPDATE_LATENCY = DateUtils.DAY_IN_MILLIS;
	protected boolean mRetry = true;
	private boolean mConnectionError = false;
	private StringBuilder mBodyBuilder = null;
	protected ArrayList<ContentProviderOperation> mDbOperations;
	protected Uri mUriNotify;
	

	public BaseFetcher(Context c, Intent intent)
	{
		mContext = c;
		mContentResolver = c.getContentResolver();
		mMaxCacheTime = 2000;
		if(intent!=null)
			mForceUpdate = intent.getBooleanExtra(ARGS_FORCE_UPDATE, false);
		mDbOperations = new ArrayList<ContentProviderOperation>();
	}

	public boolean startFetch()
	{
		boolean started = start();
		if (started)
		{
			String errorMsg = null;
			boolean unauthorized = false;
			boolean errorFatal = false;
			try
			{
				doWork();
			}
			catch (java.net.SocketTimeoutException e)
			{
				mConnectionError = true;
				e.printStackTrace();
				errorMsg = mContext.getString(R.string.time_out);
			}
			catch (java.net.SocketException e)
			{
				mConnectionError = true;
				e.printStackTrace();
				errorMsg = mContext.getString(R.string.time_out);
			}
			catch (java.net.UnknownHostException e)
			{
				mConnectionError = true;
				e.printStackTrace();
				errorMsg = mContext.getString(R.string.time_out);
			}
			catch (java.io.FileNotFoundException e)
			{
				mConnectionError = true;
				e.printStackTrace();
				errorMsg = mContext.getString(R.string.search_failed_404);
			}
			catch (Exception e)
			{
				errorFatal = true;
				e.printStackTrace();
				errorMsg = e.getMessage();
			}
			if(errorFatal)
			{
				onFatalError();
			}
			if(mConnectionError && mRetry)
			{
				mRetry = false;
				end();
				startFetch();
			}
			else if (errorMsg != null)
			{
				broadcastError(errorMsg, unauthorized);
			}
			
		}
		end();
		return started;
	}
	
	protected ContentProviderResult[] saveData(String authority) throws RemoteException, OperationApplicationException
	{
		ContentProviderResult[] results = null;
		if(!mDbOperations.isEmpty())
		{
			results  = mContentResolver.applyBatch(authority, mDbOperations);
			Log.d(tag, "applyBatch: "+results.length);
			if(mUriNotify!=null)
			{
				//mContentResolver.notifyChange(mUriNotify, null);
			}			
			mDbOperations.clear();
		}
		return results;
	}

	protected boolean start(){return true;}

	protected void end(){}
	
	protected void onFatalError(){}
	
	public static void resetLoadingState(Context c){}

	abstract void doWork() throws Exception;

	/*
	 * protected BasicHeader[] getHeaders() { BasicHeader[] headers = { new
	 * BasicHeader("Content-Type", "application/json"), new
	 * BasicHeader("x-minby-apikey", mFactory.getAppId()), new
	 * BasicHeader("install-id", Installation.id(mContext)) }; return headers; }
	 */

	protected void broadcastError(String errorMsg, boolean unauthorized)
	{
		LocalBroadcastManager r = LocalBroadcastManager.getInstance(mContext);
		Intent broadcast = new Intent(BROADCAST_SERVER_RESPONSE_ACTION);
		broadcast.putExtra(BROADCAST_MSG_SUCCESS_BOOLEAN, false);
		broadcast.putExtra(BROADCAST_UNAUTHORIZED_BOOLEAN, unauthorized);
		broadcast.putExtra(BROADCAST_MSG, errorMsg);
		r.sendBroadcast(broadcast);
	}

	protected void broadcastSuccess()
	{
		LocalBroadcastManager r = LocalBroadcastManager.getInstance(mContext);
		Intent broadcast = new Intent(BROADCAST_SERVER_RESPONSE_ACTION);
		broadcast.putExtra(BROADCAST_MSG_SUCCESS_BOOLEAN, true);
		r.sendBroadcast(broadcast);
		mSuccess = true;
	}

	protected void broadcastLoading()
	{
		LocalBroadcastManager r = LocalBroadcastManager.getInstance(mContext);
		Intent broadcast = new Intent(BROADCAST_SERVER_RESPONSE_ACTION);
		broadcast.putExtra(BaseFetcher.LOADING_DATA, true);
		r.sendBroadcast(broadcast);
	}

	protected void log(String tag, String msg)
	{
		if (log)
		{
			Log.d(tag, msg);
		}
	}

	protected HttpURLConnection initHttpURLConnection(String url) throws IOException
	{
		URL url1 = new URL(url);
		HttpURLConnection urlConnection = (HttpURLConnection) url1.openConnection();
		urlConnection.setConnectTimeout(TIME_OUT);
		urlConnection.setReadTimeout(TIME_OUT);
		urlConnection.addRequestProperty(CONTENT_TYPE, MIME_FORM_ENCODED);
		urlConnection.setRequestProperty("Accept-Encoding", "gzip");
		urlConnection.setRequestProperty("Accept", "*/*");		
		return urlConnection;
	}
	protected HttpURLConnection initPostHttpURLConnection(String url) throws IOException
	{
		HttpURLConnection urlConnection = initHttpURLConnection(url);
		urlConnection.setDoOutput(true);
		urlConnection.setRequestMethod("POST");
		urlConnection.setUseCaches(false);
		mBodyBuilder = new StringBuilder();
		return urlConnection;
	}
	protected String getPostBody()
	{
		if(mBodyBuilder!=null)
		{
			return mBodyBuilder.toString();
		}
		else
		{
			return "";
		}
	}
	
	protected StringBuilder addToPostBodyUrlEncoded(String key, String value) throws UnsupportedEncodingException
	{
		if(mBodyBuilder.length()>0)
		{
			mBodyBuilder.append("&");
		}
		return mBodyBuilder.append(key).append("=").append(URLEncoder.encode(value,  HTTP.UTF_8));
	}
	protected StringBuilder addToPostBody(String key, String value) throws UnsupportedEncodingException
	{
		if(mBodyBuilder.length()>0)
		{
			mBodyBuilder.append("&");
		}
		return mBodyBuilder.append(key).append("=").append(URLEncoder.encode(value,  HTTP.UTF_8));
	}
	
	protected void writePostRequest(HttpURLConnection conn) throws IOException
	{		
		writePostRequest(conn, mBodyBuilder.toString());	
	}
	
	protected void writePostRequest(HttpURLConnection conn, String query) throws IOException
	{					
		byte[] postData = null;
		postData = query.getBytes();
		conn.setRequestProperty("Content-Length", Integer.toString(postData.length));
		OutputStream out = conn.getOutputStream();
		out.write(postData);
		out.close();
	}

	protected JSONObject getJsonResponse(InputStream instream, String iso) throws Exception
	{
		JSONObject jsonObjRecv = null;
		if (instream != null)
		{
			String resultString = Utils.convertStreamToString(instream, iso);
			// instream.close();
			jsonObjRecv = new JSONObject(resultString);
		}

		return jsonObjRecv;
	}
	
	protected String safeNextText(XmlPullParser parser) throws XmlPullParserException, IOException
	{
		
		String result = parser.nextText();
		if (parser.getEventType() != XmlPullParser.END_TAG)
		{
			parser.nextTag();
		}
		return result;
	}
	
	
	private boolean isConnectedOnWifi()
	{
		ConnectivityManager cm =
		        (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		 
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		boolean isConnected = activeNetwork.isConnectedOrConnecting();
		return (isConnected &&  activeNetwork.getType() == ConnectivityManager.TYPE_WIFI);	
	}
}
