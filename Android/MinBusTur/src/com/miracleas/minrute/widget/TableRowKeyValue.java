package com.miracleas.minrute.widget;

import com.miracleas.minrute.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TableRow;
import android.widget.TextView;

public class TableRowKeyValue extends TableRow
{
	private TextView mTextViewKey = null;
	private TextView mTextViewValue = null;
	
	public TableRowKeyValue(Context context)
	{
		super(context);
		LayoutInflater inflater = (LayoutInflater) context
		        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		    inflater.inflate(R.layout.item_trip_stop_details, this, true);
		    
		    mTextViewKey = (TextView)findViewById(android.R.id.text1);
		    mTextViewValue = (TextView)findViewById(android.R.id.text2);
	}
	
	public void setKey(String key)
	{
		mTextViewKey.setText(key);
	}
	public void setValue(String value)
	{
		mTextViewValue.setText(value);
	}
}
