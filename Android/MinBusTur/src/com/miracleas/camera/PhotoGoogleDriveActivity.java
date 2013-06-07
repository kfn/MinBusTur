package com.miracleas.camera;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.miracleas.imagedownloader.IImageDownloader;
import com.miracleas.imagedownloader.ImageDownloaderActivity;
import com.miracleas.imagedownloader.ImageFetcher;
import com.miracleas.minrute.MinRuteBaseActivity;
import com.miracleas.minrute.R;
import com.miracleas.minrute.model.TripLeg;
import com.miracleas.minrute.model.TripLegStop;
import com.miracleas.minrute.provider.StopImagesMetaData;
import com.miracleas.minrute.service.UploadImagesService;
import com.miracleas.minrute.utils.MyPrefs;

public class PhotoGoogleDriveActivity extends MinRuteBaseActivity implements IImageDownloader
{
	private static final String FILE_URI = "viewbitmap";
	private static final String LAT = "lat";
	private static final String LNG = "lng";
	private String mAccountName = null;

	static final int REQUEST_ACCOUNT_PICKER = 1;
	static final int REQUEST_AUTHORIZATION = 2;
	private static final int MY_ACTIVITYS_AUTH_REQUEST_CODE = 4;
	static final int CAPTURE_IMAGE = 3;
	static final String AUTH_TOKEN = "AUTH_TOKEN";

	private IImageDownloader mImageLoader = null;
	private static final String CACHE_DIR = IImageDownloader.CACHE_DIR;
	private static Uri fileUri;
	private static Drive service;
	protected GoogleAccountCredential credential;
	private String mAuth = null;
	private TripLegStop mStop;
	private TripLeg mLeg;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		String auth = getAuthToken();
		setAuthTokenHeader(auth);
	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode)
		{
		case REQUEST_ACCOUNT_PICKER:
			if (resultCode == RESULT_OK && data != null && data.getExtras() != null)
			{
				mAccountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
				getAndUseAuthTokenInAsyncTask(mAccountName);
			}
			break;
		case REQUEST_AUTHORIZATION:
			if (resultCode == Activity.RESULT_OK)
			{
				// saveFileToDrive();
				// saveFileToDb();
			} else
			{
				startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
			}
			break;
		case CAPTURE_IMAGE:
			if (resultCode == Activity.RESULT_OK)
			{
				// saveFileToDrive();
				saveFileToDb();
			}
			break;
		case MY_ACTIVITYS_AUTH_REQUEST_CODE:
			if (resultCode == RESULT_OK) {
	               getAndUseAuthTokenInAsyncTask(mAccountName);
	           }

		}
	}

	// Example of how to use AsyncTask to call blocking code on a background
	// thread.
	private void getAndUseAuthTokenInAsyncTask(String accountName)
	{
		LoadDriveAuthTask mLoadDriveAuthTask = new LoadDriveAuthTask();
		mLoadDriveAuthTask.execute(accountName);
	}

	private class LoadDriveAuthTask extends AsyncTask<String, Void, Void>
	{
		private Dialog mAlert;
		private boolean mStartService = false;

		@Override
		protected Void doInBackground(String... params)
		{
			String accountName = params[0];
			getAndUseAuthTokenBlocking(accountName);
			return null;
		}

		public void onPostExecute(Void result)
		{
			if (mAlert != null)
			{
				mAlert.show();
			}
			if(mStartService)
			{
				startUploadServiceHelper();
			}
		}

		private void getAndUseAuthTokenBlocking(String accountName)
		{

			credential.setSelectedAccountName(accountName);
			service = getDriveService(credential);

			try
			{
				String authToken = credential.getToken();
				if (!TextUtils.isEmpty(authToken))
				{
					saveAuthToken(authToken, accountName);
				}

				setAuthTokenHeader(authToken);
				mStartService = true;
				
			} catch (GooglePlayServicesAvailabilityException playEx)
			{
				mAlert = GooglePlayServicesUtil.getErrorDialog(playEx.getConnectionStatusCode(), PhotoGoogleDriveActivity.this, MY_ACTIVITYS_AUTH_REQUEST_CODE);

			} catch (UserRecoverableAuthException userAuthEx)
			{
				// Start the user recoverable action using the intent
				// returned
				// by
				// getIntent()
				startActivityForResult(userAuthEx.getIntent(), MY_ACTIVITYS_AUTH_REQUEST_CODE);
				return;
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (GoogleAuthException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				// GoogleAuthUtil.invalidateToken(Context, String)(context,
				// token);
			}

		}
	}

	private void startCameraIntent()
	{

		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
		{
			String mediaStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath();
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
			fileUri = Uri.fromFile(new java.io.File(mediaStorageDir + java.io.File.separator + mStop.lat + "-" + mStop.lng + "-" + timeStamp + ".jpg"));

			Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
			startActivityForResult(cameraIntent, CAPTURE_IMAGE);
		} else
		{
			Toast.makeText(this, getString(R.string.cache_unavailable), Toast.LENGTH_SHORT).show();
		}

	}

	private void saveFileToDb()
	{
		Thread t = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				// File's binary content
				java.io.File fileContent = new java.io.File(fileUri.getPath());
				if (fileUri != null)
				{
					ContentResolver cr = getContentResolver();
					ContentValues values = new ContentValues();
					values.put(StopImagesMetaData.TableMetaData.LAT, mStop.lat);
					values.put(StopImagesMetaData.TableMetaData.LNG, mStop.lng);
					values.put(StopImagesMetaData.TableMetaData.STOP_NAME, mStop.name);
					values.put(StopImagesMetaData.TableMetaData.TRANSPORT_DIRECTION, mLeg.notes);
					values.put(StopImagesMetaData.TableMetaData.FILE_LOCALE_PATH, fileUri.getPath());
					values.put(StopImagesMetaData.TableMetaData.FILE_MIME_TYPE, "image/jpeg");
					values.put(StopImagesMetaData.TableMetaData.FILE_TITLE, fileContent.getName());
					values.put(StopImagesMetaData.TableMetaData.UPLOADED, "0");
					cr.insert(StopImagesMetaData.TableMetaData.CONTENT_URI, values);
				}
			}
		});
		t.start();
	}

	private void saveFileToDrive()
	{
		Thread t = new Thread(new Runnable()
		{

			@Override
			public void run()
			{
				try
				{
					// File's binary content
					java.io.File fileContent = new java.io.File(fileUri.getPath());
					FileContent mediaContent = new FileContent("image/jpeg", fileContent);

					// File's metadata.
					File body = new File();
					body.setTitle(fileContent.getName());
					body.setMimeType("image/jpeg");

					File file = service.files().insert(body, mediaContent).execute();
					if (file != null)
					{
						showToast("Photo uploaded: " + file.getTitle());
						ContentResolver cr = getContentResolver();
						ContentValues values = new ContentValues();
						values.put(StopImagesMetaData.TableMetaData.LAT, mStop.lat);
						values.put(StopImagesMetaData.TableMetaData.LNG, mStop.lng);
						values.put(StopImagesMetaData.TableMetaData.STOP_NAME, mStop.name);
						values.put(StopImagesMetaData.TableMetaData.TRANSPORT_DIRECTION, mLeg.notes);
						values.put(StopImagesMetaData.TableMetaData.URL, file.getDownloadUrl());
						values.put(StopImagesMetaData.TableMetaData.FILE_ID, file.getId());
						cr.insert(StopImagesMetaData.TableMetaData.CONTENT_URI, values);
					}
				} catch (UserRecoverableAuthIOException e)
				{
					startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		});
		t.start();
	}

	private Drive getDriveService(GoogleAccountCredential credential)
	{
		return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).build();
	}

	public void showToast(final String toast)
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				Toast.makeText(PhotoGoogleDriveActivity.this, toast, Toast.LENGTH_SHORT).show();
			}
		});
	}

	public void setBtnListenerOrDisable(Button btn, final TripLegStop stop, final TripLeg leg)
	{
		mStop = stop;
		mLeg = leg;
		if (isIntentAvailable(this, MediaStore.ACTION_IMAGE_CAPTURE))
		{
			btn.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					startCameraIntent();
				}
			});
		} else
		{
			btn.setClickable(false);
		}
	}

	/**
	 * Indicates whether the specified action can be used as an intent. This
	 * method queries the package manager for installed packages that can
	 * respond to an intent with the specified action. If no suitable package is
	 * found, this method returns false.
	 * http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html
	 * 
	 * @param context
	 *            The application's environment.
	 * @param action
	 *            The Intent action to check for availability.
	 * 
	 * @return True if an Intent with the specified action can be sent and
	 *         responded to, false otherwise.
	 */
	public static boolean isIntentAvailable(Context context, String action)
	{
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	// Some lifecycle callbacks so that the image can survive orientation change
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putParcelable(FILE_URI, fileUri);
		outState.putString(AUTH_TOKEN, mAuth);
		outState.putString("account", mAccountName);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
		fileUri = savedInstanceState.getParcelable(FILE_URI);
		mAuth = savedInstanceState.getString(AUTH_TOKEN);
		mAccountName = savedInstanceState.getString("account");
	}

	public IImageDownloader getIImageDownloader()
	{
		if (mImageLoader == null)
		{
			final int height = getResources().getDimensionPixelSize(R.dimen.image_height);// displayMetrics.heightPixels;
			final int width = getResources().getDimensionPixelSize(R.dimen.image_width);
			mImageLoader = ImageFetcher.getInstance(this, getSupportFragmentManager(), getImgCacheDir(), height, width);
		}
		return mImageLoader;
	}

	public String getImgCacheDir()
	{
		return CACHE_DIR;
	}

	@Override
	public void onResume()
	{
		super.onResume();
		setExitTasksEarly(false);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		setExitTasksEarly(true);
		flushCache();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		closeCache();
	}

	@Override
	public void closeCache()
	{

		getIImageDownloader().closeCache();
	}

	@Override
	public void flushCache()
	{

		getIImageDownloader().flushCache();

	}

	@Override
	public void setExitTasksEarly(boolean exitEarly)
	{
		getIImageDownloader().setExitTasksEarly(exitEarly);
	}

	@Override
	public boolean download(String url, ImageView imageView)
	{
		// TODO Auto-generated method stub
		return getIImageDownloader().download(url, imageView);
	}

	@Override
	public void setPauseWork(boolean pause)
	{
		getIImageDownloader().setPauseWork(pause);

	}

	@Override
	public void setLoadingImage(int resId)
	{
		getIImageDownloader().setLoadingImage(resId);

	}

	@Override
	public int getPlaceHolderRessource()
	{
		return getIImageDownloader().getPlaceHolderRessource();
	}

	@Override
	public void setImageFadeIn(boolean fadeIn)
	{
		getIImageDownloader().setImageFadeIn(fadeIn);

	}

	@Override
	public void setImageSize(int size)
	{
		getIImageDownloader().setImageSize(size);

	}

	@Override
	public void cancelMyWork(ImageView imageView)
	{
		getIImageDownloader().cancelMyWork(imageView);

	}

	@Override
	public void setContext(Context c)
	{
		getIImageDownloader().setContext(c);

	}

	@Override
	public void setAuthTokenHeader(String authToken)
	{
		mAuth = authToken;
		if (authToken != null)
		{
			getIImageDownloader().setAuthTokenHeader(authToken);
		}

	}

	public void saveAuthToken(String authToken, String accountName)
	{
		mAuth = authToken;
		MyPrefs.setString(this, MyPrefs.GOOGLE_DRIVE_AUTH, authToken);
		MyPrefs.setString(this, AccountManager.KEY_ACCOUNT_NAME, accountName);
	}

	public String getAuthToken()
	{
		if (mAuth == null)
		{
			mAuth = MyPrefs.getString(this, MyPrefs.GOOGLE_DRIVE_AUTH, null);
		}
		return mAuth;
	}

	public void startUploadService()
	{
		if (service == null || TextUtils.isEmpty(mAccountName))
		{
			credential = GoogleAccountCredential.usingOAuth2(PhotoGoogleDriveActivity.this, DriveScopes.DRIVE);
			startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
		} else
		{
			startUploadServiceHelper();
		}
	}

	private void startUploadServiceHelper()
	{
		if (!TextUtils.isEmpty(mAccountName))
		{
			Toast.makeText(this, getString(R.string.uploading_pictures), Toast.LENGTH_LONG).show();
			Intent service = new Intent(this, UploadImagesService.class);
			service.putExtra(AccountManager.KEY_ACCOUNT_NAME, mAccountName);
			startService(service);
		} else
		{
			Toast.makeText(this, "Account mangler", Toast.LENGTH_LONG).show();
		}

	}

	@Override
	public void onConnectedServiceLocation()
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected void onServerResponse(boolean success)
	{
		// TODO Auto-generated method stub

	}
}