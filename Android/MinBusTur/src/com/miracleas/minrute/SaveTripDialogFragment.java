package com.miracleas.minrute;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockDialogFragment;

/**
 * Created by kfn on 04-06-13.
 */
public class SaveTripDialogFragment extends SherlockDialogFragment implements View.OnClickListener
{
    private Callbacks mCallbacks = sDummyCallbacks;
    public static final String tag = SaveTripDialogFragment.class.getName();
    private EditText mEditTextListTitleValue = null;
    private Button mBtnOk = null;
    private Button mBtnCancel = null;

    public static SaveTripDialogFragment newInstance()
    {
        SaveTripDialogFragment frag = new SaveTripDialogFragment();
        Bundle args = new Bundle();
        frag.setArguments(args);
        return frag;
    }

    public SaveTripDialogFragment(){}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setCancelable(true);
        int style = STYLE_NORMAL;
        int theme = 0;
        setStyle(style, theme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Dialog dialog = getDialog();
        dialog.setTitle(R.string.confirm_save_trip);
        dialog.setCanceledOnTouchOutside(false);
        View view = inflater.inflate(R.layout.fragment_dialog_save_trip, null);
        mEditTextListTitleValue = (EditText)view.findViewById(R.id.editTextTitle);
        mBtnOk = (Button)view.findViewById(R.id.btnOk);
        mBtnCancel = (Button)view.findViewById(R.id.btnCancel);
        mBtnOk.setOnClickListener(this);
        mBtnCancel.setOnClickListener(this);
        return view;
    }

    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
    }




    @Override
    public void onClick(View v)
    {
        if(v.getId()==R.id.btnOk)
        {
            btnOkClicked(v);
        }
        else if(v.getId()==R.id.btnCancel)
        {
            btnCancelClicked(v);
        }
    }

    private void btnOkClicked(View v)
    {
        String title = mEditTextListTitleValue.getText().toString().trim();
        if(!TextUtils.isEmpty(title))
        {
            mCallbacks.onOk(title);
            dismiss();
        }
        else
        {
            Toast.makeText(getActivity(), getString(R.string.enter_title_pls), Toast.LENGTH_SHORT).show();
        }
    }
    private void btnCancelClicked(View v)
    {
        dismiss();
    }


    public interface Callbacks
    {
        public void onOk(String title);
        public void onCancel();
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks()
    {

        @Override
        public void onOk(String title)
        {

        }

        @Override
        public void onCancel()
        {

        }
    };
    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks))
        {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }
        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }
}