package com.miracleas.minrute;

import java.util.Calendar;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.widget.TimePicker;

public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener
{

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		// Use the current time as the default values for the picker
		final Calendar c = Calendar.getInstance();
		int hour = c.get(Calendar.HOUR_OF_DAY);
		int minute = c.get(Calendar.MINUTE);

		// Create a new instance of TimePickerDialog and return it
		return new TimePickerDialog(getActivity(), this, hour, minute, DateFormat.is24HourFormat(getActivity()));
	}

	public void onTimeSet(TimePicker view, int hourOfDay, int minute)
	{
		((TimePickerDialog.OnTimeSetListener)getActivity()).onTimeSet(view, hourOfDay, minute);
	}
	
	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof TimePickerDialog.OnTimeSetListener))
		{
			throw new IllegalStateException("Activity must implement TimePickerDialog.OnTimeSetListener");
		}			
		
	}
}
