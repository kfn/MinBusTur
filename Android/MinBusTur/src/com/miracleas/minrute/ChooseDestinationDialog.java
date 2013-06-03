package com.miracleas.minrute;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Created by kfn on 03-06-13.
 */
public class ChooseDestinationDialog extends DialogFragment
{
    public static final int MY_LOCATION = 0;
    public static final int CONTACTS = 1;
    public static final int MAP = 2;
    public static final int MY_PLACES = 3;
    private NoticeDialogListener mListener;
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_choose_location)
        .setItems(R.array.destination_types, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                // The 'which' argument contains the index position
                // of the selected item
                mListener.onDialogLocationTypeClick(dialog, which, getTag());

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
            mListener = (NoticeDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }


    public interface NoticeDialogListener {
        public void onDialogLocationTypeClick(DialogInterface dialog, int which, String tag);

    }

}