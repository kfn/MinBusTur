package com.miracleas.minrute.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.miracleas.camera.PhotoGoogleDriveActivity;
import com.miracleas.minrute.R;
import com.miracleas.minrute.model.MyLittleImage;
import com.miracleas.minrute.net.BaseFetcher;
import com.miracleas.minrute.net.TripFetcher;
import com.miracleas.minrute.provider.TripLegDetailStopDeparturesMetaData;
import com.miracleas.minrute.provider.StopImagesMetaData;
import com.miracleas.minrute.provider.TripLegMetaData;
import com.miracleas.minrute.provider.StopImagesMetaData.TableMetaData;

import android.accounts.AccountManager;
import android.app.IntentService;
import android.app.Service;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

public class DeleteImagesService extends Service
{
	public final static String tag = DeleteImagesService.class.getName();
	private static Drive service;
	protected GoogleAccountCredential credential;
	private String mAccountName;
	private ContentResolver mContentResolver;

	@Override
	public void onCreate()
	{
		super.onCreate();
		mContentResolver = getContentResolver();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Log.i("LocalService", "Received start id " + startId + ": " + intent);
		mAccountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
		credential = GoogleAccountCredential.usingOAuth2(this, DriveScopes.DRIVE);
		credential.setSelectedAccountName(mAccountName);
		service = getDriveService(credential);
		if(!TextUtils.isEmpty(mAccountName) && service!=null)
		{
			LocalBroadcastManager r = LocalBroadcastManager.getInstance(this);
			Intent broadcast = new Intent(BaseFetcher.BROADCAST_SERVER_RESPONSE_ACTION);
			broadcast.putExtra(BaseFetcher.BROADCAST_MSG, getString(R.string.deleting_picture));
			broadcast.putExtra(BaseFetcher.BROADCAST_SHOW_PROGRESS, false);
			r.sendBroadcast(broadcast);
			new GetDeletedFilesTask().execute(null,null,null);
		}
		
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	private class GetDeletedFilesTask extends AsyncTask<Void, Void, Void>
	{
		@Override
		protected Void doInBackground(Void... params)
		{
			List<MyLittleImage> list = getDeletedImages();
			for(MyLittleImage img : list)
			{
				String where = StopImagesMetaData.TableMetaData.URL + "=?";
				String[] selectionArgs = {img.url};
				try
				{
					service.files().delete(img.path);
					mContentResolver.delete(StopImagesMetaData.TableMetaData.CONTENT_URI, where, selectionArgs);
				} catch (IOException e)
				{
					e.printStackTrace();
				}
				
			}
			
			
			return null;
		}
		
		private List<MyLittleImage> getDeletedImages()
		{
			List<MyLittleImage> deleted = new ArrayList<MyLittleImage>();
			Cursor c = null;
			try
			{
				String[] projection = {StopImagesMetaData.TableMetaData.FILE_TITLE, StopImagesMetaData.TableMetaData.URL, StopImagesMetaData.TableMetaData.FILE_LOCALE_PATH};
				String selection = StopImagesMetaData.TableMetaData.DELETED + "=?";
				String[] selectionArgs = { "1"};
				ContentResolver cr = getContentResolver();
				c = cr.query(StopImagesMetaData.TableMetaData.CONTENT_URI, projection, selection, selectionArgs, null);
				if (c.moveToFirst())
				{
					int iLocalePath = c.getColumnIndex(StopImagesMetaData.TableMetaData.FILE_LOCALE_PATH);
					int iUrl = c.getColumnIndex(StopImagesMetaData.TableMetaData.URL);
					int iTitle = c.getColumnIndex(StopImagesMetaData.TableMetaData.FILE_TITLE);
					do
					{						
						MyLittleImage img = new MyLittleImage();
						img.path = c.getString(iLocalePath);
						img.url = c.getString(iUrl);
						deleted.add(img);
					} while (c.moveToNext());
				}

			} finally
			{
				if (c != null)
				{
					c.close();
				}
			}
			return deleted;
		}
		
		public void onPostExecute(Void result)
		{
			stopSelf();
		}

	}
	
	private Drive getDriveService(GoogleAccountCredential credential)
	{
		return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).build();
	}
	

	@Override
	public IBinder onBind(Intent intent)
	{

		return null;
	}

}
