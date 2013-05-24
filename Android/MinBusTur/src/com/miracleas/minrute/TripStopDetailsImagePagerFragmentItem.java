/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.miracleas.minrute;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockFragment;
import com.miracleas.imagedownloader.IImageDownloader;
import com.miracleas.imagedownloader.ImageDownloaderActivity;
import com.miracleas.imagedownloader.Utils;
import com.miracleas.minrute.model.MyLittleImage;


/**
 * This fragment will populate the children of the ViewPager from {@link TripStopDetailsImagePagerActivity}.
 */
public class TripStopDetailsImagePagerFragmentItem extends LocaleImageHandlerFragment {
    private static final String IMAGE_DATA_EXTRA = "extra_image_data";
    private MyLittleImage mImg;
    private ImageView mImageView;
    private IImageDownloader mImageDownloaderActivity;
    private int mHeight = 0;
    private int mWidth = 0;

    /**
     * Factory method to generate a new instance of the fragment given an image number.
     *
     * @param imageUrl The image url to load
     * @return A new instance of ImageDetailFragment with imageNum extras
     */
    public static TripStopDetailsImagePagerFragmentItem newInstance(MyLittleImage img) {
        final TripStopDetailsImagePagerFragmentItem f = new TripStopDetailsImagePagerFragmentItem();
        final Bundle args = new Bundle();
        args.putParcelable(IMAGE_DATA_EXTRA, img);
        f.setArguments(args);
        return f;
    }

    /**
     * Empty constructor as per the Fragment documentation
     */
    public TripStopDetailsImagePagerFragmentItem() {}
    
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		mImageDownloaderActivity = (IImageDownloader)activity;
	}

    /**
     * Populate image using a url from extras, use the convenience factory method
     * {@link TripStopDetailsImagePagerFragmentItem#newInstance(String)} to create this fragment.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImg = getArguments().getParcelable(IMAGE_DATA_EXTRA);
        mHeight = getResources().getDimensionPixelOffset(R.dimen.image_height_full);
        mWidth = getResources().getDimensionPixelOffset(R.dimen.image_width_full);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate and locate the main ImageView
        final View v = inflater.inflate(R.layout.fragment_trip_stop_details_image, container, false);
        mImageView = (ImageView) v.findViewById(R.id.imageView);
        
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if(TextUtils.isEmpty(mImg.url) && !TextUtils.isEmpty(mImg.path))
        {
        	loadLocaleImage(mImg.id, mImg.path, mImageView);
        }
        else if (mImageDownloaderActivity!=null && !TextUtils.isEmpty(mImg.url)) 
        {
        	mImageDownloaderActivity.download(mImg.url, mImageView);
        }
        
        // Pass clicks on the ImageView to the parent activity to handle
        if (OnClickListener.class.isInstance(getActivity()) && Utils.hasHoneycomb()) {
            mImageView.setOnClickListener((OnClickListener) getActivity());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mImageView != null) {
        	// Cancel any pending image work
        	mImageDownloaderActivity.cancelMyWork(mImageView);
            mImageView.setImageDrawable(null);
        }
    }

	@Override
	protected int getImageHeight()
	{
		return mHeight;
	}

	@Override
	protected int getImageWidth()
	{
		return mWidth;
	}
}
