package com.miracleas.camera;

import java.io.File;

import android.annotation.TargetApi;
import android.os.Environment;

public final class FroyoAlbumDirFactory extends AlbumStorageDirFactory {

	@TargetApi(8)
	@Override
	public File getAlbumStorageDir(String albumName) {
		// TODO Auto-generated method stub
		return new File(
		  Environment.getExternalStoragePublicDirectory(
		    Environment.DIRECTORY_PICTURES
		  ), 
		  albumName
		);
	}
}
