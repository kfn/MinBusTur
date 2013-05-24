package com.miracleas.minrute.service;

import java.io.IOException;
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
import com.miracleas.minrute.net.BaseFetcher;
import com.miracleas.minrute.net.TripFetcher;
import com.miracleas.minrute.provider.JourneyDetailStopDeparturesMetaData;
import com.miracleas.minrute.provider.JourneyDetailStopImagesMetaData;

import android.accounts.AccountManager;
import android.app.IntentService;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

public class UploadImagesService extends Service
{
	private static Drive service;
	protected GoogleAccountCredential credential;
	private String mAccountName;
	private String mStopName = null;

	@Override
	public void onCreate()
	{
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Log.i("LocalService", "Received start id " + startId + ": " + intent);
		mAccountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
		credential = GoogleAccountCredential.usingOAuth2(this, DriveScopes.DRIVE);
		credential.setSelectedAccountName(mAccountName);
		service = getDriveService(credential);
		if(!TextUtils.isEmpty(mAccountName))
		{
			LocalBroadcastManager r = LocalBroadcastManager.getInstance(this);
			Intent broadcast = new Intent(BaseFetcher.BROADCAST_SERVER_RESPONSE_ACTION);
			broadcast.putExtra(BaseFetcher.BROADCAST_MSG, getString(R.string.uploading_pictures));
			broadcast.putExtra(BaseFetcher.BROADCAST_SHOW_PROGRESS, true);
			r.sendBroadcast(broadcast);
			new UploadImages().execute(null,null,null);
		}
		
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	private class UploadImages extends AsyncTask<Void, Void, Void>
	{

		@Override
		protected Void doInBackground(Void... params)
		{
			Cursor c = null;
			try
			{
				String[] projection = { JourneyDetailStopImagesMetaData.TableMetaData._ID, JourneyDetailStopImagesMetaData.TableMetaData.FILE_ID, JourneyDetailStopImagesMetaData.TableMetaData.FILE_LOCALE_PATH, JourneyDetailStopImagesMetaData.TableMetaData.FILE_MIME_TYPE, JourneyDetailStopImagesMetaData.TableMetaData.FILE_TITLE };
				String selection = JourneyDetailStopImagesMetaData.TableMetaData.UPLOADED + "=? AND "+ JourneyDetailStopImagesMetaData.TableMetaData.UPLOADED+"=?";
				String[] selectionArgs = { "0", "0" };
				ContentResolver cr = getContentResolver();
				c = cr.query(JourneyDetailStopImagesMetaData.TableMetaData.CONTENT_URI, projection, selection, selectionArgs, null);
				if (c.moveToFirst())
				{
					int iLocalePath = c.getColumnIndex(JourneyDetailStopImagesMetaData.TableMetaData.FILE_LOCALE_PATH);
					int iMimeType = c.getColumnIndex(JourneyDetailStopImagesMetaData.TableMetaData.FILE_MIME_TYPE);
					int iTitle = c.getColumnIndex(JourneyDetailStopImagesMetaData.TableMetaData.FILE_TITLE);
					int iId = c.getColumnIndex(JourneyDetailStopImagesMetaData.TableMetaData._ID);
					do
					{						
						int id = c.getInt(iId);
						String path = c.getString(iLocalePath);
						String title = c.getString(iTitle);
						String mime = c.getString(iMimeType);						
						if(!TextUtils.isEmpty(path))
						{
							saveFileToDrive(id, path, mime, title);
						}						
					} while (c.moveToNext());
				}

			} finally
			{
				if (c != null)
				{
					c.close();
				}
			}
			return null;
			
		}
		
		public void onPostExecute(Void result)
		{
			LocalBroadcastManager r = LocalBroadcastManager.getInstance(UploadImagesService.this);
			Intent broadcast = new Intent(BaseFetcher.BROADCAST_SERVER_RESPONSE_ACTION);
			broadcast.putExtra(BaseFetcher.BROADCAST_SHOW_PROGRESS, true);
			r.sendBroadcast(broadcast);
			stopSelf();
		}

	}

	private void saveFileToDrive(final long id, final String path, final String mimeType, final String title)
	{
		ContentResolver cr = getContentResolver();
		Uri uri = Uri.withAppendedPath(JourneyDetailStopImagesMetaData.TableMetaData.CONTENT_URI, id+"");
		ContentValues values = new ContentValues();
		values.put(JourneyDetailStopImagesMetaData.TableMetaData.IS_UPLOADING, "1");		
		cr.update(uri, values, null, null);
		int uploaded = 0;
		try
		{
			// File's binary content
			java.io.File fileContent = new java.io.File(path);
			FileContent mediaContent = new FileContent(mimeType, fileContent);

			// File's metadata.
			File body = new File();
			body.setTitle(title);
			body.setMimeType(mimeType);

			File file = service.files().insert(body, mediaContent).execute();
			if (file != null)
			{				
				uploaded = 1;						
			}
		}

		catch (UserRecoverableAuthIOException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		values = new ContentValues();
		values.put(JourneyDetailStopImagesMetaData.TableMetaData.IS_UPLOADING, "0");
		values.put(JourneyDetailStopImagesMetaData.TableMetaData.UPLOADED, uploaded+"");
		cr.update(uri, values, null, null);
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
