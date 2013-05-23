package com.miracleas.camera;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class PhotoIntentActivity extends SherlockFragmentActivity
{

	protected static final int ACTION_TAKE_PHOTO_B = 1;
	private static final int ACTION_TAKE_PHOTO_S = 2;
	private static final int ACTION_TAKE_VIDEO = 3;

	private static final String BITMAP_STORAGE_KEY = "viewbitmap";
	private static final String IMAGEVIEW_VISIBILITY_STORAGE_KEY = "imageviewvisibility";
	
	private Bitmap mImageBitmap;

	private static final String VIDEO_STORAGE_KEY = "viewvideo";
	private static final String VIDEOVIEW_VISIBILITY_STORAGE_KEY = "videoviewvisibility";
	private Uri mVideoUri;

	protected String mCurrentPhotoPath;

	private static final String JPEG_FILE_PREFIX = "IMG_";
	private static final String JPEG_FILE_SUFFIX = ".jpg";

	private AlbumStorageDirFactory mAlbumStorageDirFactory = null;
	protected Uri mImageUri = null;	

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.main);
		mImageBitmap = null;
		mVideoUri = null;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO)
		{
			mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
		} else
		{
			mAlbumStorageDirFactory = new BaseAlbumDirFactory();
		}
	}

	/* Photo album for this application */
	private String getAlbumName()
	{
		return "minrutevejledning";
	}

	private File getAlbumDir()
	{
		File storageDir = null;

		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
		{

			storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName());

			if (storageDir != null)
			{
				if (!storageDir.mkdirs())
				{
					if (!storageDir.exists())
					{
						Log.d("CameraSample", "failed to create directory");
						return null;
					}
				}
			}

		} else
		{
			Log.v(PhotoIntentActivity.class.getName(), "External storage is not mounted READ/WRITE.");
		}

		return storageDir;
	}

	private File createImageFile() throws IOException
	{
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
		File albumF = getAlbumDir();
		File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
		return imageF;
	}

	private File setUpPhotoFile() throws IOException
	{

		File f = createImageFile();
		mCurrentPhotoPath = f.getAbsolutePath();

		return f;
	}

	public static Bitmap scaleImage(String pathToBitmap)
	{

		/* There isn't enough memory to open up more than a couple camera photos */
		/* So pre-scale the target bitmap into which the file is decoded */

		/* Get the size of the ImageView */
		int targetW = 640;
		int targetH = 480;

		/* Get the size of the image */
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(pathToBitmap, bmOptions);
		int photoW = bmOptions.outWidth;
		int photoH = bmOptions.outHeight;

		/* Figure out which way needs to be reduced less */
		int scaleFactor = 1;
		if ((targetW > 0) || (targetH > 0))
		{
			scaleFactor = Math.min(photoW / targetW, photoH / targetH);
		}

		/* Set bitmap options to scale the image decode target */
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;

		/* Decode the JPEG file into a Bitmap */
		return BitmapFactory.decodeFile(pathToBitmap, bmOptions);
		
	}

	private void galleryAddPic()
	{
		Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
		File f = new File(mCurrentPhotoPath);
		Uri contentUri = Uri.fromFile(f);
		mImageUri = contentUri;
		mediaScanIntent.setData(contentUri);
		this.sendBroadcast(mediaScanIntent);
	}

	protected void dispatchTakePictureIntent(int actionCode)
	{

		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		switch (actionCode)
		{
		case ACTION_TAKE_PHOTO_B:
			File f = null;

			try
			{
				f = setUpPhotoFile();
				mCurrentPhotoPath = f.getAbsolutePath();
				mImageUri = Uri.fromFile(f);
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
			} catch (IOException e)
			{
				e.printStackTrace();
				f = null;
				mCurrentPhotoPath = null;
			}
			break;

		default:
			break;
		} // switch

		startActivityForResult(takePictureIntent, actionCode);
	}

	private void dispatchTakeVideoIntent()
	{
		Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		startActivityForResult(takeVideoIntent, ACTION_TAKE_VIDEO);
	}

	private void handleSmallCameraPhoto(Intent intent)
	{
		Bundle extras = intent.getExtras();
		mImageBitmap = (Bitmap) extras.get("data");		
		mVideoUri = null;
	}

	private void handleBigCameraPhoto()
	{

		if (mCurrentPhotoPath != null)
		{
			//mImageBitmap = setPic(mCurrentPhotoPath);
			galleryAddPic();
			mCurrentPhotoPath = null;
		}

	}

	private void handleCameraVideo(Intent intent)
	{
		mVideoUri = intent.getData();
		mImageBitmap = null;
	}

	Button.OnClickListener mTakePicOnClickListener = new Button.OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
		}
	};

	Button.OnClickListener mTakePicSOnClickListener = new Button.OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			dispatchTakePictureIntent(ACTION_TAKE_PHOTO_S);
		}
	};

	Button.OnClickListener mTakeVidOnClickListener = new Button.OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			dispatchTakeVideoIntent();
		}
	};


	

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		
		switch (requestCode)
		{
		case ACTION_TAKE_PHOTO_B:
		{
			if (resultCode == RESULT_OK)
			{
				handleBigCameraPhoto();
			}
			break;
		} // ACTION_TAKE_PHOTO_B

		case ACTION_TAKE_PHOTO_S:
		{
			if (resultCode == RESULT_OK)
			{
				handleSmallCameraPhoto(data);
			}
			break;
		} // ACTION_TAKE_PHOTO_S

		case ACTION_TAKE_VIDEO:
		{
			if (resultCode == RESULT_OK)
			{
				handleCameraVideo(data);
			}
			break;
		} // ACTION_TAKE_VIDEO
		} // switch
		
		super.onActivityResult(requestCode, resultCode, data);
	}

	// Some lifecycle callbacks so that the image can survive orientation change
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putParcelable(BITMAP_STORAGE_KEY, mImageBitmap);
		outState.putParcelable(VIDEO_STORAGE_KEY, mVideoUri);
		outState.putBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY, (mImageBitmap != null));
		outState.putBoolean(VIDEOVIEW_VISIBILITY_STORAGE_KEY, (mVideoUri != null));
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
		mImageBitmap = savedInstanceState.getParcelable(BITMAP_STORAGE_KEY);
		mVideoUri = savedInstanceState.getParcelable(VIDEO_STORAGE_KEY);
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

	public void setBtnListenerOrDisable(Button btn, Button.OnClickListener onClickListener, String intentName)
	{
		if (isIntentAvailable(this, intentName))
		{
			btn.setOnClickListener(onClickListener);
		} else
		{
			//btn.setText(getText(R.string.cannot).toString() + " " + btn.getText());
			btn.setClickable(false);
		}
	}
	
	public void setBtnListenerOrDisable(Button btn)
	{
		if (isIntentAvailable(this, MediaStore.ACTION_IMAGE_CAPTURE))
		{			
			btn.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
				}
			});		
		} else
		{
			btn.setClickable(false);
		}
	}

}