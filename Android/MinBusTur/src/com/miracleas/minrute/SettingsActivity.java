package com.miracleas.minrute;

import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends SherlockPreferenceActivity
{
	/**
	 * Determines whether to always show the simplified settings UI, where
	 * settings are presented in a single list. When false, settings are shown
	 * as a master/detail two-pane view on tablets. When true, a single pane is
	 * shown on tablets.
	 */
	private static final boolean ALWAYS_SIMPLE_PREFS = false;
	public static final int REQUEST_CODE = 123;
	public static final int RESULT_CODE = 124;

	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);	
		setupSimplePreferencesScreen();
	}
	@Override
	public void onCreate(Bundle bundle)
	{
		super.onCreate(bundle);
		ActionBar bar = getSupportActionBar();
        
        bar.setDisplayShowHomeEnabled(true);
        bar.setHomeButtonEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
        setResult(RESULT_CODE, getIntent());
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent upIntent = new Intent(this, ChooseOriginDestActivity.class);
            NavUtils.navigateUpTo(this, upIntent);
            return true;		
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	


	/**
	 * Shows the simplified settings UI if the device configuration if the
	 * device configuration dictates that a simplified, single-pane UI should be
	 * shown.
	 */
	private void setupSimplePreferencesScreen()
	{
		if (!isSimplePreferences(this))
		{
			return;
		}

		// In the simplified UI, fragments are not used at all and we instead
		// use the older PreferenceActivity APIs.

		// Add 'general' preferences.
		addPreferencesFromResource(R.xml.pref_general);

		// Add 'notifications' preferences, and a corresponding header.
		PreferenceCategory fakeHeader = new PreferenceCategory(this);
		fakeHeader.setTitle(R.string.pref_header_voices);
		getPreferenceScreen().addPreference(fakeHeader);
		addPreferencesFromResource(R.xml.pref_voices);

		// Add 'data and sync' preferences, and a corresponding header.
		fakeHeader = new PreferenceCategory(this);
		fakeHeader.setTitle(R.string.pref_header_images);
		getPreferenceScreen().addPreference(fakeHeader);
		addPreferencesFromResource(R.xml.pref_images);

		// Bind the summaries of EditText/List/Dialog/Ringtone preferences to
		// their values. When their values change, their summaries are updated
		// to reflect the new value, per the Android Design guidelines.
	
		bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_voice_language_key)));
		//bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_images_turn_on_key)));
	}

	/** {@inheritDoc} */
	@Override
	public boolean onIsMultiPane()
	{
		return isXLargeTablet(this) && !isSimplePreferences(this);
	}

	/**
	 * Helper method to determine if the device has an extra-large screen. For
	 * example, 10" tablets are extra-large.
	 */
	private static boolean isXLargeTablet(Context context)
	{
		return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	/**
	 * Determines whether the simplified settings UI should be shown. This is
	 * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
	 * doesn't have newer APIs like {@link PreferenceFragment}, or the device
	 * doesn't have an extra-large screen. In these cases, a single-pane
	 * "simplified" settings UI should be shown.
	 */
	private static boolean isSimplePreferences(Context context)
	{
		return ALWAYS_SIMPLE_PREFS || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB || !isXLargeTablet(context);
	}

	/** {@inheritDoc} */
	@Override
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void onBuildHeaders(List<Header> target)
	{
		if (!isSimplePreferences(this))
		{
			loadHeadersFromResource(R.xml.pref_headers, target);
		}
	}

	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener()
	{
		@Override
		public boolean onPreferenceChange(Preference preference, Object value)
		{
			String stringValue = value.toString();

			if (preference instanceof ListPreference)
			{
				// For list preferences, look up the correct display value in
				// the preference's 'entries' list.
				ListPreference listPreference = (ListPreference) preference;
				
				int index = listPreference.findIndexOfValue(stringValue);				
				// Set the summary to reflect the new value.
				preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
			}else
			{
				// For all other preferences, set the summary to the value's
				// simple string representation.
				preference.setSummary(stringValue);
				
			}
			return true;
		}
	};

	/**
	 * Binds a preference's summary to its value. More specifically, when the
	 * preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also
	 * immediately updated upon calling this method. The exact display format is
	 * dependent on the type of preference.
	 * 
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	private static void bindPreferenceSummaryToValue(Preference preference)
	{
		// Set the listener to watch for value changes.
		preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
		SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
		// Trigger the listener immediately with the preference's
		// current value.
		

		String key = shared.getString(preference.getKey(), "");
		sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, key);
		
	}

	/**
	 * This fragment shows general preferences only. It is used when the
	 * activity is showing a two-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class GeneralPreferenceFragment extends PreferenceFragment
	{
		@Override
		public void onCreate(Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_general);

			// Bind the summaries of EditText/List/Dialog/Ringtone preferences
			// to their values. When their values change, their summaries are
			// updated to reflect the new value, per the Android Design
			// guidelines.
			bindPreferenceSummaryToValue(findPreference("example_text"));
			bindPreferenceSummaryToValue(findPreference("example_list"));
		}
	}

	/**
	 * This fragment shows notification preferences only. It is used when the
	 * activity is showing a two-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class VoicePreferenceFragment extends PreferenceFragment
	{
		@Override
		public void onCreate(Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_voices);

			// Bind the summaries of EditText/List/Dialog/Ringtone preferences
			// to their values. When their values change, their summaries are
			// updated to reflect the new value, per the Android Design
			// guidelines.
			bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_voice_on_key)));
		}
	}

	/**
	 * This fragment shows data and sync preferences only. It is used when the
	 * activity is showing a two-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class ImagesPreferenceFragment extends PreferenceFragment
	{
		@Override
		public void onCreate(Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_voices);

			// Bind the summaries of EditText/List/Dialog/Ringtone preferences
			// to their values. When their values change, their summaries are
			// updated to reflect the new value, per the Android Design
			// guidelines.
			bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_images_turn_on_key)));
		}
	}
	
	public static boolean isVoiceOn(Context c)
	{
		return PreferenceManager.getDefaultSharedPreferences(c).getBoolean(c.getString(R.string.pref_voice_on_key), true);
	}
	
	public static void setIsVoiceOn(Context c, boolean value)
	{
		Editor e =  PreferenceManager.getDefaultSharedPreferences(c).edit();
		e.putBoolean(c.getString(R.string.pref_voice_on_key), value);
		e.commit();
	}
	
	public static boolean isVoiceDepartureOn(Context c)
	{
		return PreferenceManager.getDefaultSharedPreferences(c).getBoolean(c.getString(R.string.pref_voice_departure_key), true);
	}
	
	public static boolean isVoiceStopBeforeArrivalOn(Context c)
	{
		return PreferenceManager.getDefaultSharedPreferences(c).getBoolean(c.getString(R.string.pref_voice_arrival_before_last_stop_key), true);
	}
	
	public static boolean isImagesOn(Context c)
	{
		return PreferenceManager.getDefaultSharedPreferences(c).getBoolean(c.getString(R.string.pref_images_turn_on_key), true);
	}
	
	public static boolean isImagesBigOn(Context c)
	{
		return PreferenceManager.getDefaultSharedPreferences(c).getBoolean(c.getString(R.string.pref_images_big_key), false);
	}
	
	public static String getSelectedLanguage(Context c)
	{
		return PreferenceManager.getDefaultSharedPreferences(c).getString(c.getString(R.string.pref_voice_language_key), c.getString(R.string.pref_voice_language_danish_key));
	}
	
	public static void setLanguage(Context c, String value)
	{
		Editor e =  PreferenceManager.getDefaultSharedPreferences(c).edit();
		e.putString(c.getString(R.string.pref_voice_language_key), value);
		e.commit();
	}
	
	public static SharedPreferences getSharedPreferences(Context c)
	{
		return  PreferenceManager.getDefaultSharedPreferences(c);
	}
}
