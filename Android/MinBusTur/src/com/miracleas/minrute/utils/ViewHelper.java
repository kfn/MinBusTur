package com.miracleas.minrute.utils;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class ViewHelper
{
	public static void hideKeyboard(Context c, View v)
	{
		InputMethodManager imm = (InputMethodManager)c.getSystemService(
			      Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
	}
}
