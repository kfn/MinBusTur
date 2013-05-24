package com.miracleas.minrute;

import java.util.Calendar;

import com.miracleas.minrute.TripLegDetailsFragment.Callbacks;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener
{

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		// Use the current date as the default date in the picker
		final Calendar c = Calendar.getInstance();
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH);
		int day = c.get(Calendar.DAY_OF_MONTH);
		
		// Create a new instance of DatePickerDialog and return it
		return new DatePickerDialog(getActivity(), this, year, month, day);
	}

	public void onDateSet(DatePicker view, int year, int month, int day)
	{
		((DatePickerDialog.OnDateSetListener)getActivity()).onDateSet(view, year, month, day);
	}
	
	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof DatePickerDialog.OnDateSetListener))
		{
			throw new IllegalStateException("Activity must implement DatePickerDialog.OnDateSetListener");
		}			
		
	}
}
