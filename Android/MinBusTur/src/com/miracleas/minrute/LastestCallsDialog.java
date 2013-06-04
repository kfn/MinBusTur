package com.miracleas.minrute;

import java.util.ArrayList;
import java.util.List;

import com.miracleas.minrute.model.TripLegStop;
import com.miracleas.minrute.provider.AddressGPSMetaData;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.v4.app.DialogFragment;

/**
 * Created by kfn on 03-06-13.
 */
public class LastestCallsDialog extends DialogFragment
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
    
    private class LoadRecentContactsTask extends AsyncTask<Void, Void, Void>
    {

		@Override
		protected Void doInBackground(Void... params)
		{
			String inClause = createInClauseQuery(getNamesOfLastCalls());
			
			return null;
		}
		
		private List<String> getNamesOfLastCalls()
		{
			List<String> names = new ArrayList<String>();
			
			String[] projection = {CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME};
			ContentResolver cr = getActivity().getContentResolver();
			String strOrder = CallLog.Calls.DATE + " DESC LIMIT 5";
			
			Cursor c = null;
			try
			{
				c = cr.query(CallLog.Calls.CONTENT_URI, projection, null, null, strOrder);
				if(c.moveToFirst())
				{
					int iNumber = c.getColumnIndex(CallLog.Calls.NUMBER);
					int iName = c.getColumnIndex(CallLog.Calls.CACHED_NAME);
					do
					{
						String number = c.getString(iNumber);
						String name = c.getString(iName);
						names.add(name);
					}while(c.moveToNext());
				}
			}
			finally
			{
				c.close();
			}
			
			return names;
		}
    	
    }
    
	private String createInClauseQuery(List<String> names)
	{
		StringBuilder b = new StringBuilder();
		if (!names.isEmpty())
		{
			b.append(ContactsContract.CommonDataKinds.StructuredPostal.DISPLAY_NAME).append(" IN (");
		}
		int size = names.size();
		for (int i = 0; i < size; i++)
		{
			b.append("'").append(names.get(i)).append("'");
			if (i + 1 < size)
			{
				b.append(",");
			}
		}

		if (!names.isEmpty())
		{
			b.append(") ");
		}

		return b.toString();
	}

}