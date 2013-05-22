package com.miracleas.minbustur;

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.miracleas.minbustur.provider.JourneyDetailMetaData;
import com.miracleas.minbustur.provider.JourneyDetailNoteMetaData;
/**
 * @author kfn
 * 
 */
public class TripLegDetailNotesDialog extends SherlockDialogFragment implements LoaderCallbacks<Cursor>
{
	public static final String tag = TripLegDetailNotesDialog.class.getName();
	private Context cxt;
	private LinearLayout mContainerNotes = null;	
	private static final String[] PROJECTION_NOTES = { 
		JourneyDetailNoteMetaData.TableMetaData._ID, 
		JourneyDetailNoteMetaData.TableMetaData.NOTE
	};

	public static TripLegDetailNotesDialog createInstance(long journeyDetailId)
	{
		TripLegDetailNotesDialog instance = new TripLegDetailNotesDialog();				
		Bundle args = new Bundle();
		args.putLong(JourneyDetailMetaData.TableMetaData._ID, journeyDetailId);
		instance.setArguments(args);
		return instance;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.setCancelable(true);
		int style = DialogFragment.STYLE_NORMAL;
		int theme = 0;
		setStyle(style, theme);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		Dialog dialog = getDialog();
		if (dialog != null)
		{
			dialog.setTitle(getString(R.string.notes));
			dialog.setCanceledOnTouchOutside(true);
			/*WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
			lp.copyFrom(dialog.getWindow().getAttributes());
			lp.width = getResources().getDimensionPixelSize(R.dimen.about_width);
			lp.height = getResources().getDimensionPixelSize(R.dimen.about_height);
			dialog.getWindow().setAttributes(lp);*/
			getLoaderManager().initLoader(LoaderConstants.LOADER_TRIP_LEG_NOTE_DETAILS, getArguments(), this);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.trip_details_notes_dialog, null);
		mContainerNotes = (LinearLayout)v.findViewById(R.id.containerOfNotes);

		return v;
	}
	
	public static void show(SherlockFragmentActivity a, long id)
	{
		FragmentManager manager = a.getSupportFragmentManager();
		FragmentTransaction transaction = manager.beginTransaction();
		Fragment prev = manager.findFragmentByTag(TripLegDetailNotesDialog.tag);
		if (prev != null)
		{
			transaction.remove(prev);
		}
		transaction.addToBackStack(null);
		DialogFragment dialog = TripLegDetailNotesDialog.createInstance(id);
		dialog.show(transaction, TripLegDetailNotesDialog.tag);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args)
	{
		String selection = JourneyDetailNoteMetaData.TableMetaData.JOURNEY_DETAIL_ID + "=?";
		String[] selectionArgs = {args.getLong(JourneyDetailMetaData.TableMetaData._ID)+""};
		return new CursorLoader(getActivity(), JourneyDetailNoteMetaData.TableMetaData.CONTENT_URI, PROJECTION_NOTES, selection, selectionArgs, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor newCursor)
	{
		if(loader.getId()==LoaderConstants.LOADER_TRIP_LEG_NOTE_DETAILS && newCursor.moveToFirst())
		{
			mContainerNotes.removeAllViews();
			int iNote = newCursor.getColumnIndex(JourneyDetailNoteMetaData.TableMetaData.NOTE);
			int margin = getResources().getDimensionPixelOffset(R.dimen.note_marginTop);
			do{
				TextView tv = new TextView(getActivity());
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
				tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_info_details, 0, 0, 0);
				tv.setText(newCursor.getString(iNote));
				tv.setGravity(Gravity.CENTER_VERTICAL);
				params.topMargin = margin; 
				params.bottomMargin = margin;
				
				tv.setLayoutParams(params);
				mContainerNotes.addView(tv);
				
			}while(newCursor.moveToNext());
		}
		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0)
	{
		// TODO Auto-generated method stub
		
	}

}
