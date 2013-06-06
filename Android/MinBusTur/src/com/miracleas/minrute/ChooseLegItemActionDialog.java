package com.miracleas.minrute;

import com.miracleas.minrute.provider.TripLegMetaData;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Created by kfn on 03-06-13.
 */
public class ChooseLegItemActionDialog extends DialogFragment
{
    public static final int I_AM_HERE = 0;
    public static final int AT_STOP_BEFORE_LEG_DESTINATION = 1;
    public static final int STOP_DETAILS = 2;
	public static final String POSITION = "ARG_POSITION";
    
    private LegItemActionDialogListener mListener;
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if(getArguments()==null || !getArguments().containsKey(TripLegMetaData.TableMetaData._ID) || !getArguments().containsKey(POSITION))
        {
        	throw new ClassCastException("Arguments most contain TripLegMetaData.TableMetaData._ID and POSITION key");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	
    	final long legId = getArguments().getLong(TripLegMetaData.TableMetaData._ID);
    	final int listPosition = getArguments().getInt(POSITION);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.leg_item_action_title)
        .setItems(R.array.leg_item_actions, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                // The 'which' argument contains the index position
                // of the selected item
                mListener.onLegItemActionClick(dialog, which, legId, listPosition);

            }
        });
        return builder.create();
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (LegItemActionDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement LegItemActionDialogListener");
        }
    }


    public interface LegItemActionDialogListener {
        public void onLegItemActionClick(DialogInterface dialog, int which, long itemId, int listPosition);

    }

}